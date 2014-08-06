package uk.ac.bham.cs.commdet.servlet;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import edu.cmu.graphchi.ChiLogger;

import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;

@WebServlet("/GetCommunitySize")
public class GetCommunitySize extends HttpServlet {

	private static final Logger logger = ChiLogger.getLogger("servlet");

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession(false);
		String responseString;
		
		if (session != null) {
			GraphResult result = (GraphResult)session.getAttribute("result");
			int fileLevel = Integer.parseInt(request.getParameter("graphLevel"));
			int community = Integer.parseInt(request.getParameter("selectedNode"));
			int communityLevel = Integer.parseInt(request.getParameter("currentLevel"));

			try {
				responseString = result.getCommunityEdgeCount(community, communityLevel - 1, fileLevel) + "";
				
				logger.info("Response written succesfully");
			} catch (Exception e) {
				responseString = "{ \"success\" : false }";
				logger.info(e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()));
			}
		} else {
			responseString = "error: no session found";
		}
		response.setContentType("text/plain");
		response.getWriter().println(responseString);
	}

}