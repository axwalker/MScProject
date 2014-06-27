package uk.ac.bham.cs.commdet.servlet;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

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
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		PrintWriter out = response.getWriter();
        JsonObject responseJson = new JsonObject();        
		
		Part filePart = request.getPart("file");
	    String filename = getFilename(filePart);
	    InputStream filecontent = filePart.getInputStream();
	    String webInfPath = getServletConfig().getServletContext().getRealPath("WEB-INF");
	    String workingFolder = webInfPath + "/tmp/";
	    
	    FileOutputStream outputStream = new FileOutputStream(new File(workingFolder + filename));
		int read = 0;
		byte[] bytes = new byte[1024];
		while ((read = filecontent.read(bytes)) != -1) {
			outputStream.write(bytes, 0, read);
		}
		outputStream.close();
		
        response.setContentType("text/html");
		try {
			LabelPropagation.run(workingFolder + filename, 1, "edgelist");
			responseJson.addProperty("success", true);
			JSONConverterNode nodeConverter = new JSONConverterNodeInt();
			JSONConverterEdge  edgeConverter = new JSONConverterEdgelistUnweighted();
			FileInputStream nodeStream = new FileInputStream(new File(workingFolder + filename + ".4Bj.vout"));
			FileInputStream edgeStream = new FileInputStream(new File(workingFolder + filename));
			responseJson.add("nodes", nodeConverter.getNodesJson(nodeStream));
			responseJson.add("edges", edgeConverter.getEdgesJson(edgeStream));
		} catch (IllegalArgumentException e) {
			responseJson.addProperty("success", false);
			responseJson.addProperty("error", "label propagation failed: " + e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()));
		} catch (Exception e) {
			responseJson.addProperty("success", false);
			responseJson.addProperty("error", "label propagation failed: " + e.toString());
		}
        out.println(responseJson.toString());
	    
        FileUtils.cleanDirectory(new File(workingFolder));
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

