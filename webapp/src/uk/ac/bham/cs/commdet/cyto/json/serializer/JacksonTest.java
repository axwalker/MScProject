package uk.ac.bham.cs.commdet.cyto.json.serializer;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(using = JacksonTestSerializer.class)
public class JacksonTest {

	private String a;
	private String b;
	
	public JacksonTest(String a, String b) {
		super();
		this.a = a;
		this.b = b;
	}

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

	public String getB() {
		return b;
	}

	public void setB(String b) {
		this.b = b;
	}
}

class JacksonTestSerializer extends JsonSerializer<JacksonTest> {
    @Override
    public void serialize(JacksonTest test, JsonGenerator jsonGenerator, 
            SerializerProvider serializerProvider) throws IOException {    	
    	
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.getSerializationConfig().disable(Feature.USE_ANNOTATIONS);
    	jsonGenerator.writeStartObject();
    	jsonGenerator.writeFieldName("data");
    	jsonGenerator.writeRawValue(mapper.writeValueAsString(test));
    	jsonGenerator.writeEndObject();
    	//jsonGenerator.writeRaw("{data: " + mapper.writeValueAsString(test) + "}");
    }
}
