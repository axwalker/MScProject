package uk.ac.bham.cs.commdet.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;

import uk.ac.bham.cs.commdet.cyto.graph.GraphGenerator;
import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import uk.ac.bham.cs.commdet.graphchi.labelprop.LabelPropagationProgram;
import uk.ac.bham.cs.commdet.graphchi.louvain.LouvainProgram;
import uk.ac.bham.cs.commdet.graphchi.orca.OrcaProgram;
import uk.ac.bham.cs.commdet.mapper.EdgelistMapper;
import uk.ac.bham.cs.commdet.mapper.FileMapper;
import uk.ac.bham.cs.commdet.mapper.GMLMapper;
import edu.cmu.graphchi.ChiLogger;

@MultipartConfig
@WebServlet("/ProcessGraph")
public class ProcessGraph extends HttpServlet {

	private static final Logger logger = ChiLogger.getLogger("servlet");

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		//create session
		HttpSession session = request.getSession(true);
		
		final int timeoutInSeconds = 60 * 60;
		session.setMaxInactiveInterval(timeoutInSeconds);

		if (!session.isNew()) {
			logger.info("removing previous session");
			File tempFolderToDelete = new File((String)session.getAttribute("folder"));
			FileUtils.deleteDirectory(tempFolderToDelete);
			session.removeAttribute("folder");
			session.removeAttribute("result");
			logger.info("previous session data removed");
		}

		//parse file contents
		String filename = getFilename(request.getPart("file"));
		InputStream filecontent = request.getPart("file").getInputStream();		

		//initialise program
		String detectionAlgorithm = request.getParameter("algorithm");
		DetectionProgram GCprogram; 
		if (detectionAlgorithm.equals("Louvain Method")){
			GCprogram = new LouvainProgram();
		} else if (detectionAlgorithm.equals("ORCA")){
			GCprogram = new OrcaProgram();
		} else {
			GCprogram = new LabelPropagationProgram();
		}
		
		//make temporary folder
		String currentTime = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
		String tempFolderPath = getServletConfig().getServletContext().getRealPath("WEB-INF") + "/tmp/";
		tempFolderPath += currentTime + "_" + filename.hashCode() + "/";
		File tempFolder = new File(tempFolderPath);
		tempFolder.mkdir();
		session.setAttribute("folder", tempFolderPath);

		//write graph file to temporary folder
		FileOutputStream outputStream = new FileOutputStream(new File(tempFolderPath + filename));
		int read = 0;
		byte[] bytes = new byte[1024];
		while ((read = filecontent.read(bytes)) != -1) {
			outputStream.write(bytes, 0, read);
		}
		outputStream.close();

		//process graph
		String responseString;
		GraphResult result = null;
		String filetype = request.getParameter("filetype");;
		try {
			FileMapper mapper;
			if (filetype.equals("GML")) {
				mapper = new GMLMapper();
			} else {
				mapper = new EdgelistMapper();
			}
			mapper.inputGraph(tempFolderPath + filename);
			if (!mapper.hasValidGraph()) {
				throw new IllegalArgumentException("Graph does not contain enough nodes or edges");
			}
			
			result = GCprogram.run(tempFolderPath + filename + "_mapped", 1);
			result.setMapper(mapper);
			result.writeAllSortedEdgeLists();
			session.setAttribute("result", result);
			
			GraphGenerator generator = new GraphGenerator(result);
			generator.setIncludeEdges(true);
			responseString = generator.getParentGraphJson();
			
		} catch (IOException | IllegalArgumentException e) {
			responseString = "{ \"success\": false , " +
					"\"error\": \"" + e.getMessage() + "\"}";
			session.invalidate();
		} catch (Exception e) {
			responseString = "{ \"success\": false , " +
					"\"error\": \"" + "SERVER ERROR: " + e.getMessage() + "\"}";	
			e.printStackTrace();
			session.invalidate();
		}

		//send response
		response.setContentType("application/json");
		response.getWriter().println(responseString);
		response.getWriter().flush();

	}

	//add source
	private static String getFilename(Part part) {
		for (String cd : part.getHeader("content-disposition").split(";")) {
			if (cd.trim().startsWith("filename")) {
				String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
				return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); // MSIE fix.
			}
		}
		return null;
	}

}

