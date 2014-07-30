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
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

public class LabelPropagationProgram implements GraphChiProgram<Integer, Integer>, DetectionProgram  {
	
	private boolean finalUpdate;
	private GraphStatus status = new GraphStatus();
	private HashMap<UndirectedEdge, Integer> contractedGraph = new HashMap<UndirectedEdge, Integer>();
	private VertexIdTranslate trans;

	@Override
	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			Community community = new Community(vertex.getId());
			community.setTotalSize(1);
			status.getNodeToCommunityMap()[vertex.getId()] = community;
			context.getScheduler().addTask(vertex.getId());
		} else {
			Community currentCommunity = status.getNodeToCommunityMap()[vertex.getId()];
			Community mostFrequentNeighbour = mostFrequentNeighbourCommunity(vertex);
			if (mostFrequentNeighbour != currentCommunity) {
				for (int i = 0; i < vertex.numEdges(); i++) {
					context.getScheduler().addTask(vertex.edge(i).getVertexId());
				}
				synchronized(status) {
					currentCommunity.decreaseTotalSize(1);
					mostFrequentNeighbour.increaseTotalSize(1);
				}
				status.getNodeToCommunityMap()[vertex.getId()] = mostFrequentNeighbour;
			}
		}
		if (finalUpdate) {
			synchronized (contractedGraph) {
				addToContractedGraph(vertex);
			}
		}
	}
	
	private Community mostFrequentNeighbourCommunity(ChiVertex<Integer, Integer> vertex) {
		Map<Community, Integer> labelCounts = new HashMap<Community, Integer>();
		for (int i = 0; i < vertex.numEdges(); i++) {
			int neighbour = vertex.edge(i).getVertexId();
			Community neighbourCommunity = status.getNodeToCommunityMap()[neighbour];
			if (labelCounts.containsKey(neighbourCommunity)) {
				int previousCount = labelCounts.get(neighbourCommunity);
				labelCounts.put(neighbourCommunity, previousCount + 1);
			} else {
				labelCounts.put(neighbourCommunity, 1);
			}
		}
		int maxCount = -1;
		Community maxCommunity = new Community(-1);
		for (Map.Entry<Community, Integer> entry : labelCounts.entrySet()) {
			if (entry.getValue() > maxCount || 
					(entry.getValue() == maxCount && entry.getKey().getSeedNode() > maxCommunity.getSeedNode())) {
				maxCount = entry.getValue();
				maxCommunity = entry.getKey();
			}
		}
		return maxCommunity;
	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex) {
		int source = vertex.getId();
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			Community sourceCommunity = status.getNodeToCommunityMap()[source];
			Community targetCommunity = status.getNodeToCommunityMap()[target];
			int sourceCommunityId = sourceCommunity.getSeedNode();
			int targetCommunityId = targetCommunity.getSeedNode();
			int weight = vertex.outEdge(i).getValue();
			status.setTotalGraphWeight(status.getTotalGraphWeight() + weight*2);
			if (sourceCommunityId != targetCommunityId) {
				int actualSourceCommunityId = trans.backward(sourceCommunityId);
				int actualTargetCommunityId = trans.backward(targetCommunityId);
				UndirectedEdge edge = new UndirectedEdge(actualSourceCommunityId, actualTargetCommunityId);
				if (contractedGraph.containsKey(edge)) {
					int oldWeight = contractedGraph.get(edge);
					contractedGraph.put(edge, oldWeight + weight);
				} else {
					contractedGraph.put(edge, weight);
				}
				sourceCommunity.increaseTotalEdges(weight);
				targetCommunity.increaseTotalEdges(weight);
			} else {
				sourceCommunity.increaseInternalEdges(weight * 2);
				sourceCommunity.increaseTotalEdges(weight * 2);
			}
		}
	}
		
	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			trans = ctx.getVertexIdTranslate();
			int noOfVertices = (int)ctx.getNumVertices();
			status.setNodeToCommunityMap(new Community[noOfVertices]);
		}
	}
	
	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			status.setOriginalVertexTrans(ctx.getVertexIdTranslate());
			status.setUpdatedVertexTrans(ctx.getVertexIdTranslate());
			status.initialiseCommunitiesMap();
		}
		if (!finalUpdate && !ctx.getScheduler().hasTasks()) {
			ctx.getScheduler().addAllTasks();
			finalUpdate = true;
			status.updateSizesMap();
			status.updateCommunitiesMap();
		} else {
			status.updateModularity(0);
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
				return token != null ? Integer.parseInt(token) : 1;
			}
		}, new IntConverter(), new IntConverter());
	}

	public GraphResult run(String baseFilename, int nShards) throws  Exception {
		FastSharder sharder = this.createSharder(baseFilename, nShards);
		sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
		GraphChiEngine<Integer, Integer> engine = new GraphChiEngine<Integer, Integer>(baseFilename, nShards);
		engine.setEdataConverter(new IntConverter());
		engine.setVertexDataConverter(new IntConverter());
		engine.setEnableScheduler(true);
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 1000);
		
		writeNextLevelEdgeList(baseFilename);
		
		int hierarchyHeight = 1;
		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), status.getModularities(), hierarchyHeight);
	}
	
	public String writeNextLevelEdgeList(String baseFilename) throws IOException {
		String newFilename = baseFilename + "_pass_" + 1;

		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
		for (Entry<UndirectedEdge, Integer> entry : contractedGraph.entrySet()) {
			bw.write(entry.getKey().toStringWeightless() + " " + entry.getValue() + "\n");
		}
		bw.close();

		contractedGraph = new HashMap<UndirectedEdge, Integer>();
		return newFilename;
	}

}