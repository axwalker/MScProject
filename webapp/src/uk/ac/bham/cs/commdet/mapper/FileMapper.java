package uk.ac.bham.cs.commdet.mapper;

import java.io.IOException;
import java.util.Map;

public interface FileMapper {
	
	/**
	 * @return map of properties for a node such as external id, label or
	 *         other information
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

	public boolean hasValidGraph();
}
