package uk.ac.bham.cs.commdet.cyto.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UndirectedEdge {
	
	@Expose
	@SerializedName("source")
	int smallerNode;
	
	@Expose
	@SerializedName("target")
	int largerNode;
	
	@Expose
	int weight;
	 
	public UndirectedEdge(int node1, int node2, int weight) {
		this.smallerNode = Math.min(node1, node2);
		this.largerNode = Math.max(node1, node2);
		this.weight = weight;
	}
	
	public static UndirectedEdge getEdge(String line) {
		String[] edgeInfo = line.split(" ");
		int weight = edgeInfo.length == 3 ? Integer.parseInt(edgeInfo[2]) : 1;
		return new UndirectedEdge(Integer.parseInt(edgeInfo[0]), Integer.parseInt(edgeInfo[1]), weight);
	}

	public int getSmallerNode() {
		return smallerNode;
	}

	public void setSmallerNode(int smallerNode) {
		this.smallerNode = smallerNode;
	}

	public int getLargerNode() {
		return largerNode;
	}

	public void setLargerNode(int largerNode) {
		this.largerNode = largerNode;
	}	
	
	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	public void increaseWeightBy(int weight) {
		this.weight += weight;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + largerNode;
		result = prime * result + smallerNode;
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
		UndirectedEdge other = (UndirectedEdge) obj;
		if (largerNode != other.largerNode)
			return false;
		if (smallerNode != other.smallerNode)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Smaller: " + smallerNode + ", larger: " + largerNode + ", weight: " + weight;
	}
}