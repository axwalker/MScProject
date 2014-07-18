package uk.ac.bham.cs.commdet.graphchi.all;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(using = UndirectedEdgeSerializer.class)
public class UndirectedEdge {
	
	int source;
	int target;
	int weight;
	 
	public UndirectedEdge(int node1, int node2, int weight) {
		this.source = Math.min(node1, node2);
		this.target = Math.max(node1, node2);
		this.weight = weight;
	}
	
	public static UndirectedEdge getEdge(String line) {
		String[] edgeInfo = line.split(" ");
		int weight = edgeInfo.length == 3 ? Integer.parseInt(edgeInfo[2]) : 1;
		return new UndirectedEdge(Integer.parseInt(edgeInfo[0]), Integer.parseInt(edgeInfo[1]), weight);
	}

	public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}	
	
	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	public void increaseWeightBy(int weight) {
		this.weight += weight;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + target;
		result = prime * result + source;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UndirectedEdge other = (UndirectedEdge) obj;
		if (target != other.target)
			return false;
		if (source != other.source)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return source + " " + target + " " + weight + "\n";
	}
}

class UndirectedEdgeSerializer extends JsonSerializer<UndirectedEdge> {
    @Override
    public void serialize(UndirectedEdge edge, JsonGenerator jsonGenerator, 
            SerializerProvider serializerProvider) throws IOException {    	
    	
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.getSerializationConfig().disable(Feature.USE_ANNOTATIONS);
    	jsonGenerator.writeStartObject();
    	jsonGenerator.writeFieldName("data");
    	jsonGenerator.writeRawValue(mapper.writeValueAsString(edge));
    	jsonGenerator.writeEndObject();
    }
}