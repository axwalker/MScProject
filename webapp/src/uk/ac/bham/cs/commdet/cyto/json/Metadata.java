package uk.ac.bham.cs.commdet.cyto.json;

import com.google.gson.annotations.Expose;

public class Metadata {
	
	private int NoOfCommunities;
	private int maxCommunitySize;
	private int avgCommunitySize;
	private int minCommunitySize;
	private int modularity = -1;
	private int maxEdgeConnection;
	private int test = -1;
	
	
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

	public int getModularity() {
		return modularity;
	}

	public void setModularity(int modularity) {
		this.modularity = modularity;
	}

	public int getMaxEdgeConnection() {
		return maxEdgeConnection;
	}

	public void setMaxEdgeConnection(int maxEdgeConnection) {
		this.maxEdgeConnection = maxEdgeConnection;
	}

	public int getTest() {
		return test;
	}

	public void setTest(int test) {
		this.test = test;
	}	

}
