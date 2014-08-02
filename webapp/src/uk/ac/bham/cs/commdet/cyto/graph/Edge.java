package uk.ac.bham.cs.commdet.cyto.graph;

import java.io.Serializable;

public class Edge implements Serializable {

	String source;
	String target;
	double weight;
	 
	public Edge(String source, String target, double weight) {
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
	
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public void increaseWeightBy(double weight) {
		this.weight += weight;
	}
	
}
