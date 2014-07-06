package uk.ac.bham.cs.commdet.cyto.json;

import com.google.gson.annotations.Expose;

public class CompoundNode {

	@Expose
	private String id;
	
	@Expose
	private int size;
	
	@Expose
	private double intraClusterDensity = -1;
	
	@Expose
	private double interClusterDensity = -1;
	
	@Expose
	private double clusterRating = -1;
	
	private int degree;

	public CompoundNode(String id, int size) {
		this.id = id;
		this.size = size;
	}

	public String getId() {
		return id;
	}

	public int getSize() {
		return size;
	}

	protected void setSize(int size) {
		this.size = size;
	}
	
	public void increaseSizeBy(int increase) {
		this.setSize(this.getSize() + increase);
	}

	public double getIntraClusterDensity() {
		return intraClusterDensity;
	}

	public void setIntraClusterDensity(double intraClusterDensity) {
		this.intraClusterDensity = intraClusterDensity;
	}

	public double getInterClusterDensity() {
		return interClusterDensity;
	}

	public void setInterClusterDensity(int graphSize) {
		double possibleEdges = this.size * (graphSize - this.size);
		this.interClusterDensity = possibleEdges == 0 ? 0 : degree/possibleEdges;
	}

	public int getDegree() {
		return degree;
	}

	public void setDegree(int degree) {
		this.degree = degree;
	}
	
	public void updateClusterRating() {
		this.clusterRating = (0.9 * intraClusterDensity) + (0.1 * (1 - interClusterDensity));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		CompoundNode other = (CompoundNode) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
}
