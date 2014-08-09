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

public class EdgelistMapperTest {
	
	EdgelistMapper mapper;
	String inputPath;
	String outputPath;
	
	private static final String VALID_EDGE_LIST =
			"A B 1" + "\n" +
			"A C 1.5" + "\n" +
			"B C 0.5" + "\n" +
			"C D 1" + "\n" +
			"C E 0.5" + "\n";
	
	private static final String EDGE_LIST_NO_REAL_EDGES =
			"A A 1" + "\n" +
			"B B 1.5" + "\n";
	
	private static final String INVALID_TYPES_EDGE_LIST =
			"A B E" + "\n" +
			"B C G" + "\n";
	
	private static final String INVALID_NUMBER_ARGUMENTS_EDGE_LIST =
			"A B 1 1" + "\n" +
			"B C 2 1 " + "\n";

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws IOException {
		inputPath = testFolder.getRoot() + "test.edg";
		outputPath = inputPath + "_mapped";
		mapper = new EdgelistMapper();
	}

	
	@Test
	public void inputGraph_validFile_correctOutput() throws IOException {
		InputStream is = IOUtils.toInputStream(VALID_EDGE_LIST, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
		
		String expectedEdgelist = 
			"0 1 1.0" + "\n" +
			"0 2 1.5" + "\n" +
			"1 2 0.5" + "\n" +
			"2 3 1.0" + "\n" +
			"2 4 0.5" + "\n";
		String actualEdgelist = FileUtils.readFileToString(new File(outputPath));
		assertEquals(expectedEdgelist, actualEdgelist);
	}
	
	@Test (expected = IOException.class)
	public void inputGraph_invalidTypes_exception() throws IOException {
		InputStream is = IOUtils.toInputStream(INVALID_TYPES_EDGE_LIST, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
	}
	
	@Test (expected = IOException.class)
	public void inputGraph_invalidNumberArguments_exception() throws IOException {
		InputStream is = IOUtils.toInputStream(INVALID_NUMBER_ARGUMENTS_EDGE_LIST, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
	}
	
	@Test
	public void getExternalId_validFile() throws IOException {
		InputStream is = IOUtils.toInputStream(VALID_EDGE_LIST, "UTF-8");
		int someInternalId = 2;
		
		mapper.inputGraph(inputPath, is);
		
		String expectedId = "C";
		String actualId = mapper.getExternalid(someInternalId);
		assertEquals(expectedId, actualId);
	}
	
	@Test
	public void getInternalId_validFile() throws IOException {
		InputStream is = IOUtils.toInputStream(VALID_EDGE_LIST, "UTF-8");
		String someExternalId = "C";
		
		mapper.inputGraph(inputPath, is);
		
		int expectedId = 2;
		int actualId = mapper.getInternalId(someExternalId);
		assertEquals(expectedId, actualId);
	}
	
	@Test
	public void hasValidGraph_validFile_true() throws IOException {
		InputStream is = IOUtils.toInputStream(VALID_EDGE_LIST, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
		
		assertTrue(mapper.hasValidGraph());
	}
	
	@Test
	public void hasValidGraph_invalidFileWithNoRealEdges_false() throws IOException {
		InputStream is = IOUtils.toInputStream(EDGE_LIST_NO_REAL_EDGES, "UTF-8");
		
		mapper.inputGraph(inputPath, is);
		
		assertFalse(mapper.hasValidGraph());
	}

}
