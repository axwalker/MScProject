package uk.ac.bham.cs.commdet.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;

public class EdgelistMapper implements FileMapper {

	private int nodeCount = 0;
	private int edgeCount = 0;
	private int lineNo = 1;
	private Map<Integer, Map<String, Object>> internalToExternal;
	private Map<String, Integer> externalToInternal;
	private Writer writer;
	private int sourceColumn;
	private int targetColumn;
	private int weightColumn;
	private String separator;

	public EdgelistMapper() {
		this.internalToExternal = new HashMap<Integer, Map<String, Object>>();
		this.externalToInternal = new HashMap<String, Integer>();
	}

	public Map<Integer, Map<String, Object>> getInternalToExternal() {
		return internalToExternal;
	}
	
	@Override
	public Map<String, Object> getExternalMetadata(int internalId) {
		return internalToExternal.get(internalId);
	}

	@Override
	public String getExternalid(int internalId) {
		Map<String, Object> properties = internalToExternal.get(internalId);
		return properties.get("label") + "";
	}

	@Override
	public int getInternalId(String externalID) {
		return externalToInternal.get(externalID);
	}

	@Override
	public void inputGraph(String inputFilename) throws IOException {
		inputGraph(inputFilename, new FileInputStream(inputFilename));
	}

	private void parse(final BufferedReader br) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(separator);
			
			int requiredColumnCount = Math.max(Math.max(sourceColumn, targetColumn), weightColumn);
			
			if (tokens.length < requiredColumnCount) {
				throw new IOException("invalid number of arguments (" + tokens.length + ")");
			}
			
			String externalSource = tokens[sourceColumn];
			String externalTarget = tokens[targetColumn];
			int internalSource = addNode(externalSource);
			int internalTarget = addNode(externalTarget);

			double weight = 1;
			if (weightColumn < tokens.length) {
				if (NumberUtils.isNumber(tokens[weightColumn])) {
					weight = Double.parseDouble(tokens[weightColumn]);
				} else {
					throw new IOException("Edge " + externalSource + " to " + 
							externalTarget + " has a non-numeric weight value");
				}
			}
			
			addEdge(internalSource, internalTarget, weight);
			lineNo++;
			
			if (internalSource != internalTarget) {
				edgeCount++;
			}
		}
	}

	private int addNode(String externalId) {
		Integer internalId = externalToInternal.get(externalId);
		if (internalId == null) {
			internalId = nodeCount;
			Map<String, Object> nodeProperties = new HashMap<String, Object>();
			//nodeProperties.put("id", externalId);
			nodeProperties.put("id", internalId);
			nodeProperties.put("label", externalId);
			internalToExternal.put(nodeCount, nodeProperties);
			externalToInternal.put(externalId, nodeCount);
			nodeCount++;
		}
		return internalId;
	}

	private void addEdge(int source, int target, double weight) throws IOException {
		writer.write(source + " " + target + " " + weight + "\n");
	}

	/**
	 * Load the csv file into the maps and write new edgelist file.
	 *
	 * @param inputStream      csv file
	 * @throws IOException thrown if the data is not valid
	 */
	public void inputGraph(String filename, final InputStream inputStream) throws IOException {

		final BufferedReader br = new BufferedReader(
				new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));

		writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(filename+ "_mapped"), "utf-8"));

		try {
			parse(br);
		} catch (IOException e) {
			throw new IOException("CSV line number " + lineNo + ": " + e.getMessage(), e);
		} finally {
			br.close();
			writer.close();
		}
	}

	@Override
	public boolean hasValidGraph() {
		return edgeCount > 0 && nodeCount > 0;
	}

	public void setSourceColumn(int sourceColumn) {
		this.sourceColumn = sourceColumn;
	}

	public void setTargetColumn(int targetColumn) {
		this.targetColumn = targetColumn;
	}

	public void setWeightColumn(int weightColumn) {
		this.weightColumn = weightColumn;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
}
