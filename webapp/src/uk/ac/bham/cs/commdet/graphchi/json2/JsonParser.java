package uk.ac.bham.cs.commdet.graphchi.json2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.cmu.graphchi.datablocks.IntConverter;

public class JsonParser {

	private JsonObject highLevel = new JsonObject();
	private Map<Integer, JsonObject> lowLevel = new HashMap<Integer, JsonObject>();
	private Map<Integer, Integer> allNodes = new HashMap<Integer, Integer>();
	private Map<Edge, Integer> highLevelEdges = new HashMap<Edge, Integer>();
	
	public JsonParser() {
		JsonArray nodes = new JsonArray();
		JsonArray edges = new JsonArray();
		this.highLevel.add("nodes", nodes);
		this.highLevel.add("edges", edges);
	}
	
	public JsonObject parseGraph(InputStream vertexInputStream, InputStream edgeInputStream) {
		parseVertices(vertexInputStream);
		parseEdges(edgeInputStream);
		JsonObject graphs = new JsonObject();
		graphs.add("HighLevel", highLevel);
		for (Map.Entry<Integer, JsonObject> entry : lowLevel.entrySet()) {
			graphs.add("" + entry.getKey(), entry.getValue());
		}
		return graphs;
	}

	private void parseVertices(InputStream inputStream) {
		byte[] input;
		try {
			input = IOUtils.toByteArray(inputStream);
			for (int i = 0; i < input.length; i += 4) {
				byte[] labelArray = new byte[4];
				int nodeIndex = i/4;
				labelArray[0] = input[i];
				labelArray[1] = input[i+1];
				labelArray[2] = input[i+2];
				labelArray[3] = input[i+3];
				int label = (new IntConverter()).getValue(labelArray);
				if (label >= 0) {
					if (!labelExists(label)) {
						addNewLabelGraph(label);
					}
					addNodeToLabelGraph(nodeIndex, label);
					allNodes.put(nodeIndex, label);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
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
		node.addProperty("id", "n" + label);
		node.addProperty("label", "" + label);
		node.addProperty("size", size);
		node.addProperty("colour", intToARGB(label));
		nodeContainer.add("data", node);
		return nodeContainer;
	}
	
	private void addNodeToLabelGraph(int nodeID, int label) {
		JsonObject nodeContainer = new JsonObject();
		JsonObject node = new JsonObject();
		node.addProperty("id", "n" + nodeID);
		node.addProperty("label", "" + label);
		node.addProperty("colour", intToARGB(label));
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
				if (getNodeLabel(edge.getSource()) == getNodeLabel(edge.getTarget())) {
					JsonObject edgeContainer = new JsonObject();
			        JsonObject edgeJson = new JsonObject();
			        edgeJson.addProperty("id", "e" + edgeID);
			        edgeJson.addProperty("source", "n" + edge.getSource());
			        edgeJson.addProperty("target", "n" + edge.getTarget());
					edgeContainer.add("data", edgeJson);
					JsonArray labelEdges = lowLevel.get(getNodeLabel(edge.getSource())).getAsJsonArray("edges");
					labelEdges.add(edgeContainer);
				} else {
					Edge interLabelEdge = new Edge(edge.getId(), getNodeLabel(edge.getSource()), getNodeLabel(edge.getTarget()));
					Edge interLabelEdgeRev = new Edge(edge.getId(), getNodeLabel(edge.getTarget()), getNodeLabel(edge.getSource()));
					if (highLevelEdges.containsKey(interLabelEdge) || highLevelEdges.containsKey(interLabelEdge)) {
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
					edgeID++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		JsonArray highLevelEdgesJson = highLevel.getAsJsonArray("edges");
		for (Map.Entry<Edge, Integer> entry : highLevelEdges.entrySet()) {
			JsonObject edgeContainer = new JsonObject();
	        JsonObject edgeJson = new JsonObject();
	        edgeJson.addProperty("id", "e" + edgeID);
	        edgeJson.addProperty("source", "n" + entry.getKey().getSource());
	        edgeJson.addProperty("target", "n" + entry.getKey().getTarget());
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

	private static String intToARGB(int i){
		i = smear(i * 9999);
		return "#" + Integer.toHexString(((i>>24)&0xFF))+
				Integer.toHexString(((i>>16)&0xFF))+
				Integer.toHexString(((i>>8)&0xFF))+
				Integer.toHexString((i&0xFF));
	}

	/*
	 * This method is identical to the (package private) hash
	 * method in OpenJDK 7's java.util.HashMap class.
	 */
	private static int smear(int hashCode) {
		hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
		return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
	}
}

class Edge {
	private int id;
	private int source;
	private int target;
	
	public Edge(int id, int source, int target) {
		this.id = id;
		this.source = source;
		this.target = target;
	}	
	
	public int getId() {
		return id;
	}

	public int getSource() {
		return source;
	}

	public int getTarget() {
		return target;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		if (source != other.source)
			return false;
		if (target != other.target)
			return false;
		return true;
	}
}
