package uk.ac.bham.cs.commdet.graphchi.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Edge {

	@Expose
	@SerializedName("data")
	private UndirectedEdge edgeData;
	
	public Edge(UndirectedEdge edgeData) {
		this.edgeData = edgeData;
	}
	
	public static Edge makeEdge(int node1, int node2, int weight) {
		return new Edge(new UndirectedEdge(node1, node2, weight));
	}
	
	public UndirectedEdge getData() {
		return this.edgeData;
	}
	
	public void incrementWeight(int weight) {
		this.edgeData.increaseWeightBy(weight);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((edgeData == null) ? 0 : edgeData.hashCode());
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
		Edge other = (Edge) obj;
		if (edgeData == null) {
			if (other.edgeData != null)
				return false;
		} else if (!edgeData.equals(other.edgeData))
			return false;
		return true;
	}
	
	
	
}
