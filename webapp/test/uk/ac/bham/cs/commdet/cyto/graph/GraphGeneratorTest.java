package uk.ac.bham.cs.commdet.cyto.graph;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import uk.ac.bham.cs.commdet.graphchi.all.CommunityID;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import uk.ac.bham.cs.commdet.mapper.FileMapper;

public class GraphGeneratorTest {

	private GraphGenerator generator;
	private GraphResult result;
	private Comparator<NodeData> nodeComparator;
	private Comparator<EdgeData> edgeComparator;

	private static final String COMMUNITY_ONE_LEVEL_ZERO =
			"1 2 1" + "\n" +
					"1 3 1" + "\n" +
					"2 3 1" + "\n" +
					"2 4 1" + "\n" +
					"3 4 1" + "\n";

	private static final String COMMUNITY_FIVE_LEVEL_ZERO =
			"5 6 1" + "\n" +
					"5 8 1" + "\n" +
					"6 7 1" + "\n" +
					"6 8 1" + "\n" +
					"7 8 1" + "\n";

	private static final String COMMUNITY_NINE_LEVEL_ZERO =
			"9 10 1" + "\n" +
					"9 12 1" + "\n" +
					"10 11 1" + "\n" +
					"10 12 1" + "\n" +
					"11 12 1" + "\n";

	private static final String COMMUNITY_THIRTEEN_LEVEL_ZERO =
			"13 14 1" + "\n" +
					"13 15 1" + "\n" +
					"14 15 1" + "\n" +
					"14 16 1" + "\n" +
					"15 16 1" + "\n";

	private static final String INTER_COMMUNITY_LEVEL_ZERO =
			"2 6 1" + "\n" +
					"4 5 1" + "\n" +
					"4 9 1" + "\n" +
					"5 13 1" + "\n" +
					"9 13 1" + "\n" +
					"12 15 1" + "\n";

	private static final String TEST_EDGELIST_LEVEL_ZERO =
			COMMUNITY_ONE_LEVEL_ZERO +
			COMMUNITY_FIVE_LEVEL_ZERO +
			COMMUNITY_NINE_LEVEL_ZERO +
			COMMUNITY_THIRTEEN_LEVEL_ZERO +
			INTER_COMMUNITY_LEVEL_ZERO;

	private static final String TEST_EDGELIST_LEVEL_ONE =
			"1 5 2" +"\n" +
					"1 9 1" +"\n" +
					"5 13 1" +"\n" +
					"9 13 2" +"\n";

