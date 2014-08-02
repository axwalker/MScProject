package uk.ac.bham.cs.commdet.graphchi.all;

/**
 * Edge with lowest id value is always used as the source in the edge.
 */
public class UndirectedEdge {
	
	int source;
	int target;
	double weight;
	 
	public UndirectedEdge(int node1, int node2) {
		this.source = Math.min(node1, node2);
		this.target = Math.max(node1, node2);
	}
	
	public UndirectedEdge(int node1, int node2, double weight) {
		this.source = Math.min(node1, node2);
		this.target = Math.max(node1, node2);
		this.weight = weight;
	}
	
	public static UndirectedEdge getEdge(String line) {
		String[] edgeInfo = line.split(" ");
		double weight = edgeInfo.length == 3 ? Double.parseDouble(edgeInfo[2]) : 1.;
		return new UndirectedEdge(Integer.parseInt(edgeInfo[0]), Integer.parseInt(edgeInfo[1]), weight);
	}

	public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}	
	
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public void increaseWeightBy(double weight) {
		this.weight += weight;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + target;
		result = prime * result + source;
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
		if (target != other.target)
			return false;
		if (source != other.source)
			return false;
		return true;
	}
	
	public String toStringWeightless() {
		return source + " " + target;
	}

	@Override
	public String toString() {
		return source + " " + target + " " + weight + "\n";
	}
}