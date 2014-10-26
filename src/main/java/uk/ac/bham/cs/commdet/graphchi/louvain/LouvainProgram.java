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
import uk.ac.bham.cs.commdet.graphchi.all.CommunityID;
import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import uk.ac.bham.cs.commdet.graphchi.all.GraphStatus;
import uk.ac.bham.cs.commdet.graphchi.all.Node;
import uk.ac.bham.cs.commdet.graphchi.all.UndirectedEdge;
import edu.cmu.graphchi.ChiVertex;
import edu.cmu.graphchi.GraphChiContext;
import edu.cmu.graphchi.GraphChiProgram;
import edu.cmu.graphchi.datablocks.FloatConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

/**
 * Given an edge list file, used to generate a GraphResult object with corresponding edge list
 * files that progressively group nodes into communities with increasing modularity.
 * 
 * Algorithm as described by Blondel at al (http://arxiv.org/pdf/0803.0476v2.pdf)
 * Partly adapted from C++ source code (https://sites.google.com/site/findcommunities/)
 */
public class LouvainProgram implements GraphChiProgram<Float, Float>, DetectionProgram  {

	private int passIndex;
	private boolean improvedOnPass;
	private double iterationModularityImprovement;
	private boolean isReadyToContract;
	private static final double MINIMUM_REQUIRED_IMPROVEMENT = 0.00001;
	private GraphStatus status = new GraphStatus();
	private VertexIdTranslate trans;

