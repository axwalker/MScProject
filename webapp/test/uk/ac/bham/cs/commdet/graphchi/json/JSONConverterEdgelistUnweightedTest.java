package uk.ac.bham.cs.commdet.graphchi.json;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

import uk.ac.bham.cs.commdet.graphchi.json.JSONConverterEdgelistUnweighted;

public class JSONConverterEdgelistUnweightedTest {

	String edgelist1 = "1 3 \n" +
			  "1 4 \n" +
			  "2 7 \n";
	String testFilepath = "testdata/unweightedEdgelistTest.txt";
	JSONConverterEdge converter = new JSONConverterEdgelistUnweighted();
	
	@Test
	public void getEdgesJsonTest() throws UnsupportedEncodingException {
		InputStream in = new ByteArrayInputStream(edgelist1.getBytes("UTF-8"));
		String expected = "[{\"data\":{\"id\":\"e0\",\"source\":\"n1\",\"target\":\"n3\"}}," +
				"{\"data\":{\"id\":\"e1\",\"source\":\"n1\",\"target\":\"n4\"}}," +
				"{\"data\":{\"id\":\"e2\",\"source\":\"n2\",\"target\":\"n7\"}}]";
		
		String actual = converter.getEdgesJson(in).toString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void getEdgesJsonFileTest() throws FileNotFoundException {
		InputStream in = new FileInputStream(testFilepath);
		String expected = "[{\"data\":{\"id\":\"e0\",\"source\":\"n1\",\"target\":\"n3\"}}," +
				"{\"data\":{\"id\":\"e1\",\"source\":\"n1\",\"target\":\"n4\"}}," +
				"{\"data\":{\"id\":\"e2\",\"source\":\"n2\",\"target\":\"n7\"}}]";
		
		String actual = converter.getEdgesJson(in).toString();
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void getEdgeTest() {
		String edge = "1 3";
		String expected = "{\"data\":{\"id\":\"e1\",\"source\":\"n1\",\"target\":\"n3\"}}";
		
		JSONConverterEdgelistUnweighted converter = new JSONConverterEdgelistUnweighted();
		String actual = converter.getEdge(1, edge).toString();
		
		assertEquals(expected, actual);
	}

	@Test (expected = IllegalArgumentException.class)
	public void getEdgeTestException() {
		String edge = "1 3 8";
		JSONConverterEdgelistUnweighted converter = new JSONConverterEdgelistUnweighted();
		converter.getEdge(1, edge);
	}

}
