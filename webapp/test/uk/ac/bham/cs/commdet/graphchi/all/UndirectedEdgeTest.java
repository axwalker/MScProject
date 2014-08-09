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
	
	@Test
	public void test() throws IOException {
		System.out.println("WRITING TO TEST FILE");
		RandomAccessFile file = new RandomAccessFile("testRAF", "rw");
		UndirectedEdge edge1 = new UndirectedEdge(2, 4, 0.5);
		UndirectedEdge edge2 = new UndirectedEdge(2, 4, 0.5);
		UndirectedEdge edge3 = new UndirectedEdge(7, 13, 4.5);
		byte[] bytes1 = UndirectedEdge.toByteArray(edge1);
		file.write(bytes1);
		byte[] bytes2 = UndirectedEdge.toByteArray(edge2);
		file.write(bytes2);
		byte[] bytes3 = UndirectedEdge.toByteArray(edge3);
		file.write(bytes3);
		file.close();
		
		System.out.println("READING FROM TEST FILE");
		RandomAccessFile file2 = new RandomAccessFile("testRAF", "r");
		while (file2.getFilePointer() < file2.length()) {
			byte[] bytes = new byte[12];
			System.out.println("bytes read: " + file2.read(bytes));
			System.out.println(UndirectedEdge.fromByteArray(bytes));
        }
		file2.close();
		
	}
	

}
