package uk.ac.bham.cs.commdet.graphchi.all;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

public class GraphResultTest {

	private GraphResult result;
	private Map<Integer, List<Integer>> hierarchy;
	private double someWeight = 0.5;
	private UndirectedEdge e1 = new UndirectedEdge(1, 2, someWeight);
	private UndirectedEdge e2 = new UndirectedEdge(2, 3, someWeight);
	private UndirectedEdge e3 = new UndirectedEdge(3, 4, someWeight);
	private UndirectedEdge e5 = new UndirectedEdge(5, 6, someWeight);
	private UndirectedEdgeComparator comparator;
	
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
	public void testWriteSortedEdgeList() throws IOException {
		StringWriter writer = new StringWriter();
		UndirectedEdge[] edgeList = {e1, e2};
		int someLevel = 0;
		comparator = new UndirectedEdgeComparator(hierarchy, someLevel);
		TreeSet<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(comparator);
		edges.addAll(Arrays.asList(edgeList));
		
		result.writeSortedEdgeList(writer, edges, someLevel);
		
		String expected = "1 2 " + someWeight + "\n2 3 " + someWeight + "\n";  //TODO  change to bytearray from random access
		String actual = writer.toString();
		assertEquals(expected, actual);
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
