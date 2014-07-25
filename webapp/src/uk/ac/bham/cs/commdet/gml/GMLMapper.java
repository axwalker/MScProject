package uk.ac.bham.cs.commdet.gml;

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

/**
 * @author Stuart Hendren (http://stuarthendren.net)
 * @author Stephen Mallette
 */

public class GMLMapper {

	//public static final String DEFAULT_LABEL = "undefined";

	//private static final int DEFAULT_BUFFER_SIZE = 1000;

	//private final String vertexIdKey;

	private int nodeCount = 1;

	private Map<Integer, Map<String, Object>> internalToGml;

	private Map<String, Integer> gmlToInternal;
	
	private Writer writer;

	public GMLMapper() {//final String vertexIdKey) {
		this.internalToGml = new HashMap<Integer, Map<String, Object>>();
		this.gmlToInternal = new HashMap<String, Integer>();
		//this.vertexIdKey = vertexIdKey;
	}
	
	public String getGMLid(int internalId) {
		Map<String, Object> gmlProperties = internalToGml.get(internalId);
		return (int)gmlProperties.get(GMLTokens.ID) + "";
	}
	
	public int getInternalId(int gmlID) {
		return gmlToInternal.get(gmlID + "");
	}

	public void parse(final StreamTokenizer st) throws IOException {
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
			// st.nextToken();
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
		
		internalToGml.put(nodeCount, nodeProperties);
		gmlToInternal.put((int)id + "", nodeCount);
		nodeCount++;
	}

	private void addEdge(final Map<String, Object> map) throws IOException {
		Object source = map.remove(GMLTokens.SOURCE);
		Object target = map.remove(GMLTokens.TARGET);

		if (source == null) {
			throw new IOException("Edge has no source");
		}

		if (target == null) {
			throw new IOException("Edge has no target");
		}

		Integer sourceId = gmlToInternal.get(source + "");
		Integer targetId = gmlToInternal.get(target + "");

		if (sourceId == null) {
			throw new IOException("Edge source " + source + " not found");
		}
		if (targetId == null) {
			throw new IOException("Edge target " + target + " not found");
		}
		
		//TODO write edge to file instead
		if (sourceId != null && targetId != null) {
			writer.write(sourceId + " " + targetId + "\n");
		}
		
		/*if (sourceId != null && targetId != null) {
			System.out.println(sourceId + " " + targetId);
		}*/

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

	public Map<String, Integer> getGmlToInternal() {
		return gmlToInternal;
	}

	public Map<Integer, Map<String, Object>> getInternalToGml() {
		return internalToGml;
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
		          new FileOutputStream(filename+ "_fromGML"), "utf-8"));

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
			throw new IOException("GML malformed line number " + st.lineno() + ": ", e);
		} finally {
			r.close();
			writer.close();
		}
	}

	public static void main(String[] args) throws IOException {
		//String testGML = "graph [\r\n\tnode [\r\n\t\tid 1\r\n\t\tblueprintsId \"3\"\r\n\t\tname \"lop\"\r\n\t\tlang \"java\"\r\n\t]\r\n\tnode [\r\n\t\tid 2\r\n\t\tblueprintsId \"2\"\r\n\t\tname \"vadas\"\r\n\t\tage 27\r\n\t]\r\n\tnode [\r\n\t\tid 3\r\n\t\tblueprintsId \"1\"\r\n\t\tname \"marko\"\r\n\t\tage 29\r\n\t]\r\n\tnode [\r\n\t\tid 4\r\n\t\tblueprintsId \"6\"\r\n\t\tname \"peter\"\r\n\t\tage 35\r\n\t]\r\n\tnode [\r\n\t\tid 5\r\n\t\tblueprintsId \"5\"\r\n\t\tname \"ripple\"\r\n\t\tlang \"java\"\r\n\t]\r\n\tnode [\r\n\t\tid 6\r\n\t\tblueprintsId \"4\"\r\n\t\tname \"josh\"\r\n\t\tage 32\r\n\t]\r\n\tedge [\r\n\t\tsource 6\r\n\t\ttarget 5\r\n\t\tlabel \"created\"\r\n\t\tblueprintsId \"10\"\r\n\t\tweight 1.0\r\n\t]\r\n\tedge [\r\n\t\tsource 3\r\n\t\ttarget 2\r\n\t\tlabel \"knows\"\r\n\t\tblueprintsId \"7\"\r\n\t\tweight 0.5\r\n\t]\r\n\tedge [\r\n\t\tsource 3\r\n\t\ttarget 1\r\n\t\tlabel \"created\"\r\n\t\tblueprintsId \"9\"\r\n\t\tweight 0.4\r\n\t]\r\n\tedge [\r\n\t\tsource 3\r\n\t\ttarget 6\r\n\t\tlabel \"knows\"\r\n\t\tblueprintsId \"8\"\r\n\t\tweight 1.0\r\n\t]\r\n\tedge [\r\n\t\tsource 6\r\n\t\ttarget 1\r\n\t\tlabel \"created\"\r\n\t\tblueprintsId \"11\"\r\n\t\tweight 0.4\r\n\t]\r\n\tedge [\r\n\t\tsource 4\r\n\t\ttarget 1\r\n\t\tlabel \"created\"\r\n\t\tblueprintsId \"12\"\r\n\t\tweight 0.2\r\n\t]\r\n]";
		//System.out.println(testGML);
		
		//InputStream in = IOUtils.toInputStream(testGML, "UTF-8");
		
		InputStream in = new FileInputStream("testGMLin.txt");
		
		GMLMapper mapper = new GMLMapper();
		mapper.inputGraph("testOut", in);
		System.out.println(mapper.getGmlToInternal());
		System.out.println(mapper.getInternalToGml());
	}

}
