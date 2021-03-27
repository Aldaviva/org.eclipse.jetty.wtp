package org.eclipse.jst.server.jetty.core.internal.jetty9;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.server.jetty.core.internal.jetty7.Jetty7Configuration;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.ServerInstance;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.Server;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.WebApp;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty9.Jetty9ServerInstance;
import org.eclipse.wst.server.core.ServerPort;

public class Jetty9Configuration extends Jetty7Configuration {

	public Jetty9Configuration(final IFolder path) {
		super(path);
	}

	@Override
	protected ServerInstance createServerInstance(final IPath runtimeBaseDirectory, final List<Server> servers, final WebApp webApp) {
		return new Jetty9ServerInstance(servers, webApp, runtimeBaseDirectory);
	}

	@Override
	protected void writeStartConfigFile(final IFolder folder, final IProgressMonitor monitor)
	    throws ZipException, IOException, CoreException {
		// Do nothing because Jetty 9 does not use start.config
	}

	@Override
	public ServerPort getMainPort() {
		// TODO Generate a special Jetty 9 start.ini file when saving the configuration, and read the optional jetty.http.port value here 
		//		return new ServerPort("0", "HTTP", 8080, "HTTP"); //not sure why I needed this
		return super.getMainPort();
	}

}
