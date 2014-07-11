package uk.ac.bham.cs.commdet.graphchi.louvain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import uk.ac.bham.cs.commdet.cyto.json.UndirectedEdge;

public class GraphResult {

	private String filename;
	private Map<Integer, List<Integer>> hierarchy;
	private Map<Integer, CommunityEdgePositions> edgePositions;
	private Map<Community, Integer> sizes;
	
	public GraphResult(String filename, Map<Integer, List<Integer>> hierarchy, Map<Community, Integer> sizes) {
		this.filename = filename;
		this.hierarchy = hierarchy;
		this.sizes = sizes;
	}
	
	public void writeSortedEdgeList() throws IOException {
		Set<UndirectedEdge> edges = readInUnsortedEdgeList();
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename + "_sorted"));
		for (UndirectedEdge edge : edges) {
			bw.write(edge.toString());
		}
		bw.close();
	}
	
	private Set<UndirectedEdge> readInUnsortedEdgeList() throws IOException {
		Set<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(new EdgeComparator(hierarchy));
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line = null;
		while ((line = br.readLine()) != null) {
			UndirectedEdge edge = UndirectedEdge.getEdge(line);
			edges.add(edge);
		}
		br.close();
		return edges;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Map<Integer, List<Integer>> getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(Map<Integer, List<Integer>> hierarchy) {
		this.hierarchy = hierarchy;
	}

	public Map<Integer, CommunityEdgePositions> getEdgePositions() {
		return edgePositions;
	}

	public void setEdgePositions(Map<Integer, CommunityEdgePositions> edgePositions) {
		this.edgePositions = edgePositions;
	}

	public Map<Community, Integer> getSizes() {
		return sizes;
	}

	public void setSizes(Map<Community, Integer> sizes) {
		this.sizes = sizes;
	}
	
}
