package uk.ac.bham.cs.commdet.graphchi.program;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import uk.ac.bham.cs.commdet.cyto.json.CommunityGraphGenerator;
import uk.ac.bham.cs.commdet.cyto.json.CompoundNode;
import uk.ac.bham.cs.commdet.cyto.json.SubGraph;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

/*
 * Note that links are considered half links, ie. one half in each direction.
 */
public class LouvainProgram implements GraphChiProgram<Integer, Integer>  {

	private int passIndex;
	private boolean improvedOnPass;
	private boolean modularityImproved;
	private int[] nodeToCommunity;
	private int[] communityInternalEdges;
	private int[] communityTotalEdges;
	private int[] nodeWeightedDegree;
	private int[] nodeSelfLoops;
	private long totalGraphWeight;
	
	public void remove(int node, int community, int noNodeLinksToComm) {
		communityTotalEdges[community] -= nodeWeightedDegree[node];
		communityInternalEdges[community] -= 2*noNodeLinksToComm + nodeSelfLoops[node];
		nodeToCommunity[node] = -1;
	}
	
	public void insert(int node, int community, int noNodeLinksToComm) {
		communityTotalEdges[community] += nodeWeightedDegree[node];
		communityInternalEdges[community] += 2*noNodeLinksToComm + nodeSelfLoops[node];
		nodeToCommunity[node] = community;
	}
	
	/*
	 * Can remove outer division by m2 and will still work
	 */
	public double modularityGain(int node, int community, int noNodeLinksToComm) {
		double totc = (double)communityTotalEdges[community];
		double degc = (double)nodeWeightedDegree[node];
		double m2 = (double)totalGraphWeight;
		double dnc = (double)noNodeLinksToComm;
		
		return (dnc - (totc*degc)/m2) / m2; 
	}
	
	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		int node = vertex.getId();
		if (context.getIteration() == 0) {
			for (int i = 0; i < vertex.numEdges(); i++) {
				int edgeWeight = vertex.edge(i).getValue();
				totalGraphWeight += edgeWeight;
				nodeWeightedDegree[node] += edgeWeight;
			}
			nodeSelfLoops[node] += vertex.getValue();
			communityTotalEdges[node] += nodeWeightedDegree[node];
			if (vertex.numEdges() == 0) {
				vertex.setValue(-1);
			} else {
				vertex.setValue(node);	
				nodeToCommunity[node] = node;
			}
		} else if (vertex.numEdges() != 0){
			Map<Integer, Integer> neighbourCommunities = getNeighbourCommunities(vertex);
			
			int nodeCommunity = nodeToCommunity[node];
			
			remove(node, nodeCommunity, neighbourCommunities.get(nodeCommunity)); //
			
			int bestCommunity = nodeCommunity;
			int bestNoOfLinks = 0;
			double bestModularityGain = 0.;
			
			for (Map.Entry<Integer, Integer> entry : neighbourCommunities.entrySet()) {	
				double gain = modularityGain(node, entry.getKey(), entry.getValue());
				System.out.println("!!!Node: " + node + ", oldCommunity: " + nodeCommunity + ", potentialNew: " 
						+ entry.getKey() + ", connectionsTo: " + entry.getValue() + ", gain: " + gain);
				if (gain > bestModularityGain) {
					bestCommunity = entry.getKey();
					bestNoOfLinks = entry.getValue();
					bestModularityGain = gain;
				}
			}
			
			insert(node, bestCommunity, bestNoOfLinks);
			
			if (bestCommunity != nodeCommunity) {
				improvedOnPass = true;
				modularityImproved = true;
				vertex.setValue(bestCommunity);
			}	
		}
	}
	
	public double getModularity() {
		double q = 0.;
		for (int i = 0; i < nodeToCommunity.length; i++) {
			if (communityTotalEdges[i] > 0) {
				q += (communityInternalEdges[i] / (double)totalGraphWeight);
				q -= Math.pow(communityTotalEdges[i] / (double)totalGraphWeight, 2);
			}
		}
		return q;
	}
	
	private Map<Integer, Integer> getNeighbourCommunities(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		Map<Integer, Integer> result = new HashMap<Integer, Integer>();
		result.put(nodeToCommunity[node], 0);
		
		for (int i = 0; i < vertex.numEdges(); i++) {
			int neighbour = vertex.edge(i).getVertexId();
			int neighbourCommunity = nodeToCommunity[neighbour];
			int edgeWeight = vertex.edge(i).getValue();
			if (result.containsKey(neighbourCommunity)) {
				int previousWeightToCommunity = result.get(neighbourCommunity);
				result.put(neighbourCommunity, previousWeightToCommunity + edgeWeight);
			} else {
				result.put(neighbourCommunity, edgeWeight);
			}
		}
		
		return result;
	}

	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			int noOfVertices = (int)ctx.getNumVertices();
			nodeToCommunity = new int[noOfVertices];
			communityInternalEdges = new int[noOfVertices];
			communityTotalEdges = new int[noOfVertices];
			nodeWeightedDegree = new int[noOfVertices];
			nodeSelfLoops = new int[noOfVertices];
			totalGraphWeight = 0;
		}
		modularityImproved = false;
	}
	
	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0 || modularityImproved) {
			ctx.getScheduler().addAllTasks();
		}
	}
	
	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	protected FastSharder createSharder(String graphName, int numShards) throws IOException {
		return new FastSharder<Integer, Integer>(graphName, numShards, new VertexProcessor<Integer>() {
			public Integer receiveVertexValue(int vertexId, String token) {
				return token != null ? Integer.parseInt(token) : 0;
			}
		}, new EdgeProcessor<Integer>() {
			public Integer receiveEdge(int from, int to, String token) {
				return (token != null ? Integer.parseInt(token) : 1);
			}
		}, new IntConverter(), new IntConverter());
	}

	public GraphChiEngine run(String baseFilename, int nShards) throws  Exception {
		FastSharder sharder = this.createSharder(baseFilename, nShards);
		sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
		GraphChiEngine<Integer, Integer> engine = new GraphChiEngine<Integer, Integer>(baseFilename, nShards);
		engine.setEdataConverter(new IntConverter());
		engine.setVertexDataConverter(new IntConverter());
		engine.setEnableScheduler(true);
		engine.run(this, 10);	
		
		do {
			passIndex++;
			improvedOnPass = false;
			
			String baseFilename = writeNewEdgelist(passIndex, baseFilename);
			
			sharder = this.createSharder(baseFilename, nShards);
			sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
			engine = new GraphChiEngine<Integer, Integer>(baseFilename, nShards);
			engine.setEdataConverter(new IntConverter());
			engine.setVertexDataConverter(new IntConverter());
			engine.setEnableScheduler(true);
			engine.run(this, 10);
		} while (improvedOnPass);
		
		return engine;
	}

	public String writeNewEdgeList(int passIndex, String baseFilename) {
		String filename = baseFilename + "_pass_" + passIndex;
		File newEdgeList = new File()
	}
	
	public static void main(String[] args) throws Exception {
		String folder = "sampledata/"; 
		String file = "sample_label.txt";
		LouvainProgram program = new LouvainProgram();
		GraphChiEngine engine = program.run(folder + file, 1);
		CommunityGraphGenerator generator = new CommunityGraphGenerator(engine, folder + file);
		generator.parseGraphs();
		System.out.println("FINAL MODULARITY: " + program.getModularity());
		System.out.println(generator.getJacksonJson());
		
		FileUtils.moveFile(new File(folder + file), new File(file));
		FileUtils.cleanDirectory(new File(folder));
		FileUtils.moveFile(new File(file), new File(folder + file));
	}

}