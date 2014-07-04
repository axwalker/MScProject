package uk.ac.bham.cs.commdet.cyto.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.vertexdata.ForeachCallback;
import edu.cmu.graphchi.vertexdata.VertexAggregator;

public class CommunityGraph {
	
	@Expose	private Map<String, CompoundGraph> compoundGraph = new HashMap<String, CompoundGraph>();
	@Expose	private Map<String, SubGraph> subGraphs = new HashMap<String, SubGraph>();
	private Map<String, String> nodeLabels = new HashMap<String, String>();
	private GraphChiEngine engine;
	private String filepath;
	
	private CommunityGraph() {
		compoundGraph.put("HighLevel", new CompoundGraph());
	}
	
	public CommunityGraph(GraphChiEngine engine, String filepath) {
		this.engine = engine;
		this.filepath = filepath;
		compoundGraph.put("HighLevel", new CompoundGraph());
	}
	
	public void parseGraphs() throws IOException {
		parseAllNodesFromEngine();
		generateCompoundNodes();
		parseEdgesFromEngine();
		//generateSubNodeMetadata();
		generateCompoundMetadata();
	}
	
	private void parseAllNodesFromEngine() throws IOException {
		final VertexIdTranslate trans = engine.getVertexIdTranslate();
		VertexAggregator.foreach(engine.numVertices(), filepath, new IntConverter(), new ForeachCallback<Integer>() {
            public void callback(int id, Integer label) {
            	if (label >= 0) {
            		if (!subGraphs.containsKey("" + label)) {
            			subGraphs.put("" + label, new SubGraph());
                	}
            		String nodeID = "" + trans.backward(id);
            		subGraphs.get("" + label).getNodes().add(new SubNode(nodeID, "" + label));            		
                    nodeLabels.put(nodeID, "" + label);
            	}
            }
        });
	}
	
	private void generateCompoundNodes() {
		Set<CompoundNode> nodes = compoundGraph.get("HighLevel").getNodes();
		for (Map.Entry<String, SubGraph> entry : subGraphs.entrySet()) {			
			int size = entry.getValue().getSize();
			nodes.add(new CompoundNode(entry.getKey(), size));
		}
	}
	
	private void parseEdgesFromEngine() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line = null;
		while ((line = br.readLine()) != null) {
			UndirectedEdge edge = UndirectedEdge.getEdge(line);
			int source = edge.getSmallerNode();
			int target = edge.getLargerNode();
			int weight = edge.getWeight();			
			if (nodeLabels.get("" + source).equals(nodeLabels.get("" + target))) {
				SubGraph subGraph = subGraphs.get(nodeLabels.get("" + source));
				subGraph.getEdges().add(new UndirectedEdge(source, target, weight));
			} else {
				source = Integer.parseInt(nodeLabels.get("" + source));
				target = Integer.parseInt(nodeLabels.get("" + target));
				compoundGraph.get("HighLevel").addEdge(new UndirectedEdge(source, target, weight));
			}
		}
		br.close();
	}
	
	public void generateCompoundMetadata() {
		int minSize = 1, maxSize = 1, maxEdge = 1;
		for(CompoundNode node : compoundGraph.get("HighLevel").getNodes()) {
			minSize = Math.min(minSize, node.getSize());
			maxSize = Math.max(maxSize, node.getSize());
		}
		for (UndirectedEdge edge : compoundGraph.get("HighLevel").getEdges()) {
			maxEdge = Math.max(maxEdge, edge.getWeight());
		}
		Metadata metadata = compoundGraph.get("HighLevel").getMetadata();
		metadata.setNoOfCommunities(compoundGraph.get("HighLevel").getNodes().size());
		metadata.setAvgCommunitySize(nodeLabels.size()/compoundGraph.get("HighLevel").getNodes().size());
		metadata.setMinCommunitySize(minSize);
		metadata.setMaxCommunitySize(maxSize);
		metadata.setMaxEdgeConnection(maxEdge);
	}
	
}
