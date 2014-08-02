package uk.ac.bham.cs.commdet.servlet;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import edu.cmu.graphchi.ChiLogger;

import uk.ac.bham.cs.commdet.cyto.graph.GraphGenerator;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;

@WebServlet("/DownloadGraph")
public class DownloadGraph extends HttpServlet {

	private static final Logger logger = ChiLogger.getLogger("servlet");

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		HttpSession session = request.getSession(false);
		//String responseString;
		OutputStream os = response.getOutputStream();
		response.setContentType("text/plain");

		if (session != null) {
			try {
				GraphResult result = (GraphResult)session.getAttribute("result");
				GraphGenerator generator = new GraphGenerator(result);
				generator.setIncludeEdges(true);
				int fileLevel = Integer.parseInt(request.getParameter("graphLevel"));
				int colourLevel = Integer.parseInt(request.getParameter("colourLevel"));
				response.setHeader("Content-Disposition", "attachment;filename=graph.gml");
				if (request.getParameter("selectedNode") != null) {
					int community = Integer.parseInt(request.getParameter("selectedNode"));
					int communityLevel = Integer.parseInt(request.getParameter("currentLevel"));
					generator.ouputCommunityGML(community, communityLevel - 1, fileLevel, colourLevel, os);
				} else {
					generator.outputGraphGML(fileLevel, colourLevel, os);
				}
				logger.info("Response written succesfully");
			} catch (Exception e) {
				logger.info(e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()));
			}

			os.close();

		} else {
			//responseString = "{ \"success\" : false }";
		}		
	}

}