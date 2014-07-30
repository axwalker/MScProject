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
import uk.ac.bham.cs.commdet.graphchi.all.Node;
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
			Node node = status.getNodes()[vertex.getId()];
			Community community = status.getNodeToCommunityMap()[vertex.getId()];
			Map<Community, Integer> neighbourCommunities = getNeighbourCommunities(vertex);

			remove(node, community, neighbourCommunities.get(community));

			Community bestCommunity = community;
			int bestNoOfLinks = 0;
			double bestModularityGain = 0.;
			for (Map.Entry<Community, Integer> entry : neighbourCommunities.entrySet()) {	
				double gain = modularityGain(node, entry.getKey(), entry.getValue());
				if (gain > bestModularityGain || (gain == bestModularityGain && entry.getKey().equals(community))) {
					bestCommunity = entry.getKey();
					bestNoOfLinks = entry.getValue();
					bestModularityGain = gain;
				}
			}

			insert(node, bestCommunity, bestNoOfLinks);

			if (bestCommunity != community) {
				improvedOnPass = true;
				iterationModularityImprovement += bestModularityGain;
			}
		}
	}

	private void remove(Node node, Community community, int noNodeLinksToComm) {
		int totalSize = (passIndex == 0)
				? 1
				: status.getCommunitySizes().get(new CommunityIdentity(trans.backward(node.getId()), passIndex - 1));
		community.setTotalSize(community.getTotalSize() - totalSize);
		community.setTotalEdges(community.getTotalEdges() - node.getWeightedDegree());
		community.setInternalEdges(community.getInternalEdges() - (2*noNodeLinksToComm + node.getSelfLoops()));
	}
	
	private void insert(Node node, Community community, int noNodeLinksToComm) {
		int totalSize = (passIndex == 0)
				? 1
				: status.getCommunitySizes().get(new CommunityIdentity(trans.backward(node.getId()), passIndex - 1));
		community.setTotalSize(community.getTotalSize() + totalSize);
		community.setTotalEdges(community.getTotalEdges() + node.getWeightedDegree());
		community.setInternalEdges(community.getInternalEdges() + (2*noNodeLinksToComm + node.getSelfLoops()));
		status.getNodeToCommunityMap()[node.getId()] = community;
		
	}

	private double modularityGain(Node node, Community community, int noNodeLinksToComm) {
		double totc = (double)community.getTotalEdges();
		double degc = (double)node.getWeightedDegree();
		double m2 = (double)status.getTotalGraphWeight();
		double dnc = (double)noNodeLinksToComm;

		return (dnc - (totc*degc)/m2) / (m2/2); 
	}

	private Map<Community, Integer> getNeighbourCommunities(ChiVertex<Integer, Integer> vertex) {
		Map<Community, Integer> result = new HashMap<Community, Integer>();
		result.put(status.getNodeToCommunityMap()[vertex.getId()], 0);

		for (int i = 0; i < vertex.numEdges(); i++) {
			int neighbour = vertex.edge(i).getVertexId();
			Community neighbourCommunity = status.getNodeToCommunityMap()[neighbour];
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
		int externalWeightedDegree = 0;
		for (int i = 0; i < vertex.numEdges(); i++) {
			int edgeWeight = vertex.edge(i).getValue();
			externalWeightedDegree += edgeWeight;
		}
		if (passIndex == 0) {
			status.increaseTotalGraphWeight(externalWeightedDegree);
		}
		int selfLoops = 2 * vertex.getValue();
		int weightedDegree = selfLoops + externalWeightedDegree;
		Node node = new Node(vertex.getId());
		node.setSelfLoops(selfLoops);
		node.setWeightedDegree(weightedDegree);

		if (passIndex == 0) {
			status.increaseTotalGraphWeight(selfLoops);
		}

		community.setInternalEdges(selfLoops);
		community.setTotalEdges(weightedDegree);

		if (passIndex == 0) {
			community.setTotalSize(1);
		} else {
			System.out.println(status.getCommunitySizes());
			System.out.println("id: " + vertex.getId());
			System.out.println("trans: " + trans.backward(vertex.getId()));
			int previousSize = status.getCommunitySizes()
					.get(new CommunityIdentity(trans.backward(vertex.getId()), passIndex - 1));
			community.setTotalSize(previousSize);
			
		}
		status.getNodes()[vertex.getId()] = node;
		status.getNodeToCommunityMap()[vertex.getId()] = community;
	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex) {
		int source = vertex.getId();
		Community community = status.getNodeToCommunityMap()[source];
		if (community.getInternalEdges() > 0) {
			int actualNode = trans.backward(community.getSeedNode());
			contractedGraph.put(new UndirectedEdge(actualNode, actualNode), community.getInternalEdges() / 2);
		}
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int sourceCommunity = community.getSeedNode();
			int targetCommunity = status.getNodeToCommunityMap()[target].getSeedNode();
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
			status.setNodeToCommunityMap(new Community[(int) ctx.getNumVertices()]);
			status.setNodes(new Node[(int) ctx.getNumVertices()]);
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