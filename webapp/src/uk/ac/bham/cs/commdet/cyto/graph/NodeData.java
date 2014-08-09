package uk.ac.bham.cs.commdet.cyto.graph;

import java.io.Serializable;

public class NodeData implements Serializable {

	private Node data;

	public NodeData(Node data) {
		this.data = data;
	}

	public Node getData() {
		return data;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data.getId() == null) ? 0 : data.getId().hashCode());
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
		NodeData other = (NodeData) obj;
		if (data.getId() == null) {
			if (other.getData().getId() != null)
				return false;
		} else if (!data.getId().equals(other.getData().getId()))
			return false;
		return true;
	}
	
}
