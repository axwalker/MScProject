package uk.ac.bham.cs.commdet.graphchi.program;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import uk.ac.bham.cs.commdet.cyto.json.CommunityGraphGenerator;

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
	private double iterationModularityImprovement;
	private boolean finalUpdate;
	private int[] nodeToCommunity;
	private int[] communityInternalEdges;
	private int[] communityTotalEdges;
	private int[] nodeWeightedDegree;
	private int[] nodeSelfLoops;
	private int[] communitySize;
	private long totalGraphWeight;
	private HashMap<Edge, Integer> contractedGraph = new HashMap<Edge, Integer>();
	
	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			addToInitialGraphStatus(vertex);
		} else if (!finalUpdate) {
			int node = vertex.getId();
			int nodeCommunity = nodeToCommunity[node];
			
			Map<Integer, Integer> neighbourCommunities = getNeighbourCommunities(vertex);
			
			remove(node, nodeCommunity, neighbourCommunities.get(nodeCommunity));
			
			int bestCommunity = nodeCommunity;
			int bestNoOfLinks = 0;
			double bestModularityGain = 0.;

			for (Map.Entry<Integer, Integer> entry : neighbourCommunities.entrySet()) {	
				double gain = modularityGain(node, entry.getKey(), entry.getValue());
				if (gain > bestModularityGain || (gain == bestModularityGain && entry.getKey() == nodeCommunity)) {
					bestCommunity = entry.getKey();
					bestNoOfLinks = entry.getValue();
					bestModularityGain = gain;
				}
			}
			
			insert(node, bestCommunity, bestNoOfLinks);
			
			if (bestCommunity != nodeCommunity) {
				//System.out.println("bestGain: " + bestModularityGain);
				//System.out.println("old mod: " + nodeCommunity + ", new community: " + bestCommunity);
				//double newModularity = getModularity();
				//System.out.println("previous modularity: " + previousModularity);
				//System.out.println("Current modularity : " + newModularity);
				//if ((previousModularity + bestModularityGain) - newModularity > 0.0001) {
				//	System.out.println("bestGain inaccuracy! £££££££££££££££££££££££££££££££££££££££££££££££££££££");
				//}
				improvedOnPass = true;
				iterationModularityImprovement += bestModularityGain;
				vertex.setValue(bestCommunity);
			}	
		} else {
			addToContractedGraph(vertex, context);
		}

	}
	
	public void remove(int node, int community, int noNodeLinksToComm) {
		communityTotalEdges[community] -= nodeWeightedDegree[node];
		communityInternalEdges[community] -= 2*noNodeLinksToComm + nodeSelfLoops[node];
		nodeToCommunity[node] = -1;
		communitySize[community]--;
	}

	public void insert(int node, int community, int noNodeLinksToComm) {
		communityTotalEdges[community] += nodeWeightedDegree[node];
		communityInternalEdges[community] += 2*noNodeLinksToComm + nodeSelfLoops[node];
		nodeToCommunity[node] = community;
		communitySize[community]++;
	}

	public double modularityGain(int node, int community, int noNodeLinksToComm) {
		double totc = (double)communityTotalEdges[community];
		double degc = (double)nodeWeightedDegree[node];
		double m2 = (double)totalGraphWeight;
		double dnc = (double)noNodeLinksToComm;
				
		return (dnc - (totc*degc)/m2) / (m2/2); 
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
	
	private void addToInitialGraphStatus(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		for (int i = 0; i < vertex.numEdges(); i++) {
			int edgeWeight = vertex.edge(i).getValue();
			totalGraphWeight += edgeWeight;
			nodeWeightedDegree[node] += edgeWeight;
		}
		nodeSelfLoops[node] = 2 * vertex.getValue();
		totalGraphWeight += nodeSelfLoops[node];
		nodeWeightedDegree[node] += nodeSelfLoops[node];
		communityInternalEdges[node] = nodeSelfLoops[node];
		communityTotalEdges[node] = nodeWeightedDegree[node];
		//set community label to node id
		vertex.setValue(node);
		nodeToCommunity[node] = node;
	}
	
	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex, GraphChiContext ctx) {
		int node = vertex.getId();
		VertexIdTranslate trans = ctx.getVertexIdTranslate();
		int actualNode = trans.backward(node);
		if (node == nodeToCommunity[node]) {
			contractedGraph.put(new Edge(actualNode, actualNode), communityInternalEdges[node]/2);
		}
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int sourceCommunity = nodeToCommunity[node];
			int targetCommunity = nodeToCommunity[target];
			if (sourceCommunity != targetCommunity) {
				int actualSourceCommunity = trans.backward(sourceCommunity);
				int actualTargetCommunity = trans.backward(targetCommunity);
				int weight = vertex.outEdge(i).getValue();
				Edge edge = new Edge(actualSourceCommunity, actualTargetCommunity);
				if (contractedGraph.containsKey(edge)) {
					int oldWeight = contractedGraph.get(edge);
					contractedGraph.put(edge, oldWeight + weight);
				} else {
					contractedGraph.put(edge, weight);
				}
			}
		}
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
			if (passIndex == 0) {
				communitySize = new int[noOfVertices];
			}
		}
		iterationModularityImprovement = 0.;
	}

	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0 || iterationModularityImprovement > 0.0001) {
			ctx.getScheduler().addAllTasks();
		} else  if (!finalUpdate && improvedOnPass) {
			ctx.getScheduler().addAllTasks();
			finalUpdate = true;
		}
		//System.out.println("Current modularity : " + getModularity());
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	protected FastSharder createSharder(String graphName, int numShards) throws IOException {
		return new FastSharder<Integer, Integer>(graphName, numShards, new VertexProcessor<Integer>() {
			public Integer receiveVertexValue(int vertexId, String token) {
				return token != null ? Integer.parseInt(token) : -1;
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
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 1000);

		while (improvedOnPass && contractedGraph.size() > 1) {
			finalUpdate = false;
			passIndex++;
			improvedOnPass = false;
			
			baseFilename = writeNewEdgeList(baseFilename, engine);
			System.out.println(baseFilename + " ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

			sharder = this.createSharder(baseFilename, 1);
			sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
			engine = new GraphChiEngine<Integer, Integer>(baseFilename, 1);
			engine.setEdataConverter(new IntConverter());
			engine.setVertexDataConverter(new IntConverter());
			engine.setEnableScheduler(true);
			engine.setSkipZeroDegreeVertices(true);
			engine.run(this, 1000);
		}

		return engine;
	}

	public String writeNewEdgeList(String baseFilename, GraphChiEngine oldEngine) throws IOException {
		String base;
		if (passIndex > 1) {
			base = baseFilename.substring(0, baseFilename.indexOf("_pass_"));
		} else {
			base = baseFilename;
		}
		String newFilename = base + "_pass_" + passIndex;

		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
		for (Entry<Edge, Integer> entry : contractedGraph.entrySet()) {
			bw.write(entry.getKey().toString() + " " + entry.getValue() + "\n");
		}

		bw.close();
		contractedGraph = new HashMap<Edge, Integer>();
		return newFilename;
	}

	public static void main(String[] args) throws Exception {
		String folder = "sampledata/"; 
		String file = "sample_label.txt";
		LouvainProgram program = new LouvainProgram();
		GraphChiEngine engine = program.run(folder + file, 1);
		CommunityGraphGenerator generator = new CommunityGraphGenerator(engine, folder + file + "_pass_" + program.passIndex);
		generator.parseGraphs();
		System.out.println("FINAL MODULARITY: " + program.getModularity());
		System.out.println(generator.getJacksonJson());
		
		for (int i = 0; i < program.communitySize.length; i++) {
			if (program.communitySize[i] + 1 > 0) {
				System.out.println("community: " + i + ", size: " + (program.communitySize[i] + 1));
			}
		}

		//FileUtils.moveFile(new File(folder + file), new File(file));
		//FileUtils.cleanDirectory(new File(folder));
		//FileUtils.moveFile(new File(file), new File(folder + file));
	}

}