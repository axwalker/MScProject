package uk.ac.bham.cs.commdet.graphchi.json;

import com.google.gson.annotations.Expose;

public class SubNodeData {
	
	@Expose
	private String id;
	
	@Expose
	private String label;

	public SubNodeData(String id, String label) {
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
