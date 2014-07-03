package uk.ac.bham.cs.commdet.graphchi.program;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import uk.ac.bham.cs.commdet.graphchi.behaviours.UpdateBehaviour;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

public class GCProgram implements GraphChiProgram<Integer, BidirectionalLabel>  {

	private UpdateBehaviour<Integer, BidirectionalLabel> updateBehaviour;
	
	public GCProgram(UpdateBehaviour<Integer, BidirectionalLabel> updateBehaviour) {
		this.updateBehaviour = updateBehaviour;
	}

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
		
		engine.setEnableScheduler(updateBehaviour.hasScheduler());
		engine.run(this, 10);
		
		return engine;
	}

	public void update(ChiVertex<Integer, BidirectionalLabel> vertex, GraphChiContext context) { 
		updateBehaviour.update(vertex, context);
	}
	
	public void beginIteration(GraphChiContext ctx) {
		updateBehaviour.beginIteration(ctx);
	}

	public void endIteration(GraphChiContext ctx) {
		updateBehaviour.endIteration(ctx);
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {
		updateBehaviour.beginInterval(ctx, interval);
	}

	public void endInterval(GraphChiContext ctx, VertexInterval interval) {
		updateBehaviour.endInterval(ctx, interval);
	}

	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {
		updateBehaviour.beginSubInterval(ctx, interval);
	}

	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {
		updateBehaviour.endSubInterval(ctx, interval);
	}


}