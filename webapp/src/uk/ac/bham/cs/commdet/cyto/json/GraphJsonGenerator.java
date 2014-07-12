package uk.ac.bham.cs.commdet.cyto.json;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import uk.ac.bham.cs.commdet.graphchi.louvain.Community;
import uk.ac.bham.cs.commdet.graphchi.louvain.GraphResult;
import uk.ac.bham.cs.commdet.graphchi.louvain.UndirectedEdge;

public class GraphJsonGenerator {

	private GraphResult result;
	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	private Graph graph;

	public GraphJsonGenerator(GraphResult result) {
		this.result = result;
		this.graph = new Graph();
	}

	public String getParentGraphJson() {
		parseGraph(result.getHeight() - 1);
		return serializeGraph();
	}

	public String getBottomGraphJson() {
		parseGraph(0);
		return serializeGraph();
	}
	
	public String getGraphJson(int level) {
		parseGraph(level);
		return serializeGraph();
	}

	private void parseGraph(int level) {
		String baseFilename = result.getFilename();
		parseCompoundEdgeFile(baseFilename, level);
		for (NodeData nodeData : graph.getNodes()) {
			Node node = nodeData.getData();
			int nodeId = Integer.parseInt(node.getId());
			int parentId = result.getHierarchy().get(nodeId).get(result.getHeight()-1);
			node.setColour(getColour(parentId + ""));
		}
		double modularity = (level == 0 ? 0 : result.getModularities().get(level));
		Metadata metadata = graph.getMetadata();
		metadata.setModularity(modularity);
		metadata.setNoOfCommunities(graph.getNodes().size());
		metadata.setAvgCommunitySize(result.getHierarchy().size() / graph.getNodes().size());
	}
	
	private void parseCompoundEdgeFile(String baseFilename, int level) {
		String edgeFilename = baseFilename + (level == 0 ? "" : "_pass_" + (level + 1));
		Set<Integer> nodesAdded = new HashSet<Integer>();
		int maxCommunitySize = 0;
		int minCommunitySize = Integer.MAX_VALUE;
		int maxEdgeConnection = 0;
		try {
			for(String line: FileUtils.readLines(new File(edgeFilename))) {
				UndirectedEdge edge = UndirectedEdge.getEdge(line);
				int source = edge.getSource();
				int target = edge.getTarget();
				int weight = edge.getWeight();
				if (source != target) {
					graph.getEdges().add(new EdgeData(new Edge("" + source, "" + target, weight)));
					maxEdgeConnection = Math.max(maxEdgeConnection, weight);
				}
				if (!nodesAdded.contains(source)) {
					int size = (level == 0 ? 1 : result.getSizes().get(new Community(source, level)));
					graph.getNodes().add(new NodeData(new Node("" + source, size)));
					nodesAdded.add(source);
					maxCommunitySize = Math.max(maxCommunitySize, size);
					minCommunitySize = Math.min(minCommunitySize, size);
				}
				if (!nodesAdded.contains(target)) {
					int size = (level == 0 ? 1 : result.getSizes().get(new Community(target, level)));
					graph.getNodes().add(new NodeData(new Node("" + target, size)));
					nodesAdded.add(target);
					maxCommunitySize = Math.max(maxCommunitySize, size);
					minCommunitySize = Math.min(minCommunitySize, size);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Metadata metadata = graph.getMetadata();
		metadata.setMaxEdgeConnection(maxEdgeConnection);
		metadata.setMaxCommunitySize(maxCommunitySize);
		metadata.setMinCommunitySize(minCommunitySize);
	}

	private String serializeGraph() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.getSerializationConfig().enable(Feature.INDENT_OUTPUT);
		try {
			return mapper.writeValueAsString(graph);
		} catch (Exception e) {
			e.printStackTrace();
			return "unsuccessful";
		}
	}
	
	//http://stackoverflow.com/questions/3816466/evenly-distributed-hash-function
	private static String getColour(String id) {
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
}
