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
	
	
}
