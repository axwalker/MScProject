package uk.ac.bham.cs.commdet.graphchi.json;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import uk.ac.bham.cs.commdet.graphchi.program.BidirectionalLabel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.vertexdata.ForeachCallback;
import edu.cmu.graphchi.vertexdata.VertexAggregator;

public class JsonParser {

	private JsonObject highLevelGraph = new JsonObject();
	private Map<Integer, JsonObject> lowLevelGraphs = new HashMap<Integer, JsonObject>();
	private Map<Integer, Integer> allNodes = new HashMap<Integer, Integer>();
	private Map<Integer, String> labelColours = new HashMap<Integer, String>();
	private Map<BidirectionalLabel, Integer> highLevelEdges = new HashMap<BidirectionalLabel, Integer>();
	private GraphChiEngine engine;
	private String filepath;
	
	public JsonParser(GraphChiEngine engine, String filepath) {
		JsonArray nodes = new JsonArray();
		JsonArray edges = new JsonArray();
		this.highLevelGraph.add("nodes", nodes);
		this.highLevelGraph.add("edges", edges);
		this.engine = engine;
		this.filepath = filepath;
	}
	
	public JsonObject parseGraph() throws IOException {
		parseVertices();
		parseLowEdges();
		parseHighEdges();
		JsonObject graphs = new JsonObject();
		graphs.add("HighLevel", highLevelGraph);
		for (Map.Entry<Integer, JsonObject> entry : lowLevelGraphs.entrySet()) {
			graphs.add("" + entry.getKey(), entry.getValue());
		}
		return graphs;
	}

	private void parseVertices() throws IOException {
        VertexAggregator.foreach(engine.numVertices(), filepath, new IntConverter(), new ForeachCallback<Integer>() {
            public void callback(int nodeID, Integer label) {
            	if (label >= 0) {
            		if (!lowLevelGraphs.containsKey(label)) {
            			labelColours.put(label, getRandomColour());
                		addNewLabelGraph(label);
                	}
            		JsonArray labelNodes = lowLevelGraphs.get(label).getAsJsonArray("nodes");
            		labelNodes.add(newLowNode(nodeID, label));
                    allNodes.put(nodeID, label);	
            	}
            }
        });
		updateHighLevelNodes();
	}
	
	private JsonObject newHighNode(int label, int size) {
		return newNode(label, label, size, "high");
	}
	
	private JsonObject newLowNode(int nodeID, int label) {
		return newNode(nodeID, label, 0, "low");
	}
	
	private JsonObject newNode(int nodeID, int label, int size, String level) {
		JsonObject nodeContainer = new JsonObject();
		JsonObject node = new JsonObject();
		node.addProperty("id", "" + nodeID);
		node.addProperty("label", "" + label);
		node.addProperty("colour", getColour(label));
		if (level.equals("high")) {
			node.addProperty("size", size);
		} else {
			//node.addProperty("size", 1);
		}
		node.addProperty("type", level);
		nodeContainer.add("data", node);
		return nodeContainer;
	}
	
	private void addNewLabelGraph(int label) {
		JsonObject labelGraph = new JsonObject();
		JsonArray nodes = new JsonArray();
		JsonArray edges = new JsonArray();
		labelGraph.add("nodes", nodes);
		labelGraph.add("edges", edges);
		this.lowLevelGraphs.put(label, labelGraph);
	}

	private void updateHighLevelNodes() {
		JsonArray highNodes = highLevelGraph.getAsJsonArray("nodes");
		for (Map.Entry<Integer, JsonObject> entry : lowLevelGraphs.entrySet()) {
			int size = entry.getValue().getAsJsonArray("nodes").size();
			highNodes.add(newHighNode(entry.getKey(), size));
		}
	}
	
	/////////////////////////////////////////////////////////////////////////
	
	private void parseLowEdges() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));		
		int edgeID = 1;
		String line = null;
		while ((line = br.readLine()) != null) {
			BidirectionalLabel edge = getEdge(edgeID, line);
			int source = edge.getSmallerOne();
			int target = edge.getLargerOne();
			if (getNodeLabel(source).equals(getNodeLabel(target))) {
				JsonObject labelObject = lowLevelGraphs.get(getNodeLabel(source));
				JsonArray labelEdges = labelObject.getAsJsonArray("edges");
				labelEdges.add(newEdge(edgeID, source, target, 0));
			} else {
				int smallerLabel = Math.min(getNodeLabel(source), getNodeLabel(target));
				int largerLabel = Math.max(getNodeLabel(source), getNodeLabel(target));
				addHighEdge(edgeID, smallerLabel, largerLabel);
			}
			edgeID++;
		}
		br.close();
	}
	
	private void parseHighEdges() {
		JsonArray highLevelEdgesJson = highLevelGraph.getAsJsonArray("edges");
		for (Map.Entry<BidirectionalLabel, Integer> entry : highLevelEdges.entrySet()) {
			int id = entry.getKey().getEdgeID();
			int source = entry.getKey().getSmallerOne();
			int target = entry.getKey().getLargerOne();
			int weight = entry.getValue();
			highLevelEdgesJson.add(newEdge(id, source, target, weight));
		}
	}
	
	private void addHighEdge(int edgeID, int sourceLabel, int targetLabel) {
		BidirectionalLabel interLabelEdge = new BidirectionalLabel(edgeID, sourceLabel, targetLabel, 0);
		if (highLevelEdges.containsKey(interLabelEdge)) {
			int weight = highLevelEdges.get(interLabelEdge);
			highLevelEdges.put(interLabelEdge, weight + 1);
		} else {
			highLevelEdges.put(interLabelEdge, 1);
		}	
	}
	
	private JsonObject newEdge(int id, int source, int target, int weight) {
		JsonObject edgeContainer = new JsonObject();
        JsonObject edgeJson = new JsonObject(); 
        edgeJson.addProperty("id", "e" + id);
        edgeJson.addProperty("source", source);
        edgeJson.addProperty("target", target);
        if (weight > 0) {
        	edgeJson.addProperty("weight", weight);
        }
		edgeContainer.add("data", edgeJson);
		return edgeContainer;
	}
	
	private BidirectionalLabel getEdge(int edgeID, String line) {
		String[] edgeInfo = line.split(" ");
		int smallerOne = Math.min(Integer.parseInt(edgeInfo[0]), Integer.parseInt(edgeInfo[1]));
		int largerOne = Math.max(Integer.parseInt(edgeInfo[0]), Integer.parseInt(edgeInfo[1]));
		if (edgeInfo.length == 2) {
			return new BidirectionalLabel(edgeID, smallerOne, largerOne, 0);
		} else if (edgeInfo.length == 3) {
			return new BidirectionalLabel(edgeID, smallerOne, largerOne, Integer.parseInt(edgeInfo[2]));
		} else {
			throw new IllegalArgumentException("malformed edge list data");
		}
	}
	
	private Integer getNodeLabel(int nodeID) {
		return allNodes.get(nodeID);
	}

	private String getColour(int label) {
		return labelColours.get(label);
	}
	
	private static String getRandomColour() {
		int r = (int) Math.round(Math.random() * 255);
		int g = (int) Math.round(Math.random() * 255);
		int b = (int) Math.round(Math.random() * 255);
		Color color = new Color(r, g, b);
		return "#" + Integer.toHexString(color.getRGB()).substring(2);
	}
}