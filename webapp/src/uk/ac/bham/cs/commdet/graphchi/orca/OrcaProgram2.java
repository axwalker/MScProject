package uk.ac.bham.cs.commdet.graphchi.orca;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import uk.ac.bham.cs.commdet.cyto.json.GraphJsonGenerator;
import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.Edge;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;

import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

public class OrcaProgram2 implements DetectionProgram {

	private OrcaGraphStatus status = new OrcaGraphStatus();

	protected static FastSharder createSharder(String graphName, int numShards) throws IOException {
		return new FastSharder<Integer, Integer>(graphName, numShards, new VertexProcessor<Integer>() {
			public Integer receiveVertexValue(int vertexId, String token) {
				return token != null ? Integer.parseInt(token) : 0;
			}
		}, new EdgeProcessor<Integer>() {
			public Integer receiveEdge(int from, int to, String token) {
				return (token != null ? Integer.parseInt(token) : 100);
			}
		}, new IntConverter(), new IntConverter());
	}

	public GraphResult run(final String baseFilename, int nShards) throws  Exception {
		new TwoCore().run(baseFilename, nShards, status);
		String newFilename = baseFilename;
		int nodeCount = status.getNodeCount();
		while (nodeCount > 2) {
			status.incrementHeight();
			newFilename = writeNextLevelEdgeList(newFilename);
			new DenseRegion().run(newFilename, nShards, status);
			nodeCount = status.getNodeCount();
			System.out.println("NODE COUNT: " + nodeCount);
		}

		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), status.getModularities(), status.getHierarchyHeight());
	}

	public String writeNextLevelEdgeList(String baseFilename) throws IOException {
		String base;
		if (status.getHierarchyHeight() > 1) {
			base = baseFilename.substring(0, baseFilename.indexOf("_pass_"));
		} else {
			base = baseFilename;
		}
		String newFilename = base + "_pass_" + status.getHierarchyHeight();

		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
		System.out.println("Contracted graph: " + status.getContractedGraph());
		for (Entry<Edge, Integer> entry : status.getContractedGraph().entrySet()) {
			bw.write(entry.getKey().toString() + " " + entry.getValue() + "\n");
		}
		bw.close();

		status.setContractedGraph(new HashMap<Edge, Integer>());
		return newFilename;
	}

	public static void main(String[] args) throws Exception {
		String folder = "sampledata/"; 
		String file = "karate.edg";
		OrcaProgram2 program = new OrcaProgram2();
		GraphResult result = program.run(folder + file, 1);
		//System.out.println("FINAL MODULARITY: " + program.getModularity());
		result.writeSortedEdgeLists();
		GraphJsonGenerator generator = new GraphJsonGenerator(result);
		System.out.println("hierarchy: " + result.getHierarchy());
		System.out.println(result.getSizes());
		System.out.println(result.getHeight());
		System.out.println(result.getAllEdgePositions());
		System.out.println(program.status.getModularities());
		//System.out.println(generator.getCommunityJson(36, 1, 1));
		System.out.println(generator.getParentGraphJson());
		//System.out.println(generator.getGraphJson(1));

		//System.out.println(Arrays.toString(program.status.getCommunitySize()));
		//System.out.println(result.getEdgePositions());

		//System.out.println(result.getHierarchy());
		//FileUtils.moveFile(new File(folder + file), new File(file));
		//FileUtils.cleanDirectory(new File(folder));
		//FileUtils.moveFile(new File(file), new File(folder + file));
	}

}