package uk.ac.bham.cs.commdet.graphchi.json;

import com.google.gson.JsonObject;

public class JSONConverterEdgelistWeighted extends JSONConverterEdgelist {

	@Override
	public JsonObject getEdge(int edgeID, String line) {
		return getUnweightedEdge(edgeID, line);
	}
	
	public JsonObject getUnweightedEdge(int edgeID, String line) {
		String[] edgeInfo = line.split(" ");
		if (edgeInfo.length != 3) {
			throw new IllegalArgumentException("malformed edge list data");
		}
		JsonObject edgeContainer = new JsonObject();
        JsonObject edge = new JsonObject();
        edge.addProperty("id", "e" + edgeID);
        edge.addProperty("source", "n" + edgeInfo[0]);
        edge.addProperty("target", "n" + edgeInfo[1]);
        edge.addProperty("weight", edgeInfo[2]);
		edgeContainer.add("data", edge);
        
        return edgeContainer;
	}

}
