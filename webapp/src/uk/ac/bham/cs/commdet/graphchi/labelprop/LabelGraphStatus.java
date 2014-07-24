package uk.ac.bham.cs.commdet.graphchi.labelprop;

import java.util.*;

import uk.ac.bham.cs.commdet.graphchi.all.Community;

import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

public class LabelGraphStatus {

	private int[] nodeToCommunity;
	private int[] communityInternalEdges;
	private int[] communityTotalEdges;
	private int[] communitySize;
	private long totalGraphWeight;
	private VertexIdTranslate vertexTrans;
	private Map<Integer, Double> modularities = new HashMap<Integer, Double>();
	private Map<Integer, List<Integer>> communityHierarchy = new HashMap<Integer, List<Integer>>();
	private Map<Community, Integer> allCommunitySizes = new HashMap<Community, Integer>();

	/*
	 * must be called after initial single node communities are set, before any further updates.
	 */
	public void initialiseCommunitiesMap() {
		for (int i = 0; i < nodeToCommunity.length; i++) {
			if (nodeToCommunity[i] != -1) {
				communityHierarchy.put(vertexTrans.backward(nodeToCommunity[i]), new ArrayList<Integer>());
			}
		}
	}

	public void updateCommunitiesMap() {
		for (int i = 0; i < nodeToCommunity.length; i++) {
			if (communityHierarchy.containsKey(vertexTrans.backward(i))) {
				List<Integer> communities = communityHierarchy.get(vertexTrans.backward(i));
				communities.add(vertexTrans.backward(nodeToCommunity[i]));
			}
		}
	}

	public void updateSizesMap() {
		for (int i = 0; i < communitySize.length; i++) {
			if (communitySize[i] > 0) {
				allCommunitySizes.put(new Community(vertexTrans.backward(i), 0), communitySize[i]);
			}
		}
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

	public Map<Integer, Double> getModularities() {
		return modularities;
	}

	public Map<Integer, List<Integer>> getCommunityHierarchy() {
		return communityHierarchy;
	}

	public Map<Community, Integer> getCommunitySizes() {
		return allCommunitySizes;
	}

	public void setVertexTrans(VertexIdTranslate trans) {
		this.vertexTrans = trans;
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

}
