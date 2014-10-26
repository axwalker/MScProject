package uk.ac.bham.cs.commdet.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

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

/**
 * Process a request to perform community detection on a given edgelist or GML
 * file. The result is stored in the session and a JSON representing the top
 * level of the resulting community structure is returned.
 */
@MultipartConfig
@WebServlet("/ProcessGraph")
public class ProcessGraph extends HttpServlet {

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// create session
		HttpSession session = request.getSession(true);
		final int timeoutInSeconds = 60 * 60;
		session.setMaxInactiveInterval(timeoutInSeconds);

		//clean up previous active session from this user
		if (!session.isNew()) {
			File tempFolderToDelete = new File((String) session.getAttribute("folder"));
			FileUtils.deleteDirectory(tempFolderToDelete);
			session.removeAttribute("folder");
			session.removeAttribute("result");
		}

		// parse file contents
		String filename = getFilename(request.getPart("file"));
		InputStream filecontent = request.getPart("file").getInputStream();

		// initialise program
		String detectionAlgorithm = request.getParameter("algorithm");
		DetectionProgram GCprogram;
		if (detectionAlgorithm.equals("Louvain Method")) {
			GCprogram = new LouvainProgram();
		} else if (detectionAlgorithm.equals("ORCA")) {
			GCprogram = new OrcaProgram();
		} else {
			GCprogram = new LabelPropagationProgram();
		}

		// make temporary folder
		String tempFolderPath = getTempFolderPath(filename);
		File tempFolder = new File(tempFolderPath);
		tempFolder.mkdir();
		session.setAttribute("folder", tempFolderPath);

		// write graph file to temporary folder
		FileOutputStream outputStream = new FileOutputStream(new File(tempFolderPath + filename));
		int read = 0;
		byte[] bytes = new byte[1024];
		while ((read = filecontent.read(bytes)) != -1) {
			outputStream.write(bytes, 0, read);
		}
		outputStream.close();

		// process graph
		String responseString;
		GraphResult result = null;
		try {
			final long preMap = System.currentTimeMillis();
			System.out.println("mapping...");

			//map the input file to an internal edgelist file
			String filetype = request.getParameter("filetype");
			FileMapper mapper;
			if (filetype.equals("GML")) {
				mapper = new GMLMapper();
			} else if (filetype.equals("Edgelist")){
				mapper = new EdgelistMapper();
				((EdgelistMapper)mapper).setSourceColumn(0);
				((EdgelistMapper)mapper).setTargetColumn(1);
				((EdgelistMapper)mapper).setSeparator(" ");
				((EdgelistMapper)mapper).setWeightColumn(2);
			} else {
				mapper = new EdgelistMapper();
				int sourceColumn = Integer.parseInt(request.getParameter("sourceColumn"));
				int targetColumn = Integer.parseInt(request.getParameter("targetColumn"));
				String separator = request.getParameter("separator");
				if (!request.getParameter("weightColumn").equals("NA")) {
					int weightColumn = Integer.parseInt(request.getParameter("weightColumn"));
					((EdgelistMapper)mapper).setWeightColumn(weightColumn);
				}
				((EdgelistMapper)mapper).setSourceColumn(sourceColumn);
				((EdgelistMapper)mapper).setTargetColumn(targetColumn);
				((EdgelistMapper)mapper).setSeparator(separator);
			}
			mapper.inputGraph(tempFolderPath + filename);

			if (!mapper.hasValidGraph()) {
				throw new IllegalArgumentException("Graph does not contain enough nodes or edges");
			}

			final long preRun = System.currentTimeMillis();
			System.out.println("finding communities...");

			result = GCprogram.run(tempFolderPath + filename + "_mapped", 1);
			result.setMapper(mapper);

			final long preSorted = System.currentTimeMillis();
			System.out.println("sorting edgelists...");

			result.writeAllSortedEdgeLists();
			session.setAttribute("result", result);

			final long preGenerator = System.currentTimeMillis();
			System.out.println("generating JSON...");

			GraphGenerator generator = new GraphGenerator(result);
			generator.setIncludeEdges(true);
			responseString = generator.getParentGraphJson();

			final long allDone = System.currentTimeMillis();

			String timings = "Mapping: " + (preRun - preMap) + "\n" +
					"Detection: " + (preSorted - preRun) + "\n" +
					"Sorting: " + (preGenerator - preSorted) + "\n" +
					"Json: " + (allDone - preGenerator) + "\n" +
					"TOTAL: " + (allDone - preMap);

			System.out.println(timings);

		} catch (IOException | IllegalArgumentException e) {
			String errorMessage = e.getMessage().replaceAll("\"", "\\\\\"");
			responseString = "{ \"success\": false , " + "\"error\": \"" 
					+ errorMessage + "\"}";
			session.invalidate();
		} catch (Exception e) {
			responseString = "{ \"success\": false , " + "\"error\": \"" + "SERVER ERROR: "
					+ "Please try again later" + "\"}";
			e.printStackTrace();
			session.invalidate();
		}

		// send response
		response.setContentType("application/json");
		response.getWriter().println(responseString);

	}

	/*
	 * create new temporary folder from the file name and current time - so the name is unique
	 */
	protected String getTempFolderPath(String filename) {
		String currentTime = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
		String tempFolderPath = getServletConfig().getServletContext().getRealPath("WEB-INF")
				+ "/tmp/";
		tempFolderPath += currentTime + "_" + filename.hashCode() + "/";
		return tempFolderPath;
	}

	// http://stackoverflow.com/questions/7114087/html5-file-upload-to-java-servlet
	protected String getFilename(Part part) {
		for (String cd : part.getHeader("content-disposition").split(";")) {
			if (cd.trim().startsWith("filename")) {
				String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
				return filename.substring(filename.lastIndexOf('/') + 1).substring(
						filename.lastIndexOf('\\') + 1); // MSIE fix.
			}
		}
		return null;
	}

}
