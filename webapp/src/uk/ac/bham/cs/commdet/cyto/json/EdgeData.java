package uk.ac.bham.cs.commdet.cyto.json;

import uk.ac.bham.cs.commdet.graphchi.louvain.UndirectedEdge;


public class EdgeData {

	private Edge data;
	
	public EdgeData(Edge data) {
		this.data = data;
	}
	
	public Edge getData() {
		return data;
	}
}
