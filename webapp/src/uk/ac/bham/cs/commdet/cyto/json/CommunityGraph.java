package uk.ac.bham.cs.commdet.cyto.json;

import java.util.*;

import com.google.gson.annotations.Expose;

public class CommunityGraph {
	
	@Expose	private Map<String, CompoundGraph> compoundGraph = new HashMap<String, CompoundGraph>();
	@Expose	private Map<String, SubGraph> subGraphs = new HashMap<String, SubGraph>();
	@Expose private boolean success = true;
	
	public CommunityGraph() {
		compoundGraph.put("HighLevel", new CompoundGraph());
	}

	public Map<String, CompoundGraph> getCompoundGraph() {
		return compoundGraph;
	}

	public void setCompoundGraph(Map<String, CompoundGraph> compoundGraph) {
		this.compoundGraph = compoundGraph;
	}

	public Map<String, SubGraph> getSubGraphs() {
		return subGraphs;
	}

	public void setSubGraphs(Map<String, SubGraph> subGraphs) {
		this.subGraphs = subGraphs;
	}

}
