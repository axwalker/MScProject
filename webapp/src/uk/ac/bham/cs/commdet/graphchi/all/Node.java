package uk.ac.bham.cs.commdet.graphchi.all;

/**
 * A node with an id where self loops is the number of half edges the node has
 * to itself and weight degree is the total number of half edges incident to
 * this node.
 */
public class Node {

	private int id;
	private double selfLoops;
	private double weightedDegree;
	
	public Node(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public double getSelfLoops() {
		return selfLoops;
	}
	
	public void setSelfLoops(double selfLoops) {
		this.selfLoops = selfLoops;
	}
	
	public double getWeightedDegree() {
		return weightedDegree;
	}
	
	public void setWeightedDegree(double weightedDegree) {
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
