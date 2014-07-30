package uk.ac.bham.cs.commdet.graphchi.orca;

import java.io.File;
import java.io.FileInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

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

public class DenseRegion implements GraphChiProgram<Integer, Integer> {

	private double maxDistance = 100;
	private double densityDegree = 2;
	private Map<Integer, Map<Integer, Path>> pathsWithinD;
	private Map<Integer, Neighbourhood> neighbourhoods;
	private PriorityQueue<Neighbourhood> denseRegionsRanked;
	private TreeSet<Neighbourhood> denseRegions;
	private GraphStatus status = new GraphStatus();

	private static final int ADD_INITIAL = 0;
	private static final int SHORTEST_PATHS = 1;
	private static final int UPDATE_NEIGHBOURHOODS = 2;
	private static final int RANK_DENSE_REGIONS = 3;
	private static final int CONTRACT_SINGULAR_NODES = 4;
	private static final int ADD_TO_CONTRACTED = 5;


	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		switch (context.getIteration()) {
		case ADD_INITIAL:
			addToInitialGraph(vertex);
			break;
		case SHORTEST_PATHS:
			addImmediateShortestPaths(vertex, context);
			break;
		case UPDATE_NEIGHBOURHOODS:
			updateNeighbourhoodMembers(vertex, context);
			break;
		case RANK_DENSE_REGIONS:
			rankDenseRegion(vertex, context);
			break;
		case CONTRACT_SINGULAR_NODES:
			contractSingleNode(vertex, context);
			break;
		case ADD_TO_CONTRACTED:
			addToContractedGraph(vertex, context.getVertexIdTranslate());
			break;
		default:
			break;
		}
	}

	private void addToInitialGraph(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		pathsWithinD.put(node, new HashMap<Integer, Path>());
		neighbourhoods.put(node, new Neighbourhood(node));
		//int nodeInternalDegree = 2 * vertex.getValue();
		//status.getCommunityInternalEdges()[node] = nodeInternalDegree;
		//status.getNodeWeightedDegree()[node] = nodeInternalDegree;
		//status.getCommunityTotalEdges()[node] = nodeInternalDegree + vertex.numEdges();
	}

	private void addImmediateShortestPaths(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Map<Integer, Path> thisPaths = new HashMap<Integer, Path>();
		for (int i = 0; i < vertex.numEdges(); i++) {
			int edgeWeight = vertex.edge(i).getValue();
			if (edgeWeight <= maxDistance) {
				int targetId = vertex.edge(i).getVertexId();
				Path path = new Path(vertex.getId(), targetId, edgeWeight);
				path.setAdjacent(true);
				thisPaths.put(targetId, path);
				neighbourhoods.get(targetId).addMember(vertex.getId());
			}
		}
		pathsWithinD.put(vertex.getId(), thisPaths);
	}

	private void updateNeighbourhoodMembers(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Neighbourhood neighbourhood = neighbourhoods.get(vertex.getId());

		for (Integer member : neighbourhood.getMembersSeenCount().keySet()) {
			for (int target : pathsWithinD.get(member).keySet()) {
				neighbourhood.incrementMembersSeenCount(target);
			}
		}

		//System.out.println("**PRE: " + neighbourhood);

		int neighbourhoodSize = neighbourhood.getMembersSeenCount().size() + 1;
		double seenCountRequirement = (neighbourhoodSize / densityDegree);

		final Iterator<Integer> iterator = neighbourhood.getMembersSeenCount().keySet().iterator();
		while(iterator.hasNext()) {
			int member = iterator.next();
			int seenCount = neighbourhood.getMembersSeenCount().get(member);
			if (seenCount < seenCountRequirement) {
				iterator.remove();
			}
		}

		neighbourhood.addMember(vertex.getId());

		//System.out.println("post: " + neighbourhood);

		int doubleTotalEdgeWeight = 0;
		Set<Integer> members = neighbourhood.getMembersSeenCount().keySet();
		for (Integer member : members) {
			for (Map.Entry<Integer, Path> entry : pathsWithinD.get(member).entrySet()) {
				int target = entry.getKey();
				Path path = entry.getValue();
				if (members.contains(target) && path.isAdjacent()) {
					doubleTotalEdgeWeight += path.getWeight();
					neighbourhood.setEdgeCount(neighbourhood.getEdgeCount() + 1);
				}
			}
		}
		neighbourhood.setTotalEdgeWeight(doubleTotalEdgeWeight / 2);
		neighbourhood.setEdgeCount(neighbourhood.getEdgeCount() / 2);

		//System.out.println("seed: " + neighbourhood.getSeedNode() + 
		//		", PrimarRank: " + neighbourhood.getPrimaryRankValue() + ", secondaryRank: " + neighbourhood.getSecondaryRankValue() +
		//		", totalWeight: " + neighbourhood.getTotalEdgeWeight() + "\n");

	}

	private void rankDenseRegion(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Neighbourhood neighbourhood = neighbourhoods.get(vertex.getId());
		if (denseRegions.add(neighbourhood)) {
			denseRegionsRanked.add(neighbourhood);
		}
	}

	private void assignNodesToCommunities() {
		while(!denseRegionsRanked.isEmpty()) {
			Neighbourhood neighbourhood = denseRegionsRanked.poll();
			int label = neighbourhood.getSeedNode();
			//flag that seed node is already assigned elsewhere and new label is therefore needed
			label = status.getNodeToCommunity()[label] == -1 ? label : -1;
			for (Integer member : neighbourhood.getMembersSeenCount().keySet()) {
				if (status.getNodeToCommunity()[member] == -1) {
					//set label to first member if seed node was removed
					label = (label == -1) ? member : label; 
					status.insertNodeIntoCommunity(member, label);
				}
			}
		}
	}

	private void contractSingleNode(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		//*
		if (status.getCommunitySizeAtThisLevel()[vertex.getId()] == 1) {

			//System.out.println("!!!!!!CONTRACTING NODE: " + vertex.getId());

			int currentCommunity = status.getNodeToCommunity()[vertex.getId()];

			Map<Integer, Integer> visitedCommunities = new HashMap<Integer, Integer>();
			visitedCommunities.put(currentCommunity, 0);
			for (int i = 0; i < vertex.numEdges(); i++) {
				int targetId = vertex.edge(i).getVertexId();
				int weight = vertex.edge(i).getValue();
				int targetCommunity = status.getNodeToCommunity()[targetId];
				if (targetCommunity != currentCommunity) {
					if (visitedCommunities.containsKey(targetCommunity)) {
						int previousWeight = visitedCommunities.get(targetCommunity);
						visitedCommunities.put(targetCommunity, weight + previousWeight);
					} else {
						visitedCommunities.put(targetCommunity, weight);
					}
				}
			}

			final Iterator<Integer> iterator = visitedCommunities.keySet().iterator();
			double closestWeight = Integer.MIN_VALUE;
			int closestCommunity = -1;
			while(iterator.hasNext()) {
				int neighbourCommunity = iterator.next();
				if (neighbourCommunity != currentCommunity) {
					int weight = visitedCommunities.get(neighbourCommunity);
					int targetSize = status.getCommunitySizeAtThisLevel()[neighbourCommunity];
					double adjacencyRanking = weight / (0.05*targetSize);
					if (adjacencyRanking > closestWeight) {
						closestWeight = adjacencyRanking;
						closestCommunity = neighbourCommunity;
					}
				}
			}
			status.removeNodeFromCommunity(vertex.getId(), currentCommunity);
			status.insertNodeIntoCommunity(vertex.getId(), closestCommunity);
		}
		//*/

	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex, VertexIdTranslate trans) {
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int sourceCommunity = status.getNodeToCommunity()[vertex.getId()];
			int targetCommunity = status.getNodeToCommunity()[target];
			int weight = vertex.outEdge(i).getValue();

			status.getCommunities().add(sourceCommunity);
			status.getCommunities().add(targetCommunity);

			int actualSourceCommunity = trans.backward(sourceCommunity);
			int actualTargetCommunity = trans.backward(targetCommunity);
			if (sourceCommunity != targetCommunity) {
				UndirectedEdge edge = new UndirectedEdge(actualSourceCommunity, actualTargetCommunity);
				if (status.getContractedGraph().containsKey(edge)) {
					int oldWeight = status.getContractedGraph().get(edge);
					status.getContractedGraph().put(edge, oldWeight + weight);
				} else {
					status.getContractedGraph().put(edge, weight);
				}
			}
		}
	}

	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == ADD_INITIAL) {
			int noOfVertices = (int)ctx.getNumVertices();
			status.setFromNodeCount(noOfVertices);

			pathsWithinD = new HashMap<Integer, Map<Integer, Path>>();
			neighbourhoods = new HashMap<Integer, Neighbourhood>();
			denseRegionsRanked = new PriorityQueue<Neighbourhood>(noOfVertices, new Comparator<Neighbourhood>() {
				@Override
				public int compare(Neighbourhood o1, Neighbourhood o2) {
					int primaryRank = Double.compare(o2.getPrimaryRankValue(), o1.getPrimaryRankValue());
					if (primaryRank == 0) {
						return Double.compare(o2.getSecondaryRankValue(), o1.getSecondaryRankValue());
					} else {
						return primaryRank;
					}
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
		if (ctx.getIteration() == ADD_INITIAL) {
			status.setUpdatedVertexTrans(ctx.getVertexIdTranslate());
		}
		if (ctx.getIteration() == RANK_DENSE_REGIONS) {
			assignNodesToCommunities();
		} 
		if (ctx.getIteration() == CONTRACT_SINGULAR_NODES) {
			status.getModularities().put(status.getHierarchyHeight(), -1.);
			status.updateSizesMap();
			status.updateCommunitiesMap();
		}
		if (ctx.getIteration() == ADD_TO_CONTRACTED) {
			setAverageAdjacencies();
		}
	}

	private void setAverageAdjacencies() {
		final Iterator<UndirectedEdge> iterator = status.getContractedGraph().keySet().iterator();
		while(iterator.hasNext()) {
			UndirectedEdge edge = iterator.next();
			int interCommunityEdgeWeight = status.getContractedGraph().get(edge);
			int sourceSize = status.getCommunitySizeAtThisLevel()[edge.getSource()];
			int targetSize = status.getCommunitySizeAtThisLevel()[edge.getTarget()];
			int averageAdjacency = interCommunityEdgeWeight / (sourceSize + targetSize);
			status.getContractedGraph().put(edge, Math.max(1, averageAdjacency));
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
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 6);
	}

}