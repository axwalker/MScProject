package uk.ac.bham.cs.commdet.servlet;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import edu.cmu.graphchi.ChiLogger;

import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;

/**
 * Get the number of nodes at a particular level of a graph hierarchy.
 */
@WebServlet("/GetLevelSize")
public class GetLevelSize extends HttpServlet {

	private static final Logger logger = ChiLogger.getLogger("servlet");

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession(false);
		String responseString;
		if (session != null) {
			GraphResult result = (GraphResult)session.getAttribute("result");
			try {
				int fileLevel = Integer.parseInt(request.getParameter("graphLevel"));
				responseString = result.getLevelNodeCount(fileLevel) + "";
			} catch (Exception e) {
				responseString = "{ \"success\" : false }";
				logger.info(e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()));
			}
		} else {
			responseString = "{ \"success\": false , " + "\"error\": \""
					+ "current session timed out, please try again" + "\"}";
		}
		response.setContentType("text/plain");
		response.getWriter().println(responseString);
		
	}

}