package uk.ac.bham.cs.commdet.graphchi.all;

import java.util.*;


import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

public class GraphStatus {

	private int[] nodeToCommunity;
	private int[] communityInternalEdges;
	private int[] communityTotalEdges;
	private int[] nodeWeightedDegree;
	private int[] nodeSelfLoops;
	private int[] communitySize;
	private int[] communitySizeAtThisLevel;
	private long totalGraphWeight;
	private int hierarchyHeight = 0;
	private VertexIdTranslate originalVertexTrans;
	private VertexIdTranslate updatedVertexTrans;
	private Map<Integer, Double> modularities = new HashMap<Integer, Double>();
	private Map<Integer, List<Integer>> communityHierarchy = new HashMap<Integer, List<Integer>>();
	private Map<Community, Integer> allCommunitySizes = new HashMap<Community, Integer>();
	private HashMap<UndirectedEdge, Integer> contractedGraph = new HashMap<UndirectedEdge, Integer>();
	private Set<Integer> communities;

	/*
	 * must be called after initial single node communities are set, before any further updates.
	 */
	public void initialiseCommunitiesMap() {
		for (int i = 0; i < nodeToCommunity.length; i++) {
			if (nodeToCommunity[i] != -1) {
				communityHierarchy.put(originalVertexTrans.backward(nodeToCommunity[i]), new ArrayList<Integer>());
			}
		}
	}

	public void updateCommunitiesMap() {
		if (hierarchyHeight == 0) {
			for (int i = 0; i < nodeToCommunity.length; i++) {
				if (communityHierarchy.containsKey(originalVertexTrans.backward(i))) {
					List<Integer> communities = communityHierarchy.get(originalVertexTrans.backward(i));
					communities.add(originalVertexTrans.backward(nodeToCommunity[i]));
				}
			}
		} else {
			for (Map.Entry<Integer, List<Integer>> node : communityHierarchy.entrySet()) {
				int nodeId = node.getKey();
				List<Integer> communities = node.getValue();
				int currentGcId = updatedVertexTrans.forward(nodeId);
				if (nodeId < nodeToCommunity.length && nodeToCommunity[currentGcId] != -1) {
					int latestCommunity = updatedVertexTrans.backward(nodeToCommunity[currentGcId]);
					communities.add(latestCommunity);
				} else {
					int previousCommunity = communities.get(hierarchyHeight - 1);
					int previousCommunityGcId = updatedVertexTrans.forward(previousCommunity);
					int latestCommunityGcId = nodeToCommunity[previousCommunityGcId];
					if (updatedVertexTrans.backward(latestCommunityGcId) == -1) {
						System.out.println("This is not a connected graph: " + previousCommunityGcId);
						/* this happens when graph is not connected, resulting in single disjoint communities,
						 * therefore community will simply remain the same
						 */
						communities.add(previousCommunity);
					} else {
						communities.add(updatedVertexTrans.backward(latestCommunityGcId));
					}
				}
			}
		}
	}

	/*
	 * Need to amend to account for disconnected graph. Potential to deal with inside hierarchy update method.
	 */
	public void updateSizesMap() {
		for (int i = 0; i < communitySize.length; i++) {
			if (communitySize[i] > 0) {
				allCommunitySizes.put(new Community(updatedVertexTrans.backward(i), hierarchyHeight), communitySize[i]);
			}
		}
	}

	public void insertNodeIntoCommunity(int nodeId, int communityId) {
		nodeToCommunity[nodeId] = communityId;
		if (hierarchyHeight == 0) {
			communitySize[communityId]++;
		} else {
			communitySize[communityId] += allCommunitySizes.get(new Community(updatedVertexTrans.backward(nodeId), hierarchyHeight - 1));
		}
		communitySizeAtThisLevel[communityId]++;
	}

	public void removeNodeFromCommunity(int nodeId, int communityId) {
		nodeToCommunity[nodeId] = -1;
		if (hierarchyHeight == 0) {
			communitySize[communityId]--;
		} else {
			communitySize[communityId] -= allCommunitySizes.get(new Community(updatedVertexTrans.backward(nodeId), hierarchyHeight - 1));
		}
		communitySizeAtThisLevel[communityId]--;
	}
	
	public void setFromNodeCount(int noOfVertices) {
		nodeToCommunity = new int[noOfVertices];
		for (int i = 0; i < noOfVertices; i++) {
			nodeToCommunity[i] = -1;
		}
		communityInternalEdges = new int[noOfVertices];
		communityTotalEdges = new int[noOfVertices];
		nodeWeightedDegree = new int[noOfVertices];
		nodeSelfLoops = new int[noOfVertices];
		communitySize = new int[noOfVertices];
		communitySizeAtThisLevel = new int[noOfVertices];
		communities = new HashSet<Integer>();
	}
	
	public void updateModularity(int level) {
		double q = 0.;
		for (int i = 0; i < nodeToCommunity.length; i++) {
			if (communityTotalEdges[i] > 0) {
				q += (communityInternalEdges[i] / (double)totalGraphWeight);
				q -= Math.pow(communityTotalEdges[i] / (double)totalGraphWeight, 2);
			}
		}
		modularities.put(level, q);
	}
	
	public int nodeSize(int node) {
		return allCommunitySizes.get(new Community(updatedVertexTrans.backward(node), hierarchyHeight - 1));
	}

	public int getNodeCount() {
		return communities.size();
	}
	
	public Map<Integer, Double> getModularities() {
		return modularities;
	}

	public Map<Integer, List<Integer>> getCommunityHierarchy() {
		return communityHierarchy;
	}

	public Map<Community, Integer> getCommunitySizes() {
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

	public int[] getNodeToCommunity() {
		return nodeToCommunity;
	}

	public void setNodeToCommunity(int[] nodeToCommunity) {
		this.nodeToCommunity = nodeToCommunity;
	}

	public int[] getCommunityInternalEdges() {
		return communityInternalEdges;
	}

	public void setCommunityInternalEdges(int[] communityInternalEdges) {
		this.communityInternalEdges = communityInternalEdges;
	}

	public int[] getCommunityTotalEdges() {
		return communityTotalEdges;
	}

	public void setCommunityTotalEdges(int[] communityTotalEdges) {
		this.communityTotalEdges = communityTotalEdges;
	}

	public int[] getNodeWeightedDegree() {
		return nodeWeightedDegree;
	}

	public void setNodeWeightedDegree(int[] nodeWeightedDegree) {
		this.nodeWeightedDegree = nodeWeightedDegree;
	}

	public int[] getNodeSelfLoops() {
		return nodeSelfLoops;
	}

	public void setNodeSelfLoops(int[] nodeSelfLoops) {
		this.nodeSelfLoops = nodeSelfLoops;
	}

	public int[] getCommunitySize() {
		return communitySize;
	}

	public void setCommunitySize(int[] communitySize) {
		this.communitySize = communitySize;
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

	public int[] getCommunitySizeAtThisLevel() {
		return communitySizeAtThisLevel;
	}

	public void setCommunitySizeAtThisLevel(int[] communitySizeAtThisLevel) {
		this.communitySizeAtThisLevel = communitySizeAtThisLevel;
	}

}
