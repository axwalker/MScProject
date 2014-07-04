package uk.ac.bham.cs.commdet.cyto.json.serializer;

import java.lang.reflect.Type;

import uk.ac.bham.cs.commdet.cyto.json.SubNode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SubNodeSerializer implements JsonSerializer<SubNode> {

	@Override
	public JsonElement serialize(final SubNode node, Type arg1, JsonSerializationContext arg2) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		JsonObject nodeJson = (JsonObject) gson.toJsonTree(node);
		
		JsonObject container = new JsonObject();
		container.add("data", nodeJson);
		
		return container;
	}

}
