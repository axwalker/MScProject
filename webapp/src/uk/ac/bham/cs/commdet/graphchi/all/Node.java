package uk.ac.bham.cs.commdet.graphchi.all;

public class Node {

	private int id;
	private int selfLoops;
	private int weightedDegree;
	
	public Node(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public int getSelfLoops() {
		return selfLoops;
	}
	
	public void setSelfLoops(int selfLoops) {
		this.selfLoops = selfLoops;
	}
	
	public int getWeightedDegree() {
		return weightedDegree;
	}
	
	public void setWeightedDegree(int weightedDegree) {
		this.weightedDegree = weightedDegree;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		Node other = (Node) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Node [id=" + id + ", selfLoops=" + selfLoops
				+ ", weightedDegree=" + weightedDegree + "]";
	}
	
}
