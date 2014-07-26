package uk.ac.bham.cs.commdet.graphchi.orca;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import uk.ac.bham.cs.commdet.graphchi.all.Edge;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

public class DenseRegion implements GraphChiProgram<Integer, Integer> {

	private double maxDistance = 100;
	private double densityDegree = 2;

	private Map<Integer, Map<Integer, Path>> pathsWithinD;
	private Map<Integer, Map<Integer, Path>> nextIterationPathsToPropagate;
	private Map<Integer, Map<Integer, Path>> currentIterationPathsToPropagate;
	private Map<Integer, Neighbourhood> neighbourhoods;
	private PriorityQueue<Neighbourhood> denseRegionsRanked;
	private TreeSet<Neighbourhood> denseRegions;

	private Map<Integer, Integer> commInternalEdgeTotal = new HashMap<Integer, Integer>();

	private boolean shortestPathsStage = true;
	private boolean propagateShortestPathsStage = true;
	private boolean neighbourhoodUpdateStage = true;
	private boolean rankingRegionStage = true;

	private boolean finalUpdate;
	private OrcaGraphStatus status = new OrcaGraphStatus();
	private VertexIdTranslate trans;


	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (!finalUpdate) {
			if (context.getIteration() == 0) {
				addToInitialGraphStatus(vertex);
				context.getScheduler().addTask(vertex.getId());
			} else if (shortestPathsStage) {
				addImmediateShortestPaths(vertex, context);
			} else if (propagateShortestPathsStage){
				propagateShortestPathsWithinMax(vertex, context);
			} else if (neighbourhoodUpdateStage) {
				updateNeighbourhoodMembers(vertex, context);
			} else if (rankingRegionStage) {
				rankDenseRegionUpdate(vertex, context);
			}
		} else {
			addToContractedGraph(vertex);
		}
	}

	private void addToInitialGraphStatus(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		pathsWithinD.put(node, new HashMap<Integer, Path>());
		neighbourhoods.put(node, new Neighbourhood(node));
	}

	private void addImmediateShortestPaths(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Map<Integer, Path> thisPaths = new HashMap<Integer, Path>();
		Map<Integer, Path> pathsToPropagate = new HashMap<Integer, Path>();
		boolean hasPathsToPropagate = false;
		for (int i = 0; i < vertex.numEdges(); i++) {
			int edgeWeight = vertex.edge(i).getValue();
			if (edgeWeight <= maxDistance) {
				int targetId = vertex.edge(i).getVertexId();
				Path path = new Path(vertex.getId(), targetId, edgeWeight);
				path.setAdjacent(true);
				thisPaths.put(targetId, path);
				neighbourhoods.get(targetId).addMember(vertex.getId());
				if (edgeWeight < maxDistance) {
					pathsToPropagate.put(targetId, path);
					hasPathsToPropagate = true;
				}
			}
		}
		pathsWithinD.put(vertex.getId(), thisPaths);

		if (hasPathsToPropagate) {
			for (int i = 0; i < vertex.numEdges(); i++) {
				int edgeWeight = vertex.edge(i).getValue();
				if (edgeWeight < maxDistance) {
					context.getScheduler().addTask(vertex.edge(i).getVertexId());
				}
			}
			nextIterationPathsToPropagate.put(vertex.getId(), pathsToPropagate);
		}
	}

	private void propagateShortestPathsWithinMax(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {	
		Map<Integer, Path> thisPaths = pathsWithinD.get(vertex.getId());
		Map<Integer, Path> pathsToPropagate = new HashMap<Integer, Path>();
		boolean hasPathsToPropagate = false;
		for (int i = 0; i < vertex.numEdges(); i++) {
			int edgeWeight = vertex.edge(i).getValue();
			if (edgeWeight < maxDistance) {
				int targetId = vertex.edge(i).getVertexId();
				Map<Integer, Path> propagatedPaths = currentIterationPathsToPropagate.get(targetId);
				if (propagatedPaths != null) {
					for (Map.Entry<Integer, Path> entry : propagatedPaths.entrySet()) {
						int target = entry.getKey();
						Path path = entry.getValue();
						if (target == vertex.getId()) {
							continue;
						} else {
							int totalPathWeight = path.getWeight() + edgeWeight;
							Path totalPath = new Path(vertex.getId(), target, totalPathWeight);
							if (thisPaths.containsKey(target)){
								Path currentPath = thisPaths.get(target);
								int currentPathWeight = currentPath.getWeight();
								if (totalPathWeight < currentPathWeight) {
									thisPaths.put(target, totalPath);
									neighbourhoods.get(target).addMember(vertex.getId());
									pathsToPropagate.put(target, totalPath);
									hasPathsToPropagate = true;
								}
							} else {
								if (totalPathWeight <= maxDistance) {
									thisPaths.put(target, totalPath);
									neighbourhoods.get(target).addMember(vertex.getId());
									pathsToPropagate.put(target, totalPath);
									hasPathsToPropagate = true;
								}
							}
						}
					}
				}
			}
		}

		if (hasPathsToPropagate) {
			for (int i = 0; i < vertex.numEdges(); i++) {
				int edgeWeight = vertex.edge(i).getValue();
				if (edgeWeight < maxDistance) {
					hasPathsToPropagate = true;
					context.getScheduler().addTask(vertex.edge(i).getVertexId());
				}
			}
			nextIterationPathsToPropagate.put(vertex.getId(), pathsToPropagate);
		}
	}

	private void updateNeighbourhoodMembers(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Neighbourhood neighbourhood = neighbourhoods.get(vertex.getId());

		for (Integer member : neighbourhood.getMembersSeenCount().keySet()) {
			for (int target : pathsWithinD.get(member).keySet()) {
				neighbourhood.incrementMembersSeenCount(target);
			}
		}

		System.out.println("**PRE: " + neighbourhood);

		int neighbourhoodSize = neighbourhood.getMembersSeenCount().size() + 1;
		double seenCountRequirement = (neighbourhoodSize / densityDegree);
		Set<Integer> membersToRemove = new HashSet<Integer>();

		for (Map.Entry<Integer, Integer> member : neighbourhood.getMembersSeenCount().entrySet()) {
			int memberId = member.getKey();
			int seenCount = member.getValue();
			if (seenCount < seenCountRequirement) {
				membersToRemove.add(memberId);
			}
		}

		for (int memberId : membersToRemove) {
			neighbourhood.getMembersSeenCount().remove(memberId);
		}

		neighbourhood.addMember(vertex.getId());

		System.out.println("post: " + neighbourhood);

		int doubleTotalEdgeWeight = 0;
		Set<Integer> members = neighbourhood.getMembersSeenCount().keySet();
		for (Integer member : members) {
			for (Map.Entry<Integer, Path> entry : pathsWithinD.get(member).entrySet()) {
				int target = entry.getKey();
				Path path = entry.getValue();
				if (members.contains(target) && path.isAdjacent()) {
					doubleTotalEdgeWeight += path.getWeight();
				}
			}
		}
		neighbourhood.setTotalEdgeWeight(doubleTotalEdgeWeight / 2);
		System.out.println("seed: " + neighbourhood.getSeedNode() + ", rank: " + neighbourhood.getRankValue() + ", totalWeight: " + neighbourhood.getTotalEdgeWeight() + "\n");

	}

	private void rankDenseRegionUpdate(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Neighbourhood neighbourhood = neighbourhoods.get(vertex.getId());
		if (denseRegions.add(neighbourhood)) {
			denseRegionsRanked.add(neighbourhood);
		}
	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex) {
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int sourceCommunity = status.getNodeToCommunity()[vertex.getId()];
			int targetCommunity = status.getNodeToCommunity()[target];
			int weight = vertex.outEdge(i).getValue();
			status.getCommunities().add(sourceCommunity);
			status.getCommunities().add(targetCommunity);
			if (sourceCommunity != targetCommunity) {
				int actualSourceCommunity = trans.backward(sourceCommunity);
				int actualTargetCommunity = trans.backward(targetCommunity);
				Edge edge = new Edge(actualSourceCommunity, actualTargetCommunity);
				if (status.getContractedGraph().containsKey(edge)) {
					int oldWeight = status.getContractedGraph().get(edge);
					status.getContractedGraph().put(edge, oldWeight + weight);
				} else {
					status.getContractedGraph().put(edge, weight);
				}
			} else {
				if (commInternalEdgeTotal.containsKey(sourceCommunity)) {
					int previousTotal = commInternalEdgeTotal.get(sourceCommunity);
					commInternalEdgeTotal.put(sourceCommunity, previousTotal + weight);
				} else {
					commInternalEdgeTotal.put(sourceCommunity, weight);
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
			status.setCommunities(new HashSet<Integer>());

			pathsWithinD = new HashMap<Integer, Map<Integer, Path>>();
			nextIterationPathsToPropagate = new HashMap<Integer, Map<Integer, Path>>();
			neighbourhoods = new HashMap<Integer, Neighbourhood>();
			denseRegionsRanked = new PriorityQueue<Neighbourhood>(noOfVertices, new Comparator<Neighbourhood>() {
				@Override
				public int compare(Neighbourhood o1, Neighbourhood o2) {
					return Double.compare(o2.getRankValue(), o1.getRankValue());
				}
			});
			denseRegions = new TreeSet<Neighbourhood>(new Comparator<Neighbourhood>() {
				@Override
				public int compare(Neighbourhood arg0, Neighbourhood arg1) {
					if (arg0.getMembersSeenCount().keySet().equals(arg1.getMembersSeenCount().keySet())) {
						return 0;
					} else {
						return arg0.getSeedNode() - arg1.getSeedNode(); 
					}
				}
			});
		}
	}

	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			status.setUpdatedVertexTrans(ctx.getVertexIdTranslate());
		}
		if ((shortestPathsStage || propagateShortestPathsStage) && ctx.getIteration() > 0) {
			shortestPathsStage = false;
			currentIterationPathsToPropagate = nextIterationPathsToPropagate;
			nextIterationPathsToPropagate = new HashMap<Integer, Map<Integer, Path>>();
			if (!ctx.getScheduler().hasTasks()) {
				propagateShortestPathsStage = false;
				ctx.getScheduler().addAllTasks();
			}
		} else if (neighbourhoodUpdateStage && !ctx.getScheduler().hasTasks()) {
			neighbourhoodUpdateStage = false;
			rankingRegionStage = true;
			ctx.getScheduler().addAllTasks();
		} else if (rankingRegionStage && !ctx.getScheduler().hasTasks()) {
			System.out.println("ranked regions: " + denseRegionsRanked);
			rankingRegionStage = false;
			assignNodesToCommunities();
			finalUpdate = true;
			ctx.getScheduler().addAllTasks();
			status.getModularities().put(status.getHierarchyHeight(), -1.);
			status.updateSizesMap();
			status.updateCommunitiesMap();
		} else {
			fixContractedGraphEdges();
		}
	}

	private void assignNodesToCommunities() {
		while(!denseRegionsRanked.isEmpty()) {
			Neighbourhood neighbourhood = denseRegionsRanked.poll();
			int label = neighbourhood.getSeedNode();
			for (Integer member : neighbourhood.getMembersSeenCount().keySet()) {
				if (status.getNodeToCommunity()[member] == -1) {
					status.insertNodeIntoCommunity(member, label);
				}
			}
		}
	}

	private void fixContractedGraphEdges() {
		final Iterator<Edge> iterator = status.getContractedGraph().keySet().iterator();
		while(iterator.hasNext()) {
			Edge edge = iterator.next();
			int interCommunityEdges = status.getContractedGraph().get(edge);
			Integer sourceInternal = commInternalEdgeTotal.get(edge.getNode1());
			sourceInternal = sourceInternal != null ? sourceInternal : 0;
			Integer targetInternal = commInternalEdgeTotal.get(edge.getNode2());
			targetInternal = targetInternal != null ? targetInternal : 0;
			double averageAdjacency = (double)interCommunityEdges / (0.75*(sourceInternal + targetInternal) + interCommunityEdges);
			status.getContractedGraph().put(edge, (int)(averageAdjacency * 100));
		}
	}

	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

	public void run(String filename, int nShards, OrcaGraphStatus status) throws  Exception {
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