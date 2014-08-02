package uk.ac.bham.cs.commdet.cyto.graph;

import java.io.Serializable;

public class Metadata implements Serializable {
	
	private int NoOfCommunities;
	private int maxCommunitySize;
	private int avgCommunitySize;
	private int minCommunitySize;
	private double modularity;
	private double maxEdgeConnection;
	private int hierarchyHeight;
	private int currentLevel;
	
	
	public int getNoOfCommunities() {
		return NoOfCommunities;
	}

	public void setNoOfCommunities(int noOfCommunities) {
		NoOfCommunities = noOfCommunities;
	}

	public int getMaxCommunitySize() {
		return maxCommunitySize;
	}

	public void setMaxCommunitySize(int maxCommunitySize) {
		this.maxCommunitySize = maxCommunitySize;
	}

	public int getAvgCommunitySize() {
		return avgCommunitySize;
	}

	public void setAvgCommunitySize(int avgCommunitySize) {
		this.avgCommunitySize = avgCommunitySize;
	}

	public int getMinCommunitySize() {
		return minCommunitySize;
	}

	public void setMinCommunitySize(int minCommunitySize) {
		this.minCommunitySize = minCommunitySize;
	}

	public double getModularity() {
		return modularity;
	}

	public void setModularity(double modularity) {
		this.modularity = modularity;
	}

	public double getMaxEdgeConnection() {
		return maxEdgeConnection;
	}

	public void setMaxEdgeConnection(double maxEdgeConnection) {
		this.maxEdgeConnection = maxEdgeConnection;
	}

	public int getHierarchyHeight() {
		return hierarchyHeight;
	}

	public void setHierarchyHeight(int hierarchyHeight) {
		this.hierarchyHeight = hierarchyHeight;
	}

	public int getCurrentLevel() {
		return currentLevel;
	}

	public void setCurrentLevel(int currentLevel) {
		this.currentLevel = currentLevel;
	}

}
