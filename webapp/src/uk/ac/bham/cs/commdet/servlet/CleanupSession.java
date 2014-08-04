package uk.ac.bham.cs.commdet.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.io.FileUtils;

@WebListener
public class CleanupSession implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		HttpSession session = event.getSession();
		File tempFolderToDelete = new File((String)session.getAttribute("folder"));
		try {
			FileUtils.deleteDirectory(tempFolderToDelete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
