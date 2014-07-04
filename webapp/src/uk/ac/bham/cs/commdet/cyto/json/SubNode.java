package uk.ac.bham.cs.commdet.cyto.json;

import com.google.gson.annotations.Expose;

public class SubNode {
	
	@Expose
	private String id;
	
	@Expose
	private String label;

	public SubNode(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	protected void setLabel(String label) {
		this.label = label;
	}
	
}
