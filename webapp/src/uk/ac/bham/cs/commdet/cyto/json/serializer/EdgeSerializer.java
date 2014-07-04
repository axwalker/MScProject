package uk.ac.bham.cs.commdet.cyto.json.serializer;

import java.lang.reflect.Type;

import uk.ac.bham.cs.commdet.cyto.json.UndirectedEdge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EdgeSerializer implements JsonSerializer<UndirectedEdge> {

	@Override
	public JsonElement serialize(final UndirectedEdge edge, Type arg1, JsonSerializationContext arg2) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		JsonObject edgeJson = (JsonObject) gson.toJsonTree(edge);
		
		JsonObject container = new JsonObject();
		container.add("data", edgeJson);
		
		return container;
	}

}
