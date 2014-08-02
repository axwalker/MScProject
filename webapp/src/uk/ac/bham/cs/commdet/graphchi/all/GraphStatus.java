package uk.ac.bham.cs.commdet.graphchi.all;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

/**
 * Holds all information about a graph and its community structure and
 * hierarchy. Used by a DetectionProgram to keep track of the current state of
 * the graph, its communities and membership of each node to a community at each
 * level of the hierarchy so far.
 */
public class GraphStatus {
	
	private Community[] communities;
	private Node[] nodes;
	private double totalGraphWeight;
	private int hierarchyHeight = 0;
	private VertexIdTranslate originalVertexTrans;
	private VertexIdTranslate updatedVertexTrans;
	private Map<Integer, Double> modularities = new HashMap<Integer, Double>();
	private Map<Integer, List<Integer>> communityHierarchy = new HashMap<Integer, List<Integer>>();
	private Map<CommunityID, Integer> allCommunitySizes = new HashMap<CommunityID, Integer>();
	private HashMap<UndirectedEdge, Double> contractedGraph = new HashMap<UndirectedEdge, Double>();
	private Set<Integer> uniqueCommunities;
	private Map<UndirectedEdge, Double> interCommunityEdgeCounts;

	/**
	 * Add each node in an initial input graph to the community hierarchy map
	 */
	public void initialiseCommunitiesMap() {
		for (int i = 0; i < communities.length; i++) {
			if (communities[i] != null) {
				int community = communities[i].getSeedNode();
				communityHierarchy.put(originalVertexTrans.backward(community), new ArrayList<Integer>());
			}
		}
	}

	/**
	 * Update community hierarchy map once communities for each node have been determined
	 */
	public void updateCommunitiesMap() {
		if (hierarchyHeight == 0) {
			for (int i = 0; i < communities.length; i++) {
				if (communityHierarchy.containsKey(originalVertexTrans.backward(i))) {
					List<Integer> nodeCommunities = communityHierarchy.get(originalVertexTrans.backward(i));
					nodeCommunities.add(originalVertexTrans.backward(communities[i].getSeedNode()));
				}
			}
		} else {
			for (Map.Entry<Integer, List<Integer>> node : communityHierarchy.entrySet()) {
				int nodeId = node.getKey();
				List<Integer> nodeCommunities = node.getValue();
				int currentGcId = updatedVertexTrans.forward(nodeId);
				if (nodeId < communities.length && communities[currentGcId] != null) {
					int latestCommunity = updatedVertexTrans.backward(communities[currentGcId].getSeedNode());
					nodeCommunities.add(latestCommunity);
				} else {
					int previousCommunity = nodeCommunities.get(hierarchyHeight - 1);
					int previousCommunityGcId = updatedVertexTrans.forward(previousCommunity);
					int latestCommunityGcId = communities[previousCommunityGcId].getSeedNode();
					if (updatedVertexTrans.backward(latestCommunityGcId) == -1) {
						System.out.println("This is not a connected graph: " + previousCommunityGcId);
						// this happens when graph is not connected, resulting in single disjoint communities
						nodeCommunities.add(previousCommunity);
					} else {
						nodeCommunities.add(updatedVertexTrans.backward(latestCommunityGcId));
					}
				}
			}
		}
	}

	/**
	 * Update how many individual vertices belong to each community once communities have been determined
	 */
	public void updateSizesMap() {
		Set<Community> seenCommunites = new HashSet<Community>();
		for (int i = 0; i < communities.length; i++) {
			Community community = communities[i];
			if (community != null && !seenCommunites.contains(community) && community.getTotalSize() > 0) {
				allCommunitySizes.put(new CommunityID(updatedVertexTrans.backward(community.getSeedNode()),
						hierarchyHeight), community.getTotalSize());
				seenCommunites.add(community);
			}
		}
	}
	
	public void removeNodeFromCommunity(Node node, Community community, double noNodeLinksToComm) {
		communities[node.getId()] = community;
		if (hierarchyHeight == 0) {
			community.decreaseTotalSize(1);
		} else {
			int nodeSize = allCommunitySizes.get(new CommunityID(updatedVertexTrans.backward(node.getId()), hierarchyHeight - 1));
			community.decreaseTotalSize(nodeSize);
		}
		community.decreaseLevelSize(1);
		
		community.setTotalEdges(community.getTotalEdges() - node.getWeightedDegree());
		community.setInternalEdges(community.getInternalEdges() - (2*noNodeLinksToComm + node.getSelfLoops()));
	}

