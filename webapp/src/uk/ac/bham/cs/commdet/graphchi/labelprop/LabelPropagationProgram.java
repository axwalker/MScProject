package uk.ac.bham.cs.commdet.graphchi.labelprop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.bham.cs.commdet.graphchi.all.Community;
import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import uk.ac.bham.cs.commdet.graphchi.all.GraphStatus;
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
 * Given an edge list file, used to generate a GraphResult object with a
 * corresponding edge list file that groups nodes into communities.
 * 
 * Algorithm as described by Raghavan et al (http://arxiv.org/pdf/0709.2938.pdf)
 */
public class LabelPropagationProgram implements GraphChiProgram<Float, Float>, DetectionProgram  {

	private boolean hasFinishedPropagation;
	private GraphStatus status = new GraphStatus();

	@Override
	public synchronized void update(ChiVertex<Float, Float> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			addToInitialGraphStatus(vertex, context);
		} else if (!hasFinishedPropagation) {
			updateLabelFromNeighbours(vertex, context);
		} else {
			addToContractedGraph(vertex, context.getVertexIdTranslate());
		}
	}

	private void addToInitialGraphStatus(ChiVertex<Float, Float> vertex, GraphChiContext context) {
		Community community = new Community(vertex.getId());
		community.setTotalSize(1);
		status.getCommunities()[vertex.getId()] = community;
		context.getScheduler().addTask(vertex.getId());
	}

	private void updateLabelFromNeighbours(ChiVertex<Float, Float> vertex, GraphChiContext context) {
		Community mostFrequentNeighbour = mostFrequentNeighbourCommunity(vertex);
		Community currentCommunity = status.getCommunities()[vertex.getId()];
		if (mostFrequentNeighbour != currentCommunity) {
			currentCommunity.decreaseTotalSize(1);
			mostFrequentNeighbour.increaseTotalSize(1);
			status.getCommunities()[vertex.getId()] = mostFrequentNeighbour;
			for (int i = 0; i < vertex.numEdges(); i++) {
				//neighbours will need to check again for their own most frequent neighbour labels
				context.getScheduler().addTask(vertex.edge(i).getVertexId());
			}
		}
	}

	private Community mostFrequentNeighbourCommunity(ChiVertex<Float, Float> vertex) {
		Map<Community, Integer> labelCounts = generateNeighbourLabelCounts(vertex);
		Community mostFrequentNeighbour = new Community(-1);
		int maxFrequency = -1;
		for (Map.Entry<Community, Integer> neighbour : labelCounts.entrySet()) {
			boolean hasGreaterFrequency = neighbour.getValue() > maxFrequency;
			boolean hasEqualFrequency = neighbour.getValue() == maxFrequency;
			boolean seedNodeHasHigherValue = neighbour.getKey().getSeedNode() > mostFrequentNeighbour.getSeedNode();
			if (hasGreaterFrequency || (hasEqualFrequency && seedNodeHasHigherValue)) {
				maxFrequency = neighbour.getValue();
				mostFrequentNeighbour = neighbour.getKey();
			}
		}
		return mostFrequentNeighbour;
	}

	private Map<Community, Integer> generateNeighbourLabelCounts(ChiVertex<Float, Float> vertex) {
		Map<Community, Integer> labelCounts = new HashMap<Community, Integer>();
		for (int i = 0; i < vertex.numEdges(); i++) {
			int neighbour = vertex.edge(i).getVertexId();
			Community neighbourCommunity = status.getCommunities()[neighbour];
			if (labelCounts.containsKey(neighbourCommunity)) {
				int previousCount = labelCounts.get(neighbourCommunity);
				labelCounts.put(neighbourCommunity, previousCount + 1);
			} else {
				labelCounts.put(neighbourCommunity, 1);
			}
		}
		return labelCounts;
	}

	/*
	 * contracted graph used to write final edge list with vertices grouped into respective communities
	 */
	private void addToContractedGraph(ChiVertex<Float, Float> vertex, VertexIdTranslate trans) {
		int source = vertex.getId();
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			double weight = vertex.outEdge(i).getValue();
			Community sourceCommunity = status.getCommunities()[source];
			Community targetCommunity = status.getCommunities()[target];
			if (sourceCommunity != targetCommunity) {
				int externalSourceCommunityId = trans.backward(sourceCommunity.getSeedNode());
				int externalTargetCommunityId = trans.backward(targetCommunity.getSeedNode());
				UndirectedEdge edge = new UndirectedEdge(externalSourceCommunityId, externalTargetCommunityId);
				status.addEdgeToContractedGraph(edge, weight);

				//update data for modularity calculation
				sourceCommunity.increaseTotalEdges(weight);
				targetCommunity.increaseTotalEdges(weight);
			} else {
				sourceCommunity.increaseInternalEdges(weight * 2);
				sourceCommunity.increaseTotalEdges(weight * 2);
			}
			status.setTotalGraphWeight(status.getTotalGraphWeight() + weight*2);
		}
	}

	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			int noOfVertices = (int)ctx.getNumVertices();
			status.setCommunities(new Community[noOfVertices]);
		}
	}

	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			status.setOriginalVertexTrans(ctx.getVertexIdTranslate());
			status.setUpdatedVertexTrans(ctx.getVertexIdTranslate());
			status.initialiseCommunitiesMap();
		}
		if (!hasFinishedPropagation && !ctx.getScheduler().hasTasks()) {
			status.updateCommunitiesMap();
			status.updateSizesMap();
			hasFinishedPropagation = true;
			ctx.getScheduler().addAllTasks();
		} else {
			int currentHierarchyLevel = 0;
			status.updateModularity(currentHierarchyLevel);
		}
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	public GraphResult run(String baseFilename, int nShards) throws  Exception {
		FastSharder<Float, Float> sharder = createSharder(baseFilename, nShards);
		sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
		GraphChiEngine<Float, Float> engine = new GraphChiEngine<Float, Float>(baseFilename, nShards);
		engine.setEdataConverter(new FloatConverter());
		engine.setVertexDataConverter(new FloatConverter());
		engine.setEnableScheduler(true);
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 200);

		writeNextLevelEdgeList(baseFilename);

		int hierarchyHeight = 1;
		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), status.getModularities(), hierarchyHeight);
	}

	/*
	 * Uses contracted graph to write final edge list
	 */
	private String writeNextLevelEdgeList(String baseFilename) throws IOException {
		String newFilename = baseFilename + "_pass_" + 1;

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
				return token != null ? Float.parseFloat(token) : 1f;
			}
		}, new FloatConverter(), new FloatConverter());
	}
}