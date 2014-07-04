package uk.ac.bham.cs.commdet.cyto.json;

import com.google.gson.annotations.Expose;

public class CompoundNode {

	@Expose
	private String id;
	
	@Expose
	private int size;
	
	@Expose
	private int intraClusterDensity = -1;
	
	@Expose
	private int interClusterDensity = -1;

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

	public int getIntraClusterDensity() {
		return intraClusterDensity;
	}

	public void setIntraClusterDensity(int intraClusterDensity) {
		this.intraClusterDensity = intraClusterDensity;
	}

	public int getInterClusterDensity() {
		return interClusterDensity;
	}

	public void setInterClusterDensity(int interClusterDensity) {
		this.interClusterDensity = interClusterDensity;
	}
	
}
