package uk.ac.bham.cs.commdet.cyto.graph;

import java.io.Serializable;
import java.util.*;

/**
 * A graph as specified by Cytoscape js to be used with a JSON serializer. Result:
 * 		{
 * 			"nodes": [
 * 				...
 * 			],
 * 			"edges": [
 * 				...
 * 			],
 * 			"metadata": {
 * 				...
 * 			},
 * 			"modularities": [
 * 				...
 * 			]
 * 		}
 */
public class Graph implements Serializable {

	private List<NodeData> nodes = new ArrayList<NodeData>();
	private List<EdgeData> edges = new ArrayList<EdgeData>();
	private Metadata metadata = new Metadata();
	private double[] modularities;
	
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

	public double[] getModularities() {
		return modularities;
	}

	public void setModularities(double[] modularities) {
		this.modularities = modularities;
	}
	
}