	public void insertNodeIntoCommunity(Node node, Community community, double noNodeLinksToComm) {
		communities[node.getId()] = community;
		if (hierarchyHeight == 0) {
			community.increaseTotalSize(1);
		} else {
			int nodeSize = allCommunitySizes.get(new CommunityID(updatedVertexTrans.backward(node.getId()), hierarchyHeight - 1));
			community.increaseTotalSize(nodeSize);
		}
		community.increaseLevelSize(1);
		
		community.setTotalEdges(community.getTotalEdges() + node.getWeightedDegree());
		community.setInternalEdges(community.getInternalEdges() + (2*noNodeLinksToComm + node.getSelfLoops()));
	}
	
	public double modularityGain(Node node, Community community, double noNodeLinksToComm) {
		double totc = community.getTotalEdges();
		double degc = node.getWeightedDegree();
		double m2 = totalGraphWeight;
		double dnc = noNodeLinksToComm;

		return (dnc - (totc*degc)/m2) / (m2/2); 
	}
	
	public void updateModularity(int level) {
		Set<Community> seenCommunites = new HashSet<Community>();
		double q = 0.;
		for (int i = 0; i < communities.length; i++) {
			Community community = communities[i];
			if (community != null && !seenCommunites.contains(community) && community.getTotalEdges() > 0) {
				q += (community.getInternalEdges() / (double)totalGraphWeight);
				q -= Math.pow(community.getTotalEdges() / (double)totalGraphWeight, 2);
				seenCommunites.add(community);
			}
		}
		modularities.put(level, q);
	}
	
	public void addEdgeToContractedGraph(UndirectedEdge edge, double weight) {
		if (contractedGraph.containsKey(edge)) {
			double oldWeight = contractedGraph.get(edge);
			contractedGraph.put(edge, oldWeight + weight);
		} else {
			contractedGraph.put(edge, weight);
		}
		
	}

	public void addEdgeToInterCommunityEdges(UndirectedEdge edge, double edgeCount) {
		if (interCommunityEdgeCounts.containsKey(edge)) {
			double oldCount = interCommunityEdgeCounts.get(edge);
			interCommunityEdgeCounts.put(edge, oldCount + edgeCount);
		} else {
			interCommunityEdgeCounts.put(edge, edgeCount);
		}
		
	}
	
	public int getNodeCount() {
		return uniqueCommunities.size();
	}
	
	public void increaseTotalGraphWeight(double increase) {
		this.totalGraphWeight += increase;
	}
	
	public Community[] getCommunities() {
		return communities;
	}

	public void setCommunities(Community[] communities) {
		this.communities = communities;
	}
	
	public Node[] getNodes() {
		return nodes;
	}

	public void setNodes(Node[] nodes) {
		this.nodes = nodes;
	}
	
	public Map<Integer, Double> getModularities() {
		return modularities;
	}

	public Map<Integer, List<Integer>> getCommunityHierarchy() {
		return communityHierarchy;
	}

	public Map<CommunityID, Integer> getCommunitySizes() {
		return allCommunitySizes;
	}

	public void setOriginalVertexTrans(VertexIdTranslate trans) {
		this.originalVertexTrans = trans;
	}

	public void setUpdatedVertexTrans(VertexIdTranslate trans) {
		this.updatedVertexTrans = trans;
	}

	public void incrementHeight() {
		hierarchyHeight++;
	}
	
	public int getHierarchyHeight() {
		return hierarchyHeight;
	}

	public double getTotalGraphWeight() {
		return totalGraphWeight;
	}

	public void setTotalGraphWeight(double totalGraphWeight) {
		this.totalGraphWeight = totalGraphWeight;
	}

	public HashMap<UndirectedEdge, Double> getContractedGraph() {
		return contractedGraph;
	}

	public void setContractedGraph(HashMap<UndirectedEdge, Double> contractedGraph) {
		this.contractedGraph = contractedGraph;
	}

	public Set<Integer> getUniqueCommunities() {
		return uniqueCommunities;
	}

	public void setUniqueCommunities(Set<Integer> communities) {
		this.uniqueCommunities = communities;
	}

	public Map<UndirectedEdge, Double> getInterCommunityEdgeCounts() {
		return interCommunityEdgeCounts;
	}

	public void setInterCommunityEdgeCounts(Map<UndirectedEdge, Double> interCommunityEdgeCounts) {
		this.interCommunityEdgeCounts = interCommunityEdgeCounts;
	}

}
