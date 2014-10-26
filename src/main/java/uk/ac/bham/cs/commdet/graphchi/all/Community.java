package uk.ac.bham.cs.commdet.graphchi.all;

/**
 * A community with a unique seed node that is used as a label for the
 * community. Internal edges is the number of half edges between vertices inside
 * the community. So one edge between two vertices in the community is
 * considered 2 half edges. Total edges is the total number of half edges from
 * all nodes in the community to any other node in the graph. Total size is the
 * total number of vertices this community has across the whole hierarchy while
 * level size considers only the nodes attached to the community at this level
 * of the hierarchy.
 */
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