	private static final String TEST_EDGELIST_LEVEL_TWO =
			"1 9 2" +"\n";


	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setUp() throws IOException {
		String baseEdgelistFileName = "test.edg_mapped";

		File fileLevelZero = testFolder.newFile(baseEdgelistFileName);
		FileUtils.writeStringToFile(fileLevelZero, TEST_EDGELIST_LEVEL_ZERO);

		File fileLevelOne = testFolder.newFile(baseEdgelistFileName + "_pass_1");
		FileUtils.writeStringToFile(fileLevelOne, TEST_EDGELIST_LEVEL_ONE);		

		File fileLevelTwo = testFolder.newFile(baseEdgelistFileName + "_pass_2");
		FileUtils.writeStringToFile(fileLevelTwo, TEST_EDGELIST_LEVEL_TWO);		

		String filename = fileLevelZero.getPath();

		Map<Integer, List<Integer>> hierarchy = new HashMap<Integer, List<Integer>>();
		hierarchy.put(1, Arrays.asList(1, 1));
		hierarchy.put(2, Arrays.asList(1, 1));
		hierarchy.put(3, Arrays.asList(1, 1));
		hierarchy.put(4, Arrays.asList(1, 1));
		hierarchy.put(5, Arrays.asList(5, 1));
		hierarchy.put(6, Arrays.asList(5, 1));
		hierarchy.put(7, Arrays.asList(5, 1));
		hierarchy.put(8, Arrays.asList(5, 1));
		hierarchy.put(9, Arrays.asList(9, 9));
		hierarchy.put(10, Arrays.asList(9, 9));
		hierarchy.put(11, Arrays.asList(9, 9));
		hierarchy.put(12, Arrays.asList(9, 9));
		hierarchy.put(13, Arrays.asList(13, 9));
		hierarchy.put(14, Arrays.asList(13, 9));
		hierarchy.put(15, Arrays.asList(13, 9));
		hierarchy.put(16, Arrays.asList(13, 9));

		Map<CommunityID, Integer> sizes = new HashMap<CommunityID, Integer>();
		sizes.put(new CommunityID(1, 0), 4);
		sizes.put(new CommunityID(5, 0), 4);
		sizes.put(new CommunityID(9, 0), 4);
		sizes.put(new CommunityID(13, 0), 4);
		sizes.put(new CommunityID(1, 1), 8);
		sizes.put(new CommunityID(9, 1), 8);

		Map<Integer, Double> modularities = new HashMap<Integer, Double>();
		modularities.put(0, 0.45);
		modularities.put(1, 0.3);


		int height = 2;

		result = new GraphResult(filename, hierarchy, sizes, modularities, height);
		result.writeAllSortedEdgeLists();
		result.setMapper(new TestMapper());
		generator = new GraphGenerator(result);
		generator.setIncludeEdges(true);

		//for sorting lists as order is unimportant on output, only equality of elements matters
		nodeComparator = new Comparator<NodeData>() {
			@Override
			public int compare(NodeData arg0, NodeData arg1) {
				return arg0.getData().getId().compareTo(arg1.getData().getId());
			}
		};
		edgeComparator = new Comparator<EdgeData>() {
			@Override
			public int compare(EdgeData arg0, EdgeData arg1) {
				int result = arg0.getData().getSource().compareTo(arg1.getData().getSource());
				if (result != 0) {
					result = arg0.getData().getTarget().compareTo(arg1.getData().getTarget());
				}
				return result;
			}
		};
	}

	@Test
	public void parseGraph_topLevel_correctNodes() {

		generator.parseGraph(result.getHeight(), result.getHeight());

		List<NodeData> expectedNodes = new ArrayList<NodeData>();
		expectedNodes.add(new NodeData(new Node(1 + "", 8)));
		expectedNodes.add(new NodeData(new Node(9 + "", 8)));
		Collections.sort(expectedNodes, nodeComparator);

		List<NodeData> actualNodes = generator.getGraph().getNodes();
		Collections.sort(actualNodes, nodeComparator);
		assertEquals(expectedNodes, actualNodes);
	}

	@Test
	public void parseGraph_levelOne_correctNodes() {

		generator.parseGraph(1, result.getHeight());

		List<NodeData> expectedNodes = new ArrayList<NodeData>();
		expectedNodes.add(new NodeData(new Node(1 + "", 4)));
		expectedNodes.add(new NodeData(new Node(5 + "", 4)));
		expectedNodes.add(new NodeData(new Node(9 + "", 4)));
		expectedNodes.add(new NodeData(new Node(13 + "", 4)));
		Collections.sort(expectedNodes, nodeComparator);

		List<NodeData> actualNodes = generator.getGraph().getNodes();
		Collections.sort(actualNodes, nodeComparator);
		assertEquals(expectedNodes, actualNodes);
	}

