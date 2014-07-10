package uk.ac.bham.cs.commdet.cyto.json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;

import uk.ac.bham.cs.commdet.cyto.json.serializer.CompoundNodeSerializer;
import uk.ac.bham.cs.commdet.cyto.json.serializer.EdgeSerializer;
import uk.ac.bham.cs.commdet.cyto.json.serializer.SubNodeSerializer;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.vertexdata.ForeachCallback;
import edu.cmu.graphchi.vertexdata.VertexAggregator;

public class CommunityGraphGenerator {

	private CommunityGraph cg;
	private Map<String, String> nodeLabels = new HashMap<String, String>();
	private GraphChiEngine engine;
	private String filepath;
	
	public CommunityGraphGenerator(GraphChiEngine engine, String filepath) {
		this.engine = engine;
		this.filepath = filepath;
		this.cg = new CommunityGraph();
		cg.getCompoundGraph().put("HighLevel", new CompoundGraph());
	}
	
	public JsonObject getJson() {
		GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		builder.registerTypeAdapter(UndirectedEdge.class, new EdgeSerializer());
		builder.registerTypeAdapter(CompoundNode.class, new CompoundNodeSerializer());
		builder.registerTypeAdapter(SubNode.class, new SubNodeSerializer());
		Gson gson = builder.create();
		return (JsonObject) gson.toJsonTree(cg);
	}
	
	public String getJacksonJson() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.getSerializationConfig().enable(Feature.INDENT_OUTPUT);
		try {
			return mapper.writeValueAsString(cg);
		} catch (Exception e) {
			return "{\"success\" : false, \"reason\" : " +
					e.toString() + "\n" + Arrays.asList(e.getStackTrace()) + " }";
		}
	}
	
	public void parseGraphs() throws IOException {
		parseAllNodesFromEngine();
		generateCompoundNodes();
		parseEdgesFromEngine();
		generateSubNodeMetadata();
		generateCompoundMetadata();
	}
	
	private void parseAllNodesFromEngine() throws IOException {
		final VertexIdTranslate trans = engine.getVertexIdTranslate();
		VertexAggregator.foreach(engine.numVertices(), filepath, new IntConverter(), new ForeachCallback<Integer>() {
            public void callback(int id, Integer label) {
            	if (label > 0) {
            		if (!cg.getSubGraphs().containsKey("" + label)) {
            			cg.getSubGraphs().put("" + label, new SubGraph());
                	}
            		String nodeID = "" + trans.backward(id);
            		cg.getSubGraphs().get("" + label).getNodes().add(new SubNode(nodeID, "" + label));            		
                    nodeLabels.put(nodeID, "" + label);
            	}
            }
        });
	}
	
	private void generateCompoundNodes() {
		List<CompoundNode> nodes = cg.getCompoundGraph().get("HighLevel").getNodes();
		for (Map.Entry<String, SubGraph> entry : cg.getSubGraphs().entrySet()) {			
			int size = entry.getValue().getSize();
			nodes.add(new CompoundNode(entry.getKey(), size));
		}
	}
	
	private void parseEdgesFromEngine() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		String line = null;
		while ((line = br.readLine()) != null) {
			UndirectedEdge edge = UndirectedEdge.getEdge(line);
			int source = edge.getSource();
			int target = edge.getTarget();
			int weight = edge.getWeight();			
			if (nodeLabels.get("" + source).equals(nodeLabels.get("" + target))) {
				SubGraph subGraph = cg.getSubGraphs().get(nodeLabels.get("" + source));
				subGraph.getEdges().add(new UndirectedEdge(source, target, weight));
			} else {
				source = Integer.parseInt(nodeLabels.get("" + source));
				target = Integer.parseInt(nodeLabels.get("" + target));
				cg.getCompoundGraph().get("HighLevel").addEdge(new UndirectedEdge(source, target, weight));

				incrementCompoundNodeDegree("" + source, "" + target);
			}
		}
		br.close();
	}
	
	private void incrementCompoundNodeDegree(String sourceID, String targetID) {
		List<CompoundNode> nodes = cg.getCompoundGraph().get("HighLevel").getNodes();
		CompoundNode sourceNode = nodes.get(nodes.indexOf(new CompoundNode(sourceID, 0)));
		CompoundNode targetNode = nodes.get(nodes.indexOf(new CompoundNode(targetID, 0)));
		sourceNode.setDegree(sourceNode.getDegree() + 1);
		targetNode.setDegree(targetNode.getDegree() + 1);
	}
	
	private void generateSubNodeMetadata() {
		for(CompoundNode node : cg.getCompoundGraph().get("HighLevel").getNodes()) {
			SubGraph subGraph = cg.getSubGraphs().get(node.getId());
			node.setIntraClusterDensity(subGraph.getIntraClusterDensity());
			node.setInterClusterDensity(nodeLabels.size());
			node.updateClusterRating();
		}
	}
	
	private void generateCompoundMetadata() {
		int minSize = Integer.MAX_VALUE, maxSize = 1, maxEdge = 1;
		for(CompoundNode node : cg.getCompoundGraph().get("HighLevel").getNodes()) {
			minSize = Math.min(minSize, node.getSize());
			maxSize = Math.max(maxSize, node.getSize());
		}
		for (UndirectedEdge edge : cg.getCompoundGraph().get("HighLevel").getEdges()) {
			maxEdge = Math.max(maxEdge, edge.getWeight());
		}
		Metadata metadata = cg.getCompoundGraph().get("HighLevel").getMetadata();
		metadata.setNoOfCommunities(cg.getCompoundGraph().get("HighLevel").getNodes().size());
		metadata.setAvgCommunitySize(nodeLabels.size()/cg.getCompoundGraph().get("HighLevel").getNodes().size());
		metadata.setMinCommunitySize(minSize);
		metadata.setMaxCommunitySize(maxSize);
		metadata.setMaxEdgeConnection(maxEdge);
		//metadata.setModularity()
	}
	
}
