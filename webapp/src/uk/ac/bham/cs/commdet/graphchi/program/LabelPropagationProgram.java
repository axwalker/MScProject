package uk.ac.bham.cs.commdet.graphchi.program;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

public class LabelPropagationProgram implements GraphChiProgram<Integer, BidirectionalLabel>  {

	@Override
	public void update(ChiVertex<Integer, BidirectionalLabel> vertex, GraphChiContext context) {
		int newLabel;
		if (context.getIteration() == 0) {
			newLabel = getInitialLabel(vertex);
			context.getScheduler().addTask(vertex.getId());
		} else {
			newLabel = mostFrequentNeighbourLabel(vertex);
		}
		if (newLabel != vertex.getValue() || context.getIteration() == 0) {
			setBidirectionalLabelOfOutEdges(vertex, newLabel);
			setBidirectionalLabelOfInEdges(vertex, newLabel);
			for (int i = 0; i < vertex.numEdges(); i++) {
				if (context.getIteration() > 0) {
					context.getScheduler().addTask(vertex.edge(i).getVertexId());
				}
			}
			vertex.setValue(newLabel);
		}
	}

	private static int mostFrequentNeighbourLabel(ChiVertex<Integer, BidirectionalLabel> vertex) {
		Map<Integer, Integer> labels = new HashMap<Integer, Integer>();
		addLabelsFromInEdges(vertex, labels);
		addLabelsFromOutEdges(vertex, labels);
		int maxCount = -1;
		int maxLabel = -1;
		for (Map.Entry<Integer, Integer> entry : labels.entrySet()) {
			if (entry.getValue() > maxCount || (entry.getValue() == maxCount && entry.getKey() > maxLabel)) {
				maxCount = entry.getValue();
				maxLabel = entry.getKey();
			}
		}
		return maxLabel;
	}

	private static int getInitialLabel(ChiVertex<Integer, BidirectionalLabel> vertex) {
		return vertex.getId();
	}

	private static void addLabelsFromOutEdges(ChiVertex<Integer, BidirectionalLabel> vertex, Map<Integer, Integer> labels) {
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			boolean thisIsSmallerOne = vertex.getId() < vertex.outEdge(i).getVertexId();
			int neighbourLabel;
			if (thisIsSmallerOne) {
				neighbourLabel = vertex.outEdge(i).getValue().getLargerOne();
			} else {
				neighbourLabel = vertex.outEdge(i).getValue().getSmallerOne();
			}
			if (labels.containsKey(neighbourLabel)) {
				int labelCount = labels.get(neighbourLabel);
				labels.put(neighbourLabel, labelCount + 1);
			} else {
				labels.put(neighbourLabel, 1);
			}
		}
	}

	private static void addLabelsFromInEdges(ChiVertex<Integer, BidirectionalLabel> vertex, Map<Integer, Integer> labels) {
		for (int i = 0; i < vertex.numInEdges(); i++) {
			boolean thisIsSmallerOne = vertex.getId() < vertex.inEdge(i).getVertexId();
			int neighbourLabel;
			if (thisIsSmallerOne) {
				neighbourLabel = vertex.inEdge(i).getValue().getLargerOne();
			} else {
				neighbourLabel = vertex.inEdge(i).getValue().getSmallerOne();
			}
			if (labels.containsKey(neighbourLabel)) {
				int labelCount = labels.get(neighbourLabel);
				labels.put(neighbourLabel, labelCount + 1);
			} else {
				labels.put(neighbourLabel, 1);
			}
		}
	}

	private static void setBidirectionalLabelOfOutEdges(ChiVertex<Integer, BidirectionalLabel> vertex, int newLabel) {
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			boolean thisIsSmallerOne = vertex.getId() < vertex.outEdge(i).getVertexId();
			BidirectionalLabel bi = vertex.outEdge(i).getValue();
			if (thisIsSmallerOne) {
				bi.setSmallerOne(newLabel);
			} else {
				bi.setLargerOne(newLabel);
			}
			vertex.outEdge(i).setValue(bi);
		}
	}

	private static void setBidirectionalLabelOfInEdges(ChiVertex<Integer, BidirectionalLabel> vertex, int newLabel) {
		for (int i = 0; i < vertex.numInEdges(); i++) {
			boolean thisIsSmallerOne = vertex.getId() < vertex.inEdge(i).getVertexId();
			BidirectionalLabel bi = vertex.inEdge(i).getValue();
			if (thisIsSmallerOne) {
				bi.setSmallerOne(newLabel);
			} else {
				bi.setLargerOne(newLabel);
			}
			vertex.inEdge(i).setValue(bi);
		}
	}
	
	public void beginIteration(GraphChiContext ctx) {}
	public void endIteration(GraphChiContext ctx) {}
	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	protected FastSharder createSharder(String graphName, int numShards) throws IOException {
		return new FastSharder<Integer, BidirectionalLabel>(graphName, numShards, new VertexProcessor<Integer>() {
			public Integer receiveVertexValue(int vertexId, String token) {
				return token != null ? Integer.parseInt(token) : 0;
			}
		}, new EdgeProcessor<BidirectionalLabel>() {
			public BidirectionalLabel receiveEdge(int from, int to, String token) {
				return new BidirectionalLabel(0, 0, (token != null ? Integer.parseInt(token) : 0));
			}
		}, new IntConverter(), new BidirectionalLabelConverter());
	}

	public GraphChiEngine run(String baseFilename, int nShards) throws  Exception {
		
		FastSharder sharder = this.createSharder(baseFilename, nShards);
		sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
		GraphChiEngine<Integer, BidirectionalLabel> engine = new GraphChiEngine<Integer, BidirectionalLabel>(baseFilename, nShards);
		engine.setEdataConverter(new BidirectionalLabelConverter());
		engine.setVertexDataConverter(new IntConverter());
		
		engine.setEnableScheduler(true);
		engine.run(this, 10);
		
		return engine;
	}

}