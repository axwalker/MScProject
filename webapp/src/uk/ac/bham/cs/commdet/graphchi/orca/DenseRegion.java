package uk.ac.bham.cs.commdet.graphchi.orca;

import java.io.File;
import java.io.FileInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.bham.cs.commdet.graphchi.all.Community;
import uk.ac.bham.cs.commdet.graphchi.all.GraphStatus;
import uk.ac.bham.cs.commdet.graphchi.all.Node;
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

	private double maxDistance = 1000;
	private double densityDegree = 2;
	private Map<Integer, Map<Integer, Path>> pathsWithinD;
	private Map<Integer, Neighbourhood> neighbourhoods;
	private PriorityQueue<Neighbourhood> denseRegionsRanked;
	private TreeSet<Neighbourhood> denseRegions;
	private GraphStatus status = new GraphStatus();
	private Map<UndirectedEdge, Integer> previousInterCommunityEdges;

	private static final int ADD_INITIAL = 0;
	private static final int SHORTEST_PATHS = 1;
	private static final int UPDATE_NEIGHBOURHOODS = 2;
	private static final int RANK_DENSE_REGIONS = 3;
	private static final int CONTRACT_SINGULAR_NODES = 4;
	private static final int ADD_TO_CONTRACTED = 5;

	@Override
	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		switch (context.getIteration()) {
		case ADD_INITIAL:
			addToInitialGraph(vertex, context.getVertexIdTranslate());
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
			contractSingleNode(vertex, context.getVertexIdTranslate());
			break;
		case ADD_TO_CONTRACTED:
			addToContractedGraph(vertex, context.getVertexIdTranslate());
			break;
		default:
			break;
		}
	}

	private void addToInitialGraph(ChiVertex<Integer, Integer> vertex, VertexIdTranslate trans) {
		Node node = new Node(vertex.getId());
		int internalDegree = 2 * vertex.getValue();
		int weightedDegree = internalDegree;
		int actualSource = trans.backward(vertex.getId());
		for (int i = 0; i < vertex.numEdges(); i++) {
			int actualTarget = trans.backward(vertex.edge(i).getVertexId());
			UndirectedEdge edge = new UndirectedEdge(actualSource, actualTarget);
			int edgeWeight = previousInterCommunityEdges.get(edge);
			weightedDegree += edgeWeight;
		}
		node.setSelfLoops(internalDegree);
		node.setWeightedDegree(weightedDegree);
		status.getNodes()[vertex.getId()] = node;

		Community community = new Community(vertex.getId());
		status.insertNodeIntoCommunity(node, community, 0);

		pathsWithinD.put(vertex.getId(), new HashMap<Integer, Path>());
		neighbourhoods.put(vertex.getId(), new Neighbourhood(vertex.getId()));
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
	}

	private void rankDenseRegion(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Neighbourhood neighbourhood = neighbourhoods.get(vertex.getId());
		if (denseRegions.add(neighbourhood)) {
			denseRegionsRanked.add(neighbourhood);
		}
	}

	private void assignNodesToCommunities(VertexIdTranslate trans) {
		Set<Integer> assignedNodes = new HashSet<Integer>();
		while(!denseRegionsRanked.isEmpty()) {
			Neighbourhood neighbourhood = denseRegionsRanked.poll();
			int seed = neighbourhood.getSeedNode();
			Community labelCommunity = status.getNodeToCommunity()[seed];
			//flag that seed node is already assigned elsewhere and new label is therefore needed
			boolean newSeedNeeded = assignedNodes.contains(seed);
			Set<Integer> neighbourhoodMembersAdded = new HashSet<Integer>();
			for (Integer member : neighbourhood.getMembersSeenCount().keySet()) {
				if (assignedNodes.add(member)) {
					//set label to first member if seed node was removed
					if (newSeedNeeded) {
						labelCommunity = status.getNodeToCommunity()[member];
						seed = member;
						newSeedNeeded = false;
					}
					neighbourhoodMembersAdded.add(seed);
					if (member != seed) {
						int noLinksToCommunity = 0;
						int actualSource = trans.backward(member);
						for (int target : neighbourhoodMembersAdded) {
							int actualTarget = trans.backward(target);
							UndirectedEdge edge = new UndirectedEdge(actualSource, actualTarget);
							int linkCount = previousInterCommunityEdges.containsKey(edge) 
									? previousInterCommunityEdges.get(edge)
									: 0;
							noLinksToCommunity += linkCount;
						}
						Node memberNode = status.getNodes()[member];
						status.insertNodeIntoCommunity(memberNode, labelCommunity, noLinksToCommunity);
					}
					neighbourhoodMembersAdded.add(member);
				}
			}
		}
	}

	private void contractSingleNode(ChiVertex<Integer, Integer> vertex, VertexIdTranslate trans) {

		Community currentCommunity = status.getNodeToCommunity()[vertex.getId()];
		Node node = status.getNodes()[vertex.getId()];

		if (currentCommunity.getLevelSize() == 1) {

			int actualSource = trans.backward(vertex.getId());
			Map<Community, Integer> neighbourCommunities = new HashMap<Community, Integer>();
			for (int i = 0; i < vertex.numEdges(); i++) {
				int targetId = vertex.edge(i).getVertexId();
				Community targetCommunity = status.getNodeToCommunity()[targetId];
				int actualTarget = trans.backward(targetId);
				UndirectedEdge previousEdge = new UndirectedEdge(actualSource, actualTarget);
				int linkCount = previousInterCommunityEdges.get(previousEdge);
				if (neighbourCommunities.containsKey(targetCommunity)) {
					int previousCount = neighbourCommunities.get(targetCommunity);
					neighbourCommunities.put(targetCommunity, linkCount + previousCount);
				} else {
					neighbourCommunities.put(targetCommunity, linkCount);
				}
			}

			status.removeNodeFromCommunity(node, currentCommunity, 0);
			
			Community bestCommunity = currentCommunity;
			int bestNoOfLinks = 0;
			double bestModularityGain = 0.;
			for (Map.Entry<Community, Integer> entry : neighbourCommunities.entrySet()) {	
				double gain = status.modularityGain(node, entry.getKey(), entry.getValue());
				if (gain > bestModularityGain || (gain == bestModularityGain && entry.getKey().equals(currentCommunity))) {
					bestCommunity = entry.getKey();
					bestNoOfLinks = entry.getValue();
					bestModularityGain = gain;
				}
			}

			status.insertNodeIntoCommunity(node, bestCommunity, bestNoOfLinks);
		}

	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex, VertexIdTranslate trans) {
		int actualSource = trans.backward(vertex.getId());
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int actualTarget = trans.backward(target);
			int sourceCommunityId = status.getNodeToCommunity()[vertex.getId()].getSeedNode();
			int targetCommunityId = status.getNodeToCommunity()[target].getSeedNode();
			int weight = vertex.outEdge(i).getValue();

			status.getUniqueCommunities().add(sourceCommunityId);
			status.getUniqueCommunities().add(targetCommunityId);

			int actualSourceCommunity = trans.backward(sourceCommunityId);
			int actualTargetCommunity = trans.backward(targetCommunityId);
			if (sourceCommunityId != targetCommunityId) {
				UndirectedEdge edge = new UndirectedEdge(actualSourceCommunity, actualTargetCommunity);
				if (status.getContractedGraph().containsKey(edge)) {
					int oldWeight = status.getContractedGraph().get(edge);
					status.getContractedGraph().put(edge, oldWeight + weight);
				} else {
					status.getContractedGraph().put(edge, weight);
				}
				UndirectedEdge previousEdge = new UndirectedEdge(actualSource, actualTarget);
				int edgeCount = previousInterCommunityEdges.get(previousEdge);
				status.addEdgeToInterCommunityEdges(edge, edgeCount);
			}
		}

		Community community = status.getNodeToCommunity()[vertex.getId()];
		int communitySelfLoops =  community.getInternalEdges();
		if (communitySelfLoops > 0) {
			int actualNode = trans.backward(community.getSeedNode());
			status.getContractedGraph().put(new UndirectedEdge(actualNode, actualNode), communitySelfLoops / 2);
		}
	}

	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == ADD_INITIAL) {
			int noOfVertices = (int)ctx.getNumVertices();
			status.setNodeToCommunity(new Community[noOfVertices]);
			status.setNodes(new Node[noOfVertices]);
			status.setUniqueCommunities(new HashSet<Integer>());
			previousInterCommunityEdges = status.getInterCommunityEdgeCounts();
			status.setInterCommunityEdgeCounts(new HashMap<UndirectedEdge, Integer>());

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
			assignNodesToCommunities(ctx.getVertexIdTranslate());
		} 
		if (ctx.getIteration() == CONTRACT_SINGULAR_NODES) {
			status.updateModularity(status.getHierarchyHeight());
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
			if (edge.getSource() == edge.getTarget()) {
				continue;
			}
			int interCommunityEdgeWeight = status.getContractedGraph().get(edge);
			Community sourceCommunity = status.getNodeToCommunity()[edge.getSource()];
			Community targetCommunity = status.getNodeToCommunity()[edge.getTarget()];
			int sourceSize = sourceCommunity.getLevelSize();
			int targetSize = targetCommunity.getLevelSize();
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
		FastSharder<Integer, Integer> sharder = OrcaProgram.createSharder(filename, 1);
		sharder.shard(new FileInputStream(new File(filename)), "edgelist");
		GraphChiEngine<Integer, Integer> engine = new GraphChiEngine<Integer, Integer>(filename, 1);
		engine.setEdataConverter(new IntConverter());
		engine.setVertexDataConverter(new IntConverter());
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 6);
	}

}