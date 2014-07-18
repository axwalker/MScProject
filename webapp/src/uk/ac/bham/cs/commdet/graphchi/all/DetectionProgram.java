package uk.ac.bham.cs.commdet.graphchi.all;


public interface DetectionProgram {

	public GraphResult run(String baseFilename, int nShards) throws Exception;
	
}
