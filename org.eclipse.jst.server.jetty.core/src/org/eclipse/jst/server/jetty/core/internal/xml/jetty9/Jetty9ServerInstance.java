package org.eclipse.jst.server.jetty.core.internal.xml.jetty9;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.server.jetty.core.internal.JettyConstants;
import org.eclipse.jst.server.jetty.core.internal.JettyServer;
import org.eclipse.jst.server.jetty.core.internal.Trace;
import org.eclipse.jst.server.jetty.core.internal.config.Jetty9StartIni;
import org.eclipse.jst.server.jetty.core.internal.util.IOUtils;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.ServerInstance;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.Server;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.webapp.WebAppContext;
import org.xml.sax.SAXException;

public class Jetty9ServerInstance extends ServerInstance {

	private final Jetty9StartIni startIni;

	public Jetty9ServerInstance(final IPath runtimeBaseDirectory) {
		super(Collections.<Server> emptyList(), null, runtimeBaseDirectory);
		final File startIniFile = new File(runtimeBaseDirectory.toFile(), JettyConstants.START_INI);
		startIni = new Jetty9StartIni(startIniFile);
		Trace.trace(Trace.CONFIG, "Jetty9ServerInstance() read " + startIniFile);
	}

	public Jetty9StartIni getStartIni() {
		return startIni;
	}

	@Override
	protected IPath getXMLContextFolderPath() {
		return _runtimeBaseDirectory.append(JettyServer.DEFAULT_DEPLOYDIR);
	}

	/**
	 * Do nothing, Jetty 9 does not need special context XML files for anything.
	 * @return {@code true} if anything was removed
	 */
	@Override
	public boolean removeContext(final int index) {
		return false;
	}

	/**
	 * Do nothing, Jetty 9 does not need special context XML files for anything.
	 */
	@Override
	public WebAppContext createContext(final String documentBase, final String memento, final String path) throws IOException, SAXException {
		return null;
	}

	@Override
	public void save(final IFolder folder, final IProgressMonitor monitor) throws IOException, CoreException {
		// Write start.ini
		final IFile startIniFile = folder.getFile(JettyConstants.START_INI);
		startIni.save(startIniFile);
		Trace.trace(Trace.CONFIG, "Jetty9ServerInstance.save() wrote " + startIniFile);

		// Write adminPort
		FileWriter adminPortWriter = null;
		try {
			final File adminPortFile = new File(folder.getFile(JettyConstants.ADMIN_PORT).getLocation().toOSString());
			adminPortWriter = new FileWriter(adminPortFile, false);
			adminPortWriter.write(String.valueOf(getAdminPort()));
			adminPortWriter.flush();
			Trace.trace(Trace.CONFIG, "Jetty9ServerInstance.save() wrote " + adminPortFile);
		} finally {
			IOUtils.closeQuietly(adminPortWriter);
		}
	}

}
