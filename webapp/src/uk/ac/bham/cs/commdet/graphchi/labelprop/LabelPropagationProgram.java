package uk.ac.bham.cs.commdet.graphchi.labelprop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.Edge;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

public class LabelPropagationProgram implements GraphChiProgram<Integer, Integer>, DetectionProgram  {
	
	private boolean finalUpdate;
	private LabelGraphStatus status = new LabelGraphStatus();
	private HashMap<Edge, Integer> contractedGraph = new HashMap<Edge, Integer>();
	private VertexIdTranslate trans;

	@Override
	public void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		int newLabel;
		if (context.getIteration() == 0) {
			newLabel = vertex.getId();
			status.getCommunitySize()[vertex.getId()] = 1;
			status.getNodeToCommunity()[vertex.getId()] = newLabel;
			context.getScheduler().addTask(vertex.getId());
			vertex.setValue(newLabel);
		} else {
			newLabel = mostFrequentNeighbourLabel(vertex);
			if (newLabel != vertex.getValue()) {
				for (int i = 0; i < vertex.numEdges(); i++) {
					if (context.getIteration() > 0) {
						context.getScheduler().addTask(vertex.edge(i).getVertexId());
					}
				}
				int oldLabel = vertex.getValue();
				synchronized(status) {
					status.getCommunitySize()[oldLabel]--;
					status.getCommunitySize()[newLabel]++;
				}
				status.getNodeToCommunity()[vertex.getId()] = newLabel;
				vertex.setValue(newLabel);
				
			}
		}
		if (finalUpdate) {
			synchronized (contractedGraph) {
				addToContractedGraph(vertex);
			}
		}
	}
	
	private int mostFrequentNeighbourLabel(ChiVertex<Integer, Integer> vertex) {
		Map<Integer, Integer> labelCounts = new HashMap<Integer, Integer>();
		for (int i = 0; i < vertex.numEdges(); i++) {
			int neighbourLabel = status.getNodeToCommunity()[vertex.edge(i).getVertexId()];
			if (labelCounts.containsKey(neighbourLabel)) {
				int previousCount = labelCounts.get(neighbourLabel);
				labelCounts.put(neighbourLabel, previousCount + 1);
			} else {
				labelCounts.put(neighbourLabel, 1);
			}
		}
		int maxCount = -1;
		int maxLabel = -1;
		for (Map.Entry<Integer, Integer> entry : labelCounts.entrySet()) {
			if (entry.getValue() > maxCount || (entry.getValue() == maxCount && entry.getKey() > maxLabel)) {
				maxCount = entry.getValue();
				maxLabel = entry.getKey();
			}
		}
		return maxLabel;
	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		/*if (status.getCommunityInternalEdges()[node] > 0) {
			int actualNode = trans.backward(node);
			contractedGraph.put(new Edge(actualNode, actualNode), status.getCommunityInternalEdges()[node]/2);
		}*/
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
	}
	
	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			status.setVertexTrans(ctx.getVertexIdTranslate());
			status.initialiseCommunitiesMap();
		}
		if (!finalUpdate && !ctx.getScheduler().hasTasks()) {
			status.getModularities().put(0, -1.);
			ctx.getScheduler().addAllTasks();
			finalUpdate = true;
			status.updateSizesMap();
			status.updateCommunitiesMap();
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
		engine.run(this, 100);
		
		writeNewEdgeList(baseFilename);
		
		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), 1, status.getModularities());
	}
	
	public String writeNewEdgeList(String baseFilename) throws IOException {
		String newFilename = baseFilename + "_pass_" + 1;

		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
		for (Entry<Edge, Integer> entry : contractedGraph.entrySet()) {
			bw.write(entry.getKey().toString() + " " + entry.getValue() + "\n");
		}
		bw.close();

		contractedGraph = new HashMap<Edge, Integer>();
		return newFilename;
	}

}