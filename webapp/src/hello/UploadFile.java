package hello;

import java.io.*;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@MultipartConfig
public class UploadFile extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		        
		Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
	    String filename = getFilename(filePart);
		
	    InputStream filecontent = filePart.getInputStream();
	    
	    String webInfPath = getServletConfig().getServletContext().getRealPath("WEB-INF");
	    FileOutputStream outputStream = new FileOutputStream(new File(webInfPath + "/tmp/" + filename));

		int read = 0;
		byte[] bytes = new byte[1024];
	
		while ((read = filecontent.read(bytes)) != -1) {
			outputStream.write(bytes, 0, read);
		}
		outputStream.close();
		
        response.setContentType("text/html");
        response.setHeader("Cache-control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Max-Age", "86400");
 
        PrintWriter out = response.getWriter();
		JsonObject myObj = new JsonObject();
        myObj.addProperty("success", true);
        myObj.addProperty("filename", filename);
        out.println(myObj.toString());
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
	
	public static void main(String args[]) {

	}

}

