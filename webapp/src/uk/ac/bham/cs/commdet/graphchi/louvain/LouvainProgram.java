package uk.ac.bham.cs.commdet.graphchi.louvain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.bham.cs.commdet.graphchi.all.Community;
import uk.ac.bham.cs.commdet.graphchi.all.CommunityIdentity;
import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import uk.ac.bham.cs.commdet.graphchi.all.GraphStatus;
import uk.ac.bham.cs.commdet.graphchi.all.UndirectedEdge;
import edu.cmu.graphchi.ChiVertex;
import edu.cmu.graphchi.GraphChiContext;
import edu.cmu.graphchi.GraphChiProgram;
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
public class LouvainProgram implements GraphChiProgram<Integer, Integer>, DetectionProgram  {

	private int passIndex;
	private boolean improvedOnPass;
	private double iterationModularityImprovement;
	private boolean finalUpdate;
	private GraphStatus status = new GraphStatus();
	private HashMap<UndirectedEdge, Integer> contractedGraph = new HashMap<UndirectedEdge, Integer>();
	private VertexIdTranslate trans;

	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			addToInitialGraphStatus(vertex);
		} else if (finalUpdate) {
			addToContractedGraph(vertex);
		} else {
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
		}
	}

	private void decreaseCommunitySize(int node, int nodeCommunity) {
		if (passIndex == 0) {
			status.getCommunitySize()[nodeCommunity]--;
		} else {
			status.getCommunitySize()[nodeCommunity] -= status.getCommunitySizes().get(new CommunityIdentity(trans.backward(node), passIndex - 1));
		}
	}

	private void increaseCommunitySize(int node, int bestCommunity) {
		if (passIndex == 0) {
			status.getCommunitySize()[bestCommunity]++;
		} else {
			status.getCommunitySize()[bestCommunity] += status.getCommunitySizes().get(new CommunityIdentity(trans.backward(node), passIndex - 1));
		}		
	}

	private void remove(int node, int community, int noNodeLinksToComm) {
		status.getCommunityTotalEdges()[community] -= status.getNodeWeightedDegree()[node];
		status.getCommunityInternalEdges()[community] -= 2*noNodeLinksToComm + status.getNodeSelfLoops()[node];
		status.getNodeToCommunity()[node] = -1;
	}

	private void insert(int node, int community, int noNodeLinksToComm) {
		status.getCommunityTotalEdges()[community] += status.getNodeWeightedDegree()[node];
		status.getCommunityInternalEdges()[community] += 2*noNodeLinksToComm + status.getNodeSelfLoops()[node];
		status.getNodeToCommunity()[node] = community;
	}

	private double modularityGain(int node, int community, int noNodeLinksToComm) {
		double totc = (double)status.getCommunityTotalEdges()[community];
		double degc = (double)status.getNodeWeightedDegree()[node];
		double m2 = (double)status.getTotalGraphWeight();
		double dnc = (double)noNodeLinksToComm;

		return (dnc - (totc*degc)/m2) / (m2/2); 
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
		Community community = new Community(vertex.getId());	
		int nodeWeightedDegree = 0;
		for (int i = 0; i < vertex.numEdges(); i++) {
			int edgeWeight = vertex.edge(i).getValue();
			status.setTotalGraphWeight(status.getTotalGraphWeight() + edgeWeight);
			nodeWeightedDegree += edgeWeight;
			//status.getNodeWeightedDegree()[node] += edgeWeight;
		}
		//Node node = new Node();
		
		//status.getNodeSelfLoops()[node] = 2 * vertex.getValue();
		//status.setTotalGraphWeight(status.getTotalGraphWeight() + status.getNodeSelfLoops()[node]);
		//status.getNodeWeightedDegree()[node] += status.getNodeSelfLoops()[node];
		//status.getCommunityInternalEdges()[node] = status.getNodeSelfLoops()[node];
		//status.getCommunityTotalEdges()[node] = status.getNodeWeightedDegree()[node];
		
		if (passIndex == 0) {
			//status.getCommunitySize()[node] = 1;
			community.setTotalSize(1);
		} else {
			int previousSize = status.getCommunitySizes().get(new CommunityIdentity(trans.backward(vertex.getId()), passIndex - 1));
			community.setTotalSize(previousSize);
			//status.getCommunitySize()[node] = status.getCommunitySizes().get(new CommunityIdentity(trans.backward(node), passIndex - 1));
		}
		status.getNodeToCommunity()[node] = node;
	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		if (status.getCommunityInternalEdges()[node] > 0) {
			int actualNode = trans.backward(node);
			contractedGraph.put(new UndirectedEdge(actualNode, actualNode), status.getCommunityInternalEdges()[node]/2);
		}
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int sourceCommunity = status.getNodeToCommunity()[node];
			int targetCommunity = status.getNodeToCommunity()[target];
			if (sourceCommunity != targetCommunity) {
				int actualSourceCommunity = trans.backward(sourceCommunity);
				int actualTargetCommunity = trans.backward(targetCommunity);
				int weight = vertex.outEdge(i).getValue();
				UndirectedEdge edge = new UndirectedEdge(actualSourceCommunity, actualTargetCommunity);
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
			status.setCommunitySize(new int[noOfVertices]);
			status.setTotalGraphWeight(0);
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
			status.updateModularity(passIndex);
			status.updateSizesMap();
			status.updateCommunitiesMap();
			status.incrementHeight();
			ctx.getScheduler().addAllTasks();
			finalUpdate = true;
		}
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	public GraphResult run(String baseFilename, int nShards) throws  Exception {
		setupAndRunEngine(baseFilename);
		String newFilename = baseFilename;
		while (improvedOnPass && contractedGraph.size() > 1) {
			finalUpdate = false;
			passIndex++;
			improvedOnPass = false;
			newFilename = writeNextLevelEdgeList(newFilename);
			setupAndRunEngine(newFilename);
		}

		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), status.getModularities(), passIndex);
	}

	private void setupAndRunEngine(String filename) throws FileNotFoundException, IOException {
		FastSharder sharder = createSharder(filename, 1);
		sharder.shard(new FileInputStream(new File(filename)), "edgelist");
		GraphChiEngine<Integer, Integer> engine = new GraphChiEngine<Integer, Integer>(filename, 1);
		engine.setEdataConverter(new IntConverter());
		engine.setVertexDataConverter(new IntConverter());
		engine.setEnableScheduler(true);
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 1000);
	}

	public String writeNextLevelEdgeList(String baseFilename) throws IOException {
		String base;
		if (passIndex > 1) {
			base = baseFilename.substring(0, baseFilename.indexOf("_pass_"));
		} else {
			base = baseFilename;
		}
		String newFilename = base + "_pass_" + passIndex;

		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
		for (Entry<UndirectedEdge, Integer> entry : contractedGraph.entrySet()) {
			bw.write(entry.getKey().toStringWeightless() + " " + entry.getValue() + "\n");
		}
		bw.close();

		contractedGraph = new HashMap<UndirectedEdge, Integer>();
		return newFilename;
	}
	
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

}