	@Test
	public void parseGraph_levelZero_correctNodes() {

		generator.parseGraph(0, result.getHeight());

		List<NodeData> expectedNodes = new ArrayList<NodeData>();
		expectedNodes.add(new NodeData(new Node(1 + "", 1)));
		expectedNodes.add(new NodeData(new Node(2 + "", 1)));
		expectedNodes.add(new NodeData(new Node(3 + "", 1)));
		expectedNodes.add(new NodeData(new Node(4 + "", 1)));
		expectedNodes.add(new NodeData(new Node(5 + "", 1)));
		expectedNodes.add(new NodeData(new Node(6 + "", 1)));
		expectedNodes.add(new NodeData(new Node(7 + "", 1)));
		expectedNodes.add(new NodeData(new Node(8 + "", 1)));
		expectedNodes.add(new NodeData(new Node(9 + "", 1)));
		expectedNodes.add(new NodeData(new Node(10 + "", 1)));
		expectedNodes.add(new NodeData(new Node(11 + "", 1)));
		expectedNodes.add(new NodeData(new Node(12 + "", 1)));
		expectedNodes.add(new NodeData(new Node(13 + "", 1)));
		expectedNodes.add(new NodeData(new Node(14 + "", 1)));
		expectedNodes.add(new NodeData(new Node(15 + "", 1)));
		expectedNodes.add(new NodeData(new Node(16 + "", 1)));
		Collections.sort(expectedNodes, nodeComparator);

		List<NodeData> actualNodes = generator.getGraph().getNodes();
		Collections.sort(actualNodes, nodeComparator);
		assertEquals(expectedNodes, actualNodes);
	}

	@Test
	public void parseGraph_levelZeroCommunityOne_correctNodes() {
		int community = 1;
		int communityLevel = 0;
		int fileLevel = 0;
		int someColourLevel = 1;

		generator.parseCommunity(community, communityLevel, fileLevel, someColourLevel);

		List<NodeData> expectedNodes = new ArrayList<NodeData>();
		expectedNodes.add(new NodeData(new Node(1 + "", 1)));
		expectedNodes.add(new NodeData(new Node(2 + "", 1)));
		expectedNodes.add(new NodeData(new Node(3 + "", 1)));
		expectedNodes.add(new NodeData(new Node(4 + "", 1)));
		Collections.sort(expectedNodes, nodeComparator);

		List<NodeData> actualNodes = generator.getGraph().getNodes();
		Collections.sort(actualNodes, nodeComparator);
		assertEquals(expectedNodes.toString(), actualNodes.toString());
	}

	@Test
	public void parseGraph_levelOneCommunityOne_correctNodes() {
		int community = 1;
		int communityLevel = 1;
		int fileLevel = 0;
		int someColourLevel = 1;

		generator.parseCommunity(community, communityLevel, fileLevel, someColourLevel);

		List<NodeData> expectedNodes = new ArrayList<NodeData>();
		expectedNodes.add(new NodeData(new Node(1 + "", 1)));
		expectedNodes.add(new NodeData(new Node(2 + "", 1)));
		expectedNodes.add(new NodeData(new Node(3 + "", 1)));
		expectedNodes.add(new NodeData(new Node(4 + "", 1)));
		expectedNodes.add(new NodeData(new Node(5 + "", 1)));
		expectedNodes.add(new NodeData(new Node(6 + "", 1)));
		expectedNodes.add(new NodeData(new Node(7 + "", 1)));
		expectedNodes.add(new NodeData(new Node(8 + "", 1)));
		Collections.sort(expectedNodes, nodeComparator);

		List<NodeData> actualNodes = generator.getGraph().getNodes();
		Collections.sort(actualNodes, nodeComparator);
		assertEquals(expectedNodes.toString(), actualNodes.toString());
	}

	@Test
	public void parseGraph_topLevelOfHierarchy_correctEdges() {

		generator.parseGraph(result.getHeight(), result.getHeight());

		List<EdgeData> expectedEdges = new ArrayList<EdgeData>();
		expectedEdges.add(new EdgeData(new Edge(1 + "", 9 + "", 2)));
		Collections.sort(expectedEdges, edgeComparator);

		List<EdgeData> actualEdges = generator.getGraph().getEdges();
		Collections.sort(actualEdges, edgeComparator);
		assertEquals(expectedEdges, actualEdges);
	}
	
