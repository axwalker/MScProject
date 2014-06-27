package uk.ac.bham.cs.commdet.graphchi.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public abstract class JSONConverterEdgelist implements JSONConverterEdge {

	@Override
	public JsonArray getEdgesJson(InputStream inputStream) {
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader br = new BufferedReader(in);
		JsonArray edges = new JsonArray();
		
		int edgeID = 0;
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				edges.add(getEdge(edgeID, line));
				edgeID++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return edges;
	}
	
	public abstract JsonObject getEdge(int edgeID, String line);

}
