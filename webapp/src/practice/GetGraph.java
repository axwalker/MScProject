package practice;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GetGraph extends HttpServlet {

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
 
        JsonElement nameObj = gson.toJsonTree(new NameObj2(nameSt));
        myObj.addProperty("success", true);
        myObj.add("nameInfo", nameObj);
        
        //
        
        JsonObject graph = new JsonObject();
        graph.addProperty("success", true);
        
        JsonArray nodes = new JsonArray();
        JsonArray edges = new JsonArray();
        
        JsonObject dataN1 = new JsonObject();
        JsonObject n1 = new JsonObject();
        n1.addProperty("id", "n1");
        dataN1.add("data", n1);
        JsonObject dataN2 = new JsonObject();
        JsonObject n2 = new JsonObject();
        n2.addProperty("id", "n2");
        dataN2.add("data", n2);
        JsonObject dataE1 = new JsonObject();
        JsonObject e1 = new JsonObject();
        e1.addProperty("id", "e1");
        e1.addProperty("source", "n1");
        e1.addProperty("target", "n2");
        dataE1.add("data", e1);
        
        nodes.add(dataN1);
        nodes.add(dataN2);
        edges.add(dataE1);
        
        graph.add("nodes", nodes);
        graph.add("edges", edges);
        
        out.println(graph.toString());
        out.close();
	}
	
	public static void main(String args[]) {
		JsonObject graph = new JsonObject();
        graph.addProperty("success", true);
        
        JsonArray nodes = new JsonArray();
        JsonArray edges = new JsonArray();
        
        JsonObject dataN1 = new JsonObject();
        JsonObject n1 = new JsonObject();
        n1.addProperty("id", "n1");
        dataN1.add("data", n1);
        JsonObject dataN2 = new JsonObject();
        JsonObject n2 = new JsonObject();
        n2.addProperty("id", "n2");
        dataN2.add("data", n2);
        JsonObject dataE1 = new JsonObject();
        JsonObject e1 = new JsonObject();
        e1.addProperty("id", "e1");
        e1.addProperty("source", "n1");
        e1.addProperty("target", "n2");
        dataE1.add("data", e1);
        
        nodes.add(dataN1);
        nodes.add(dataN2);
        edges.add(dataE1);
        
        graph.add("nodes", nodes);
        graph.add("edges", edges);
        
        System.out.println(graph.toString());
	}

}

class NameObj2 {
	private String nameSt;
	
	public NameObj2(String nameSt) {
		this.nameSt = nameSt;
	}
	
	public String getName() {
		return nameSt;
	}
}
