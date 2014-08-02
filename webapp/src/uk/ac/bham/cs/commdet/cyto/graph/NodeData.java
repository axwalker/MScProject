package uk.ac.bham.cs.commdet.cyto.graph;

import java.io.Serializable;

public class NodeData implements Serializable {

	private Node data;

	public NodeData(Node data) {
		this.data = data;
	}

	public Node getData() {
		return data;
	}
	
}
