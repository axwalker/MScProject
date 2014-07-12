package uk.ac.bham.cs.commdet.graphchi.louvain;

public class CommunityEdgePositions {
	
	private int startIndex;
	private int endIndex; //exclusive

	public CommunityEdgePositions(int startIndex, int endIndex) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}
	
	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	@Override
	public String toString() {
		return "[startIndex=" + startIndex + ", endIndex=" + endIndex + "]";
	}

}
