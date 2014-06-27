package uk.ac.bham.cs.commdet.graphchi.json;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

import uk.ac.bham.cs.commdet.graphchi.json.JSONConverterEdgelistUnweighted;

public class JSONConverterNodeIntTest {

	String testFilepath = "testdata/nodetest.txt.4Bj.vout";
	JSONConverterNode converter = new JSONConverterNodeInt();
	
	@Test
	public void getEdgesJsonFileTest() throws FileNotFoundException {
		InputStream in = new FileInputStream(testFilepath);
		String expected = "[{\"data\":{\"id\":\"e0\",\"source\":\"1\",\"target\":\"3\"}}," +
				"{\"data\":{\"id\":\"e1\",\"source\":\"1\",\"target\":\"4\"}}," +
				"{\"data\":{\"id\":\"e2\",\"source\":\"2\",\"target\":\"7\"}}]";
		
		String actual = converter.getNodesJson(in).toString();
		
		assertEquals(expected, actual);
	}

}
