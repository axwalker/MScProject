package uk.ac.bham.cs.commdet.graphchi.all;

import java.util.*;

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
		
		for (int i = communitiesSource1.size() - 1; i >= level ; i--) {		
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