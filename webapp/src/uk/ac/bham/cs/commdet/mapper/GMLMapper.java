package uk.ac.bham.cs.commdet.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.lang.math.NumberUtils;


/**
 * @author Stuart Hendren (http://stuarthendren.net)
 * @author Stephen Mallette
 * 
 * @author limited adjustments made by Andrew Walker
 */

public class GMLMapper implements FileMapper {

	private int nodeCount = 1;
	private Map<Integer, Map<String, Object>> internalToExternal;
	private Map<String, Integer> externalToInternal;
	private Writer writer;

	public GMLMapper() {
		this.internalToExternal = new HashMap<Integer, Map<String, Object>>();
		this.externalToInternal = new HashMap<String, Integer>();
	}
	
	public String getExternalid(int internalId) {
		Map<String, Object> gmlProperties = internalToExternal.get(internalId);
		return (int)gmlProperties.get(GMLTokens.ID) + "";
	}
	
	public int getInternalId(int gmlID) {
		return externalToInternal.get(gmlID + "");
	}
	
	private void parse(final StreamTokenizer st) throws IOException {
		while (hasNext(st)) {
			int type = st.ttype;
			if (notLineBreak(type)) {
				final String value = st.sval;
				if (GMLTokens.GRAPH.equals(value)) {
					parseGraph(st);
					if (!hasNext(st)) {
						return;
					}
				}
			}
		}
		throw new IOException("Graph not complete");
	}

	private void parseGraph(final StreamTokenizer st) throws IOException {
		checkValid(st, GMLTokens.GRAPH);
		while (hasNext(st)) {
			final int type = st.ttype;
			if (notLineBreak(type)) {
				if (type == ']') {
					return;
				} else {
					final String key = st.sval;
					if (GMLTokens.NODE.equals(key)) {
						addNode(parseNode(st));
					} else if (GMLTokens.EDGE.equals(key)) {
						addEdge(parseEdge(st));
					} else {
						// IGNORE
						parseValue("ignore", st);
					}
				}
			}
		}
		throw new IOException("Graph not complete");
	}

	private void addNode(final Map<String, Object> map) throws IOException {
		final Object id = map.remove(GMLTokens.ID);
		if (id == null) {
			throw new IOException("No id found for node");
		}
		Map<String, Object> nodeProperties = new HashMap<String, Object>();
		nodeProperties.put("id", id);
		nodeProperties.putAll(map);
		
		internalToExternal.put(nodeCount, nodeProperties);
		externalToInternal.put((int)id + "", nodeCount);
		nodeCount++;
	}

	private void addEdge(final Map<String, Object> map) throws IOException {
		Object source = map.remove(GMLTokens.SOURCE);
		Object target = map.remove(GMLTokens.TARGET);
		Object value = map.remove(GMLTokens.VALUE);

		if (source == null) {
			throw new IOException("Edge has no source");
		}

		if (target == null) {
			throw new IOException("Edge has no target");
		}

		Integer sourceId = externalToInternal.get(source + "");
		Integer targetId = externalToInternal.get(target + "");

		if (sourceId == null) {
			throw new IOException("Edge source " + source + " not found");
		}
		if (targetId == null) {
			throw new IOException("Edge target " + target + " not found");
		}
		
		if (value != null && !NumberUtils.isNumber(value + "")) {
			throw new IOException("Edge " + source + " to " + target + " has a non-numeric weight value");
		}
		
		String weight = (value != null) ? " " + value : "";
		
		if (sourceId != null && targetId != null) {
			writer.write(sourceId + " " + targetId + weight + "\n");
		}

	}

	private Object parseValue(final String key, final StreamTokenizer st) throws IOException {
		while (hasNext(st)) {
			final int type = st.ttype;
			if (notLineBreak(type)) {
				if (type == StreamTokenizer.TT_NUMBER) {
					final Double doubleValue = Double.valueOf(st.nval);
					if (doubleValue.equals(Double.valueOf(doubleValue.intValue()))) {
						return doubleValue.intValue();
					} else {
						return doubleValue.floatValue();
					}
				} else {
					if (type == '[') {
						return parseMap(key, st);
					} else if (type == '"') {
						return st.sval;
					}
				}
			}
		}
		throw new IOException("value not found");
	}

	private Map<String, Object> parseNode(final StreamTokenizer st) throws IOException {
		return parseElement(st, GMLTokens.NODE);
	}

	private Map<String, Object> parseEdge(final StreamTokenizer st) throws IOException {
		return parseElement(st, GMLTokens.EDGE);
	}

	private Map<String, Object> parseElement(final StreamTokenizer st, final String node) throws IOException {
		checkValid(st, node);
		return parseMap(node, st);
	}

	private Map<String, Object> parseMap(final String node, final StreamTokenizer st) throws IOException {
		final Map<String, Object> map = new HashMap<String, Object>();
		while (hasNext(st)) {
			final int type = st.ttype;
			if (notLineBreak(type)) {
				if (type == ']') {
					return map;
				} else {
					final String key = st.sval;
					final Object value = parseValue(key, st);
					map.put(key, value);
				}
			}
		}
		throw new IOException(node + " incomplete");
	}

	private void checkValid(final StreamTokenizer st, final String token) throws IOException {
		if (st.nextToken() != '[') {
			throw new IOException(token + " not followed by [");
		}
	}

	private boolean hasNext(final StreamTokenizer st) throws IOException {
		return st.nextToken() != StreamTokenizer.TT_EOF;
	}

	private boolean notLineBreak(final int type) {
		return type != StreamTokenizer.TT_EOL;
	}

	public Map<Integer, Map<String, Object>> getInternalToExternal() {
		return internalToExternal;
	}

	public void inputGraph(String inputFilename) throws FileNotFoundException, IOException {
		inputGraph(inputFilename, new FileInputStream(inputFilename));
	}
	
	/**
	 * Load the GML file into the maps and write edgelist file.
	 *
	 * @param inputStream      GML file
	 * @throws IOException thrown if the data is not valid
	 */
	public void inputGraph(String filename, final InputStream inputStream) throws IOException {

		final Reader r = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));
		final StreamTokenizer st = new StreamTokenizer(r);
		
		writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream(filename+ "_mapped"), "utf-8"));

		try {
			st.commentChar(GMLTokens.COMMENT_CHAR);
			st.ordinaryChar('[');
			st.ordinaryChar(']');

			final String stringCharacters = "/\\(){}<>!£$%^&*-+=,.?:;@_`|~";
			for (int i = 0; i < stringCharacters.length(); i++) {
				st.wordChars(stringCharacters.charAt(i), stringCharacters.charAt(i));
			}

			parse(st);

		} catch (IOException e) {
			throw new IOException("GML line number " + st.lineno() + ": " + e.getMessage(), e);
		} finally {
			r.close();
			writer.close();
		}
	}
}
