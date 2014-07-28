package uk.ac.bham.cs.commdet.graphchi.orca;

import java.util.*;

public class Neighbourhood {
	
	private int seedNode;
	private Map<Integer, Integer> membersSeenCount;
	private int totalEdgeWeight;
	private int edgeCount;
	
	public Neighbourhood(int seedNode) {
		this.seedNode = seedNode;
		this.membersSeenCount = new HashMap<Integer, Integer>();
	}
	
	public void addMember(int member) {
		membersSeenCount.put(member, 1);
	}
	
	public void incrementMembersSeenCount(int member) {
		if (membersSeenCount.containsKey(member)) {
			int count = membersSeenCount.get(member);
			membersSeenCount.put(member, count + 1);
		}
	}

	public double getRankValue() {
		return totalEdgeWeight / membersSeenCount.size();
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

	@Override
	public String toString() {
		return "Neighbourhood [seedNode=" + seedNode + ", membersSeenCount="
				+ membersSeenCount + ", totalEdgeWeight=" + totalEdgeWeight
				+ "]";
	}

	public int getEdgeCount() {
		return edgeCount;
	}

	public void setEdgeCount(int edgeCount) {
		this.edgeCount = edgeCount;
	}
	
}
