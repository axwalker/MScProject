package uk.ac.bham.cs.commdet.mapper;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import uk.ac.bham.cs.commdet.cyto.graph.Edge;
import uk.ac.bham.cs.commdet.cyto.graph.EdgeData;
import uk.ac.bham.cs.commdet.cyto.graph.Graph;
import uk.ac.bham.cs.commdet.cyto.graph.Node;
import uk.ac.bham.cs.commdet.cyto.graph.NodeData;

public class GMLWriterTest {

	GMLWriter writer;
	Graph graph;
	
	@Before
	public void setUp() {
		
		List<NodeData> nodes = new ArrayList<NodeData>();
		nodes.add(new NodeData(new Node(1 + "", 4)));
		nodes.add(new NodeData(new Node(5 + "", 4)));
		nodes.add(new NodeData(new Node(9 + "", 4)));
		nodes.add(new NodeData(new Node(13 + "", 4)));
		
		List<EdgeData> edges = new ArrayList<EdgeData>();
		edges.add(new EdgeData(new Edge(1 + "", 5 + "", 2)));
		edges.add(new EdgeData(new Edge(1 + "", 9 + "", 1)));
		edges.add(new EdgeData(new Edge(5 + "", 13 + "", 1)));
		edges.add(new EdgeData(new Edge(9 + "", 13 + "", 2)));
		
		graph = new Graph();
		graph.setNodes(nodes);
		graph.setEdges(edges);
		
		writer = new GMLWriter(graph);
	}
	
	@Test
	public void outputGraphString_validGraph() throws IOException {
		
		String actualGML = writer.outputGraphString();
		
		String expectedGML = "graph [" + "\r\n\t" +
				"node [" + "\r\n\t\t" + "id 1" + "\r\n\t" + "]" + "\r\n\t" +
				"node [" + "\r\n\t\t" + "id 5" + "\r\n\t" + "]" + "\r\n\t" +
				"node [" + "\r\n\t\t" + "id 9" + "\r\n\t" + "]" + "\r\n\t" +
				"node [" + "\r\n\t\t" + "id 13" + "\r\n\t" + "]" + "\r\n\t" +
				"edge [" + "\r\n\t\t" + "source 1" + "\r\n\t\t" + "target 5" + "\r\n\t\t" + "value 2.0" + "\r\n\t" + "]" + "\r\n\t" +
				"edge [" + "\r\n\t\t" + "source 1" + "\r\n\t\t" + "target 9" + "\r\n\t\t" + "value 1.0" + "\r\n\t" + "]" + "\r\n\t" +
				"edge [" + "\r\n\t\t" + "source 5" + "\r\n\t\t" + "target 13" + "\r\n\t\t" + "value 1.0" + "\r\n\t" + "]" + "\r\n\t" +
				"edge [" + "\r\n\t\t" + "source 9" + "\r\n\t\t" + "target 13" + "\r\n\t\t" + "value 2.0" + "\r\n\t" + "]\r\n]\r\n";
		
		assertEquals(expectedGML, actualGML);
	}

}
