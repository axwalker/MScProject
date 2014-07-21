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

	private Map<Integer, Map<Integer, Path>> pathsWithinD;
	private Map<Integer, Map<Integer, Path>> nextIterationPathsToPropagate;
	private Map<Integer, Map<Integer, Path>> currentIterationPathsToPropagate;
	private Map<Integer, Neighbourhood> neighbourhoods;
	private PriorityQueue<Neighbourhood> denseRegionsRanked;
	private TreeSet<Neighbourhood> denseRegions;

	private boolean shortestPathsStage;
	private boolean propagateShortestPathsStage;
	private boolean neighbourhoodUpdateStage;
	private boolean rankingRegionStage;

	private int passIndex;
	private boolean finalUpdate;
	private OrcaGraphStatus status = new OrcaGraphStatus();
	private HashMap<Edge, Integer> contractedGraph = new HashMap<Edge, Integer>();
	private VertexIdTranslate trans;


	public synchronized void update(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (!twoCoreCompleted && !finalUpdate) {
			twoCoreUpdate(vertex, context);
		} else if (!finalUpdate) {
			orcaUpdate(vertex, context);
		} else {
			addToContractedGraph(vertex);
		}
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
		pathsWithinD.put(node, new HashMap<Integer, Path>());
		neighbourhoods.put(node, new Neighbourhood(node));
		status.getCommunitySize()[node] = status.getCommunitySizes().get(new Community(trans.backward(node), passIndex - 1));
		status.getNodeToCommunity()[node] = node;
	}

	private void orcaUpdate(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		if (context.getIteration() == 0) {
			addToInitialGraphStatus(vertex);
			context.getScheduler().addTask(vertex.getId());
		} else {
			if (shortestPathsStage) {
				addImmediateShortestPaths(vertex, context);
			} else if (propagateShortestPathsStage){
				propagateShortestPathsWithinMax(vertex, context);
			} else if (neighbourhoodUpdateStage) {
				updateNeighbourhoodMembers(vertex, context);
			} else if (rankingRegionStage) {
				rankDenseRegionUpdate(vertex, context);
			}
		}
	}

	private void addImmediateShortestPaths(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Map<Integer, Path> thisPaths = new HashMap<Integer, Path>();
		Map<Integer, Path> pathsToPropagate = new HashMap<Integer, Path>();
		boolean hasPathsToPropagate = false;
		for (int i = 0; i < vertex.numEdges(); i++) {
			int edgeWeight = vertex.edge(i).getValue();
			if (edgeWeight <= maxDistance) {
				int targetId = vertex.edge(i).getVertexId();
				Path path = new Path(vertex.getId(), targetId, edgeWeight);
				path.setAdjacent(true);
				thisPaths.put(targetId, path);
				neighbourhoods.get(targetId).addMember(vertex.getId());
				if (edgeWeight < maxDistance) {
					pathsToPropagate.put(targetId, path);
					hasPathsToPropagate = true;
				}
			}
		}
		pathsWithinD.put(vertex.getId(), thisPaths);

		if (hasPathsToPropagate) {
			for (int i = 0; i < vertex.numEdges(); i++) {
				int edgeWeight = vertex.edge(i).getValue();
				if (edgeWeight < maxDistance) {
					context.getScheduler().addTask(vertex.edge(i).getVertexId());
				}
			}
			nextIterationPathsToPropagate.put(vertex.getId(), pathsToPropagate);
		}
	}

	private void propagateShortestPathsWithinMax(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {	
		Map<Integer, Path> thisPaths = pathsWithinD.get(vertex.getId());
		Map<Integer, Path> pathsToPropagate = new HashMap<Integer, Path>();
		boolean hasPathsToPropagate = false;
		for (int i = 0; i < vertex.numEdges(); i++) {
			int edgeWeight = vertex.edge(i).getValue();
			if (edgeWeight >= maxDistance) {
				continue;
			} else {
				int targetId = vertex.edge(i).getVertexId();
				Map<Integer, Path> propagatedPaths = currentIterationPathsToPropagate.get(targetId);
				if (propagatedPaths == null) {
					continue;
				}
				for (Map.Entry<Integer, Path> entry : propagatedPaths.entrySet()) {
					int target = entry.getKey();
					Path path = entry.getValue();
					if (target == vertex.getId()) {
						continue;
					} else {
						int totalPathWeight = path.getWeight() + edgeWeight;
						Path totalPath = new Path(vertex.getId(), target, totalPathWeight);
						if (thisPaths.containsKey(target)){
							Path currentPath = thisPaths.get(target);
							int currentPathWeight = currentPath.getWeight();
							if (totalPathWeight < currentPathWeight) {
								thisPaths.put(target, totalPath);
								neighbourhoods.get(target).addMember(vertex.getId());
								pathsToPropagate.put(target, totalPath);
								hasPathsToPropagate = true;
							}
						} else {
							System.out.println("new");
							if (totalPathWeight <= maxDistance) {
								thisPaths.put(target, totalPath);
								neighbourhoods.get(target).addMember(vertex.getId());
								pathsToPropagate.put(target, totalPath);
								hasPathsToPropagate = true;
							}
						}
					}
				}
			}
		}

		if (hasPathsToPropagate) {
			for (int i = 0; i < vertex.numEdges(); i++) {
				int edgeWeight = vertex.edge(i).getValue();
				if (edgeWeight < maxDistance) {
					hasPathsToPropagate = true;
					context.getScheduler().addTask(vertex.edge(i).getVertexId());
				}
			}
			nextIterationPathsToPropagate.put(vertex.getId(), pathsToPropagate);
		}
	}

	private void updateNeighbourhoodMembers(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Neighbourhood neighbourhood = neighbourhoods.get(vertex.getId());

		for (Integer member : neighbourhood.getMembersSeenCount().keySet()) {
			for (int target : pathsWithinD.get(member).keySet()) {
				neighbourhood.incrementMembersSeenCount(target);
			}
		}

		int neighbourhoodSize = neighbourhood.getMembersSeenCount().size() + 1;
		double seenCountRequirement = (neighbourhoodSize / densityDegree);
		Set<Integer> membersToRemove = new HashSet<Integer>();

		for (Map.Entry<Integer, Integer> member : neighbourhood.getMembersSeenCount().entrySet()) {
			int memberId = member.getKey();
			int seenCount = member.getValue();
			if (seenCount < seenCountRequirement) {
				membersToRemove.add(memberId);
			}
		}

		for (int memberId : membersToRemove) {
			neighbourhood.getMembersSeenCount().remove(memberId);
		}

		neighbourhood.addMember(vertex.getId());

		int doubleTotalEdgeWeight = 0;
		Set<Integer> members = neighbourhood.getMembersSeenCount().keySet();
		for (Integer member : members) {
			for (Map.Entry<Integer, Path> entry : pathsWithinD.get(member).entrySet()) {
				int target = entry.getKey();
				Path path = entry.getValue();
				if (members.contains(target) && path.isAdjacent()) {
					doubleTotalEdgeWeight += path.getWeight();
				}
			}
		}
		neighbourhood.setTotalEdgeWeight(doubleTotalEdgeWeight / 2);

	}

	private void rankDenseRegionUpdate(ChiVertex<Integer, Integer> vertex, GraphChiContext context) {
		Neighbourhood neighbourhood = neighbourhoods.get(vertex.getId());
		if (denseRegions.add(neighbourhood)) {
			denseRegionsRanked.add(neighbourhood);
		}
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
				pathsWithinD = new HashMap<Integer, Map<Integer, Path>>();
				nextIterationPathsToPropagate = new HashMap<Integer, Map<Integer, Path>>();
				neighbourhoods = new HashMap<Integer, Neighbourhood>();
				denseRegionsRanked = new PriorityQueue<Neighbourhood>(noOfVertices, new Comparator<Neighbourhood>() {
					@Override
					public int compare(Neighbourhood o1, Neighbourhood o2) {
						return o1.getRankValue() - o2.getRankValue();
					}
				});
				denseRegions = new TreeSet<Neighbourhood>(new Comparator<Neighbourhood>() {
					@Override
					public int compare(Neighbourhood arg0, Neighbourhood arg1) {
						if (arg0.getMembersSeenCount().keySet().equals(arg1.getMembersSeenCount().keySet())) {
							return 0;
						} else {
							return arg0.getSeedNode() - arg1.getSeedNode(); 
						}
					}
				});
				shortestPathsStage = true;
				propagateShortestPathsStage = true;
				neighbourhoodUpdateStage = true;
				rankingRegionStage = true;
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
			if ((shortestPathsStage || propagateShortestPathsStage) && ctx.getIteration() > 0) {
				shortestPathsStage = false;
				System.out.println("initialShortestPathStage - nextIt:\n" + nextIterationPathsToPropagate);
				System.out.println("initialShortestPathStage - pathsWithin:\n" + pathsWithinD);
				System.out.println("neighbourhoods: " + neighbourhoods);
				currentIterationPathsToPropagate = nextIterationPathsToPropagate;
				nextIterationPathsToPropagate = new HashMap<Integer, Map<Integer, Path>>();
				if (!ctx.getScheduler().hasTasks()) {
					propagateShortestPathsStage = false;
					ctx.getScheduler().addAllTasks();
				}
			} else if (neighbourhoodUpdateStage) {
				if (!ctx.getScheduler().hasTasks()) {
					System.out.println("neighbourhoods: " + neighbourhoods);
					neighbourhoodUpdateStage = false;
					rankingRegionStage = true;
					ctx.getScheduler().addAllTasks();
				}
			} else if (rankingRegionStage && !ctx.getScheduler().hasTasks()) {
				System.out.println("ranked regions: " + denseRegionsRanked);
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
		//while (contractedGraph.size() > 2) {
		while (passIndex < 2) {
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
		String file = "sample_label.txt";
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