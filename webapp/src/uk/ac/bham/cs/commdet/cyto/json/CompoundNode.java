package uk.ac.bham.cs.commdet.cyto.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(using = CompoundNodeSerializer.class)
public class CompoundNode {

	private String id;
	private int size;
	private double intraClusterDensity = -1;
	private double interClusterDensity = -1;
	private double clusterRating = -1;
	private int degree;

	public CompoundNode(String id, int size) {
		this.id = id;
		this.size = size;
	}

	public String getId() {
		return id;
	}

	public int getSize() {
		return size;
	}

	protected void setSize(int size) {
		this.size = size;
	}
	
	public void increaseSizeBy(int increase) {
		this.setSize(this.getSize() + increase);
	}

	public double getIntraClusterDensity() {
		return intraClusterDensity;
	}

	public void setIntraClusterDensity(double intraClusterDensity) {
		this.intraClusterDensity = intraClusterDensity;
	}

	public double getInterClusterDensity() {
		return interClusterDensity;
	}

	public void setInterClusterDensity(int graphSize) {
		double possibleEdges = this.size * (graphSize - this.size);
		this.interClusterDensity = possibleEdges == 0 ? 0 : degree/possibleEdges;
	}

	public int getDegree() {
		return degree;
	}

	public void setDegree(int degree) {
		this.degree = degree;
	}
	
	public void updateClusterRating() {
		this.clusterRating = (0.9 * intraClusterDensity) + (0.1 * (1 - interClusterDensity));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		CompoundNode other = (CompoundNode) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
}

class CompoundNodeSerializer extends JsonSerializer<CompoundNode> {
    @Override
    public void serialize(CompoundNode node, JsonGenerator jsonGenerator, 
            SerializerProvider serializerProvider) throws IOException {    	
    	
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.getSerializationConfig().disable(Feature.USE_ANNOTATIONS);
    	jsonGenerator.writeStartObject();
    	jsonGenerator.writeFieldName("data");
    	jsonGenerator.writeRawValue(mapper.writeValueAsString(node));
    	jsonGenerator.writeEndObject();
    }
}
