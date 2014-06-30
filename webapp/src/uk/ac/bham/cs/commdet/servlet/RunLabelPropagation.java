package uk.ac.bham.cs.commdet.servlet;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;

import org.apache.commons.io.FileUtils;

import uk.ac.bham.cs.commdet.graphchi.json.*;
import uk.ac.bham.cs.commdet.graphchi.program.LabelPropagation;

import com.google.gson.JsonObject;

@MultipartConfig
public class RunLabelPropagation extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private InputStream filecontent;
	private String filename;
	private String tempFolderPath;
	private File tempFolder;
	private JsonObject responseJson = new JsonObject();
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		response.setContentType("text/html");
		parseFileContents(request.getPart("file"));
		makeTempFolder();
		writeGraphFile();
        processGraph();
        out.println(responseJson.toString());
        FileUtils.deleteDirectory(tempFolder);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }
	
	private void parseFileContents(Part filePart) throws IOException {
		filename = getFilename(filePart);
	    filecontent = filePart.getInputStream();		
	}
	
	private void makeTempFolder() {
	    String currentTime = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
	    tempFolderPath = getServletConfig().getServletContext().getRealPath("WEB-INF") + "/tmp/";
	    tempFolderPath += currentTime + "_" + filename.hashCode() + "/";
	    tempFolder = new File(tempFolderPath);
	    tempFolder.mkdir();
	}
	
	private void writeGraphFile() throws IOException {
		FileOutputStream outputStream = new FileOutputStream(new File(tempFolderPath + filename));
		int read = 0;
		byte[] bytes = new byte[1024];
		while ((read = filecontent.read(bytes)) != -1) {
			outputStream.write(bytes, 0, read);
		}
		outputStream.close();
	}
	
	private void processGraph() {
		try {
			LabelPropagation.run(tempFolderPath + filename, 1, "edgelist");
			responseJson.addProperty("success", true);
			JSONConverterNode nodeConverter = new JSONConverterNodeInt();
			JSONConverterEdge  edgeConverter = new JSONConverterEdgelistUnweighted();
			FileInputStream nodeStream = new FileInputStream(new File(tempFolderPath + filename + ".4Bj.vout"));
			FileInputStream edgeStream = new FileInputStream(new File(tempFolderPath + filename));
			responseJson.add("nodes", nodeConverter.getNodesJson(nodeStream));
			responseJson.add("edges", edgeConverter.getEdgesJson(edgeStream));
		} catch (Exception e) {
			responseJson.addProperty("success", false);
			responseJson.addProperty("error", "label propagation failed: " + e.toString());
		}
	}
	
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

