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
        node.addProperty("label", "n" + label);
        node.addProperty("colour", intToARGB(label));
		nodeContainer.add("data", node);
        
        return nodeContainer;
	}
	
	public static String intToARGB(int i){
		i = smear(i * 9999);
	    return "#" + Integer.toHexString(((i>>24)&0xFF))+
	        Integer.toHexString(((i>>16)&0xFF))+
	        Integer.toHexString(((i>>8)&0xFF))+
	        Integer.toHexString((i&0xFF));
	}
	
	/*
	   * This method was written by Doug Lea with assistance from members of JCP
	   * JSR-166 Expert Group and released to the public domain, as explained at
	   * http://creativecommons.org/licenses/publicdomain
	   * 
	   * As of 2010/06/11, this method is identical to the (package private) hash
	   * method in OpenJDK 7's java.util.HashMap class.
	*/
	public static int smear(int hashCode) {
	    hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
	    return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
	}

}
