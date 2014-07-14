package uk.ac.bham.cs.commdet.graphchi.louvain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import uk.ac.bham.cs.commdet.cyto.json.GraphJsonGenerator;

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
	private GraphStatus status = new GraphStatus();
	private HashMap<Edge, Integer> contractedGraph = new HashMap<Edge, Integer>();
	private VertexIdTranslate trans;

	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			addToInitialGraphStatus(vertex);
		} else if (!finalUpdate) {
			int node = vertex.getId();
			int nodeCommunity = status.getNodeToCommunity()[node];

			Map<Integer, Integer> neighbourCommunities = getNeighbourCommunities(vertex);

			decreaseCommunitySize(node, nodeCommunity);
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
			increaseCommunitySize(node, bestCommunity);

			if (bestCommunity != nodeCommunity) {
				improvedOnPass = true;
				iterationModularityImprovement += bestModularityGain;
			}
		} else {
			addToContractedGraph(vertex, context);
		}
	}

	private void decreaseCommunitySize(int node, int nodeCommunity) {
		if (passIndex == 0) {
			status.getCommunitySize()[nodeCommunity]--;
		} else {
			status.getCommunitySize()[nodeCommunity] -= status.getCommunitySizes().get(new Community(trans.backward(node), passIndex - 1));
		}
	}
	
	private void increaseCommunitySize(int node, int bestCommunity) {
		if (passIndex == 0) {
			status.getCommunitySize()[bestCommunity]++;
		} else {
			status.getCommunitySize()[bestCommunity] += status.getCommunitySizes().get(new Community(trans.backward(node), passIndex - 1));
		}		
	}

	public void remove(int node, int community, int noNodeLinksToComm) {
		status.getCommunityTotalEdges()[community] -= status.getNodeWeightedDegree()[node];
		status.getCommunityInternalEdges()[community] -= 2*noNodeLinksToComm + status.getNodeSelfLoops()[node];
		status.getNodeToCommunity()[node] = -1;
	}

	public void insert(int node, int community, int noNodeLinksToComm) {
		status.getCommunityTotalEdges()[community] += status.getNodeWeightedDegree()[node];
		status.getCommunityInternalEdges()[community] += 2*noNodeLinksToComm + status.getNodeSelfLoops()[node];
		status.getNodeToCommunity()[node] = community;
	}

	public double modularityGain(int node, int community, int noNodeLinksToComm) {
		double totc = (double)status.getCommunityTotalEdges()[community];
		double degc = (double)status.getNodeWeightedDegree()[node];
		double m2 = (double)status.getTotalGraphWeight();
		double dnc = (double)noNodeLinksToComm;

		return (dnc - (totc*degc)/m2) / (m2/2); 
	}

	public double getModularity() {
		double q = 0.;
		for (int i = 0; i < status.getNodeToCommunity().length; i++) {
			if (status.getCommunityTotalEdges()[i] > 0) {
				q += (status.getCommunityInternalEdges()[i] / (double)status.getTotalGraphWeight());
				q -= Math.pow(status.getCommunityTotalEdges()[i] / (double)status.getTotalGraphWeight(), 2);
			}
		}
		return q;
	}

	private Map<Integer, Integer> getNeighbourCommunities(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		Map<Integer, Integer> result = new HashMap<Integer, Integer>();
		result.put(status.getNodeToCommunity()[node], 0);

		for (int i = 0; i < vertex.numEdges(); i++) {
			int neighbour = vertex.edge(i).getVertexId();
			int neighbourCommunity = status.getNodeToCommunity()[neighbour];
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
			status.setTotalGraphWeight(status.getTotalGraphWeight() + edgeWeight);
			status.getNodeWeightedDegree()[node] += edgeWeight;
		}
		status.getNodeSelfLoops()[node] = 2 * vertex.getValue();
		status.setTotalGraphWeight(status.getTotalGraphWeight() + status.getNodeSelfLoops()[node]);
		status.getNodeWeightedDegree()[node] += status.getNodeSelfLoops()[node];
		status.getCommunityInternalEdges()[node] = status.getNodeSelfLoops()[node];
		status.getCommunityTotalEdges()[node] = status.getNodeWeightedDegree()[node];
		if (passIndex == 0) {
			status.getCommunitySize()[node] = 1;
		} else {
			status.getCommunitySize()[node] = status.getCommunitySizes().get(new Community(trans.backward(node), passIndex - 1));
		}
		status.getNodeToCommunity()[node] = node;
	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex, GraphChiContext ctx) {
		int node = vertex.getId();
		if (status.getCommunityInternalEdges()[node] > 0) {
			int actualNode = trans.backward(node);
			contractedGraph.put(new Edge(actualNode, actualNode), status.getCommunityInternalEdges()[node]/2);
		}
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int sourceCommunity = status.getNodeToCommunity()[node];
			int targetCommunity = status.getNodeToCommunity()[target];
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
			trans = ctx.getVertexIdTranslate();
			int noOfVertices = (int)ctx.getNumVertices();
			status.setNodeToCommunity(new int[noOfVertices]);
			for (int i = 0; i < noOfVertices; i++) {
				status.getNodeToCommunity()[i] = -1;
			}
			status.setCommunityInternalEdges(new int[noOfVertices]);
			status.setCommunityTotalEdges(new int[noOfVertices]);
			status.setNodeWeightedDegree(new int[noOfVertices]);
			status.setNodeSelfLoops(new int[noOfVertices]);
			status.setTotalGraphWeight(0);
			status.setCommunitySize(new int[noOfVertices]);
		}
		iterationModularityImprovement = 0.;
	}

	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0 && passIndex == 0) {
			status.setOriginalVertexTrans(ctx.getVertexIdTranslate());
			status.initialiseCommunitiesMap();
		}
		if (ctx.getIteration() == 0 || iterationModularityImprovement > 0.00001) {
			status.setUpdatedVertexTrans(ctx.getVertexIdTranslate());
			ctx.getScheduler().addAllTasks();
		} else  if (!finalUpdate && improvedOnPass) {
			status.getModularities().put(passIndex, getModularity());
			ctx.getScheduler().addAllTasks();
			finalUpdate = true;
			status.updateSizesMap();
			status.updateCommunitiesMap();
			status.incrementHeight();
		}
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	protected static FastSharder createSharder(String graphName, int numShards) throws IOException {
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

	public GraphResult run(String baseFilename, int nShards) throws  Exception {
		FastSharder sharder = createSharder(baseFilename, nShards);
		sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
		GraphChiEngine<Integer, Integer> engine = new GraphChiEngine<Integer, Integer>(baseFilename, nShards);
		engine.setEdataConverter(new IntConverter());
		engine.setVertexDataConverter(new IntConverter());
		engine.setEnableScheduler(true);
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 1000);

		String newFilename = baseFilename;
		while (improvedOnPass && contractedGraph.size() > 1) {
			finalUpdate = false;
			passIndex++;
			improvedOnPass = false;
			
			newFilename = writeNewEdgeList(newFilename);

			sharder = createSharder(newFilename, 1);
			sharder.shard(new FileInputStream(new File(newFilename)), "edgelist");
			engine = new GraphChiEngine<Integer, Integer>(newFilename, 1);
			engine.setEdataConverter(new IntConverter());
			engine.setVertexDataConverter(new IntConverter());
			engine.setEnableScheduler(true);
			engine.setSkipZeroDegreeVertices(true);
			engine.run(this, 1000);
		}

		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), passIndex, status.getModularities());
	}

	public String writeNewEdgeList(String baseFilename) throws IOException {
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
		String file = "karateclub_edg.txt";
		LouvainProgram program = new LouvainProgram();
		GraphResult result = program.run(folder + file, 1);
		System.out.println("FINAL MODULARITY: " + program.getModularity());
		result.writeSortedEdgeLists();
		GraphJsonGenerator generator = new GraphJsonGenerator(result);
		System.out.println(result.getSizes());
		System.out.println(result.getHeight());
		System.out.println(result.getEdgePositions());
		System.out.println(program.status.getModularities());
		System.out.println(generator.getCommunityJson(33, 0));
		//System.out.println(generator.getParentGraphJson());
		//System.out.println(generator.getGraphJson(3));

		//System.out.println(Arrays.toString(program.status.getCommunitySize()));
		//System.out.println(result.getEdgePositions());
		
		//System.out.println(result.getHierarchy());
		//FileUtils.moveFile(new File(folder + file), new File(file));
		//FileUtils.cleanDirectory(new File(folder));
		//FileUtils.moveFile(new File(file), new File(folder + file));
	}

}