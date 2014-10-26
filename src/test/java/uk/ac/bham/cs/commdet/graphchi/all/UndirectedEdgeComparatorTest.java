package uk.ac.bham.cs.commdet.graphchi.all;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

public class UndirectedEdgeComparatorTest {

	private UndirectedEdgeComparator comparator;
	private UndirectedEdge e1 = new UndirectedEdge(1, 2);
	private UndirectedEdge e2 = new UndirectedEdge(2, 3);
	private UndirectedEdge e3 = new UndirectedEdge(3, 4);
	private UndirectedEdge e5 = new UndirectedEdge(5, 6);
	
	@Before
	public void setUp() {
		Map<Integer, List<Integer>> nodeToCommunities = new HashMap<Integer, List<Integer>>();
		nodeToCommunities.put(1, Arrays.asList(1, 2));
		nodeToCommunities.put(2, Arrays.asList(1, 2));
		nodeToCommunities.put(3, Arrays.asList(2, 2));
		nodeToCommunities.put(4, Arrays.asList(2, 2));
		nodeToCommunities.put(5, Arrays.asList(3, 1));
		nodeToCommunities.put(6, Arrays.asList(3, 1));
		
		comparator = new UndirectedEdgeComparator(nodeToCommunities, 0);
	}
	
	@Test
	public void compare_sameCommunitiesAtThisLevel_lowerSourceFirst() {
		comparator.setLevel(0);
		UndirectedEdge lowerSource = e1;
		UndirectedEdge higherSource = e2;
		
		boolean isLowerSourceFirst = comparator.compare(lowerSource, higherSource) < 0;
		
		assertTrue(isLowerSourceFirst);
	}
	
	@Test
	public void compare_differentCommunitiesAtThisLevel_lowerCommunityFirst() {
		comparator.setLevel(1);
		UndirectedEdge lowerCommunity = e5;
		UndirectedEdge higherCommunity = e1;
		
		boolean isLowerCommunityFirst = comparator.compare(lowerCommunity, higherCommunity) < 0;
		
		assertTrue(isLowerCommunityFirst);
	}
	
	@Test
	public void compare_differentCommunitiesAtLowerLevel_lowerCommunityFirst() {
		comparator.setLevel(0);
		UndirectedEdge lowerCommunity = e1;
		UndirectedEdge higherCommunity = e3;
		
		boolean isLowerCommunityFirst = comparator.compare(lowerCommunity, higherCommunity) < 0;
		
		assertTrue(isLowerCommunityFirst);
	}
	
	@Test
	public void compare_oneEdgeInterCommunity_intraCommunityEdgeFirst() {
		comparator.setLevel(0);
		UndirectedEdge intraCommunity = e1;
		UndirectedEdge interCommunity = e2;
		
		boolean isIntraFirst = comparator.compare(intraCommunity, interCommunity) < 0;
		
		assertTrue(isIntraFirst);
	}

}
