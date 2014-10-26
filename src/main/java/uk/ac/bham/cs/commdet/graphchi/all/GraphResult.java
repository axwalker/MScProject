package uk.ac.bham.cs.commdet.graphchi.all;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
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
	private Map<Integer, Double> modularities;
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
			Map<CommunityID, Integer> sizes, Map<Integer, Double> modularities, int height) {
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
	public int getCommunityEdgeCount(int community, int communityLevel, int fileLevel) {
		CommunityID id = new CommunityID(community, communityLevel);
		CommunityEdgePositions positions = allEdgePositions.get(fileLevel).get(id);
		return positions.getEndIndex() - positions.getStartIndex();
	}

	/**
	 * Sort each edge list in the community hierarchy so that the edges are
	 * sorted by what community they belong to. Edges in the same community are
	 * grouped together.
	 */
	public void writeAllSortedEdgeLists() {
		for (int level = 0; level <= height; level++) {
			TreeSet<UndirectedEdge> edges = readInUnsortedEdgeList(level);
			
			if (edges.isEmpty()) {
				throw new IllegalArgumentException("No community structure could be found, " +
						"please try again with a different input or algorithm.");
			}
			
			generateCommunityPositions(edges, level);
			String sortedFilename = filename + (level != 0 ? "_pass_" + level : "") + "_sorted";
			
			try (RandomAccessFile file = new RandomAccessFile(sortedFilename, "rw")) {
				writeSortedEdgeList(file, edges, level);
			}  catch (IOException e) {
			    e.printStackTrace();
			}		
		}
	}

	protected void writeSortedEdgeList(RandomAccessFile file, TreeSet<UndirectedEdge> edges, int level) throws IOException {
		Set<Integer> uniqueNodes = new HashSet<Integer>();
		for (UndirectedEdge edge : edges) {
			byte[] bytes = UndirectedEdge.toByteArray(edge);
			file.write(bytes);
			uniqueNodes.add(edge.getSource());
			uniqueNodes.add(edge.getTarget());
		}
		levelNodeCounts.put(level, uniqueNodes.size());
	}

	private TreeSet<UndirectedEdge> readInUnsortedEdgeList(int level) {
		UndirectedEdgeComparator comparator = new UndirectedEdgeComparator(hierarchy, level);
		TreeSet<UndirectedEdge> edges = new TreeSet<UndirectedEdge>(comparator);
		String inputFilename = filename + (level != 0 ? "_pass_" + level : "");
		try (BufferedReader br = new BufferedReader(new FileReader(inputFilename))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				UndirectedEdge edge = UndirectedEdge.getEdge(line);
	
				//ignore zero degree vertices in bottom level edgelist
				if (level == 0 && hierarchy.get(edge.getSource()) == null) {
					continue;
				}
	
				edges.add(edge);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return edges;
	}

	protected void generateCommunityPositions(TreeSet<UndirectedEdge> edges, int fromLevel) {
		Map<CommunityID, CommunityEdgePositions> edgePositions = new HashMap<CommunityID, CommunityEdgePositions>();
		CommunityID[] previousCommunities = new CommunityID[height];
		UndirectedEdge firstEdge = edges.first();
		for (int level = fromLevel; level < height; level++) {
			CommunityID communityAtLevel = getCommunityAtLevel(firstEdge, level);
			previousCommunities[level] = communityAtLevel;
			edgePositions.put(communityAtLevel, new CommunityEdgePositions(0, 0));
		}
		int counter = 0;
		for (UndirectedEdge edge : edges) {
			for (int level = fromLevel; level < height; level++) {
				CommunityID communityAtLevel = getCommunityAtLevel(edge, level);
				CommunityID previousCommunity = previousCommunities[level];
				if (communityAtLevel.equals(previousCommunity)) {
					if (previousCommunity.getId() != -1 && (counter+1) == edges.size()) {
							edgePositions.get(previousCommunity).setEndIndex(counter);
					}
				} else {
					if (previousCommunity.getId() != -1) { // || (counter+1) == edges.size()) {
						edgePositions.get(previousCommunity).setEndIndex(counter);
					}
					if ((communityAtLevel.getId() != -1)) {
						edgePositions.put(communityAtLevel, new CommunityEdgePositions(counter, counter));
					}
					previousCommunities[level] = communityAtLevel;
				}
			}
			counter++;
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