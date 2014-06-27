package uk.ac.bham.cs.commdet.graphchi.json;

import com.google.gson.JsonObject;

public class JSONConverterEdgelistUnweighted extends JSONConverterEdgelist {

	@Override
	public JsonObject getEdge(int edgeID, String line) {
		return getWeightedEdge(edgeID, line);
	}
	
	public JsonObject getWeightedEdge(int edgeID, String line) {
		String[] edgeInfo = line.split(" ");
		if (edgeInfo.length != 2) {
			throw new IllegalArgumentException("malformed edge list data");
		}
		JsonObject edgeContainer = new JsonObject();
        JsonObject edge = new JsonObject();
        edge.addProperty("id", "e" + edgeID);
        edge.addProperty("source", "n" + edgeInfo[0]);
        edge.addProperty("target", "n" + edgeInfo[1]);
		edgeContainer.add("data", edge);
        
        return edgeContainer;
	}

}
