package uk.ac.bham.cs.commdet.graphchi.json;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

import uk.ac.bham.cs.commdet.graphchi.json.JSONConverterEdgelistWeighted;

public class JSONConverterEdgelistWeightedTest {

	String edgelist1 = "1 3 8 \n" +
			  "1 4 8 \n" +
			  "2 7 9 \n";
	String testFilepath = "testdata/weightedEdgelistTest.txt";
	JSONConverterEdge converter = new JSONConverterEdgelistWeighted();
	
	@Test
	public void getEdgesJsonTest() throws UnsupportedEncodingException {
		InputStream in = new ByteArrayInputStream(edgelist1.getBytes("UTF-8"));
		String expected = "[{\"data\":{\"id\":\"e0\",\"source\":\"n1\",\"target\":\"n3\",\"weight\":\"8\"}}," +
				"{\"data\":{\"id\":\"e1\",\"source\":\"n1\",\"target\":\"n4\",\"weight\":\"8\"}}," +
				"{\"data\":{\"id\":\"e2\",\"source\":\"n2\",\"target\":\"n7\",\"weight\":\"9\"}}]";
		
		String actual = converter.getEdgesJson(in).toString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void getEdgesJsonFileTest() throws FileNotFoundException {
		InputStream in = new FileInputStream(testFilepath);
		String expected = "[{\"data\":{\"id\":\"e0\",\"source\":\"n1\",\"target\":\"n3\",\"weight\":\"8\"}}," +
				"{\"data\":{\"id\":\"e1\",\"source\":\"n1\",\"target\":\"n4\",\"weight\":\"8\"}}," +
				"{\"data\":{\"id\":\"e2\",\"source\":\"n2\",\"target\":\"n7\",\"weight\":\"9\"}}]";
		
		String actual = converter.getEdgesJson(in).toString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void getEdgeTest() {
		String edge = "1 3 8";
		String expected = "{\"data\":{\"id\":\"e1\",\"source\":\"n1\",\"target\":\"n3\",\"weight\":\"8\"}}";
		
		JSONConverterEdgelistWeighted converter = new JSONConverterEdgelistWeighted();
		String actual = converter.getEdge(1, edge).toString();
		
		assertEquals(expected, actual);
	}

	@Test (expected = IllegalArgumentException.class)
	public void getEdgeTestException() {
		String edge = "1 3";
		JSONConverterEdgelistWeighted converter = new JSONConverterEdgelistWeighted();
		converter.getEdge(1, edge);
	}

}