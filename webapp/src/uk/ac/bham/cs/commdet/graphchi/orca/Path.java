package uk.ac.bham.cs.commdet.graphchi.orca;

public class Path {

	private int from;
	private int to;
	private int weight;
	private boolean adjacent;
	
	public Path(int from, int to, int weight) {
		this.from = from;
		this.to = to;
		this.weight = weight;
	}

	public int getFrom() {
		return from;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public int getTo() {
		return to;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
	

	@Override
	public String toString() {
		return "[from=" + from + ", to=" + to + ", weight=" + weight + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + from;
		result = prime * result + to;
		result = prime * result + weight;
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
		Path other = (Path) obj;
		if (from != other.from)
			return false;
		if (to != other.to)
			return false;
		if (weight != other.weight)
			return false;
		return true;
	}

	public boolean isAdjacent() {
		return adjacent;
	}

	public void setAdjacent(boolean adjacent) {
		this.adjacent = adjacent;
	}
	
	
}
