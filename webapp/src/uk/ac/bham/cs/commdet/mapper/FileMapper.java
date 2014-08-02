package uk.ac.bham.cs.commdet.mapper;

import java.io.IOException;
import java.util.Map;

public interface FileMapper {

	/**
	 * @return maps internal id of a node to another map of properties for that
	 *         node such as external id, label or other information
	 */
	public Map<Integer, Map<String, Object>> getInternalToExternal();

	public String getExternalid(int internalId);

	public int getInternalId(int externalID);

	/**
	 * Load the GML file into the maps and write edgelist file.
	 * 
	 * @param filename
	 *            filepath of the the input file
	 * @throws IOException
	 *             thrown if the data is not valid
	 */
	public void inputGraph(String filename) throws IOException;

}
