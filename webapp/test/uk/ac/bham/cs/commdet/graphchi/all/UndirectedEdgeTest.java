package uk.ac.bham.cs.commdet.graphchi.all;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.Test;

public class UndirectedEdgeTest {

	@Test
	public void toByteArray_fromByteArray_correctEdge() {
		int someSource = 2;
		int someTarget = 4;
		double someWeight = 1.5;
		UndirectedEdge edge = new UndirectedEdge(someSource, someTarget, someWeight);
		
		byte[] bytes = UndirectedEdge.toByteArray(edge);
		
		int actualSource = UndirectedEdge.fromByteArray(bytes).getSource();
		int actualTarget = UndirectedEdge.fromByteArray(bytes).getTarget();
		double actualWeight = UndirectedEdge.fromByteArray(bytes).getWeight();
		assertEquals(someSource, actualSource);
		assertEquals(someTarget, actualTarget);
		assertEquals(someWeight, actualWeight, 0.0001);
	}
	
	@Test
	public void fromByteArray_toByteArray_correctEdge() {
		int someSource = 2;
		int someTarget = 4;
		double someWeight = 1.5;
		byte[] bytes = UndirectedEdge.toByteArray(new UndirectedEdge(someSource, someTarget, someWeight));
		
		UndirectedEdge edge = UndirectedEdge.fromByteArray(bytes);
		
		int actualSource = edge.getSource();
		int actualTarget = edge.getTarget();
		double actualWeight = edge.getWeight();
		assertEquals(someSource, actualSource);
		assertEquals(someTarget, actualTarget);
		assertEquals(someWeight, actualWeight, 0.0001);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void fromByteArray_exception() {
		@SuppressWarnings("unused")
		UndirectedEdge edge = UndirectedEdge.fromByteArray(new byte[5]);
	}	

}
