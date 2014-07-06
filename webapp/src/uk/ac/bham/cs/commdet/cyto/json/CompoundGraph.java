package uk.ac.bham.cs.commdet.cyto.json;

import java.util.*;

import com.google.gson.annotations.Expose;

public class CompoundGraph {

	@Expose
	private List<CompoundNode> nodes = new ArrayList<CompoundNode>();
	
	@Expose
	private List<UndirectedEdge> edges = new ArrayList<UndirectedEdge>();
	
	@Expose
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
