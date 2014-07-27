package uk.ac.bham.cs.commdet.servlet;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.apache.commons.io.FileUtils;

import edu.cmu.graphchi.ChiLogger;

import uk.ac.bham.cs.commdet.cyto.json.GraphGenerator;
import uk.ac.bham.cs.commdet.gml.GMLMapper;
import uk.ac.bham.cs.commdet.graphchi.all.DetectionProgram;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import uk.ac.bham.cs.commdet.graphchi.labelprop.LabelPropagationProgram;
import uk.ac.bham.cs.commdet.graphchi.louvain.LouvainProgram;
import uk.ac.bham.cs.commdet.graphchi.orca.OrcaProgram;
import uk.ac.bham.cs.commdet.graphchi.orca.OrcaProgram2;

@MultipartConfig
@WebServlet("/ProcessGraph")
public class ProcessGraph extends HttpServlet {

	private static final Logger logger = ChiLogger.getLogger("servlet");

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		//create session
		HttpSession session = request.getSession(true);

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
			GCprogram = new OrcaProgram2();
		} else {
			GCprogram = new LabelPropagationProgram();
		}

		//make temporary folder
		String currentTime = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
		String tempFolderPath = getServletConfig().getServletContext().getRealPath("WEB-INF") + "/tmp/";
		tempFolderPath += currentTime + "_" + filename.hashCode() + "/";
		File tempFolder = new File(tempFolderPath);
		tempFolder.mkdir();

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
			if (filetype.equals("GML")) {
				GMLMapper mapper = new GMLMapper();
				mapper.inputGraph(tempFolderPath + filename);
				result = GCprogram.run(tempFolderPath + filename + "_fromGML", 1);
				result.setMapper(mapper);
				result.writeSortedEdgeLists();
			} else {
				result = GCprogram.run(tempFolderPath + filename, 1);
				result.writeSortedEdgeLists();
			}
			GraphGenerator generator = new GraphGenerator(result);
			generator.setIncludeEdges(true);
			responseString = generator.getParentGraphJson();
			logger.info("Response written succesfully for " + filename);
		} catch (Exception e) {
			responseString = "{ \"success\" : false }";
			logger.info(e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()));
		}

		//set session attributes
		try {
			session.setAttribute("folder", tempFolderPath);
			session.setAttribute("result", result);
		} catch (Exception e) {
			logger.info(e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()));
		}

		//send response
		response.setContentType("application/json");
		response.getWriter().println(responseString);

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

