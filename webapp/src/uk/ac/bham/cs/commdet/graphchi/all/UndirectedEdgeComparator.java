package uk.ac.bham.cs.commdet.graphchi.all;

import java.util.*;

/**
 * Compares edges with the following priorities: - Looks at the community that
 * each edge belongs to a given level. If both belong to different communities,
 * then the edge with the lowest value community is considered smaller. If one
 * is inter community and the other is intra community, the intra community is
 * smaller. If both are in the same community at this level, the same
 * comparisons are done for the the communities they belong to at the next level
 * down of the hierarchy. If this reaches the bottom of the hierarchy and all
 * else is equal, the edge with the lowest source value is considered smaller.
 * 
 */
public class UndirectedEdgeComparator implements Comparator<UndirectedEdge> {

	private Map<Integer, List<Integer>> nodeToCommunities;
	private int level;

	public UndirectedEdgeComparator(Map<Integer, List<Integer>> nodeToCommunities, int level) {
		this.nodeToCommunities = nodeToCommunities;
		this.level = level;
	}

	@Override
	public int compare(UndirectedEdge e1, UndirectedEdge e2) {
		List<Integer> communitiesSource1 = nodeToCommunities.get(e1.getSource());
		List<Integer> communitiesSource2 = nodeToCommunities.get(e2.getSource());
		List<Integer> communitiesTarget1 = nodeToCommunities.get(e1.getTarget());
		List<Integer> communitiesTarget2 = nodeToCommunities.get(e2.getTarget());

		for (int i = communitiesSource1.size() - 1; i >= level; i--) {
			int srcComm1 = communitiesSource1.get(i);
			int srcComm2 = communitiesSource2.get(i);
			int tgtComm1 = communitiesTarget1.get(i);
			int tgtComm2 = communitiesTarget2.get(i);

			if ((srcComm1 == tgtComm1) && (srcComm2 == tgtComm2)) {
				if (srcComm1 != srcComm2) {
					return srcComm1 - srcComm2;
				}
			} else {
				if ((srcComm1 == tgtComm1) && (srcComm2 != tgtComm2)) {
					return -1;
				}
				if ((srcComm1 != tgtComm1) && (srcComm2 == tgtComm2)) {
					return 1;
				}
				if (e1.getSource() == e2.getSource()) {
					return e1.getTarget() - e2.getTarget();
				} else {
					return e1.getSource() - e2.getSource();
				}
			}
		}
		if (e1.getSource() == e2.getSource()) {
			return e1.getTarget() - e2.getTarget();
		} else {
			return e1.getSource() - e2.getSource();
		}
	}

	public void setLevel(int level) {
		this.level = level;
	}

}