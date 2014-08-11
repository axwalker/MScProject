package uk.ac.bham.cs.commdet.graphchi.orca;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;

import uk.ac.bham.cs.commdet.graphchi.all.Community;
import uk.ac.bham.cs.commdet.graphchi.all.GraphStatus;
import uk.ac.bham.cs.commdet.graphchi.all.Node;
import uk.ac.bham.cs.commdet.graphchi.all.UndirectedEdge;
import edu.cmu.graphchi.ChiVertex;
import edu.cmu.graphchi.GraphChiContext;
import edu.cmu.graphchi.GraphChiProgram;
import edu.cmu.graphchi.datablocks.FloatConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

/**
 * Reduce a graph to its two core, with single degree nodes contracted to their
 * neighbouring node.
 */
public class TwoCore implements GraphChiProgram<Float, Float> {

	private boolean twoCoreCompleted;
	private boolean twoCoreContractionStage = true;
	private boolean propagationStage;
	private int nodeDegree[];
	private boolean contracted[];

	private boolean finalUpdate;
	private GraphStatus status = new GraphStatus();

	@Override
	public synchronized void update(ChiVertex<Float, Float> vertex, GraphChiContext context) {
		if (!twoCoreCompleted && !finalUpdate) {
			twoCoreUpdate(vertex, context);
		} else {
			addToContractedGraph(vertex, context.getVertexIdTranslate());
		}
	}

	private void twoCoreUpdate(ChiVertex<Float, Float> vertex, GraphChiContext context) {
		if (twoCoreContractionStage) {
			int degree;
			if (context.getIteration() == 0) {
				degree = vertex.numEdges();
				nodeDegree[vertex.getId()] = degree;
				Node node = new Node(vertex.getId());
				Community community = new Community(vertex.getId());
				status.getNodes()[vertex.getId()] = node;
				status.insertNodeIntoCommunity(node, community, 0);
				context.getScheduler().addTask(vertex.getId());
			} else {
				degree = nodeDegree[vertex.getId()];
				if (degree < 2 && !contracted[vertex.getId()]) {
					contracted[vertex.getId()] = true;
					for (int i = 0; i < vertex.numEdges(); i++) {
						int neighbourId = vertex.edge(i).getVertexId();
						if (!contracted[neighbourId]) {
							nodeDegree[neighbourId]--;
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
						double weight = vertex.edge(i).getValue();
						Node neighbour = status.getNodes()[neighbourId];
						Community neighbourCommunity = status.getCommunities()[neighbourId];
						Community currentCommunity = status.getCommunities()[vertex.getId()];
						status.removeNodeFromCommunity(neighbour, neighbourCommunity, 0);
						status.insertNodeIntoCommunity(neighbour, currentCommunity, weight);
						contracted[neighbourId] = false; // to propagate up the
															// branch
						context.getScheduler().addTask(neighbourId);
					}
				}
			}
		}
	}

	private void addToContractedGraph(ChiVertex<Float, Float> vertex, VertexIdTranslate trans) {
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			double weight = vertex.outEdge(i).getValue();
			int sourceCommunityId = status.getCommunities()[vertex.getId()].getSeedNode();
			int targetCommunityId = status.getCommunities()[target].getSeedNode();
			status.getUniqueCommunities().add(sourceCommunityId);
			status.getUniqueCommunities().add(targetCommunityId);
			status.setTotalGraphWeight(status.getTotalGraphWeight() + 2 * weight);
			if (sourceCommunityId != targetCommunityId) {
				int actualSourceCommunity = trans.backward(sourceCommunityId);
				int actualTargetCommunity = trans.backward(targetCommunityId);
				UndirectedEdge edge = new UndirectedEdge(actualSourceCommunity,
						actualTargetCommunity);
				status.getContractedGraph().put(edge, weight);
				status.addEdgeToInterCommunityEdges(edge, weight);
			}
		}

		Community community = status.getCommunities()[vertex.getId()];
		if (community.getInternalEdges() > 0) {
			int actualNode = trans.backward(community.getSeedNode());
			status.getContractedGraph().put(new UndirectedEdge(actualNode, actualNode),
					community.getInternalEdges() / 2);
		}
	}

	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			int noOfVertices = (int) ctx.getNumVertices();
			status.setCommunities(new Community[noOfVertices]);
			status.setNodes(new Node[noOfVertices]);
			status.setUniqueCommunities(new HashSet<Integer>());
			nodeDegree = new int[noOfVertices];
			contracted = new boolean[noOfVertices];
			status.setInterCommunityEdgeCounts(new HashMap<UndirectedEdge, Double>());
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
			status.updateCommunitiesMap();
			status.updateSizesMap();
		}
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {
	}

	public void endInterval(GraphChiContext ctx, VertexInterval interval) {
	}

	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {
	}

	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {
	}

	public void run(String filename, int nShards, GraphStatus status) throws Exception {
		this.status = status;
		FastSharder<Float, Float> sharder = OrcaProgram.createSharder(filename, nShards);
		sharder.shard(new FileInputStream(new File(filename)), "edgelist");
		GraphChiEngine<Float, Float> engine = new GraphChiEngine<Float, Float>(filename, nShards);
		engine.setEdataConverter(new FloatConverter());
		engine.setVertexDataConverter(new FloatConverter());
		engine.setEnableScheduler(true);
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 1000);
	}

}