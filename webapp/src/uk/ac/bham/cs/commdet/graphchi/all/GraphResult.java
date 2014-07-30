package uk.ac.bham.cs.commdet.graphchi.all;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import uk.ac.bham.cs.commdet.gml.GMLMapper;

public class GraphResult implements Serializable {

	private String filename;
	private Map<Integer, List<Integer>> hierarchy;
	private Map<CommunityIdentity, Integer> sizes;
	private Map<Integer, Double> modularities = new HashMap<Integer, Double>();
	private int height;
	private List<Map<CommunityIdentity, CommunityEdgePositions>> allEdgePositions;
	private Map<Integer, Integer> levelNodeCounts;
	private GMLMapper mapper;
	private boolean hasMapper;

	public GraphResult(String filename, 
					   Map<Integer, List<Integer>> hierarchy, 
					   Map<CommunityIdentity, Integer> sizes, 
					   Map<Integer, Double> modularities,
					   int height) {
		this.filename = filename;
		this.hierarchy = hierarchy;
		this.sizes = sizes;
		this.modularities = modularities;
		this.height = height;
		allEdgePositions = new ArrayList<Map<CommunityIdentity, CommunityEdgePositions>>();
		levelNodeCounts = new HashMap<Integer, Integer>();
	}
	
	public GMLMapper getMapper() {
		return this.mapper;
	}
	
	public void setMapper(GMLMapper mapper) {
		this.mapper = mapper;
		hasMapper = true;
	}
	
	public boolean hasMapper() {
		return hasMapper;
	}
	
	public int getLevelNodeCount(int level) {
		return levelNodeCounts.get(level);
	}
	
	public int getCommunityEdgeCount(int community, int communityLevel, int fileLevel) {
		CommunityEdgePositions positions = allEdgePositions.get(fileLevel).get(new CommunityIdentity(community, communityLevel));
		return positions.getEndIndex() - positions.getStartIndex();
	}

	public void writeSortedEdgeLists() throws IOException {
		for (int i = 0 ; i < height; i++) {
			TreeSet<UndirectedEdge> edges = readInUnsortedEdgeList(i);
			generateCommunityPositions(edges, i);
			String sortedFilename = filename + (i != 0 ? "_pass_" + i : "") + "_sorted";
			BufferedWriter bw = new BufferedWriter(new FileWriter(sortedFilename));
			Set<Integer> uniqueNodes = new HashSet<Integer>();
			for (UndirectedEdge edge : edges) {
				bw.write(edge.toString());
				uniqueNodes.add(edge.getSource());
				uniqueNodes.add(edge.getTarget());
			}
			bw.close();
			levelNodeCounts.put(i, uniqueNodes.size());
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
		Map<CommunityIdentity, CommunityEdgePositions> edgePositions = new HashMap<CommunityIdentity, CommunityEdgePositions>();
		CommunityIdentity[] previousCommunities = new CommunityIdentity[height];
		UndirectedEdge firstEdge = edges.first();
		for (int level = fromLevel; level < height; level++) {
			CommunityIdentity communityAtLevel = getCommunityAtLevel(firstEdge, level);
			previousCommunities[level] = communityAtLevel;
			edgePositions.put(communityAtLevel, new CommunityEdgePositions(0, 0));
		}
		int setIndex = 0;
		for (UndirectedEdge edge : edges) {
			for (int level = fromLevel; level < height; level++) {
				CommunityIdentity communityAtLevel = getCommunityAtLevel(edge, level);
				CommunityIdentity previousCommunity = previousCommunities[level];
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

	private CommunityIdentity getCommunityAtLevel(UndirectedEdge edge, int level) {
		int source = edge.getSource();
		int target = edge.getTarget();
		int sourceCommunity = hierarchy.get(source).get(level);
		int targetCommunity = hierarchy.get(target).get(level);
		if (sourceCommunity == targetCommunity) {
			return new CommunityIdentity(sourceCommunity, level);
		} else {
			return new CommunityIdentity(-1, level);
		}
	}

	public String getFilename() {
		return filename;
	}

	public Map<Integer, List<Integer>> getHierarchy() {
		return hierarchy;
	}

	public List<Map<CommunityIdentity, CommunityEdgePositions>> getAllEdgePositions() {
		return allEdgePositions;
	}

	public Map<CommunityIdentity, Integer> getSizes() {
		return sizes;
	}

	public int getHeight() {
		return height;
	}

	public Map<Integer, Double> getModularities() {
		return modularities;
	}

}
