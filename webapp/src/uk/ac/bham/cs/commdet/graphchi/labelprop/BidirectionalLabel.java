package uk.ac.bham.cs.commdet.graphchi.labelprop;

public class BidirectionalLabel {
	int edgeID;
	int smallerOne;
	int largerOne;
	int weight;

	public BidirectionalLabel(int smallerOne, int largerOne, int weight) {
		this.smallerOne = smallerOne;
		this.largerOne = largerOne;
		this.weight = weight;
	}
	
	public BidirectionalLabel(int edgeID, int smallerOne, int largerOne, int weight) {
		this.edgeID = edgeID;
		this.smallerOne = smallerOne;
		this.largerOne = largerOne;
		this.weight = weight;
	}
	
	public int getEdgeID() {
		return edgeID;
	}

	public void setEdgeID(int edgeID) {
		this.edgeID = edgeID;
	}

	public int getSmallerOne() {
		return smallerOne;
	}

	public void setSmallerOne(int smallerOne) {
		this.smallerOne = smallerOne;
	}

	public int getLargerOne() {
		return largerOne;
	}

	public void setLargerOne(int largerOne) {
		this.largerOne = largerOne;
	}	
	
	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + largerOne;
		result = prime * result + smallerOne;
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
		BidirectionalLabel other = (BidirectionalLabel) obj;
		if (largerOne != other.largerOne)
			return false;
		if (smallerOne != other.smallerOne)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "id: " + edgeID + ", smaller: " + smallerOne + ", larger: " + largerOne + ", weight: " + weight;
	}
}