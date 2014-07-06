package uk.ac.bham.cs.commdet.cyto.json;

import java.util.*;

public class SubGraph {

	private Set<SubNode> nodes = new HashSet<SubNode>();
	private Set<UndirectedEdge> edges = new HashSet<UndirectedEdge>(); 
	
	public SubGraph() {}
	
	public SubGraph(Set<SubNode> nodes, Set<UndirectedEdge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}

	public Set<SubNode> getNodes() {
		return nodes;
	}

	public void setNodes(Set<SubNode> nodes) {
		this.nodes = nodes;
	}

	public Set<UndirectedEdge> getEdges() {
		return edges;
	}

	public void setEdges(Set<UndirectedEdge> edges) {
		this.edges = edges;
	}
	
	public int getSize() {
		return nodes.size();
	}
	
	public double getIntraClusterDensity() {
		double internalEdges = edges.size();
		double possibleEdges = (nodes.size() * (nodes.size()-1)) / 2.0;
		return possibleEdges == 0 ? 1 : internalEdges/possibleEdges;
	}
}
