package uk.ac.bham.cs.commdet.graphchi.orca;

import java.io.File;
import java.io.FileInputStream;

import uk.ac.bham.cs.commdet.graphchi.all.GraphStatus;
import uk.ac.bham.cs.commdet.graphchi.all.UndirectedEdge;
import edu.cmu.graphchi.ChiVertex;
import edu.cmu.graphchi.GraphChiContext;
import edu.cmu.graphchi.GraphChiProgram;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

public class TwoCore implements GraphChiProgram<Integer, Integer> {

	private boolean twoCoreCompleted;
	private boolean twoCoreContractionStage = true;
	private boolean propagationStage;
	private int nodeDegree[];
	private boolean contracted[];

	private boolean finalUpdate;
	private GraphStatus status = new GraphStatus();


	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (!twoCoreCompleted && !finalUpdate) {
			twoCoreUpdate(vertex, context);
		} else {
			addToContractedGraph(vertex, context.getVertexIdTranslate());
		}
	}

	private void twoCoreUpdate(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (twoCoreContractionStage) {
			int degree;
			if (context.getIteration() == 0) {
				degree = vertex.numEdges();
				nodeDegree[vertex.getId()] = degree;
				//status.insertNodeIntoCommunity(vertex.getId(), vertex.getId());
				context.getScheduler().addTask(vertex.getId());
			} else {
				degree = nodeDegree[vertex.getId()];
				if (degree < 2) {
					contracted[vertex.getId()] = true;
					for (int i = 0; i < degree; i++) {
						int neighbourId = vertex.edge(i).getVertexId();
						nodeDegree[neighbourId]--;
						if (!contracted[neighbourId]) {
							context.getScheduler().addTask(neighbourId);
						}
					}
				}
			}
		} else if (propagationStage) {
			if (!contracted[vertex.getId()]) {
				for (int i = 0; i < vertex.numEdges(); i++) {
					int neighbourId = vertex.edge(i).getVertexId();
					if (contracted[neighbourId]) {
						//int thisCommunity = status.getNodeToCommunity()[vertex.getId()];
						//status.removeNodeFromCommunity(neighbourId, neighbourId);
						//status.insertNodeIntoCommunity(neighbourId, thisCommunity);
						contracted[neighbourId] = false; //to prevent looping round adding tasks infintely
						context.getScheduler().addTask(neighbourId);
					}
				}
			}
		}
	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex, VertexIdTranslate trans) {
		/*for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int weight = vertex.outEdge(i).getValue();
			int sourceCommunity = status.getNodeToCommunity()[vertex.getId()];
			int targetCommunity = status.getNodeToCommunity()[target];
			status.getCommunities().add(sourceCommunity);
			status.getCommunities().add(targetCommunity);
			status.setTotalGraphWeight(status.getTotalGraphWeight() + 2*weight);
			if (sourceCommunity != targetCommunity) {
				int actualSourceCommunity = trans.backward(sourceCommunity);
				int actualTargetCommunity = trans.backward(targetCommunity);
				UndirectedEdge edge = new UndirectedEdge(actualSourceCommunity, actualTargetCommunity);
				status.getContractedGraph().put(edge, weight);
			}
		}*/
		
		//int communitySelfLoops = status.getCommunitySizeAtThisLevel()[vertex.getId()] - 1;
		//vertex.setValue(communitySelfLoops);
	}

	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			int noOfVertices = (int)ctx.getNumVertices();
			//status.setFromNodeCount(noOfVertices);
			nodeDegree = new int[noOfVertices];
			contracted = new boolean[noOfVertices];
		}
	}

	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0 && status.getHierarchyHeight() == 0) {
			status.setOriginalVertexTrans(ctx.getVertexIdTranslate());
			status.initialiseCommunitiesMap();
		}
		if (ctx.getIteration() == 0) {
			status.setUpdatedVertexTrans(ctx.getVertexIdTranslate());
		}

		if (twoCoreContractionStage && !ctx.getScheduler().hasTasks()) {
			twoCoreContractionStage = false;
			propagationStage = true;
			ctx.getScheduler().addAllTasks();
		} else if (!finalUpdate && !ctx.getScheduler().hasTasks()) {
			propagationStage = false;
			finalUpdate = true;
			twoCoreCompleted = true;
			ctx.getScheduler().addAllTasks();
			status.getModularities().put(status.getHierarchyHeight(), 0.);
			status.updateSizesMap();
			status.updateCommunitiesMap();
		}
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	public void run(String filename, int nShards, GraphStatus status) throws  Exception {
		this.status = status;
		FastSharder sharder = OrcaProgram.createSharder(filename, 1);
		sharder.shard(new FileInputStream(new File(filename)), "edgelist");
		GraphChiEngine<Integer, Integer> engine = new GraphChiEngine<Integer, Integer>(filename, 1);
		engine.setEdataConverter(new IntConverter());
		engine.setVertexDataConverter(new IntConverter());
		engine.setEnableScheduler(true);
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 1000);
	}

}