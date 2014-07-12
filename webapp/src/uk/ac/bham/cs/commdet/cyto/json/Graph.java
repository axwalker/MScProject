package uk.ac.bham.cs.commdet.cyto.json;

import java.util.*;

public class Graph {

	private List<NodeData> nodes = new ArrayList<NodeData>();
	private List<EdgeData> edges = new ArrayList<EdgeData>();
	private Metadata metadata = new Metadata();
	
	public Graph() {}

	public List<NodeData> getNodes() {
		return nodes;
	}

	public void setNodes(List<NodeData> nodes) {
		this.nodes = nodes;
	}

	public List<EdgeData> getEdges() {
		return edges;
	}

	public void setEdges(List<EdgeData> edges) {
		this.edges = edges;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
	
	public boolean getSuccess() {
		return true;
	}
	
}
