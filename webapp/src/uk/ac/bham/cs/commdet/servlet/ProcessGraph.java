package uk.ac.bham.cs.commdet.servlet;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;

import org.apache.commons.io.FileUtils;

import edu.cmu.graphchi.engine.GraphChiEngine;

import uk.ac.bham.cs.commdet.cyto.json.CommunityGraph;
import uk.ac.bham.cs.commdet.cyto.json.CommunityGraphGenerator;
import uk.ac.bham.cs.commdet.cyto.json.CompoundNode;
import uk.ac.bham.cs.commdet.cyto.json.SubNode;
import uk.ac.bham.cs.commdet.cyto.json.UndirectedEdge;
import uk.ac.bham.cs.commdet.cyto.json.serializer.CompoundNodeSerializer;
import uk.ac.bham.cs.commdet.cyto.json.serializer.EdgeSerializer;
import uk.ac.bham.cs.commdet.cyto.json.serializer.SubNodeSerializer;
import uk.ac.bham.cs.commdet.graphchi.louvain.LouvainProgram;
import uk.ac.bham.cs.commdet.graphchi.program.LabelPropagationProgram;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@MultipartConfig
public class ProcessGraph extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private InputStream filecontent;
	private String filename;
	private String tempFolderPath;
	private File tempFolder;
	private JsonObject responseJson = new JsonObject();
	private LouvainProgram GCprogram;
	private String responseString;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		initialiseGraphChiProgram(request);
		parseFileContents(request.getPart("file"));
		makeTempFolder();
		writeGraphFileToTempFolder();
        processGraph();
        response.setContentType("text/html");
        //response.getWriter().println(responseJson.toString());
        response.getWriter().println(responseString);
        FileUtils.deleteDirectory(tempFolder);
	}
	
	public void initialiseGraphChiProgram(HttpServletRequest request) {
		//this.GCprogram = new LabelPropagationProgram();
		this.GCprogram = new LouvainProgram();
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
	
	private void writeGraphFileToTempFolder() throws IOException {
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
			GraphChiEngine engine = GCprogram.run(tempFolderPath + filename, 1);
			CommunityGraphGenerator generator = new CommunityGraphGenerator(engine, tempFolderPath + filename);
			generator.parseGraphs();
			responseString = generator.getJacksonJson();
			//JsonObject graphsJson = generator.getJson();
			//JsonObject graphsJson = new JsonObject();
			//responseJson.add("graphs", graphsJson);
			//responseJson.addProperty("success", true);
		} catch (Exception e) {
			//responseJson.addProperty("success", false);
			//responseJson.addProperty("error", "label propagation failed: " + e.toString() + "\n" + Arrays.asList(e.getStackTrace()));
		}
	}
	
	//source
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