	@Override
	public synchronized void update(ChiVertex<Float, Float> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			if ((passIndex == 0 && vertex.numEdges() > 0) || passIndex > 0) {
				addToInitialGraphStatus(vertex);
			}
		} else if (!isReadyToContract) {
			if (vertex.numEdges() > 0) {
				lookForModularityGain(vertex);
			}
		} else {
			addToContractedGraph(vertex);
		}
	}

	private void lookForModularityGain(ChiVertex<Float, Float> vertex) {
		Node node = status.getNodes()[vertex.getId()];
		Community community = status.getCommunities()[vertex.getId()];
		Map<Community, Double> neighbourCommunities = getNeighbourCommunities(vertex);

		status.removeNodeFromCommunity(node, community, neighbourCommunities.get(community));
		
		Community bestCommunity = community;
		double bestNoOfLinks = 0;
		double bestModularityGain = 0.;
		for (Map.Entry<Community, Double> entry : neighbourCommunities.entrySet()) {	
			double gain = status.modularityGain(node, entry.getKey(), entry.getValue());
			if (gain > bestModularityGain || (gain == bestModularityGain && entry.getKey().equals(community))) {
				bestCommunity = entry.getKey();
				bestNoOfLinks = entry.getValue();
				bestModularityGain = gain;
			}
		}

		status.insertNodeIntoCommunity(node, bestCommunity, bestNoOfLinks);

		if (bestCommunity != community) {
			improvedOnPass = true;
			iterationModularityImprovement += bestModularityGain;
		}
	}


	private Map<Community, Double> getNeighbourCommunities(ChiVertex<Float, Float> vertex) {
		Map<Community, Double> result = new HashMap<Community, Double>();
		result.put(status.getCommunities()[vertex.getId()], 0.);

		for (int i = 0; i < vertex.numEdges(); i++) {
			int neighbour = vertex.edge(i).getVertexId();
			Community neighbourCommunity = status.getCommunities()[neighbour];
			double edgeWeight = vertex.edge(i).getValue();
			if (result.containsKey(neighbourCommunity)) {
				double previousWeightToCommunity = result.get(neighbourCommunity);
				result.put(neighbourCommunity, previousWeightToCommunity + edgeWeight);
			} else {
				result.put(neighbourCommunity, edgeWeight);
			}
		}

		return result;
	}

	private void addToInitialGraphStatus(ChiVertex<Float, Float> vertex) {
		Community community = new Community(vertex.getId());
		Node node = new Node(vertex.getId());
		
		double externalWeightedDegree = 0;
		for (int i = 0; i < vertex.numEdges(); i++) {
			double edgeWeight = vertex.edge(i).getValue();
			externalWeightedDegree += edgeWeight;
		}
		double selfLoops = 2 * vertex.getValue();
		double weightedDegree = selfLoops + externalWeightedDegree;

		if (passIndex == 0) {
			status.increaseTotalGraphWeight(weightedDegree);
			community.setTotalSize(1);
		} else {
			Map<CommunityID, Integer> sizes = status.getCommunitySizes();
			CommunityID communityId = new CommunityID(trans.backward(vertex.getId()), passIndex - 1);
			if (!sizes.containsKey(communityId)) {
				//System.out.println("this should be a graphchi generated node... " + trans.backward(vertex.getId()));
				return;
			}
			int previousSize = sizes.get(new CommunityID(trans.backward(vertex.getId()), passIndex - 1));
			community.setTotalSize(previousSize);
		}
		community.setInternalEdges(selfLoops);
		community.setTotalEdges(weightedDegree);
		node.setSelfLoops(selfLoops);
		node.setWeightedDegree(weightedDegree);
		
		status.getNodes()[vertex.getId()] = node;
		status.getCommunities()[vertex.getId()] = community;
	}

	private void addToContractedGraph(ChiVertex<Float, Float> vertex) {
		int source = vertex.getId();
		Community community = status.getCommunities()[source];
		if (community == null) {
			return;
		}
		if (community.getInternalEdges() > 0 && vertex.numEdges() > 0) {
			int actualNode = trans.backward(community.getSeedNode());
			status.getContractedGraph().put(new UndirectedEdge(actualNode, actualNode), community.getInternalEdges() / 2);
		} 
		if (vertex.numEdges() == 0) {
			int actualNode = trans.backward(community.getSeedNode());
			status.getContractedGraph().put(new UndirectedEdge(actualNode, actualNode), community.getInternalEdges() / 2);
		}
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int sourceCommunity = community.getSeedNode();
			int targetCommunity = status.getCommunities()[target].getSeedNode();
			if (sourceCommunity != targetCommunity) {
				int actualSourceCommunity = trans.backward(sourceCommunity);
				int actualTargetCommunity = trans.backward(targetCommunity);
				double weight = vertex.outEdge(i).getValue();
				UndirectedEdge edge = new UndirectedEdge(actualSourceCommunity, actualTargetCommunity);
				if (status.getContractedGraph().containsKey(edge)) {
					double oldWeight = status.getContractedGraph().get(edge);
					status.getContractedGraph().put(edge, oldWeight + weight);
				} else {
					status.getContractedGraph().put(edge, weight);
					status.increaseEdgeCount(1);
				}
			}
		}
		
		
	}

	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			trans = ctx.getVertexIdTranslate();
			status.setCommunities(new Community[(int) ctx.getNumVertices()]);
			status.setNodes(new Node[(int) ctx.getNumVertices()]);
			status.setEdgeCount(0);
		}
		iterationModularityImprovement = 0.;
	}

	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0 && passIndex == 0) {
			status.setOriginalVertexTrans(ctx.getVertexIdTranslate());
			status.initialiseCommunitiesMap();
		}
		if (ctx.getIteration() == 0 || iterationModularityImprovement > MINIMUM_REQUIRED_IMPROVEMENT) {
			status.setUpdatedVertexTrans(ctx.getVertexIdTranslate());
			ctx.getScheduler().addAllTasks();
		} else  if (!isReadyToContract && improvedOnPass) {
			status.updateModularity(passIndex);
			status.updateCommunitiesMap();
			status.updateSizesMap();
			status.incrementHeight();
			ctx.getScheduler().addAllTasks();
			isReadyToContract = true;
		}
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	public GraphResult run(String baseFilename, int nShards) throws  Exception {
		setupAndRunEngine(baseFilename);
		String newFilename = baseFilename;
		while (improvedOnPass && status.getContractedGraph().size() > 1 && status.getEdgeCount() > 0) {
			isReadyToContract = false;
			passIndex++;
			improvedOnPass = false;
			newFilename = writeNextLevelEdgeList(newFilename);
			setupAndRunEngine(newFilename);
		}

		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), status.getModularities(), passIndex);
	}

	private void setupAndRunEngine(String filename) throws FileNotFoundException, IOException {
		FastSharder<Float, Float> sharder = createSharder(filename, 1);
		System.out.println("filename: " + filename);
		sharder.shard(new FileInputStream(new File(filename)), "edgelist");
		System.out.println("done sharding...");
		GraphChiEngine<Float, Float> engine = new GraphChiEngine<Float, Float>(filename, 1);
		engine.setEdataConverter(new FloatConverter());
		engine.setVertexDataConverter(new FloatConverter());
		engine.setEnableScheduler(true);
		engine.run(this, 1000);
	}

	/*
	 * Uses contracted graph from previous iteration to write edge list to a file
	 * for use as input in to the next iteration's engine.
	 */
	private String writeNextLevelEdgeList(String baseFilename) throws IOException {
		String base;
		if (passIndex > 1) {
			base = baseFilename.substring(0, baseFilename.indexOf("_pass_"));
		} else {
			base = baseFilename;
		}
		String newFilename = base + "_pass_" + passIndex;

		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
		for (Entry<UndirectedEdge, Double> entry : status.getContractedGraph().entrySet()) {
			bw.write(entry.getKey().toStringWeightless() + " " + entry.getValue() + "\n");
		}
		bw.close();

		status.setContractedGraph(new HashMap<UndirectedEdge, Double>());
		return newFilename;
	}

	private static FastSharder<Float, Float> createSharder(String graphName, int numShards) throws IOException {
		return new FastSharder<Float, Float>(graphName, numShards, new VertexProcessor<Float>() {
			public Float receiveVertexValue(int vertexId, String token) {
				return token != null ? Float.parseFloat(token) : 0f;
			}
		}, new EdgeProcessor<Float>() {
			public Float receiveEdge(int from, int to, String token) {
				return (token != null ? Float.parseFloat(token) : 1f);
			}
		}, new FloatConverter(), new FloatConverter());
	}

}