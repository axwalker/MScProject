package sample;

public class BidirectionalLabel {
	int smallerOne;
	int largerOne;

	public BidirectionalLabel(int smallerOne, int largerOne) {
		this.smallerOne = smallerOne;
		this.largerOne = largerOne;
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
	
	@Override
	public String toString() {
		return "smaller: " + smallerOne + ", larger: " + largerOne;
	}
}