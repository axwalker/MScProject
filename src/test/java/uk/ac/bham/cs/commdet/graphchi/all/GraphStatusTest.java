package uk.ac.bham.cs.commdet.graphchi.all;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.graphchi.preprocessing.VertexIdTranslate;

public class GraphStatusTest {

	GraphStatus status;

	@Before
	public void setUp() {
		status = new GraphStatus();
		status.setOriginalVertexTrans(new TestVertexIdTranslate());
		status.setUpdatedVertexTrans(new TestVertexIdTranslate());
	}

	@Test
	public void initialiseCommunitiesMap_nullCommunities_emptyHierarchy() {
		int positiveSize = 5;
		Community[] nullCommunities = new Community[positiveSize];
		status.setCommunities(nullCommunities);

		status.initialiseCommunitiesMap();

		int expectedSize = 0;
		int actualSize = status.getCommunityHierarchy().size();
		assertEquals(expectedSize, actualSize);
	}

	@Test
	public void initialiseCommunitiesMap_mixNullAndNonNullCommunities_filledHierarchy() {
		int someNumberOfNodes = 4;
		setUpStatusWithSize(someNumberOfNodes);
		int someSeedNode1 = 4;
		int someSeedNode2 = 6;
		status.getCommunities()[0] = new Community(someSeedNode1);
		status.getCommunities()[3] = new Community(someSeedNode2);

		status.initialiseCommunitiesMap();

		Map<Integer, List<Integer>> expectedMap = new HashMap<Integer, List<Integer>>();
		expectedMap.put(someSeedNode1, new ArrayList<Integer>());
		expectedMap.put(someSeedNode2, new ArrayList<Integer>());
		Map<Integer, List<Integer>> actualMap = status.getCommunityHierarchy();
		assertEquals(expectedMap, actualMap);
	}

	@Test
	public void updateCommunitiesMapFirstPass_NonNullCommunities_updatedHierarchy() {
		int someNumberOfNodes = 4;
		setUpSomeInitialCommunities(someNumberOfNodes);
		int someNodeId = 1;
		Community someNewCommunity = new Community(3);
		status.getCommunities()[someNodeId] = someNewCommunity;
		
		status.updateCommunitesMapFirstPass();
		
		List<Integer> expectedCommunities = new ArrayList<Integer>();
		expectedCommunities.add(someNewCommunity.getSeedNode());
		List<Integer> actualCommunities = status.getCommunityHierarchy().get(someNodeId);
		assertEquals(expectedCommunities, actualCommunities);
	}
	
	@Test
	public void updateCommunitiesMapSubsequentPasses_nodeInGraph_updatedHierarchy() {
		int someEvenNumberOfNodes = 4;
		setUpSomeIntialCommunitiesMap_withSecondHalfNodesContractedToFirst(someEvenNumberOfNodes);
		setUpCommunities(someEvenNumberOfNodes / 2); 
		int someNodeInLatestGraph = 1;
		
		status.updateCommunitiesMapSubsequentPasses();
		
		List<Integer> expectedCommunities = Arrays.asList(someNodeInLatestGraph, someNodeInLatestGraph);
		List<Integer> actualCommunities = status.getCommunityHierarchy().get(someNodeInLatestGraph);
		assertEquals(expectedCommunities, actualCommunities);
	}
	
	@Test
	public void updateCommunitiesMapSubsequentPasses_nodeNotInGraph_updatedHierarchy() {
		int someEvenNumberOfNodes = 8;
		setUpSomeIntialCommunitiesMap_withSecondHalfNodesContractedToFirst(someEvenNumberOfNodes);
		setUpCommunities(someEvenNumberOfNodes / 2); 
		int someNodeNotInLatestGraph = 6;
		
		status.updateCommunitiesMapSubsequentPasses();
		
		int expectedCommunity = someNodeNotInLatestGraph - (someEvenNumberOfNodes / 2);
		List<Integer> expectedCommunities = Arrays.asList(expectedCommunity, expectedCommunity);
		List<Integer> actualCommunities = status.getCommunityHierarchy().get(someNodeNotInLatestGraph);
		assertEquals(expectedCommunities, actualCommunities);	
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void updateCommunitiesMapSubsequentPasses_nullCommunity_OrcaException() {
		int someEvenNumberOfNodes = 8;
		setUpSomeIntialCommunitiesMap_withSecondHalfNodesContractedToFirst(someEvenNumberOfNodes);
		setUpCommunities(someEvenNumberOfNodes / 2); 
		int someNodeInOriginalGraph = 3;
		status.getCommunities()[someNodeInOriginalGraph] = null;
		
		status.updateCommunitiesMapSubsequentPasses();
	}
	
	@Test
	public void updateSizesMap_CommuntiesWithTwoNodes_updatedSizes() {
		int numberOfNodes = 4;
		setUpSomeIntialCommunitiesMap_withSecondHalfNodesContractedToFirst(numberOfNodes);
		
		status.updateSizesMap();
		
		int hierarchyLevel = 1;
		int communitySize = 2;
		CommunityID communityId0 = new CommunityID(0, hierarchyLevel);
		CommunityID communityId1 = new CommunityID(1, hierarchyLevel);
		Map<CommunityID, Integer> expectedSizes = new HashMap<CommunityID, Integer>();
		expectedSizes.put(communityId0, communitySize);
		expectedSizes.put(communityId1, communitySize);
		Map<CommunityID, Integer> actualSizes = status.getCommunitySizes();
		assertEquals(expectedSizes, actualSizes);
	}
	
	private void setUpStatusWithSize(int size) {
		status.setCommunities(new Community[size]);
		status.setNodes(new Node[size]);
	}

	private void setUpCommunities(int size) {
		setUpStatusWithSize(size);
		for (int seedNode = 0; seedNode < size; seedNode++) {
			status.getCommunities()[seedNode] = new Community(seedNode);
		}
	}
	
	private void setUpSomeInitialCommunities(int size) {
		setUpCommunities(size);
		status.initialiseCommunitiesMap();
	}
	
	/* eg with nodes [0, 1, 2, 3, 4, 5] their communities will be [0, 1, 2, 0, 1, 2] */
	private void setUpSomeIntialCommunitiesMap_withSecondHalfNodesContractedToFirst(int size) {
		setUpSomeInitialCommunities(size);
		for (int seedNode = size / 2; seedNode < size; seedNode++) {
			int nodeToContractTo = seedNode - (size / 2);
			status.getCommunities()[seedNode] = status.getCommunities()[nodeToContractTo];
			status.getCommunities()[seedNode].setTotalSize(2);
		}
		status.updateCommunitesMapFirstPass();
		status.incrementHeight();
	}

}

class TestVertexIdTranslate extends VertexIdTranslate {
	
	@Override
	public int forward(int origId) {
		return origId;
	}

	@Override
	public int backward(int origId) {
		return origId;
	}
}
