package uk.ac.bham.cs.commdet.graphchi.behaviours;

import edu.cmu.graphchi.ChiVertex;
import edu.cmu.graphchi.GraphChiContext;
import edu.cmu.graphchi.engine.VertexInterval;

public interface UpdateBehaviour<V, E> {

	public boolean hasScheduler();
	public void update(ChiVertex<V, E> vertex, GraphChiContext context);
	public void beginIteration(GraphChiContext ctx);
	public void endIteration(GraphChiContext ctx);
	public void beginInterval(GraphChiContext ctx, VertexInterval interval);
	public void endInterval(GraphChiContext ctx, VertexInterval interval);
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval);
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval);
	
}
