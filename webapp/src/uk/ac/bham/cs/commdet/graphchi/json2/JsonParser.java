package uk.ac.bham.cs.commdet.graphchi.json2;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.util.IdInt;
import edu.cmu.graphchi.util.Toplist;

public class JsonParser {

	private JsonObject highLevel = new JsonObject();
	private Map<Integer, JsonObject> lowLevel = new HashMap<Integer, JsonObject>();
	private Map<Integer, Integer> allNodes = new HashMap<Integer, Integer>();
	private Map<Edge, Integer> highLevelEdges = new HashMap<Edge, Integer>();
	private Map<Integer, String> labelColours = new HashMap<Integer, String>();
	private GraphChiEngine engine;
	private String filepath;
	
	public JsonParser(GraphChiEngine engine, String filepath) {
		JsonArray nodes = new JsonArray();
		JsonArray edges = new JsonArray();
		this.highLevel.add("nodes", nodes);
		this.highLevel.add("edges", edges);
		this.engine = engine;
		this.filepath = filepath;
	}
	
	public JsonObject parseGraph(InputStream vertexInputStream, InputStream edgeInputStream) throws IOException {
		parseVertices(vertexInputStream);
		parseEdges(edgeInputStream);
		JsonObject graphs = new JsonObject();
		graphs.add("HighLevel", highLevel);
		for (Map.Entry<Integer, JsonObject> entry : lowLevel.entrySet()) {
			graphs.add("" + entry.getKey(), entry.getValue());
		}
		return graphs;
	}

	private void parseVertices(InputStream inputStream) throws IOException {
		VertexIdTranslate trans = engine.getVertexIdTranslate();
        TreeSet<IdInt> top = Toplist.topListInt(filepath, engine.numVertices(), 1000000);
        for(IdInt vertex : top) {
        	int nodeID = trans.backward(vertex.getVertexId());
        	int label = (int)vertex.getValue();
        	if (label >= 0) {
        		if (!labelExists(label)) {
        			labelColours.put(label, getRandomColour());
            		addNewLabelGraph(label);
            	}
        		addNodeToLabelGraph(nodeID, label);
                allNodes.put(nodeID, label);	
        	}		
        }
		updateHighLevelNodes();
	}
	
	private void addNewLabelGraph(int label) {
		JsonObject labelGraph = new JsonObject();
		JsonArray nodes = new JsonArray();
		JsonArray edges = new JsonArray();
		labelGraph.add("nodes", nodes);
		labelGraph.add("edges", edges);
		this.lowLevel.put(label, labelGraph);
	}

	private boolean labelExists(int label) {
		return lowLevel.containsKey(label);
	}

	private void updateHighLevelNodes() {
		JsonArray highNodes = highLevel.getAsJsonArray("nodes");
		for (Map.Entry<Integer, JsonObject> entry : lowLevel.entrySet()) {
			int size = entry.getValue().getAsJsonArray("nodes").size();
			highNodes.add(newHighNode(entry.getKey(), size));
		}
	}
	
	private JsonObject newHighNode(int label, int size) {
		JsonObject nodeContainer = new JsonObject();
		JsonObject node = new JsonObject();
		node.addProperty("id", "" + label);
		node.addProperty("label", "" + label);
		node.addProperty("size", size);
		node.addProperty("colour", getColour(label));
		nodeContainer.add("data", node);
		return nodeContainer;
	}
	
	private void addNodeToLabelGraph(int nodeID, int label) {
		JsonObject nodeContainer = new JsonObject();
		JsonObject node = new JsonObject();
		node.addProperty("id", "" + nodeID);
		node.addProperty("label", "" + label);
		node.addProperty("type", "detail");
		node.addProperty("colour", getColour(label));
		nodeContainer.add("data", node);
		JsonArray labelNodes = lowLevel.get(label).getAsJsonArray("nodes");
		labelNodes.add(nodeContainer);
	}
	
	private void parseEdges(InputStream inputStream) {
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader br = new BufferedReader(in);		
		int edgeID = 0;
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				Edge edge = getUnweightedEdge(edgeID, line);
				if (getNodeLabel(edge.getSource()).equals(getNodeLabel(edge.getTarget()))) {
					JsonObject edgeContainer = new JsonObject();
			        JsonObject edgeJson = new JsonObject(); 
			        edgeJson.addProperty("id", "e" + edgeID);
			        edgeJson.addProperty("source", "" + edge.getSource());
			        edgeJson.addProperty("target", "" + edge.getTarget());
					edgeContainer.add("data", edgeJson);
					JsonObject labelObject = lowLevel.get(getNodeLabel(edge.getSource()));
					JsonArray labelEdges;
					try {
						labelEdges = labelObject.getAsJsonArray("edges"); //Null pointer exception
					} catch (NullPointerException npe) {
						throw new NullPointerException("label:" + getNodeLabel(edge.getSource()));
					}
					labelEdges.add(edgeContainer);
				} else {
					Edge interLabelEdge = new Edge(edgeID, getNodeLabel(edge.getSource()), getNodeLabel(edge.getTarget()));
					Edge interLabelEdgeRev = new Edge(edgeID, getNodeLabel(edge.getTarget()), getNodeLabel(edge.getSource()));
					if (highLevelEdges.containsKey(interLabelEdge) || highLevelEdges.containsKey(interLabelEdgeRev)) {
						if (highLevelEdges.containsKey(interLabelEdge)) {
							int weight = highLevelEdges.get(interLabelEdge);
							highLevelEdges.put(interLabelEdge, weight + 1);
						}
						if (highLevelEdges.containsKey(interLabelEdgeRev)) {
							int weight = highLevelEdges.get(interLabelEdgeRev);
							highLevelEdges.put(interLabelEdgeRev, weight + 1);
						}
					} else {
						highLevelEdges.put(interLabelEdge, 1);
					}		
				}
				edgeID++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		JsonArray highLevelEdgesJson = highLevel.getAsJsonArray("edges");
		for (Map.Entry<Edge, Integer> entry : highLevelEdges.entrySet()) {
			JsonObject edgeContainer = new JsonObject();
	        JsonObject edgeJson = new JsonObject();
	        edgeJson.addProperty("id", "e" + entry.getKey().getId());
	        edgeJson.addProperty("source", "" + entry.getKey().getSource());
	        edgeJson.addProperty("target", "" + entry.getKey().getTarget());
	        edgeJson.addProperty("weight", entry.getValue());
			edgeContainer.add("data", edgeJson);
			highLevelEdgesJson.add(edgeContainer);
		}
	}
	
	private Edge getUnweightedEdge(int edgeID, String line) {
		String[] edgeInfo = line.split(" ");
		if (edgeInfo.length != 2) {
			throw new IllegalArgumentException("malformed edge list data");
		}
		return new Edge(edgeID, Integer.parseInt(edgeInfo[0]), Integer.parseInt(edgeInfo[1]));
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
