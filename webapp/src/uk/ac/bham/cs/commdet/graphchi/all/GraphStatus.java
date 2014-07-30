package uk.ac.bham.cs.commdet.graphchi.all;

import java.util.*;


import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

public class GraphStatus {
	
	private Community[] nodeToCommunityMap;
	private Node[] nodes;

	private long totalGraphWeight;
	private int hierarchyHeight = 0;
	private VertexIdTranslate originalVertexTrans;
	private VertexIdTranslate updatedVertexTrans;
	private Map<Integer, Double> modularities = new HashMap<Integer, Double>();
	private Map<Integer, List<Integer>> communityHierarchy = new HashMap<Integer, List<Integer>>();
	private Map<CommunityIdentity, Integer> allCommunitySizes = new HashMap<CommunityIdentity, Integer>();
	private HashMap<UndirectedEdge, Integer> contractedGraph = new HashMap<UndirectedEdge, Integer>();
	private Set<Integer> communities;

	public void initialiseCommunitiesMap() {
		for (int i = 0; i < nodeToCommunityMap.length; i++) {
			if (nodeToCommunityMap[i] != null) {
				int community = nodeToCommunityMap[i].getSeedNode();
				communityHierarchy.put(originalVertexTrans.backward(community), new ArrayList<Integer>());
			}
		}
	}

	public void updateCommunitiesMap() {
		if (hierarchyHeight == 0) {
			for (int i = 0; i < nodeToCommunityMap.length; i++) {
				if (communityHierarchy.containsKey(originalVertexTrans.backward(i))) {
					List<Integer> communities = communityHierarchy.get(originalVertexTrans.backward(i));
					communities.add(originalVertexTrans.backward(nodeToCommunityMap[i].getSeedNode()));
				}
			}
		} else {
			for (Map.Entry<Integer, List<Integer>> node : communityHierarchy.entrySet()) {
				int nodeId = node.getKey();
				List<Integer> communities = node.getValue();
				int currentGcId = updatedVertexTrans.forward(nodeId);
				if (nodeId < nodeToCommunityMap.length && nodeToCommunityMap[currentGcId] != null) {
					int latestCommunity = updatedVertexTrans.backward(nodeToCommunityMap[currentGcId].getSeedNode());
					communities.add(latestCommunity);
				} else {
					int previousCommunity = communities.get(hierarchyHeight - 1);
					int previousCommunityGcId = updatedVertexTrans.forward(previousCommunity);
					int latestCommunityGcId = nodeToCommunityMap[previousCommunityGcId].getSeedNode();
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
		for (int i = 0; i < nodeToCommunityMap.length; i++) {
			Community community = nodeToCommunityMap[i];
			if (community != null && !seenCommunites.contains(community) && community.getTotalSize() > 0) {
				allCommunitySizes.put(new CommunityIdentity(updatedVertexTrans.backward(community.getSeedNode()),
						hierarchyHeight), community.getTotalSize());
				seenCommunites.add(community);
			}
		}
	}

	/*public void insertNodeIntoCommunity(int nodeId, int communityId) {
		nodeToCommunity[nodeId] = communityId;
		if (hierarchyHeight == 0) {
			communitySize[communityId]++;
		} else {
			communitySize[communityId] += allCommunitySizes.get(new CommunityIdentity(updatedVertexTrans.backward(nodeId), hierarchyHeight - 1));
		}
		communitySizeAtThisLevel[communityId]++;
	}

	public void removeNodeFromCommunity(int nodeId, int communityId) {
		nodeToCommunity[nodeId] = -1;
		if (hierarchyHeight == 0) {
			communitySize[communityId]--;
		} else {
			communitySize[communityId] -= allCommunitySizes.get(new CommunityIdentity(updatedVertexTrans.backward(nodeId), hierarchyHeight - 1));
		}
		communitySizeAtThisLevel[communityId]--;
	}*/
	
	
	public void updateModularity(int level) {
		Set<Community> seenCommunites = new HashSet<Community>();
		double q = 0.;
		for (int i = 0; i < nodeToCommunityMap.length; i++) {
			Community community = nodeToCommunityMap[i];
			if (community != null && !seenCommunites.contains(community) && community.getTotalEdges() > 0) {
				q += (community.getInternalEdges() / (double)totalGraphWeight);
				q -= Math.pow(community.getTotalEdges() / (double)totalGraphWeight, 2);
				seenCommunites.add(community);
			}
		}
		modularities.put(level, q);
	}
	
	public int nodeSize(int node) {
		return allCommunitySizes.get(new CommunityIdentity(updatedVertexTrans.backward(node), hierarchyHeight - 1));
	}

	public int getNodeCount() {
		return communities.size();
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

	public Set<Integer> getCommunities() {
		return communities;
	}

	public void setCommunities(Set<Integer> communities) {
		this.communities = communities;
	}

	public Community[] getNodeToCommunityMap() {
		return nodeToCommunityMap;
	}

	public void setNodeToCommunityMap(Community[] nodeToCommunityMap) {
		this.nodeToCommunityMap = nodeToCommunityMap;
	}

}
