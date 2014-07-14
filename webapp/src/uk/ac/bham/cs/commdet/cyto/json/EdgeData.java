package uk.ac.bham.cs.commdet.cyto.json;

import java.io.Serializable;


public class EdgeData implements Serializable {

	private Edge data;
	
	public EdgeData(Edge data) {
		this.data = data;
	}
	
	public Edge getData() {
		return data;
	}
}
