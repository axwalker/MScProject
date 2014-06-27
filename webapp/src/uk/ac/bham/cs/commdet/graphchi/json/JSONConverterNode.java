package uk.ac.bham.cs.commdet.graphchi.json;

import java.io.InputStream;

import com.google.gson.JsonArray;

public interface JSONConverterNode {
	
	public JsonArray getNodesJson(InputStream inputStream);
	
}
