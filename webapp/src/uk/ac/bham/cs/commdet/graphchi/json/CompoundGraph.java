package uk.ac.bham.cs.commdet.graphchi.json;

import java.util.*;

import com.google.gson.annotations.Expose;

public class CompoundGraph {

	@Expose
	private Set<CompoundNode> nodes = new HashSet<CompoundNode>();
	
	@Expose
	private List<Edge> edges = new ArrayList<Edge>();
	
	@Expose
	private Metadata metadata = new Metadata();
	
	public CompoundGraph() {}

	public Set<CompoundNode> getNodes() {
		return nodes;
	}

	public void setNodes(Set<CompoundNode> nodes) {
		this.nodes = nodes;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public void setEdges(List<Edge> edges) {
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
		if (edges.contains(new Edge(edge))) {
			Edge oldEdge = edges.get(edges.indexOf(new Edge(edge)));
			oldEdge.incrementWeight(edge.getWeight());
		} else {
			edges.add(new Edge(edge));
		}
	}
	
}
