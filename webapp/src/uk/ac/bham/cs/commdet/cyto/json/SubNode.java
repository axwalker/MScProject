package uk.ac.bham.cs.commdet.cyto.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(using = SubNodeSerializer.class)
public class SubNode {
	
	private String id;
	private String label;

	public SubNode(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	protected void setLabel(String label) {
		this.label = label;
	}
	
}

class SubNodeSerializer extends JsonSerializer<SubNode> {
    @Override
    public void serialize(SubNode node, JsonGenerator jsonGenerator, 
            SerializerProvider serializerProvider) throws IOException {    	
    	
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.getSerializationConfig().disable(Feature.USE_ANNOTATIONS);
    	jsonGenerator.writeStartObject();
    	jsonGenerator.writeFieldName("data");
    	jsonGenerator.writeRawValue(mapper.writeValueAsString(node));
    	jsonGenerator.writeEndObject();
    }
}
