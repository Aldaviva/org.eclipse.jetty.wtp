package org.eclipse.jst.server.jetty.core.internal.xml.jetty9;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.server.jetty.core.internal.JettyServer;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.ServerInstance;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.Server;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.WebApp;

public class Jetty9ServerInstance extends ServerInstance {

	public Jetty9ServerInstance(List<Server> jettyServers, WebApp webApp, IPath runtimeBaseDirectory) {
		super(jettyServers, webApp, runtimeBaseDirectory);
	}

	@Override
	protected IPath getXMLContextFolderPath() {
		return _runtimeBaseDirectory.append(JettyServer.DEFAULT_DEPLOYDIR);
	}
	
	

}
