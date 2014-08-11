package uk.ac.bham.cs.commdet.cyto.graph;

import java.io.Serializable;

/**
 * Metadata regarding the found community structure of a graph.
 */
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + NoOfCommunities;
		result = prime * result + avgCommunitySize;
		result = prime * result + currentLevel;
		result = prime * result + hierarchyHeight;
		result = prime * result + maxCommunitySize;
		long temp;
		temp = Double.doubleToLongBits(maxEdgeConnection);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + minCommunitySize;
		temp = Double.doubleToLongBits(modularity);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Metadata other = (Metadata) obj;
		if (NoOfCommunities != other.NoOfCommunities)
			return false;
		if (avgCommunitySize != other.avgCommunitySize)
			return false;
		if (currentLevel != other.currentLevel)
			return false;
		if (hierarchyHeight != other.hierarchyHeight)
			return false;
		if (maxCommunitySize != other.maxCommunitySize)
			return false;
		if (Double.doubleToLongBits(maxEdgeConnection) != Double
				.doubleToLongBits(other.maxEdgeConnection))
			return false;
		if (minCommunitySize != other.minCommunitySize)
			return false;
		if (Double.doubleToLongBits(modularity) != Double.doubleToLongBits(other.modularity))
			return false;
		return true;
	}
	

}
