package uk.ac.bham.cs.commdet.graphchi.all;

public class Community {
	
	private int seedNode;
	private int internalEdges;
	private int totalEdges;
	private int totalSize;
	private int levelSize;
	
	public Community(int seedNode) {
		this.seedNode = seedNode;
	}
	
	public void increaseInternalEdges(int increase) {
		this.internalEdges += increase;
	}
	
	public void decreaseInternalEdges(int decrease) {
		this.internalEdges -= decrease;
	}
	
	public void increaseTotalEdges(int increase) {
		this.totalEdges += increase;
	}
	
	public void decreaseTotalEdges(int decrease) {
		this.totalEdges -= decrease;
	}
	
	public void increaseTotalSize(int increase) {
		this.totalSize += increase;
	}
	
	public void decreaseTotalSize(int decrease) {
		this.totalSize -= decrease;
	}

	public int getSeedNode() {
		return seedNode;
	}

	public void setSeedNode(int seedNode) {
		this.seedNode = seedNode;
	}

	public int getInternalEdges() {
		return internalEdges;
	}

	public void setInternalEdges(int internalEdges) {
		this.internalEdges = internalEdges;
	}

	public int getTotalEdges() {
		return totalEdges;
	}

	public void setTotalEdges(int totalEdges) {
		this.totalEdges = totalEdges;
	}

	public int getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(int totalSize) {
		this.totalSize = totalSize;
	}

	public int getLevelSize() {
		return levelSize;
	}

	public void setLevelSize(int levelSize) {
		this.levelSize = levelSize;
	}

	@Override
	public String toString() {
		return "Community [seedNode=" + seedNode + ", internalEdges="
				+ internalEdges + ", totalEdges=" + totalEdges + ", totalSize="
				+ totalSize + ", levelSize=" + levelSize + "]";
	}
	
}
