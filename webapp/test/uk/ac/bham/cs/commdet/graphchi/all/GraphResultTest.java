package uk.ac.bham.cs.commdet.graphchi.all;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GraphResultTest {

	private GraphResult result;
	private Map<Integer, List<Integer>> hierarchy;
	private double someWeight = 0.5;
	private UndirectedEdge e1 = new UndirectedEdge(1, 2, someWeight);
	private UndirectedEdge e2 = new UndirectedEdge(2, 3, someWeight);
	private UndirectedEdge e3 = new UndirectedEdge(3, 4, someWeight);
	private UndirectedEdge e5 = new UndirectedEdge(5, 6, someWeight);
	private UndirectedEdgeComparator comparator;
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	@Before
	public void setUp() {
		String filename = "";
		
		hierarchy = new HashMap<Integer, List<Integer>>();
		hierarchy.put(1, Arrays.asList(1, 2));
		hierarchy.put(2, Arrays.asList(1, 2));
		hierarchy.put(3, Arrays.asList(2, 2));
		hierarchy.put(4, Arrays.asList(2, 2));
		hierarchy.put(5, Arrays.asList(3, 1));
		hierarchy.put(6, Arrays.asList(3, 1));

		Map<CommunityID, Integer> sizes = new HashMap<CommunityID, Integer>();
		sizes.put(new CommunityID(2, 1), 4);
		sizes.put(new CommunityID(1, 1), 2);
		sizes.put(new CommunityID(1, 0), 2);
		sizes.put(new CommunityID(2, 0), 2);
		sizes.put(new CommunityID(3, 0), 2);
		
		Map<Integer, Double> modularities = new HashMap<Integer, Double>();
		int height = 2;
		
		result = new GraphResult(filename, hierarchy, sizes, modularities, height);
	}
	
	@Test
	public void writeSortedEdgeList_correctEdges() throws IOException {
		RandomAccessFile file = new RandomAccessFile(testFolder.newFile(), "rw");
		UndirectedEdge[] edgeList = {e1, e2};
		int someLevel = 0;
		comparator = new UndirectedEdgeComparator(hierarchy, someLevel);
		TreeSet<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(comparator);
		edges.addAll(Arrays.asList(edgeList));
		
		result.writeSortedEdgeList(file, edges, someLevel);
		
		file.seek(0);
		byte[] e1bytes = new byte[12];
		file.read(e1bytes);
		UndirectedEdge actualE1 = UndirectedEdge.fromByteArray(e1bytes);
		assertEquals(e1, actualE1);
		byte[] e2bytes = new byte[12];
		file.read(e2bytes);
		UndirectedEdge actualE2 = UndirectedEdge.fromByteArray(e2bytes);
		assertEquals(e2, actualE2);
	}

	@Test
	public void generateCommunityPositions_firstLevel() {
		UndirectedEdge[] edgeList = {e1, e2, e3, e5};
		comparator = new UndirectedEdgeComparator(hierarchy, 0);
		TreeSet<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(comparator);
		edges.addAll(Arrays.asList(edgeList));
		
		result.generateCommunityPositions(edges, 0);
		
		Map<CommunityID, CommunityEdgePositions> expectedPositions = new HashMap<CommunityID, CommunityEdgePositions>();
		expectedPositions.put(new CommunityID(3, 0), new CommunityEdgePositions(0, 1));
		expectedPositions.put(new CommunityID(1, 0), new CommunityEdgePositions(1, 2));
		expectedPositions.put(new CommunityID(2, 0), new CommunityEdgePositions(2, 3));
		expectedPositions.put(new CommunityID(1, 1), new CommunityEdgePositions(0, 1));
		expectedPositions.put(new CommunityID(2, 1), new CommunityEdgePositions(1, 3));
		Map<CommunityID, CommunityEdgePositions> actualPositions = result.getAllEdgePositions().get(0);
		assertEquals(expectedPositions.toString(), actualPositions.toString());
	}

	@Test
	public void generateCommunityPositions_secondLevel() {
		UndirectedEdge[] edgeList = {e1, e2, e3, e5};
		comparator = new UndirectedEdgeComparator(hierarchy, 1);
		TreeSet<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(comparator);
		edges.addAll(Arrays.asList(edgeList));
		
		result.generateCommunityPositions(edges, 1);
		
		Map<CommunityID, CommunityEdgePositions> expectedPositions = new HashMap<CommunityID, CommunityEdgePositions>();
		expectedPositions.put(new CommunityID(1, 1), new CommunityEdgePositions(0, 1));
		expectedPositions.put(new CommunityID(2, 1), new CommunityEdgePositions(1, 3));
		Map<CommunityID, CommunityEdgePositions> actualPositions = result.getAllEdgePositions().get(0);
		assertEquals(expectedPositions.toString(), actualPositions.toString());
	}
}
