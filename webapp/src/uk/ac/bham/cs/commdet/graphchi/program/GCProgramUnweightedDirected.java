package uk.ac.bham.cs.commdet.graphchi.program;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import com.google.gson.JsonObject;

import uk.ac.bham.cs.commdet.graphchi.behaviours.UpdateBehaviour;
import uk.ac.bham.cs.commdet.graphchi.json.*;
import uk.ac.bham.cs.commdet.graphchi.json2.JsonParser;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexProcessor;

public class GCProgramUnweightedDirected extends GCProgram<Integer, BidirectionalLabel> {

	public GCProgramUnweightedDirected(UpdateBehaviour<Integer, BidirectionalLabel> updateBehaviour) {
		super(updateBehaviour);
	}

	protected FastSharder createSharder(String graphName, int numShards) throws IOException {
		return new FastSharder<Integer, BidirectionalLabel>(graphName, numShards, new VertexProcessor<Integer>() {
			public Integer receiveVertexValue(int vertexId, String token) {
				return 0;
			}
		}, new EdgeProcessor<BidirectionalLabel>() {
			public BidirectionalLabel receiveEdge(int from, int to, String token) {
				return new BidirectionalLabel(0, 0);
			}
		}, new IntConverter(), new BidirectionalLabelConverter());
	}

	public GraphChiEngine run(String baseFilename, int nShards) throws  Exception {
		
		FastSharder sharder = this.createSharder(baseFilename, nShards);
		sharder.shard(new FileInputStream(new File(baseFilename)), "edgelist");
		GraphChiEngine<Integer, BidirectionalLabel> engine = new GraphChiEngine<Integer, BidirectionalLabel>(baseFilename, nShards);
		engine.setEdataConverter(new BidirectionalLabelConverter());
		engine.setVertexDataConverter(new IntConverter());
		
		engine.setEnableScheduler(getUpdateBehaviour().hasScheduler());
		engine.run(this, 10);
		
		return engine;
	}

}