package uk.ac.bham.cs.commdet.graphchi.all;

import java.io.Serializable;

public class CommunityIdentity implements Serializable{

	private int id;
	private int level;
	
	public CommunityIdentity(int id, int level) {
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
		CommunityIdentity other = (CommunityIdentity) obj;
		if (id != other.id)
			return false;
		if (level != other.level)
			return false;
		return true;
	}
	
}
