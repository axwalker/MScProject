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
	private Map<Integer, Neighbourhood> neighbourhoods;
	private PriorityQueue<Neighbourhood> denseRegionsRanked;
	private TreeSet<Neighbourhood> denseRegions;

	private OrcaGraphStatus status = new OrcaGraphStatus();
	private int initialNodeCount;
	private Set<Integer> communities;
	private boolean needsShortcuts;
	private Set<Integer> nodesAlreadyShortcutTo;

	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			addToInitialGraphStatus(vertex);
		}
		if (context.getIteration() == 1) {
			addImmediateShortestPaths(vertex, context);
		}
		if (context.getIteration() == 2) {
			updateNeighbourhoodMembers(vertex, context);
		}
		if (context.getIteration() == 3) {
			rankDenseRegion(vertex, context);
		}
		if (context.getIteration() == 4) {
			contractSingleNode(vertex, context);
		}
		if (context.getIteration() == 5) {
			if (needsShortcuts) {
				shortcut(vertex, context);
			}
		}
		if (context.getIteration() == 6) {
			addToContractedGraph(vertex, context.getVertexIdTranslate());
		}
	}

	private void contractSingleNode(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		/*if (status.getCommunitySizeAtThisLevel()[vertex.getId()] == 1) {
			int degree = 0;
			Set<Integer> visitedCommunities = new HashSet<Integer>();
			for (int i = 0; i < vertex.numEdges(); i++) {
				int targetId = vertex.edge(i).getVertexId();
				int targetCommunity = status.getNodeToCommunity()[targetId];
				if (visitedCommunities.add(targetCommunity)) {
					degree++;
				}
			}
			if (degree == 1) {
				int neighbourId = vertex.edge(0).getVertexId();
				int neighbourCommunity = status.getNodeToCommunity()[neighbourId];
				int currentCommunity = status.getNodeToCommunity()[vertex.getId()];
				status.removeNodeFromCommunity(vertex.getId(), currentCommunity);
				status.insertNodeIntoCommunity(vertex.getId(), neighbourCommunity);
			}

		}*/

		if (status.getCommunitySizeAtThisLevel()[vertex.getId()] == 1) {
			int currentCommunity = status.getNodeToCommunity()[vertex.getId()];

			if (nodesAlreadyShortcutTo.contains(currentCommunity)) {
				return;
			}

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
			int closestCommunity = -1;
			int closestWeight = -1;
			while(iterator.hasNext()) {
				int neighbourCommunity = iterator.next();
				if (neighbourCommunity != currentCommunity) {
					//nodesAlreadyShortcutTo.add(neighbourCommunity);
					int weight = visitedCommunities.get(neighbourCommunity);
					if (weight > closestWeight) {
						closestWeight = weight;
						closestCommunity = neighbourCommunity;
					}
				}
			}
			status.removeNodeFromCommunity(vertex.getId(), currentCommunity);
			status.insertNodeIntoCommunity(vertex.getId(), closestCommunity);
		}
		
		
		//build communities size
		if (status.getCommunitySizeAtThisLevel()[vertex.getId()] > 0) {
			communities.add(vertex.getId());
		}

	}

	private void shortcut(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		int degree = 0;
		int currentCommunity = status.getNodeToCommunity()[vertex.getId()];

		if (nodesAlreadyShortcutTo.contains(currentCommunity)) {
			return;
		}

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
					degree++;
					visitedCommunities.put(targetCommunity, weight);
				}
			}
		}
		if (degree == 2) {
			final Iterator<Integer> iterator = visitedCommunities.keySet().iterator();
			int closestCommunity = -1;
			int closestWeight = -1;
			while(iterator.hasNext()) {
				int neighbourCommunity = iterator.next();
				if (neighbourCommunity != currentCommunity) {
					//nodesAlreadyShortcutTo.add(neighbourCommunity);
					int weight = visitedCommunities.get(neighbourCommunity);
					if (weight > closestWeight) {
						closestWeight = weight;
						closestCommunity = neighbourCommunity;
					}
				}
			}
			status.removeNodeFromCommunity(vertex.getId(), currentCommunity);
			status.insertNodeIntoCommunity(vertex.getId(), closestCommunity);
		}
	}

	private void addToInitialGraphStatus(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		pathsWithinD.put(node, new HashMap<Integer, Path>());
		neighbourhoods.put(node, new Neighbourhood(node));
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

		System.out.println("**PRE: " + neighbourhood);

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

		System.out.println("post: " + neighbourhood);

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

		System.out.println("seed: " + neighbourhood.getSeedNode() + ", rank: " + neighbourhood.getRankValue() + ", totalWeight: " + neighbourhood.getTotalEdgeWeight() + "\n");
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
				Edge edge = new Edge(actualSourceCommunity, actualTargetCommunity);
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
		if (ctx.getIteration() == 0) {
			int noOfVertices = (int)ctx.getNumVertices();
			status.setFromNodeCount(noOfVertices);
			initialNodeCount = noOfVertices;
			communities = new HashSet<Integer>();
			nodesAlreadyShortcutTo = new HashSet<Integer>();

			pathsWithinD = new HashMap<Integer, Map<Integer, Path>>();
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
		if (ctx.getIteration() == 3) {
			assignNodesToCommunities();
		} 
		if (ctx.getIteration() == 4) {
			int nodeCount = communities.size();
			needsShortcuts = (nodeCount*1 > initialNodeCount) && (nodeCount > 2); 
			System.out.println("nodeCount = " + nodeCount);
		}
		if (ctx.getIteration() == 5) {
			status.getModularities().put(status.getHierarchyHeight(), -1.);
			status.updateSizesMap();
			status.updateCommunitiesMap();
		}
		if (ctx.getIteration() == 6) {
			setAverageAdjacencies();
			System.out.println(status.getCommunityHierarchy());
		}
	}

	private void setAverageAdjacencies() {
		final Iterator<Edge> iterator = status.getContractedGraph().keySet().iterator();
		while(iterator.hasNext()) {
			Edge edge = iterator.next();
			int interCommunityEdgeWeight = status.getContractedGraph().get(edge);
			int sourceSize = status.getCommunitySizeAtThisLevel()[edge.getNode1()];
			int targetSize = status.getCommunitySizeAtThisLevel()[edge.getNode2()];
			int averageAdjacency = interCommunityEdgeWeight / (sourceSize + targetSize);
			status.getContractedGraph().put(edge, averageAdjacency);
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
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 7);
	}

}