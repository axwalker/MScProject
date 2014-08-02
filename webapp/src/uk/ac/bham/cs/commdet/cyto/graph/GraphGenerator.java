package uk.ac.bham.cs.commdet.cyto.graph;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import uk.ac.bham.cs.commdet.graphchi.all.CommunityEdgePositions;
import uk.ac.bham.cs.commdet.graphchi.all.CommunityID;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import uk.ac.bham.cs.commdet.graphchi.all.UndirectedEdge;
import uk.ac.bham.cs.commdet.mapper.GMLWriter;

/**
 * Generate strings in GML or JSON for a given graph result produced by a
 * graphchi community detection program.
 */
public class GraphGenerator {

	private GraphResult result;
	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	private Graph graph;
	private int maxCommunitySize;
	private int minCommunitySize = Integer.MAX_VALUE;
	private double maxEdgeConnection;
	private boolean includeEdges;

	public GraphGenerator(GraphResult result) {
		this.result = result;
		this.graph = new Graph();
	}

	public String getParentGraphJson() {
		return getGraphJson(result.getHeight(), result.getHeight());
	}

	public String getGraphJson(int level, int colourLevel) {
		parseGraph(level, colourLevel);
		return serializeJson();
	}

	public String getCommunityJson(int community, int communityLevel, int fileLevel, int colourLevel) {
		parseCommunity(community, communityLevel, fileLevel, colourLevel);
		return serializeJson();
	}

	public void outputGraphGML(int level, int colourLevel, final OutputStream graphMLOutputStream)
			throws IOException {
		parseGraph(level, colourLevel);
		GMLWriter.outputGraph(graph, graphMLOutputStream);
	}

	public void ouputCommunityGML(int community, int communityLevel, int fileLevel,
			int colourLevel, final OutputStream graphMLOutputStream) throws IOException {
		parseCommunity(community, communityLevel, fileLevel, colourLevel);
		GMLWriter.outputGraph(graph, graphMLOutputStream);
	}

	private void parseGraph(int level, int colourLevel) {
		parseCompoundEdgeFile(result.getFilename(), level);
		double modularity = (level == 0 ? 0 : result.getModularities().get(level - 1));
		setMetadata(modularity, level);
		colourNodes(level, colourLevel);
	}

	private void parseCommunity(int community, int communityLevel, int fileLevel, int colourLevel) {
		parseEdgeFile(result.getFilename(), community, communityLevel, fileLevel);
		double modularity = (fileLevel == 0 ? 0 : result.getModularities().get(fileLevel));
		setMetadata(modularity, fileLevel);
		colourNodes(fileLevel, colourLevel);
	}

	private void colourNodes(int level, int colourLevel) {
		for (NodeData nodeData : graph.getNodes()) {
			Node node = nodeData.getData();
			int nodeId = Integer.parseInt(node.getId());
			if (result.hasMapper()) {
				nodeId = result.getMapper().getInternalId(nodeId);
			}
			int parentId;
			if (level == result.getHeight()) {
				parentId = nodeId;
			} else {
				parentId = result.getHierarchy().get(nodeId).get(colourLevel - 1);
			}
			node.setColour(toColour(parentId + ""));
			node.getMetadata().put("community", parentId);
		}
	}

	private void setMetadata(double modularity, int level) {
		Metadata metadata = graph.getMetadata();
		metadata.setModularity(modularity);
		metadata.setNoOfCommunities(graph.getNodes().size());
		metadata.setAvgCommunitySize(result.getHierarchy().size() / graph.getNodes().size());
		metadata.setHierarchyHeight(result.getHeight());
		metadata.setCurrentLevel(level);
		metadata.setMaxEdgeConnection(maxEdgeConnection);
		metadata.setMaxCommunitySize(maxCommunitySize);
		metadata.setMinCommunitySize(minCommunitySize);
	}

