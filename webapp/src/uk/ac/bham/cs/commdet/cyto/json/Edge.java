package uk.ac.bham.cs.commdet.cyto.json;

public class Edge {

	String source;
	String target;
	int weight;
	 
	public Edge(String source, String target, int weight) {
		this.source = source;
		this.target = target;
		this.weight = weight;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
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
	
}
