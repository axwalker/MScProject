package uk.ac.bham.cs.commdet.cyto.json.serializer;

import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


public class JacksonTestMain {

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		JacksonTest boonTest = new JacksonTest("String 1", "String 2");
		ArrayList<JacksonTest> list = new ArrayList<JacksonTest>();
		list.add(boonTest);
		list.add(boonTest);
		
		ObjectMapper mapper = new ObjectMapper();

		System.out.println(mapper.writeValueAsString(list));
	}
	
}
