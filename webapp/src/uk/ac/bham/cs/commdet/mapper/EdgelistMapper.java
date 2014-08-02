package uk.ac.bham.cs.commdet.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class EdgelistMapper implements FileMapper {

	private int nodeCount = 1;
	private int lineNo = 0;
	private Map<Integer, Map<String, Object>> internalToExternal;
	private Map<String, Integer> externalToInternal;
	private Writer writer;
	private double maxEdgeWeight;

	public EdgelistMapper() {
		this.internalToExternal = new HashMap<Integer, Map<String, Object>>();
		this.externalToInternal = new HashMap<String, Integer>();
	}

	@Override
	public Map<Integer, Map<String, Object>> getInternalToExternal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExternalid(int internalId) {
		Map<String, Object> properties = internalToExternal.get(internalId);
		return (int)properties.get("id") + "";
	}

	@Override
	public int getInternalId(int externalID) {
		return externalToInternal.get(externalID + "");
	}

	@Override
	public int getInternalEdgeWeight(int edgeWeight) {
		return Math.max(1, (int)(edgeWeight/maxEdgeWeight) * 1000);
	}

	@Override
	public double getExternalEdgeWeight(int edgeWeight) {
		return (edgeWeight / 1000) * maxEdgeWeight;
	}
	
	@Override
	public void inputGraph(String inputFilename) throws IOException {
		inputGraph(inputFilename, new FileInputStream(inputFilename));
	}

	private void parse(final BufferedReader br) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			
			lineNo++;
		}
	}
	
	private void addEdge(final String[] line) {
		
	}
	
	/**
	 * Load the Edgelist file into the maps and write new edgelist file.
	 *
	 * @param inputStream      Edgelist file
	 * @throws IOException thrown if the data is not valid
	 */
	public void inputGraph(String filename, final InputStream inputStream) throws IOException {

		final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));

		writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(filename+ "_fromEdgelist"), "utf-8"));

		try {
			parse(br);

		} catch (IOException e) {
			throw new IOException("Edgelist malformed line number " + lineNo + ": ", e);
		} finally {
			br.close();
			writer.close();
		}
	}

}
