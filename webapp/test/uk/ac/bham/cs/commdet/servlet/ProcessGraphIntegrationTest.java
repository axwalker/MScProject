package uk.ac.bham.cs.commdet.servlet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProcessGraphIntegrationTest {

	private static final String COMMUNITY_ONE =
			"A B 2.0" + "\n" +
			"A C 2.0" + "\n" +
			"B C 2.0" + "\n";
	
	private static final String COMMUNITY_TWO =
			"D E 1.5" + "\n" +
			"D F 1.5" + "\n" +
			"E F 1.5" + "\n";
	
	private static final String INTER_COMMUNITY =
			"C D 1.0" + "\n";
	
	private static final String EDGELIST_INPUT =
			COMMUNITY_ONE + INTER_COMMUNITY + COMMUNITY_TWO;
	
	private static final String GML_INPUT =
			"graph [" +
			"node [id 0]" +
			"node [id 1]" +
			"node [id 2]" +
			"node [id 3]" +
			"node [id 4]" +
			"node [id 5]" +
			"edge [source 0 target 1 value 2.0]" +
			"edge [source 0 target 2 value 2.0]" +
			"edge [source 1 target 2 value 2.0]" +
			"edge [source 3 target 4 value 1.5]" +
			"edge [source 3 target 5 value 1.5]" +
			"edge [source 4 target 5 value 1.5]" +
			"edge [source 2 target 3 value 1.0]]";
	
	private StringWriter sw;
	private ProcessGraph processServlet;
	private HttpServletRequest request;       
    private HttpServletResponse response;    
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	
	@Before
	public void setUp() throws Exception {
		processServlet = new ProcessGraph() {
			@Override
			protected String getTempFolderPath(String filename) {
				return tempFolder.getRoot().toString();
			}
			
			@Override
			protected String getFilename(Part part) {
				return "test.file";
			}
		};
		
		request = mock(HttpServletRequest.class);       
        response = mock(HttpServletResponse.class);    
        
        sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(writer);

        HttpSession session = mock(HttpSession.class);
        when(session.isNew()).thenReturn(true);
        when(request.getSession(true)).thenReturn(session);		
	}


	@Test
	public void doPost_louvainEdgelist_success() throws ServletException, IOException {
		setUpEdgelist();
		when(request.getParameter("algorithm")).thenReturn("Louvain Method");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"success\":true"));
	}
	
	@Test
	public void doPost_louvainEdgelist_containsTwoEqualCommunities() throws ServletException, IOException {
		setUpEdgelist();
		when(request.getParameter("algorithm")).thenReturn("Louvain Method");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"noOfCommunities\":2"));
		assertTrue(responseString.contains("\"avgCommunitySize\":3"));
	}
	
	@Test
	public void doPost_orcaEdgelist_success() throws ServletException, IOException {
		setUpEdgelist();
		when(request.getParameter("algorithm")).thenReturn("ORCA");
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"success\":true"));
	}
	
	@Test
	public void doPost_orcaEdgelist_containsTwoEqualCommunities() throws ServletException, IOException {
		setUpEdgelist();
		when(request.getParameter("algorithm")).thenReturn("Louvain Method");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"noOfCommunities\":2"));
		assertTrue(responseString.contains("\"avgCommunitySize\":3"));
	}
	
	@Test
	public void doPost_labelPropEdgelist_success() throws ServletException, IOException {
		setUpEdgelist();
		when(request.getParameter("algorithm")).thenReturn("ORCA");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"success\":true"));
	}
	
	@Test
	public void doPost_labelPropEdgelist_containsTwoEqualCommunities() throws ServletException, IOException {
		setUpEdgelist();
		when(request.getParameter("algorithm")).thenReturn("Label Propagation");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"noOfCommunities\":2"));
		assertTrue(responseString.contains("\"avgCommunitySize\":3"));
	}
	
	
	@Test
	public void doPost_louvainGML_success() throws ServletException, IOException {
		setUpGML();
		when(request.getParameter("algorithm")).thenReturn("Louvain Method");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"success\":true"));
	}
	
	@Test
	public void doPost_louvainGML_containsTwoEqualCommunities() throws ServletException, IOException {
		setUpGML();
		when(request.getParameter("algorithm")).thenReturn("Louvain Method");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"noOfCommunities\":2"));
		assertTrue(responseString.contains("\"avgCommunitySize\":3"));
	}
	
	@Test
	public void doPost_orcaGML_success() throws ServletException, IOException {
		setUpGML();
		when(request.getParameter("algorithm")).thenReturn("ORCA");
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"success\":true"));
	}
	
	@Test
	public void doPost_orcaGML_containsTwoEqualCommunities() throws ServletException, IOException {
		setUpGML();
		when(request.getParameter("algorithm")).thenReturn("Louvain Method");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"noOfCommunities\":2"));
		assertTrue(responseString.contains("\"avgCommunitySize\":3"));
	}
	
	@Test
	public void doPost_labelPropGML_success() throws ServletException, IOException {
		setUpGML();
		when(request.getParameter("algorithm")).thenReturn("ORCA");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"success\":true"));
	}
	
	@Test
	public void doPost_labelPropGML_containsTwoEqualCommunities() throws ServletException, IOException {
		setUpGML();
		when(request.getParameter("algorithm")).thenReturn("Label Propagation");
		
		processServlet.doPost(request, response);
		
		String responseString = sw.toString();
		assertTrue(responseString.contains("\"noOfCommunities\":2"));
		assertTrue(responseString.contains("\"avgCommunitySize\":3"));
	}
	
	private void setUpEdgelist() throws IOException, ServletException {
		InputStream is = IOUtils.toInputStream(EDGELIST_INPUT, "UTF-8");
		Part filePart = mock(Part.class);
		when(filePart.getInputStream()).thenReturn(is);
		when(request.getPart("file")).thenReturn(filePart);
		when(request.getParameter("filetype")).thenReturn("edgelist");
	}
	
	private void setUpGML() throws IOException, ServletException {
		InputStream is = IOUtils.toInputStream(GML_INPUT, "UTF-8");
		Part filePart = mock(Part.class);
		when(filePart.getInputStream()).thenReturn(is);
		when(request.getPart("file")).thenReturn(filePart);
		when(request.getParameter("filetype")).thenReturn("GML");
	}

}
