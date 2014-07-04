package uk.ac.bham.cs.commdet.graphchi.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CompoundNode {

	@Expose
	@SerializedName("data")
	private CompoundNodeData nodeData;

	public CompoundNode(CompoundNodeData nodeData) {
		this.nodeData = nodeData;
	}
	
	public static CompoundNode makeCompoundNode(String id, int size) {
		return new CompoundNode(new CompoundNodeData(id, size));
	}
	
	public CompoundNodeData getData() {
		return this.nodeData;
	}
	
}
