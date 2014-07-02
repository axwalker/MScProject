package uk.ac.bham.cs.commdet.graphchi.program;

import uk.ac.bham.cs.commdet.graphchi.behaviours.UpdateBehaviour;
import edu.cmu.graphchi.ChiVertex;
import edu.cmu.graphchi.GraphChiContext;
import edu.cmu.graphchi.GraphChiProgram;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;

public abstract class GCProgram<V, E> implements GraphChiProgram<V, E> {
	
	private UpdateBehaviour<V, E> updateBehaviour;
	
	public GCProgram(UpdateBehaviour<V, E> updateBehaviour) {
		this.updateBehaviour = updateBehaviour;
	}

	public abstract GraphChiEngine run(String baseFilename, int nShards) throws Exception;
	
	public UpdateBehaviour<V, E> getUpdateBehaviour() {
		return this.updateBehaviour;
	}
	
	public void update(ChiVertex<V, E> vertex, GraphChiContext context) { 
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
