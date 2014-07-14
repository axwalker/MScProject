package uk.ac.bham.cs.commdet.servlet;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import edu.cmu.graphchi.ChiLogger;

import uk.ac.bham.cs.commdet.cyto.json.GraphJsonGenerator;
import uk.ac.bham.cs.commdet.graphchi.louvain.GraphResult;

@WebServlet("/UpdateGraph")
public class UpdateGraph extends HttpServlet {

	private static final Logger logger = ChiLogger.getLogger("servlet");

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String level = request.getParameter("graphLevel");
		
		HttpSession session = request.getSession(false);
		GraphResult result = (GraphResult)session.getAttribute("result");
		
		String responseString;
		try {
			GraphJsonGenerator generator = new GraphJsonGenerator(result);
			responseString = generator.getGraphJson(Integer.parseInt(level));
			logger.info("Response written succesfully");
		} catch (Exception e) {
			responseString = "{ \"success\" : false }";
			logger.info(e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()));
		}

		response.setContentType("application/json");
		response.getWriter().println(responseString);
		
	}

}

