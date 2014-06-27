package uk.ac.bham.cs.commdet.graphchi.json;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import edu.cmu.graphchi.datablocks.IntConverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JSONConverterNodeInt implements JSONConverterNode {

	@Override
	public JsonArray getNodesJson(InputStream inputStream) {
		JsonArray nodes = new JsonArray();
		byte[] input;
		try {
			input = IOUtils.toByteArray(inputStream);
			for (int i = 0; i < input.length; i += 4) {
				byte[] labelArray = new byte[4];
				labelArray[0] = input[i];
				labelArray[1] = input[i+1];
				labelArray[2] = input[i+2];
				labelArray[3] = input[i+3];
				int label = (new IntConverter()).getValue(labelArray);
				if (label >= 0) {
					nodes.add(getNode(i/4, label));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return nodes;
	}
	
	public JsonObject getNode(int nodeID, int label) {
		JsonObject nodeContainer = new JsonObject();
        JsonObject node = new JsonObject();
        node.addProperty("id", "n" + nodeID);
        //node.addProperty("parent", "n" + label);
		nodeContainer.add("data", node);
        
        return nodeContainer;
	}

}
