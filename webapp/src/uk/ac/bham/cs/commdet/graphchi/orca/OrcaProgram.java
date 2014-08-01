package uk.ac.bham.cs.commdet.graphchi.orca;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import uk.ac.bham.cs.commdet.graphchi.all.GraphStatus;
import uk.ac.bham.cs.commdet.graphchi.all.UndirectedEdge;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

public class OrcaProgram implements DetectionProgram {

	private GraphStatus status = new GraphStatus();

	/**
	 * 
	 */
	public GraphResult run(final String baseFilename, int nShards) throws  Exception {
		new TwoCore().run(baseFilename, nShards, status);
		String newFilename = baseFilename;
		int nodeCount = status.getNodeCount();
		while (nodeCount >= 2 && status.getHierarchyHeight() < 10) {
			status.incrementHeight();
			newFilename = writeNextLevelEdgeList(newFilename);
			new DenseRegion().run(newFilename, nShards, status);
			nodeCount = status.getNodeCount();
		}

		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), status.getModularities(), status.getHierarchyHeight());
	}

	/*
	 * 
	 */
	private String writeNextLevelEdgeList(String baseFilename) throws IOException {
		String base;
		if (status.getHierarchyHeight() > 1) {
			base = baseFilename.substring(0, baseFilename.indexOf("_pass_"));
		} else {
			base = baseFilename;
		}
		String newFilename = base + "_pass_" + status.getHierarchyHeight();

		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
		for (Entry<UndirectedEdge, Integer> entry : status.getContractedGraph().entrySet()) {
			bw.write(entry.getKey().toStringWeightless() + " " + entry.getValue() + "\n");
		}
		bw.close();

		status.setContractedGraph(new HashMap<UndirectedEdge, Integer>());
		return newFilename;
	}
	
	protected static FastSharder<Integer, Integer> createSharder(String graphName, int numShards) throws IOException {
		return new FastSharder<Integer, Integer>(graphName, numShards, new VertexProcessor<Integer>() {
			public Integer receiveVertexValue(int vertexId, String token) {
				return token != null ? Integer.parseInt(token) : 0;
			}
		}, new EdgeProcessor<Integer>() {
			public Integer receiveEdge(int from, int to, String token) {
				return (token != null ? Integer.parseInt(token) : 1000);
			}
		}, new IntConverter(), new IntConverter());
	}

}