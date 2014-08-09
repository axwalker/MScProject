package uk.ac.bham.cs.commdet.mapper;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GMLMapperTest {
	
	GMLMapper mapper;
	String inputPath;
	String outputPath;
	
	private static final String VALID_GML =
			"graph [" + "\n" + "" +
			"node [" + "\n" + "id 10" + "\n" + "]" + "\n" +
			"node [" + "\n" + "id 11" + "\n" + "]" + "\n" +
			"node [" + "\n" + "id 12" + "\n" + "]" + "\n" +
			"node [" + "\n" + "id 13" + "\n" + "]" + "\n" +
			"node [" + "\n" + "id 14" + "\n" + "]" + "\n" +
			"edge [" + "\n" + "source 10" + "\n" + "target 11" + "\n" + "value 1.0" + "\n" + "]" + "\n" +
			"edge [" + "\n" + "source 10" + "\n" + "target 12" + "\n" + "value 1.5" + "\n" + "]" + "\n" +
			"edge [" + "\n" + "source 11" + "\n" + "target 12" + "\n" + "value 0.5" + "\n" + "]" + "\n" +
			"edge [" + "\n" + "source 12" + "\n" + "target 13" + "\n" + "value 1.0" + "\n" + "]" + "\n" +
			"edge [" + "\n" + "source 12" + "\n" + "target 14" + "\n" + "value 0.5" + "\n" + "]" + "\n" +
			"]";
	
	private static final String GML_NO_REAL_EDGES =
			"graph [\r\n\t" +
			"node [\r\n\t\tid 10\r\n\t]\r\n\t" +
			"]";
	
	private static final String INVALID_TYPES_GML =
			"graph [\r\n\t" +
			"node [\r\n\t\tid A\r\n\t]\r\n\t" +
			"node [\r\n\t\tid B\r\n\t]\r\n\t" +
			"edge [\r\n\t\tsource A\r\n\t\ttarget B\r\n\t\tvalue 1.0\r\n\t]\r\n\t" +
			"]";
	
	private static final String INVALID_FORMAT =
			"A B 1 1" + "\n" +
			"B C 2 1 " + "\n";

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		inputPath = testFolder.getRoot() + "test.edg";
		outputPath = inputPath + "_mapped";
		mapper = new GMLMapper();
	}

	
	@Test
	public void inputGraph_validFile_correctOutput() throws IOException {
		InputStream is = IOUtils.toInputStream(VALID_GML, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
		
		String expectedEdgelist = 
			"0 1 1" + "\n" +
			"0 2 1.5" + "\n" +
			"1 2 0.5" + "\n" +
			"2 3 1" + "\n" +
			"2 4 0.5" + "\n";
		String actualEdgelist = FileUtils.readFileToString(new File(outputPath));
		assertEquals(expectedEdgelist, actualEdgelist);
	}
	
	@Test (expected = IOException.class)
	public void inputGraph_invalidTypes_exception() throws IOException {
		InputStream is = IOUtils.toInputStream(INVALID_TYPES_GML, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
	}
	
	@Test (expected = IOException.class)
	public void inputGraph_invalidFormat_exception() throws IOException {
		InputStream is = IOUtils.toInputStream(INVALID_FORMAT, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
	}
	
	@Test
	public void getExternalId_validFile() throws IOException {
		InputStream is = IOUtils.toInputStream(VALID_GML, "UTF-8");
		int someInternalId = 2;
		
		mapper.inputGraph(inputPath, is);
		
		String expectedId = "12";
		String actualId = mapper.getExternalid(someInternalId);
		assertEquals(expectedId, actualId);
	}
	
	@Test
	public void getInternalId_validFile() throws IOException {
		InputStream is = IOUtils.toInputStream(VALID_GML, "UTF-8");
		String someExternalId = "12";
		
		mapper.inputGraph(inputPath, is);
		
		int expectedId = 2;
		int actualId = mapper.getInternalId(someExternalId);
		assertEquals(expectedId, actualId);
	}
	
	@Test
	public void hasValidGraph_validFile_true() throws IOException {
		InputStream is = IOUtils.toInputStream(VALID_GML, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
		
		assertTrue(mapper.hasValidGraph());
	}
	
	@Test
	public void hasValidGraph_invalidFileWithNoRealEdges_false() throws IOException {
		InputStream is = IOUtils.toInputStream(GML_NO_REAL_EDGES, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
		
		assertFalse(mapper.hasValidGraph());
	}

}
