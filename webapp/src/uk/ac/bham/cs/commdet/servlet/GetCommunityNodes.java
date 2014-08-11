package uk.ac.bham.cs.commdet.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codehaus.jackson.map.ObjectMapper;

import uk.ac.bham.cs.commdet.cyto.graph.GraphGenerator;
import uk.ac.bham.cs.commdet.cyto.graph.NodeData;
import uk.ac.bham.cs.commdet.graphchi.all.GraphResult;
import edu.cmu.graphchi.ChiLogger;

/**
 * Send a collection of nodes as a JSON object. These nodes are taken from a a
 * particular community at a particular level of the graph. If this particular
 * community is not yet present in the session, it is generated from the graph
 * result object stored in the session.
 */
@WebServlet("/GetCommunityNodes")
public class GetCommunityNodes extends HttpServlet {

	private static final Logger logger = ChiLogger.getLogger("servlet");

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession session = request.getSession(false);
		String responseString;
		if (session != null) {
			try {

				//parse request parameters
				int fileLevel = Integer.parseInt(request.getParameter("graphLevel"));
				int colourLevel = Integer.parseInt(request.getParameter("colourLevel"));
				int community = Integer.parseInt(request.getParameter("selectedNode"));
				int communityLevel = Integer.parseInt(request.getParameter("currentLevel"));
				int offset = Integer.parseInt(request.getParameter("offset"));
				int size = Integer.parseInt(request.getParameter("size"));

				//get previous session community parameters
				Object previousFileLevel = session.getAttribute("graphLevel");
				Object previousColourLevel = session.getAttribute("colourLevel");
				Object previousCommunity = session.getAttribute("selectedNode");
				Object previousCommunityLevel = session.getAttribute("currentLevel");

				boolean needsNewCommunityTable = previousFileLevel == null
						|| (int) previousFileLevel != fileLevel || previousColourLevel == null
						|| (int) previousColourLevel != colourLevel || previousCommunity == null
						|| (int) previousCommunity != community || previousCommunityLevel == null
						|| (int) previousCommunityLevel != communityLevel;

				if (needsNewCommunityTable) {
					GraphResult result = (GraphResult) session.getAttribute("result");
					GraphGenerator generator = new GraphGenerator(result);
					generator.setIncludeEdges(false);
					List<NodeData> nodes = generator.getNodesFromCommunity(community,
							communityLevel - 1, fileLevel, colourLevel);
					
					session.setAttribute("communityTable", nodes);
					session.setAttribute("graphLevel", fileLevel);
					session.setAttribute("colourLevel", colourLevel);
					session.setAttribute("selectedNode", community);
					session.setAttribute("currentLevel", communityLevel);
				}

				List<NodeData> nodes = (List<NodeData>) session.getAttribute("communityTable");

				offset = Math.min(nodes.size(), offset);
				int endIndex = Math.min(nodes.size(), offset + size);

				List<NodeData> nodesSublist = nodes.subList(offset, endIndex);

				ObjectMapper mapper = new ObjectMapper();
				responseString = "{ \"success\": true, \"nodes\": "
						+ mapper.writeValueAsString(nodesSublist) + "}";

			} catch (Exception e) {
				responseString = "{ \"success\" : false }";
				logger.info(e.getMessage() + "\n" + Arrays.asList(e.getStackTrace()));
			}
		} else {
			responseString = "{ \"success\": false , " + "\"error\": \""
					+ "current session timed out, please try again" + "\"}";
		}
		response.setContentType("application/json");
		response.getWriter().println(responseString);

	}

}
