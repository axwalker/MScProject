package uk.ac.bham.cs.commdet.cyto.graph;

import java.io.Serializable;

/**
 * Container for an edge object. This is to be used with a JSON serializer so the that the
 * format matches cytoscape's graph data format. For example, a graph with a single edge:
 * 		{ 
 * 			"edges": [ 
 * 				"data": { 
 * 					"source": "1",
 * 					"target": "2",
 * 					"weight": 1.5
 * 				}
 * 			]
 *		} 
 */
public class EdgeData implements Serializable {

	private Edge data;
	
	public EdgeData(Edge data) {
		this.data = data;
	}
	
	public Edge getData() {
		return data;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data.getSource() == null) ? 0 : data.getSource().hashCode());
		result = prime * result + ((data.getTarget() == null) ? 0 : data.getTarget().hashCode());
		long temp;
		temp = Double.doubleToLongBits(data.getWeight());
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		EdgeData other = (EdgeData) obj;
		if (data.getSource() == null) {
			if (other.getData().getSource() != null)
				return false;
		} else if (!data.getSource().equals(other.getData().getSource()))
			return false;
		if (data.getTarget() == null) {
			if (other.getData().getTarget() != null)
				return false;
		} else if (!data.getTarget().equals(other.getData().getTarget()))
			return false;
		if (Double.doubleToLongBits(data.getWeight()) != Double.doubleToLongBits(other.getData().getWeight()))
			return false;
		return true;
	}
}
