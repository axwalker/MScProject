package uk.ac.bham.cs.commdet.graphchi.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SubNode {

	@Expose
	@SerializedName("data")
	private SubNodeData nodeData;

	public SubNode(SubNodeData nodeData) {
		this.nodeData = nodeData;
	}
	
	public static SubNode makeNode(String id, String label) {
		return new SubNode(new SubNodeData(id, label));
	}
	
	public SubNodeData getData() {
		return this.nodeData;
	}
	
}
