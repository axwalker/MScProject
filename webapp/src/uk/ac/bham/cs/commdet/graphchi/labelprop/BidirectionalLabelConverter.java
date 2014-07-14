package uk.ac.bham.cs.commdet.graphchi.labelprop;

import edu.cmu.graphchi.datablocks.BytesToValueConverter;

public class BidirectionalLabelConverter implements BytesToValueConverter<BidirectionalLabel> {

	@Override
	public BidirectionalLabel getValue(byte[] array) {
		return new BidirectionalLabel(
				((array[3]  & 0xff) << 24) + ((array[2] & 0xff) << 16) + ((array[1] & 0xff) << 8) + (array[0] & 0xff),
				((array[7]  & 0xff) << 24) + ((array[6] & 0xff) << 16) + ((array[5] & 0xff) << 8) + (array[4] & 0xff),
				((array[11]  & 0xff) << 24) + ((array[10] & 0xff) << 16) + ((array[9] & 0xff) << 8) + (array[8] & 0xff));
	}

	@Override
	public void setValue(byte[] array, BidirectionalLabel arg1) {
		array[0] = (byte) ((arg1.getSmallerOne()) & 0xff);
		array[1] = (byte) ((arg1.getSmallerOne() >>> 8) & 0xff);
		array[2] = (byte) ((arg1.getSmallerOne() >>> 16) & 0xff);
		array[3] = (byte) ((arg1.getSmallerOne() >>> 24) & 0xff);
		array[4] = (byte) ((arg1.getLargerOne()) & 0xff);
		array[5] = (byte) ((arg1.getLargerOne() >>> 8) & 0xff);
		array[6] = (byte) ((arg1.getLargerOne() >>> 16) & 0xff);
		array[7] = (byte) ((arg1.getLargerOne() >>> 24) & 0xff);
		array[8] = (byte) ((arg1.getWeight()) & 0xff);
		array[9] = (byte) ((arg1.getWeight() >>> 8) & 0xff);
		array[10] = (byte) ((arg1.getWeight() >>> 16) & 0xff);
		array[11] = (byte) ((arg1.getWeight() >>> 24) & 0xff);
	}

	@Override
	public int sizeOf() {
		return 12;
	}

}