package uk.ac.bham.cs.commdet.graphchi.all;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

public class GraphStatus {
	
	private Community[] nodeToCommunity;
	private Node[] nodes;
	private long totalGraphWeight;
	private int hierarchyHeight = 0;
	private VertexIdTranslate originalVertexTrans;
	private VertexIdTranslate updatedVertexTrans;
	private Map<Integer, Double> modularities = new HashMap<Integer, Double>();
	private Map<Integer, List<Integer>> communityHierarchy = new HashMap<Integer, List<Integer>>();
	private Map<CommunityIdentity, Integer> allCommunitySizes = new HashMap<CommunityIdentity, Integer>();
	private HashMap<UndirectedEdge, Integer> contractedGraph = new HashMap<UndirectedEdge, Integer>();
	private Set<Integer> uniqueCommunities;
	private Map<UndirectedEdge, Integer> interCommunityEdgeCounts;

	public void initialiseCommunitiesMap() {
		for (int i = 0; i < nodeToCommunity.length; i++) {
			if (nodeToCommunity[i] != null) {
				int community = nodeToCommunity[i].getSeedNode();
				communityHierarchy.put(originalVertexTrans.backward(community), new ArrayList<Integer>());
			}
		}
	}

	public void updateCommunitiesMap() {
		if (hierarchyHeight == 0) {
			for (int i = 0; i < nodeToCommunity.length; i++) {
				if (communityHierarchy.containsKey(originalVertexTrans.backward(i))) {
					List<Integer> communities = communityHierarchy.get(originalVertexTrans.backward(i));
					communities.add(originalVertexTrans.backward(nodeToCommunity[i].getSeedNode()));
				}
			}
		} else {
			for (Map.Entry<Integer, List<Integer>> node : communityHierarchy.entrySet()) {
				int nodeId = node.getKey();
				List<Integer> communities = node.getValue();
				int currentGcId = updatedVertexTrans.forward(nodeId);
				if (nodeId < nodeToCommunity.length && nodeToCommunity[currentGcId] != null) {
					int latestCommunity = updatedVertexTrans.backward(nodeToCommunity[currentGcId].getSeedNode());
					communities.add(latestCommunity);
				} else {
					int previousCommunity = communities.get(hierarchyHeight - 1);
					int previousCommunityGcId = updatedVertexTrans.forward(previousCommunity);
					int latestCommunityGcId = nodeToCommunity[previousCommunityGcId].getSeedNode();
					if (updatedVertexTrans.backward(latestCommunityGcId) == -1) {
						System.out.println("This is not a connected graph: " + previousCommunityGcId);
						// this happens when graph is not connected, resulting in single disjoint communities
						communities.add(previousCommunity);
					} else {
						communities.add(updatedVertexTrans.backward(latestCommunityGcId));
					}
				}
			}
		}
	}

	public void updateSizesMap() {
		Set<Community> seenCommunites = new HashSet<Community>();
		for (int i = 0; i < nodeToCommunity.length; i++) {
			Community community = nodeToCommunity[i];
			if (community != null && !seenCommunites.contains(community) && community.getTotalSize() > 0) {
				allCommunitySizes.put(new CommunityIdentity(updatedVertexTrans.backward(community.getSeedNode()),
						hierarchyHeight), community.getTotalSize());
				seenCommunites.add(community);
			}
		}
	}
	
	public void removeNodeFromCommunity(Node node, Community community, int noNodeLinksToComm) {
		nodeToCommunity[node.getId()] = community;
		if (hierarchyHeight == 0) {
			community.decreaseTotalSize(1);
		} else {
			int nodeSize = allCommunitySizes.get(new CommunityIdentity(updatedVertexTrans.backward(node.getId()), hierarchyHeight - 1));
			community.decreaseTotalSize(nodeSize);
		}
		community.decreaseLevelSize(1);
		
		community.setTotalEdges(community.getTotalEdges() - node.getWeightedDegree());
		community.setInternalEdges(community.getInternalEdges() - (2*noNodeLinksToComm + node.getSelfLoops()));
	}

