package uk.ac.bham.cs.commdet.graphchi.louvain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;


public class GraphResult implements Serializable {

	private String filename;
	private Map<Integer, List<Integer>> hierarchy;
	private List<Map<Community, CommunityEdgePositions>> allEdgePositions = new ArrayList<Map<Community, CommunityEdgePositions>>();
	//private Map<Community, CommunityEdgePositions> edgePositions = new HashMap<Community, CommunityEdgePositions>();
	private Map<Community, Integer> sizes;
	private Map<Integer, Double> modularities = new HashMap<Integer, Double>();
	private int height;

	public GraphResult(String filename, Map<Integer, List<Integer>> hierarchy, Map<Community, Integer> sizes, int height,
			Map<Integer, Double> modularities) {
		this.filename = filename;
		this.hierarchy = hierarchy;
		this.sizes = sizes;
		this.height = height;
		this.modularities = modularities;
	}

	public void writeSortedEdgeLists() throws IOException {
		for (int i = 0 ; i < height; i++) {
			TreeSet<UndirectedEdge> edges = readInUnsortedEdgeList(i);
			generateCommunityPositions(edges, i);
			String sortedFilename = filename + (i != 0 ? "_pass_" + i : "") + "_sorted";
			BufferedWriter bw = new BufferedWriter(new FileWriter(sortedFilename));
			for (UndirectedEdge edge : edges) {
				bw.write(edge.toString());
			}
			bw.close();
		}
	}

	private TreeSet<UndirectedEdge> readInUnsortedEdgeList(int level) throws IOException {
		TreeSet<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(new EdgeComparator(hierarchy, level));
		String inputFilename = filename + (level != 0 ? "_pass_" + level : "");
		BufferedReader br = new BufferedReader(new FileReader(inputFilename));
		String line = null;
		while ((line = br.readLine()) != null) {
			UndirectedEdge edge = UndirectedEdge.getEdge(line);
			edges.add(edge);
		}
		br.close();
		return edges;
	}

	private void generateCommunityPositions(TreeSet<UndirectedEdge> edges, int fromLevel) throws IOException {
		Map<Community, CommunityEdgePositions> edgePositions = new HashMap<Community, CommunityEdgePositions>();
		Community[] previousCommunities = new Community[height];
		UndirectedEdge firstEdge = edges.first();
		for (int level = fromLevel; level < height; level++) {
			Community communityAtLevel = getCommunityAtLevel(firstEdge, level);
			previousCommunities[level] = communityAtLevel;
			edgePositions.put(communityAtLevel, new CommunityEdgePositions(0, 0));
		}
		int setIndex = 0;
		for (UndirectedEdge edge : edges) {
			for (int level = fromLevel; level < height; level++) {
				Community communityAtLevel = getCommunityAtLevel(edge, level);
				Community previousCommunity = previousCommunities[level];
				if (communityAtLevel.equals(previousCommunity)) {
					if (previousCommunity.getId() != -1) {
						break;
					}
				} else {
					if (previousCommunity.getId() != -1) {
						edgePositions.get(previousCommunity).setEndIndex(setIndex);
					}
					previousCommunities[level] = communityAtLevel;
					if (communityAtLevel.getId() == -1) {
						continue;
					} else {
						edgePositions.put(communityAtLevel, new CommunityEdgePositions(setIndex, setIndex));
					}
				}
			}
			setIndex++;
		}
		allEdgePositions.add(edgePositions);
	}

	private Community getCommunityAtLevel(UndirectedEdge edge, int level) {
		int source = edge.getSource();
		int target = edge.getTarget();
		int sourceCommunity = hierarchy.get(source).get(level);
		int targetCommunity = hierarchy.get(target).get(level);
		if (sourceCommunity == targetCommunity) {
			return new Community(sourceCommunity, level);
		} else {
			return new Community(-1, level);
		}
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Map<Integer, List<Integer>> getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(Map<Integer, List<Integer>> hierarchy) {
		this.hierarchy = hierarchy;
	}

	public List<Map<Community, CommunityEdgePositions>> getAllEdgePositions() {
		return allEdgePositions;
	}

	public void setAllEdgePositions(List<Map<Community, CommunityEdgePositions>> edgePositions) {
		this.allEdgePositions = edgePositions;
	}

	public Map<Community, Integer> getSizes() {
		return sizes;
	}

	public void setSizes(Map<Community, Integer> sizes) {
		this.sizes = sizes;
	}

	public int getHeight() {
		return height;
	}

	public Map<Integer, Double> getModularities() {
		return modularities;
	}

	public void setModularities(Map<Integer, Double> modularities) {
		this.modularities = modularities;
	}

}
