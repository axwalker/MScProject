package uk.ac.bham.cs.commdet.graphchi.louvain;

import java.util.*;


public class EdgeComparator implements Comparator<UndirectedEdge> {

	private Map<Integer, List<Integer>> nodeToCommunities;
	private int level;
	
	public EdgeComparator(Map<Integer, List<Integer>> nodeToCommunities, int level) {
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
	
	public static void main(String[] args) {
		
		Map<Integer, List<Integer>> lists = new HashMap<Integer, List<Integer>>();
		lists.put(1, new ArrayList<Integer>());
		lists.put(2, new ArrayList<Integer>());
		lists.put(3, new ArrayList<Integer>());
		lists.put(4, new ArrayList<Integer>());
		lists.put(5, new ArrayList<Integer>());
		lists.put(6, new ArrayList<Integer>());
		lists.put(7, new ArrayList<Integer>());
		lists.put(8, new ArrayList<Integer>());
		lists.put(9, new ArrayList<Integer>());
		lists.put(10, new ArrayList<Integer>());
		lists.put(11, new ArrayList<Integer>());
		lists.put(12, new ArrayList<Integer>());
		
		//bottom level
		lists.get(1).add(20);
		lists.get(2).add(20);
		lists.get(3).add(20);
		lists.get(4).add(21);
		lists.get(5).add(21);
		lists.get(6).add(21);
		lists.get(7).add(22);
		lists.get(8).add(22);
		lists.get(9).add(22);
		lists.get(10).add(23);
		lists.get(11).add(23);
		lists.get(12).add(23);
		
		//top level
		lists.get(1).add(30);
		lists.get(2).add(30);
		lists.get(3).add(30);
		lists.get(4).add(30);
		lists.get(5).add(30);
		lists.get(6).add(30);
		lists.get(7).add(31);
		lists.get(8).add(31);
		lists.get(9).add(31);
		lists.get(10).add(31);
		lists.get(11).add(31);
		lists.get(12).add(31);
		
		UndirectedEdge e1 = new UndirectedEdge(7, 8, 1);
		UndirectedEdge e2 = new UndirectedEdge(4, 5, 1);
		UndirectedEdge e3 = new UndirectedEdge(5, 6, 1);
		UndirectedEdge e4 = new UndirectedEdge(1, 2, 1);
		UndirectedEdge e5 = new UndirectedEdge(2, 3, 1);
		UndirectedEdge e6 = new UndirectedEdge(1, 3, 1);
		UndirectedEdge e7 = new UndirectedEdge(10, 11, 1);
		UndirectedEdge e8 = new UndirectedEdge(4, 6, 1);
		UndirectedEdge e9 = new UndirectedEdge(1, 4, 1);
		UndirectedEdge e10 = new UndirectedEdge(8, 9, 1);
		UndirectedEdge e11 = new UndirectedEdge(10, 12, 1);
		UndirectedEdge e12 = new UndirectedEdge(8, 10, 1);
		UndirectedEdge e13 = new UndirectedEdge(11, 12, 1);
		UndirectedEdge e14 = new UndirectedEdge(6, 7, 1);
		UndirectedEdge e15 = new UndirectedEdge(7, 9, 1);
		
		TreeSet<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(new EdgeComparator(lists, 0));
		
		edges.add(e1);
		edges.add(e2);
		edges.add(e3);
		edges.add(e4);
		edges.add(e5);
		edges.add(e6);
		edges.add(e7);
		edges.add(e8);
		edges.add(e9);
		edges.add(e10);
		edges.add(e11);
		edges.add(e12);
		edges.add(e13);
		edges.add(e14);
		edges.add(e15);
		
		System.out.println(edges);
		
	}
}