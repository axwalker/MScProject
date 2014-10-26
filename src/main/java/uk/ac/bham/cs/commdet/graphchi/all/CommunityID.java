package uk.ac.bham.cs.commdet.graphchi.all;

/**
 * The id label of a community and the level of the hierarchy it belongs to.
 */
public class CommunityID {

	private int id;
	private int level;

	public CommunityID(int id, int level) {
		this.id = id;
		this.level = level;
	}

	public int getId() {
		return id;
	}

	public int getLevel() {
		return level;
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
		CommunityID other = (CommunityID) obj;
		if (id != other.id)
			return false;
		if (level != other.level)
			return false;
		return true;
	}

}
