package uk.ac.bham.cs.commdet.graphchi.louvain;

public class Community {

	private int id;
	private int level;
	
	public Community(int id, int level) {
		this.id = id;
		this.level = level;
	}

	@Override
	public String toString() {
		return "[id=" + id + ", level=" + level + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + level;
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
		Community other = (Community) obj;
		if (id != other.id)
			return false;
		if (level != other.level)
			return false;
		return true;
	}
	
}
