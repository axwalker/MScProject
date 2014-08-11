package uk.ac.bham.cs.commdet.mapper;

import java.io.IOException;
import java.util.Map;

/**
 * Takes a graph file in some format, and maps each node in the graph to an
 * internal integer id. Also writes this graph to an edgelist file in the format
 * (int)source (int)target (float)weight, eg:
 * 		1 2 2.5
 * 		1 3 2.0
 * 		2 3 2.0
 */
public interface FileMapper {

	/**
	 * @return map of properties for a node such as external id, label or other
	 *         information
	 */
	public Map<String, Object> getExternalMetadata(int internalId);

	public String getExternalid(int internalId);

	public int getInternalId(String externalID);

	/**
	 * Load the GML file into the maps and write edgelist file.
	 * 
	 * @param filename
	 *            filepath of the the input file
	 * @throws IOException
	 *             thrown if the data is not valid
	 */
	public void inputGraph(String filename) throws IOException;

	/**
	 * @return  true if the graph has a positive number of nodes and edges.
	 */
	public boolean hasValidGraph();
}
