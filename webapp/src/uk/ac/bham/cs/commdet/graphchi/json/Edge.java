package uk.ac.bham.cs.commdet.graphchi.json;

class Edge {
	private int id;
	private int source;
	private int target;
	
	public Edge(int id, int source, int target) {
		this.id = id;
		this.source = source;
		this.target = target;
	}	
	
	public int getId() {
		return id;
	}

	public int getSource() {
		return source;
	}

	public int getTarget() {
		return target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + source;
		result = prime * result + target;
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
		Edge other = (Edge) obj;
		if (source != other.source)
			return false;
		if (target != other.target)
			return false;
		return true;
	}

}