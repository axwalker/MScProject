package uk.ac.bham.cs.commdet.cyto.json;

import java.util.*;

public class CompoundGraph {

	private List<CompoundNode> nodes = new ArrayList<CompoundNode>();
	private List<UndirectedEdge> edges = new ArrayList<UndirectedEdge>();
	private Metadata metadata = new Metadata();
	
	public CompoundGraph() {}

	public List<CompoundNode> getNodes() {
		return nodes;
	}

	public void setNodes(List<CompoundNode> nodes) {
		this.nodes = nodes;
	}

	public List<UndirectedEdge> getEdges() {
		return edges;
	}

	public void setEdges(List<UndirectedEdge> edges) {
		this.edges = edges;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
	
	public int getSize() {
		return nodes.size();
	}
	
	public void addEdge(UndirectedEdge edge) {
		if (edges.contains(edge)) {
			UndirectedEdge oldEdge = edges.get(edges.indexOf(edge));
			oldEdge.increaseWeightBy(edge.getWeight());
		} else {
			edges.add(edge);
		}
	}
	
}