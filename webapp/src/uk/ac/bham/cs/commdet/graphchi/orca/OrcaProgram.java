package uk.ac.bham.cs.commdet.graphchi.orca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import uk.ac.bham.cs.commdet.cyto.json.GraphJsonGenerator;
import uk.ac.bham.cs.commdet.graphchi.all.Community;
import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.Edge;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

public class OrcaProgram implements GraphChiProgram<Integer, Integer>, DetectionProgram  {

	private boolean twoCoreCompleted;
	private boolean twoCoreContractionStage = true;
	private boolean propagationStage;
	private int nodeDegree[];
	private boolean contracted[];

	private double maxDistance = 100;
	private double densityDegree = 2;

	private Map<Integer, List<Path>> pathsWithinD;
	private Map<Integer, Neighbourhood> denseRegions;
	private PriorityQueue<Neighbourhood> denseRegionsRanked;

	private boolean denseRegionStage;
	private boolean rankingRegionStage;

	private int passIndex;
	private boolean finalUpdate;
	private OrcaGraphStatus status = new OrcaGraphStatus();
	private HashMap<Edge, Integer> contractedGraph = new HashMap<Edge, Integer>();
	private VertexIdTranslate trans;


	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (!twoCoreCompleted) {
			twoCoreUpdate(vertex, context);
		} else {
			if (!finalUpdate) {
				orcaUpdate(vertex, context);
			}
		}
		if (finalUpdate){
			addToContractedGraph(vertex);
		}
	}

	private void orcaUpdate(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			addToInitialGraphStatus(vertex);
		} else {
			if (denseRegionStage) {
				findDenseRegionUpdate(vertex, context);
			} else if (rankingRegionStage) {
				rankDenseRegionUpdate(vertex, context);
			}
		}
	}

	private void findDenseRegionUpdate(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		List<Path> thisPaths = pathsWithinD.get(vertex.getId());
		Neighbourhood thisRegion = denseRegions.get(vertex.getId());
		boolean improvement = false;
		for (int i = 0; i < vertex.numEdges(); i++) {
			int neighbourId = vertex.edge(i).getVertexId();
			int neighbourDistance = vertex.edge(i).getValue();
			Neighbourhood neighbourRegion = denseRegions.get(neighbourId);
			List<Path> neighbourPaths = pathsWithinD.get(neighbourId);
			
			Path immediatePathOut = new Path(vertex.getId(), neighbourId, vertex.getId(), neighbourDistance);
			if (!thisPaths.contains(immediatePathOut) && neighbourDistance <= maxDistance) {
				thisPaths.add(immediatePathOut);
				thisRegion.getMembersSeenCount().put(neighbourId, 1);
			}
			
			//need to traverse through neighbour paths until distance goes over maxDistance
		}
		
		if (improvement) {
			for (int i = 0; i < vertex.numEdges(); i++) {
				int neighbourId = vertex.edge(i).getVertexId();
				context.getScheduler().addTask(neighbourId);
			}
		}
	}

	private void rankDenseRegionUpdate(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {

	}

	private void twoCoreUpdate(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (twoCoreContractionStage) {
			int degree;
			if (context.getIteration() == 0) {
				degree = vertex.numEdges();
				nodeDegree[vertex.getId()] = degree;
				status.insertNodeIntoCommunity(vertex.getId(), vertex.getId());
				context.getScheduler().addTask(vertex.getId());
			} else {
				degree = nodeDegree[vertex.getId()];
				if (degree < 2) {
					contracted[vertex.getId()] = true;
					for (int i = 0; i < degree; i++) {
						int neighbourId = vertex.edge(i).getVertexId();
						nodeDegree[neighbourId]--;
						if (!contracted[neighbourId]) {
							context.getScheduler().addTask(neighbourId);
						}
					}
				}
			}
		} else if (propagationStage) {
			if (!contracted[vertex.getId()]) {
				for (int i = 0; i < vertex.numEdges(); i++) {
					int neighbourId = vertex.edge(i).getVertexId();
					if (contracted[neighbourId]) {
						int thisCommunity = status.getNodeToCommunity()[vertex.getId()];
						status.removeNodeFromCommunity(neighbourId, neighbourId);
						status.insertNodeIntoCommunity(neighbourId, thisCommunity);
						contracted[neighbourId] = false; //to prevent looping round adding tasks infintely
						context.getScheduler().addTask(neighbourId);
					}
				}
			}
		}
	}

	private void addToInitialGraphStatus(ChiVertex<Integer, Integer> vertex) {
		int node = vertex.getId();
		pathsWithinD.put(node, new ArrayList<Path>());
		denseRegions.put(node, new Neighbourhood(node));
		status.getCommunitySize()[node] = status.getCommunitySizes().get(new Community(trans.backward(node), passIndex - 1));
		status.getNodeToCommunity()[node] = node;
	}

	private void addToContractedGraph(ChiVertex<Integer, Integer> vertex) {
		if (passIndex == 0) {
			addToContractedGraphTwoCore(vertex);
		} else {
			addToContractedGraphOrca(vertex);
		}
	}

	private void addToContractedGraphOrca(ChiVertex<Integer, Integer> vertex) {

	}

	private void addToContractedGraphTwoCore(ChiVertex<Integer, Integer> vertex) {
		for (int i = 0; i < vertex.numOutEdges(); i++) {
			int target = vertex.outEdge(i).getVertexId();
			int sourceCommunity = status.getNodeToCommunity()[vertex.getId()];
			int targetCommunity = status.getNodeToCommunity()[target];
			if (sourceCommunity != targetCommunity) {
				int actualSourceCommunity = trans.backward(sourceCommunity);
				int actualTargetCommunity = trans.backward(targetCommunity);
				int weight = vertex.outEdge(i).getValue();
				Edge edge = new Edge(actualSourceCommunity, actualTargetCommunity);
				if (contractedGraph.containsKey(edge)) {
					int oldWeight = contractedGraph.get(edge);
					contractedGraph.put(edge, oldWeight + weight);
				} else {
					contractedGraph.put(edge, weight);
				}
			}
		}
	}

	public void beginIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0) {
			trans = ctx.getVertexIdTranslate();
			int noOfVertices = (int)ctx.getNumVertices();
			status.setNodeToCommunity(new int[noOfVertices]);
			for (int i = 0; i < noOfVertices; i++) {
				status.getNodeToCommunity()[i] = -1;
			}
			status.setCommunityInternalEdges(new int[noOfVertices]);
			status.setCommunityTotalEdges(new int[noOfVertices]);
			status.setNodeWeightedDegree(new int[noOfVertices]);
			status.setNodeSelfLoops(new int[noOfVertices]);
			status.setTotalGraphWeight(0);
			status.setCommunitySize(new int[noOfVertices]);
			nodeDegree = new int[noOfVertices];
			contracted = new boolean[noOfVertices];
			if (passIndex != 0) {
				pathsWithinD = new HashMap<Integer, List<Path>>();
				denseRegions = new HashMap<Integer, Neighbourhood>();
				denseRegionsRanked = new PriorityQueue<Neighbourhood>();
				denseRegionStage = true;
				rankingRegionStage = false;
			}
		}
	}

	public void endIteration(GraphChiContext ctx) {
		if (ctx.getIteration() == 0 && passIndex == 0) {
			status.setOriginalVertexTrans(ctx.getVertexIdTranslate());
			status.initialiseCommunitiesMap();
		}
		if (ctx.getIteration() == 0) {
			status.setUpdatedVertexTrans(ctx.getVertexIdTranslate());
		}

		if (passIndex == 0) {
			if (twoCoreContractionStage && !ctx.getScheduler().hasTasks()) {
				twoCoreContractionStage = false;
				propagationStage = true;
				ctx.getScheduler().addAllTasks();
			} else if (!finalUpdate && !ctx.getScheduler().hasTasks()) {
				propagationStage = false;
				finalUpdate = true;
				twoCoreCompleted = true;
				ctx.getScheduler().addAllTasks();
				status.getModularities().put(passIndex, -1.);
				status.updateSizesMap();
				status.updateCommunitiesMap();
				status.incrementHeight();
			}
		} else {
			if (denseRegionStage && !ctx.getScheduler().hasTasks()) {
				denseRegionStage = false;
				rankingRegionStage = true;
				ctx.getScheduler().addAllTasks();
			} else if (rankingRegionStage && !ctx.getScheduler().hasTasks()) {
				rankingRegionStage = false;
				finalUpdate = true;
				ctx.getScheduler().addAllTasks();
				status.getModularities().put(passIndex, -1.);
				status.updateSizesMap();
				status.updateCommunitiesMap();
				status.incrementHeight();
			}
		}
		
	}


	public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}
	public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

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

	public GraphResult run(String baseFilename, int nShards) throws  Exception {
		FastSharder sharder = createSharder(baseFilename, nShards);
		sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
		GraphChiEngine<Integer, Integer> engine = new GraphChiEngine<Integer, Integer>(baseFilename, nShards);
		engine.setEdataConverter(new IntConverter());
		engine.setVertexDataConverter(new IntConverter());
		engine.setEnableScheduler(true);
		engine.setSkipZeroDegreeVertices(true);
		engine.run(this, 1000);

		String newFilename = baseFilename;
		while (contractedGraph.size() > 2) {
			finalUpdate = false;
			passIndex++;

			newFilename = writeNewEdgeList(newFilename);

			sharder = createSharder(newFilename, 1);
			sharder.shard(new FileInputStream(new File(newFilename)), "edgelist");
			engine = new GraphChiEngine<Integer, Integer>(newFilename, 1);
			engine.setEdataConverter(new IntConverter());
			engine.setVertexDataConverter(new IntConverter());
			engine.setEnableScheduler(true);
			engine.setSkipZeroDegreeVertices(true);
			engine.run(this, 1000);
		}

		return new GraphResult(baseFilename, status.getCommunityHierarchy(), 
				status.getCommunitySizes(), passIndex, status.getModularities());
	}

	public String writeNewEdgeList(String baseFilename) throws IOException {
		String base;
		if (passIndex > 1) {
			base = baseFilename.substring(0, baseFilename.indexOf("_pass_"));
		} else {
			base = baseFilename;
		}
		String newFilename = base + "_pass_" + passIndex;

		BufferedWriter bw = new BufferedWriter(new FileWriter(newFilename));
		for (Entry<Edge, Integer> entry : contractedGraph.entrySet()) {
			bw.write(entry.getKey().toString() + " " + entry.getValue() + "\n");
		}
		bw.close();

		contractedGraph = new HashMap<Edge, Integer>();
		return newFilename;
	}

	public static void main(String[] args) throws Exception {
		String folder = "sampledata/"; 
		String file = "test_2core2.txt";
		OrcaProgram program = new OrcaProgram();
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