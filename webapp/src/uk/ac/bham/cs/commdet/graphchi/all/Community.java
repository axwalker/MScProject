package uk.ac.bham.cs.commdet.graphchi.all;

public class Community {
	
	private int seedNode;
	private double internalEdges;
	private double totalEdges;
	private int totalSize;
	private int levelSize;
	
	public Community(int seedNode) {
		this.seedNode = seedNode;
	}
	
	public void increaseInternalEdges(double increase) {
		this.internalEdges += increase;
	}
	
	public void decreaseInternalEdges(double decrease) {
		this.internalEdges -= decrease;
	}
	
	public void increaseTotalEdges(double increase) {
		this.totalEdges += increase;
	}
	
	public void decreaseTotalEdges(double decrease) {
		this.totalEdges -= decrease;
	}
	
	public void increaseTotalSize(int increase) {
		this.totalSize += increase;
	}
	
	public void decreaseTotalSize(int decrease) {
		this.totalSize -= decrease;
	}
	
	public void increaseLevelSize(int increase) {
		this.levelSize += increase;
	}
	
	public void decreaseLevelSize(int decrease) {
		this.levelSize -= decrease;
	}

	public int getSeedNode() {
		return seedNode;
	}

	public void setSeedNode(int seedNode) {
		this.seedNode = seedNode;
	}

	public double getInternalEdges() {
		return internalEdges;
	}

	public void setInternalEdges(double internalEdges) {
		this.internalEdges = internalEdges;
	}

	public double getTotalEdges() {
		return totalEdges;
	}

	public void setTotalEdges(double totalEdges) {
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