	/**
	 * 
	 * @param baseFilename
	 *            the original base name of the graphs's edgelist file
	 * @param community
	 *            id of the community to get edges for
	 * @param communityLevel
	 *            the level of the hierarchy to get this community data from
	 * @param fileLevel
	 *            the level of the hierarchy to retrieve the edges for
	 */
	private void parseEdgeFile(String baseFilename, int community, int communityLevel, int fileLevel) {
		String edgeFilename = baseFilename + (fileLevel == 0 ? "" : "_pass_" + (fileLevel))
				+ "_sorted";
		CommunityEdgePositions positions = result.getAllEdgePositions().get(fileLevel)
				.get(new CommunityID(community, communityLevel));
		int startIndex = positions.getStartIndex();
		int endIndex = positions.getEndIndex();
		Set<Integer> nodesAdded = new HashSet<Integer>();
		try {
			int lineIndex = 0;
			for (String line : FileUtils.readLines(new File(edgeFilename))) {
				if (lineIndex < startIndex) {
					lineIndex++;
					continue;
				}
				if (lineIndex >= endIndex) {
					break;
				}
				addLine(fileLevel, line, nodesAdded);
				lineIndex++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param baseFilename
	 *            the original base name of the graphs's edgelist file
	 * @param level
	 *            the level of the hierarchy to retrieve the edges for
	 */
	private void parseCompoundEdgeFile(String baseFilename, int level) {
		String edgeFilename = baseFilename + (level == 0 ? "" : "_pass_" + (level));

		Set<Integer> nodesAdded = new HashSet<Integer>();
		try {
			for (String line : FileUtils.readLines(new File(edgeFilename))) {
				addLine(level, line, nodesAdded);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addLine(int level, String line, Set<Integer> nodesAdded) {
		UndirectedEdge edge = UndirectedEdge.getEdge(line);
		int source = edge.getSource();
		int target = edge.getTarget();
		double weight = edge.getWeight();
		if (includeEdges) {
			if (source != target) {
				graph.getEdges().add(
						new EdgeData(new Edge(mapNode(source), mapNode(target), weight)));
				maxEdgeConnection = Math.max(maxEdgeConnection, weight);
			}
		}
		if (!nodesAdded.contains(source)) {
			addNode(level, source, nodesAdded);
		}
		if (!nodesAdded.contains(target)) {
			addNode(level, target, nodesAdded);
		}
	}

	private void addNode(int level, int nodeId, Set<Integer> nodesAdded) {
		int size = (level == 0 ? 1 : result.getSizes().get(new CommunityID(nodeId, level - 1)));
		Node node = new Node(mapNode(nodeId), size);
		Map<String, Object> nodeMetadata = new HashMap<String, Object>();
		if (result.hasMapper() && level == 0) {
			nodeMetadata.putAll(result.getMapper().getInternalToExternal().get(nodeId));
		}
		node.setMetadata(nodeMetadata);
		graph.getNodes().add(new NodeData(node));
		nodesAdded.add(nodeId);
		maxCommunitySize = Math.max(maxCommunitySize, size);
		minCommunitySize = Math.min(minCommunitySize, size);
	}

	private String mapNode(int node) {
		if (result.hasMapper()) {
			return result.getMapper().getExternalid(node);
		} else {
			return node + "";
		}
	}

	/*private double mapWeight(int edgeWeight) {
		
	}*/
	
	private String serializeJson() {
		ObjectMapper mapper = new ObjectMapper();
		// mapper.getSerializationConfig().enable(Feature.INDENT_OUTPUT);
		try {
			return mapper.writeValueAsString(graph);
		} catch (Exception e) {
			e.printStackTrace();
			return "unsuccessful";
		}
	}

	// http://stackoverflow.com/questions/3816466/evenly-distributed-hash-function
	private static String toColour(String id) {
		char[] chars = id.toCharArray();
		int Q = 433494437;
		int result = 0;
		for (int i = 0; i < chars.length; i++) {
			result = result * Q + (chars[i] + 12345) * chars[i];
		}
		result *= Q;
		result = Math.abs(result % (Integer.MAX_VALUE));

		return String.format("#%06X", 0xFFFFFF & result);
	}

	public boolean isIncludeEdges() {
		return includeEdges;
	}

	public void setIncludeEdges(boolean includeEdges) {
		this.includeEdges = includeEdges;
	}
}