	public void insertNodeIntoCommunity(Node node, Community community, int noNodeLinksToComm) {
		nodeToCommunity[node.getId()] = community;
		if (hierarchyHeight == 0) {
			community.increaseTotalSize(1);
		} else {
			int nodeSize = allCommunitySizes.get(new CommunityIdentity(updatedVertexTrans.backward(node.getId()), hierarchyHeight - 1));
			community.increaseTotalSize(nodeSize);
		}
		community.increaseLevelSize(1);
		
		community.setTotalEdges(community.getTotalEdges() + node.getWeightedDegree());
		community.setInternalEdges(community.getInternalEdges() + (2*noNodeLinksToComm + node.getSelfLoops()));
	}
	
	public double modularityGain(Node node, Community community, int noNodeLinksToComm) {
		double totc = (double)community.getTotalEdges();
		double degc = (double)node.getWeightedDegree();
		double m2 = (double)totalGraphWeight;
		double dnc = (double)noNodeLinksToComm;

		return (dnc - (totc*degc)/m2) / (m2/2); 
	}
	
	public void updateModularity(int level) {
		Set<Community> seenCommunites = new HashSet<Community>();
		double q = 0.;
		for (int i = 0; i < nodeToCommunity.length; i++) {
			Community community = nodeToCommunity[i];
			if (community != null && !seenCommunites.contains(community) && community.getTotalEdges() > 0) {
				q += (community.getInternalEdges() / (double)totalGraphWeight);
				q -= Math.pow(community.getTotalEdges() / (double)totalGraphWeight, 2);
				seenCommunites.add(community);
			}
		}
		modularities.put(level, q);
	}
	
	public void addEdgeToContractedGraph(UndirectedEdge edge, int weight) {
		if (contractedGraph.containsKey(edge)) {
			int oldWeight = contractedGraph.get(edge);
			contractedGraph.put(edge, oldWeight + weight);
		} else {
			contractedGraph.put(edge, weight);
		}
		
	}

	public void addEdgeToInterCommunityEdges(UndirectedEdge edge, int edgeCount) {
		if (interCommunityEdgeCounts.containsKey(edge)) {
			int oldCount = interCommunityEdgeCounts.get(edge);
			interCommunityEdgeCounts.put(edge, oldCount + edgeCount);
		} else {
			interCommunityEdgeCounts.put(edge, edgeCount);
		}
		
	}
	
	public int getNodeCount() {
		return uniqueCommunities.size();
	}
	
	public void increaseTotalGraphWeight(int increase) {
		this.totalGraphWeight += increase;
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

	public Map<CommunityIdentity, Integer> getCommunitySizes() {
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

	public long getTotalGraphWeight() {
		return totalGraphWeight;
	}

	public void setTotalGraphWeight(long totalGraphWeight) {
		this.totalGraphWeight = totalGraphWeight;
	}

	public HashMap<UndirectedEdge, Integer> getContractedGraph() {
		return contractedGraph;
	}

	public void setContractedGraph(HashMap<UndirectedEdge, Integer> contractedGraph) {
		this.contractedGraph = contractedGraph;
	}

	public Set<Integer> getUniqueCommunities() {
		return uniqueCommunities;
	}

	public void setUniqueCommunities(Set<Integer> communities) {
		this.uniqueCommunities = communities;
	}

	public Community[] getNodeToCommunity() {
		return nodeToCommunity;
	}

	public void setNodeToCommunity(Community[] nodeToCommunity) {
		this.nodeToCommunity = nodeToCommunity;
	}

	public Map<UndirectedEdge, Integer> getInterCommunityEdgeCounts() {
		return interCommunityEdgeCounts;
	}

	public void setInterCommunityEdgeCounts(Map<UndirectedEdge, Integer> interCommunityEdgeCounts) {
		this.interCommunityEdgeCounts = interCommunityEdgeCounts;
	}

}
