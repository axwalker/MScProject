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
import edu.cmu.graphchi.datablocks.FloatConverter;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

/**
 * Given an edge list file, used to generate a GraphResult object with corresponding edge list
 * files that progressively group nodes into fewer larger communities.
 * 
 * Based on algorithm as described by Delling et al 
 * (http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.164.2905&rep=rep1&type=pdf)
 */
public class OrcaProgram implements DetectionProgram {

	private GraphStatus status = new GraphStatus();

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
	 * Uses contracted graph from previous iteration to write edge list to a file
	 * for use as input in to the next iteration's engine.
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
		for (Entry<UndirectedEdge, Double> entry : status.getContractedGraph().entrySet()) {
			bw.write(entry.getKey().toStringWeightless() + " " + entry.getValue() + "\n");
		}
		bw.close();

		status.setContractedGraph(new HashMap<UndirectedEdge, Double>());
		return newFilename;
	}
	
	protected static FastSharder<Float, Float> createSharder(String graphName, int numShards) throws IOException {
		return new FastSharder<Float, Float>(graphName, numShards, new VertexProcessor<Float>() {
			public Float receiveVertexValue(int vertexId, String token) {
				return token != null ? Float.parseFloat(token) : 0f;
			}
		}, new EdgeProcessor<Float>() {
			public Float receiveEdge(int from, int to, String token) {
				return token != null ? Float.parseFloat(token) : 1f;
			}
		}, new FloatConverter(), new FloatConverter());
	}

}