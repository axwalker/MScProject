package practice;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class HelloWorld extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String nameSt = request.getParameter("name") + " test";
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        response.setHeader("Cache-control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
 
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Max-Age", "86400");
 
        Gson gson = new Gson(); 
        JsonObject myObj = new JsonObject();
 
        JsonElement nameObj = gson.toJsonTree(new NameObj(nameSt));
        myObj.addProperty("success", true);
        myObj.add("nameInfo", nameObj);
        
        out.println(myObj.toString());
        out.close();
	}

}

class NameObj {
	private String nameSt;
	
	public NameObj(String nameSt) {
		this.nameSt = nameSt;
	}
	
	public String getName() {
		return nameSt;
	}
}
