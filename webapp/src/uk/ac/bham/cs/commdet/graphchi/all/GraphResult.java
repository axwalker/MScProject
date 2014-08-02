package uk.ac.bham.cs.commdet.graphchi.all;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.bham.cs.commdet.mapper.FileMapper;

/**
 * The result of a DetectionProgram being run on a graph. Holds the location of
 * edgelist files related to this graph. Holds details on the hierarchical
 * community structure of the graph, memberships of vertices and edges to each
 * community, and other metadata about the structure.
 */
public class GraphResult implements Serializable {

	private static final long serialVersionUID = 3330383923363076417L;
	private String filename;
	private Map<Integer, List<Integer>> hierarchy;
	private Map<CommunityID, Integer> sizes;
	private Map<Integer, Double> modularities = new HashMap<Integer, Double>();
	private int height;
	private List<Map<CommunityID, CommunityEdgePositions>> allEdgePositions;
	private Map<Integer, Integer> levelNodeCounts;
	private FileMapper mapper;

	/**
	 * 
	 * @param filename
	 *            the filepath of the initial input edgelist file
	 * @param hierarchy
	 *            map describing the community each node belongs to at each
	 *            level of the community hierarchy
	 * @param sizes
	 *            map holding the number of vertices in each community
	 * @param modularities
	 *            map for the modularity at each level of the hierarchy
	 * @param height
	 *            the number of levels in the hierarchy
	 */
	public GraphResult(String filename, Map<Integer, List<Integer>> hierarchy,
			Map<CommunityID, Integer> sizes, Map<Integer, Double> modularities,
			int height) {
		this.filename = filename;
		this.hierarchy = hierarchy;
		this.sizes = sizes;
		this.modularities = modularities;
		this.height = height;
		allEdgePositions = new ArrayList<Map<CommunityID, CommunityEdgePositions>>();
		levelNodeCounts = new HashMap<Integer, Integer>();
	}

	public FileMapper getMapper() {
		return this.mapper;
	}

	public void setMapper(FileMapper mapper) {
		this.mapper = mapper;
	}

	public int getLevelNodeCount(int level) {
		return levelNodeCounts.get(level);
	}

	/**
	 * 
	 * @param community
	 *            the id of the community
	 * @param communityLevel
	 *            the level of the hierarchy at which this community exists
	 * @param fileLevel
	 *            the level of the hierarchy from which to find the individual
	 *            edges
	 * @return the number of edges in this community at the chosen file level
	 */
	public int getCommunityEdgeCount(int community, int communityLevel,
			int fileLevel) {
		CommunityEdgePositions positions = allEdgePositions.get(fileLevel).get(
				new CommunityID(community, communityLevel));
		return positions.getEndIndex() - positions.getStartIndex();
	}

	/**
	 * Sort each edge list in the community hierarchy so that the edges are
	 * sorted by what community they belong to. Edges in the same community are
	 * grouped together.
	 * 
	 * @throws IOException
	 */
	public void writeSortedEdgeLists() throws IOException {
		for (int i = 0; i < height; i++) {
			TreeSet<UndirectedEdge> edges = readInUnsortedEdgeList(i);
			generateCommunityPositions(edges, i);
			String sortedFilename = filename + (i != 0 ? "_pass_" + i : "")
					+ "_sorted";
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					sortedFilename));
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
		TreeSet<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(
				new UndirectedEdgeComparator(hierarchy, level));
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
		Map<CommunityID, CommunityEdgePositions> edgePositions = new HashMap<CommunityID, CommunityEdgePositions>();
		CommunityID[] previousCommunities = new CommunityID[height];
		UndirectedEdge firstEdge = edges.first();
		for (int level = fromLevel; level < height; level++) {
			CommunityID communityAtLevel = getCommunityAtLevel(firstEdge, level);
			previousCommunities[level] = communityAtLevel;
			edgePositions.put(communityAtLevel,
					new CommunityEdgePositions(0, 0));
		}
		int setIndex = 0;
		for (UndirectedEdge edge : edges) {
			for (int level = fromLevel; level < height; level++) {
				CommunityID communityAtLevel = getCommunityAtLevel(edge, level);
				CommunityID previousCommunity = previousCommunities[level];
				if (communityAtLevel.equals(previousCommunity)) {
					if (previousCommunity.getId() != -1) {
						break;
					}
				} else {
					if (previousCommunity.getId() != -1) {
						edgePositions.get(previousCommunity).setEndIndex(
								setIndex);
					}
					previousCommunities[level] = communityAtLevel;
					if (communityAtLevel.getId() == -1) {
						continue;
					} else {
						edgePositions.put(communityAtLevel,
								new CommunityEdgePositions(setIndex, setIndex));
					}
				}
			}
			setIndex++;
		}
		allEdgePositions.add(edgePositions);
	}

	private CommunityID getCommunityAtLevel(UndirectedEdge edge, int level) {
		int source = edge.getSource();
		int target = edge.getTarget();
		int sourceCommunity = hierarchy.get(source).get(level);
		int targetCommunity = hierarchy.get(target).get(level);
		if (sourceCommunity == targetCommunity) {
			return new CommunityID(sourceCommunity, level);
		} else {
			return new CommunityID(-1, level);
		}
	}

	public String getFilename() {
		return filename;
	}

	public Map<Integer, List<Integer>> getHierarchy() {
		return hierarchy;
	}

	public List<Map<CommunityID, CommunityEdgePositions>> getAllEdgePositions() {
		return allEdgePositions;
	}

	public Map<CommunityID, Integer> getSizes() {
		return sizes;
	}

	public int getHeight() {
		return height;
	}

	public Map<Integer, Double> getModularities() {
		return modularities;
	}

}
