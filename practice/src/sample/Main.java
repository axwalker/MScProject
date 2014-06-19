package sample;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.util.IdInt;
import edu.cmu.graphchi.util.LabelAnalysis;
import edu.cmu.graphchi.util.Toplist;

import java.io.File;
import java.io.FileInputStream;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Main {

	private static Logger logger = ChiLogger.getLogger("labelpropagation");

	/**
	 * Usage: java edu.cmu.graphchi.demo.ConnectedComponents graph-name num-shards filetype(edgelist|adjlist)
	 * For specifying the number of shards, 20-50 million edges/shard is often a good configuration.
	 */
	public static void main(String[] args) throws  Exception {
		String baseFilename = args[0];
		int nShards = Integer.parseInt(args[1]);
		String fileType = (args.length >= 3 ? args[2] : null);
		LabelPropagation program = new LabelPropagation();

		/* Create shards */
		FastSharder sharder = program.createSharder(baseFilename, nShards);
		if (!new File(ChiFilenames.getFilenameIntervals(baseFilename, nShards)).exists()) {
			sharder.shard(new FileInputStream(new File(baseFilename)), fileType);
		} else {
			logger.info("Found shards -- no need to preprocess");
		}

		/* Run GraphChi ... */
		GraphChiEngine<Integer, BidirectionalLabel> engine = new GraphChiEngine<Integer, BidirectionalLabel>(baseFilename, nShards);
		engine.setEdataConverter(new BidirectionalLabelConverter());
		engine.setVertexDataConverter(new IntConverter());
		engine.setEnableScheduler(true);
		engine.run(program, 4);

		logger.info("Ready. Going to output...");
		
		int i = 0;
		VertexIdTranslate trans = engine.getVertexIdTranslate();
		TreeSet<IdInt> top = Toplist.topListInt(baseFilename, engine.numVertices(), 30);
		for(IdInt vertexLabel : top) {
			System.out.println(++i + ": " + trans.backward(vertexLabel.getVertexId()) + " has label: " 
					+ trans.backward((int) vertexLabel.getValue()));
		}
	}
}