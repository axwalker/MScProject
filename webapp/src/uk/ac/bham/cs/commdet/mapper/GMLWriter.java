package uk.ac.bham.cs.commdet.mapper;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.output.ByteArrayOutputStream;

import uk.ac.bham.cs.commdet.cyto.graph.Edge;
import uk.ac.bham.cs.commdet.cyto.graph.EdgeData;
import uk.ac.bham.cs.commdet.cyto.graph.Graph;
import uk.ac.bham.cs.commdet.cyto.graph.Node;
import uk.ac.bham.cs.commdet.cyto.graph.NodeData;

/**
 * GMLWriter writes a Graph to a GML OutputStream.
 *
 * GML definition taken from
 * (http://www.fim.uni-passau.de/fileadmin/files/lehrstuhl/brandenburg/projekte/gml/gml-documentation.tar.gz)
 *
 * @author Stuart Hendren (http://stuarthendren.net)
 * @author limited adjustments made by Andrew Walker
 */
public class GMLWriter {

    private static final String DELIMITER = " ";
    private static final String TAB = "\t";
    private static final String NEW_LINE = "\r\n";
    private static final String OPEN_LIST = " [" + NEW_LINE;
    private static final String CLOSE_LIST = "]" + NEW_LINE;
    private final Graph graph;

    /**
     * Property keys must be alphanumeric and not exceed 254 characters. They must start with an alpha character.
     */
    private static final String GML_PROPERTY_KEY_REGEX = "[a-zA-Z][a-zA-Z0-9]{0,253}";
    private static final Pattern regex = Pattern.compile(GML_PROPERTY_KEY_REGEX);

    /**
     * @param graph the Graph to pull the data from
     */
    public GMLWriter(final Graph graph) {
        this.graph = graph;
    }
    
    /**
     * Write the data in a Graph to a GML OutputStream.
     *
     * @param filename the GML file to write the Graph data to
     * @throws IOException thrown if there is an error generating the GML data
     */
    public void outputGraph(final String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        outputGraph(fos);
        fos.close();
    }
    
	public String outputGraphString() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		outputGraph(baos);
		baos.close();
		return baos.toString();
	}

    /**
     * Write the data in a Graph to a GML OutputStream.
     *
     * @param gMLOutputStream the GML OutputStream to write the Graph data to
     * @throws IOException thrown if there is an error generating the GML data
     */
    public void outputGraph(final OutputStream gMLOutputStream) throws IOException {

        // ISO 8859-1 as specified in the GML documentation
        final Writer writer = new BufferedWriter(new OutputStreamWriter(gMLOutputStream, Charset.forName("ISO-8859-1")));

        final List<NodeData> vertices = graph.getNodes();
        final List<EdgeData> edges = graph.getEdges();

        writeGraph(writer, vertices, edges);

        // just flush, don't close...allow the underlying stream to stay open and let the calling function close it
        writer.flush();
    }

    private void writeGraph(final Writer writer, final List<NodeData> vertices, final List<EdgeData> edges) throws IOException {
        //final Map<Vertex, Integer> ids = new HashMap<Vertex, Integer>();

        writer.write(GMLTokens.GRAPH);
        writer.write(OPEN_LIST);
        writeVertices(writer, vertices); //, ids);
        writeEdges(writer, edges); //, ids);
        writer.write(CLOSE_LIST);

    }

    private void writeVertices(final Writer writer, final List<NodeData> vertices) throws IOException {
        //int count = 1;
        for (NodeData v : vertices) {
        	Node node = v.getData();
            //if (useId) {
                final Integer id = Integer.valueOf(node.getId());
                writeVertex(writer, v, id);
                //ids.put(v, id);
            /*} else {
                writeVertex(writer, v, count);
                //ids.put(v, count++);
            }*/

        }
    }

    private void writeVertex(final Writer writer, final NodeData v, final int id) throws IOException {
        writer.write(TAB);
        writer.write(GMLTokens.NODE);
        writer.write(OPEN_LIST);
        writeKey(writer, GMLTokens.ID);
        writeNumberProperty(writer, id);
        writeVertexProperties(writer, v);
        writer.write(TAB);
        writer.write(CLOSE_LIST);
    }
    
    private void writeVertexProperties(final Writer writer, final NodeData nodeData) throws IOException {
    	Node node = nodeData.getData();
        for (String key : node.getMetadata().keySet()) {
            if (regex.matcher(key).matches() && !key.equals("id")) {
                final Object property = node.getMetadata().get(key);
                writeKey(writer, key);
                writeProperty(writer, property, 0);
            }
        }
    }

    private void writeEdges(final Writer writer, final List<EdgeData> edges) throws IOException {
        for (EdgeData e : edges) {
        	Edge edge = e.getData();
            writeEdgeProperties(writer, Integer.parseInt(edge.getSource()), Integer.parseInt(edge.getTarget()));
        }
    }

    private void writeEdgeProperties(final Writer writer,
                                     final Integer source, final Integer target) throws IOException {
        writer.write(TAB);
        writer.write(GMLTokens.EDGE);
        writer.write(OPEN_LIST);
        writeKey(writer, GMLTokens.SOURCE);
        writeNumberProperty(writer, source);
        writeKey(writer, GMLTokens.TARGET);
        writeNumberProperty(writer, target);
        writer.write(TAB);
        writer.write(CLOSE_LIST);
    }

    private void writeProperty(final Writer writer, final Object property, int tab) throws IOException {
        if (property instanceof Number) {
            writeNumberProperty(writer, (Number) property);
        } else {
            writeStringProperty(writer, property.toString());
        }
    }

    private void writeNumberProperty(final Writer writer, final Number integer) throws IOException {
        writer.write(integer.toString());
        writer.write(NEW_LINE);
    }

    private void writeStringProperty(final Writer writer, final Object string) throws IOException {
        writer.write("\"");
        writer.write(string.toString().replaceAll("\"","\\\\\""));
        writer.write("\"");
        writer.write(NEW_LINE);
    }

    private void writeKey(final Writer writer, final String command) throws IOException {
        writer.write(TAB);
        writer.write(TAB);
        writer.write(command);
        writer.write(DELIMITER);
    }

    /**
     * Write the data in a Graph to a GML OutputStream.
     *
     * @param graph               the Graph to pull the data from
     * @param graphMLOutputStream the GML OutputStream to write the Graph data to
     * @throws IOException thrown if there is an error generating the GML data
     */
    public static void outputGraph(final Graph graph, final OutputStream graphMLOutputStream) throws IOException {
        final GMLWriter writer = new GMLWriter(graph);
        writer.outputGraph(graphMLOutputStream);
    }

    /**
     * Write the data in a Graph to a GML OutputStream.
     *
     * @param graph    the Graph to pull the data from
     * @param filename the GML file to write the Graph data to
     * @throws IOException thrown if there is an error generating the GML data
     */
    public static void outputGraph(final Graph graph, final String filename) throws IOException {
        final GMLWriter writer = new GMLWriter(graph);
        writer.outputGraph(filename);
    }
    
    public static String outputGraph(final Graph graph) throws IOException {
    	final GMLWriter writer = new GMLWriter(graph);
    	return writer.outputGraphString();
    }

}
