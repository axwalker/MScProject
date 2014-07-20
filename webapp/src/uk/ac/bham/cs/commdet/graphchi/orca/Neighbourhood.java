package uk.ac.bham.cs.commdet.graphchi.orca;

import java.util.*;

public class Neighbourhood {
	
	private int seedNode;
	private Map<Integer, Integer> membersSeenCount;
	private int totalEdgeWeight;
	
	public Neighbourhood(int seedNode) {
		this.seedNode = seedNode;
		this.membersSeenCount = new HashMap<Integer, Integer>();
	}

	public int getSeedNode() {
		return seedNode;
	}

	public void setSeedNode(int seedNode) {
		this.seedNode = seedNode;
	}

	public Map<Integer, Integer> getMembersSeenCount() {
		return membersSeenCount;
	}

	public void setMembersSeenCount(Map<Integer, Integer> membersSeenCount) {
		this.membersSeenCount = membersSeenCount;
	}

	public int getTotalEdgeWeight() {
		return totalEdgeWeight;
	}

	public void setTotalEdgeWeight(int totalEdgeWeight) {
		this.totalEdgeWeight = totalEdgeWeight;
	}
	
	
}
