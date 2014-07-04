package uk.ac.bham.cs.commdet.graphchi.json;

import java.util.*;

import com.google.gson.annotations.Expose;

public class SubGraph {

	@Expose
	private Set<SubNode> nodes = new HashSet<SubNode>();
	
	@Expose
	private Set<Edge> edges = new HashSet<Edge>(); 
	
	public SubGraph() {}
	
	public SubGraph(Set<SubNode> nodes, Set<Edge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}

	public Set<SubNode> getNodes() {
		return nodes;
	}

	public void setNodes(Set<SubNode> nodes) {
		this.nodes = nodes;
	}

	public Set<Edge> getEdges() {
		return edges;
	}

	public void setEdges(Set<Edge> edges) {
		this.edges = edges;
	}
	
	public int getSize() {
		return nodes.size();
	}
}