	@Test
	public void parseGraph_levelOne_correctEdges() {

		generator.parseGraph(1, result.getHeight());

		List<EdgeData> expectedEdges = new ArrayList<EdgeData>();
		expectedEdges.add(new EdgeData(new Edge(1 + "", 5 + "", 2)));
		expectedEdges.add(new EdgeData(new Edge(1 + "", 9 + "", 1)));
		expectedEdges.add(new EdgeData(new Edge(5 + "", 13 + "", 1)));
		expectedEdges.add(new EdgeData(new Edge(9 + "", 13 + "", 2)));
		Collections.sort(expectedEdges, edgeComparator);

		List<EdgeData> actualEdges = generator.getGraph().getEdges();
		Collections.sort(actualEdges, edgeComparator);
		assertEquals(expectedEdges, actualEdges);
	}
	
	@Test
	public void parseGraph_communityOneLevelZero_correctEdges() {
		int community = 1;
		int communityLevel = 0;
		int fileLevel = 0;
		int someColourLevel = 1;

		generator.parseCommunity(community, communityLevel, fileLevel, someColourLevel);

		List<EdgeData> expectedEdges = new ArrayList<EdgeData>();
		expectedEdges.add(new EdgeData(new Edge(1 + "", 2 + "", 1)));
		expectedEdges.add(new EdgeData(new Edge(1 + "", 3 + "", 1)));
		expectedEdges.add(new EdgeData(new Edge(2 + "", 3 + "", 1)));
		expectedEdges.add(new EdgeData(new Edge(2 + "", 4 + "", 1)));
		expectedEdges.add(new EdgeData(new Edge(3 + "", 4 + "", 1)));
		Collections.sort(expectedEdges, edgeComparator);

		List<EdgeData> actualEdges = generator.getGraph().getEdges();
		Collections.sort(actualEdges, edgeComparator);
		assertEquals(expectedEdges.toString(), actualEdges.toString());
	}

	@Test
	public void parseGraph_topLevel_correctMetadata() {

		generator.parseGraph(result.getHeight(), result.getHeight());

		Metadata expectedMetadata = new Metadata();
		expectedMetadata.setAvgCommunitySize(8);
		expectedMetadata.setCurrentLevel(2);
		expectedMetadata.setHierarchyHeight(2);
		expectedMetadata.setMaxCommunitySize(8);
		expectedMetadata.setMaxEdgeConnection(2);
		expectedMetadata.setMinCommunitySize(8);
		expectedMetadata.setModularity(0.3);
		expectedMetadata.setNoOfCommunities(2);

		Metadata actualMetadata = generator.getGraph().getMetadata();
		assertEquals(expectedMetadata, actualMetadata);
	}
	
	@Test
	public void parseGraph_levelOne_correctMetadata() {

		generator.parseGraph(1, result.getHeight());

		Metadata expectedMetadata = new Metadata();
		expectedMetadata.setAvgCommunitySize(4);
		expectedMetadata.setCurrentLevel(1);
		expectedMetadata.setHierarchyHeight(2);
		expectedMetadata.setMaxCommunitySize(4);
		expectedMetadata.setMaxEdgeConnection(2);
		expectedMetadata.setMinCommunitySize(4);
		expectedMetadata.setModularity(0.45);
		expectedMetadata.setNoOfCommunities(4);

		Metadata actualMetadata = generator.getGraph().getMetadata();
		assertEquals(expectedMetadata, actualMetadata);
	}


}

class TestMapper implements FileMapper {

	@Override
	public String getExternalid(int internalId) {
		return internalId + "";
	}

	@Override
	public int getInternalId(String externalID) {
		//only using integers in the tests so safe to convert
		return Integer.parseInt(externalID);
	}

	@Override
	public void inputGraph(String filename) throws IOException {}

	@Override
	public boolean hasValidGraph() {
		return false;
	}

	@Override
	public Map<String, Object> getExternalMetadata(int internalId) {
		return new HashMap<String, Object>();
	}

}