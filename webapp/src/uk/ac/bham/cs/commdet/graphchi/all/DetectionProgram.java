package uk.ac.bham.cs.commdet.graphchi.all;

/**
 * A program to perform community detection on an edge list file and return a GraphResult.
 */
public interface DetectionProgram {

	/**
	 * Run a community detection algorithm on an input edgelistfile
	 * 
	 * @param baseFilename  filepath and name of input edgelist file to run algorithm on
	 * @param nShards  number of shards for graphchi to use - typically 1 per 500 million edges
	 * @return  a GraphResult object with community hierarchy found by algorithm
	 * @throws Exception
	 */
	public GraphResult run(String baseFilename, int nShards) throws Exception;
	
}
