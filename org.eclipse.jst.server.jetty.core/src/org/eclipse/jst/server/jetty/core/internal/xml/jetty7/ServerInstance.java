/*******************************************************************************
 * Copyright (c) 2010 Angelo Zerr and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - Initial API and implementation 
 *******************************************************************************/
package org.eclipse.jst.server.jetty.core.internal.xml.jetty7;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.server.core.internal.ProgressUtil;
import org.eclipse.jst.server.jetty.core.internal.JettyServer;
import org.eclipse.jst.server.jetty.core.internal.Trace;
import org.eclipse.jst.server.jetty.core.internal.util.IOUtils;
import org.eclipse.jst.server.jetty.core.internal.xml.Factory;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.Connector;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.Server;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.WebApp;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.webapp.WebAppContext;
import org.xml.sax.SAXException;

public class ServerInstance {

	private final List<Server> _jettyServers;
	protected IPath _runtimeBaseDirectory;
	private boolean _contextsLoaded = false;
	private final List<WebAppContext> _webAppContexts = new ArrayList<WebAppContext>();
	private WebApp _webApp = null;
	private int _adminPort = 8082;

	public ServerInstance(final List<Server> jettyServers, final WebApp webApp, final IPath runtimeBaseDirectory) {
		if (jettyServers == null) {
			throw new IllegalArgumentException("Jetty Server argument may not be null.");
		}

		this._jettyServers = jettyServers;
		this._runtimeBaseDirectory = runtimeBaseDirectory;
		this._webApp = webApp;
	}

	public List<Connector> getConnectors() {
		List<Connector> allConnectors = null;
		List<Connector> serverConnectors = null;
		for (final Server server : _jettyServers) {
			serverConnectors = server.getConnectors();
			if (serverConnectors != null) {
				if (allConnectors == null) {
					allConnectors = new ArrayList<Connector>();
				}
				allConnectors.addAll(serverConnectors);
			}
		}
		return allConnectors;
	}

	public boolean removeContext(final int index) {
		if (index >= _webAppContexts.size()) {
			return false;
		}
		final WebAppContext webAppContext = _webAppContexts.remove(index);
		if (webAppContext != null) {
			final IPath contextFilePath = getXMLContextFilePath(webAppContext.getContextPath());
			final File contextFile = contextFilePath.toFile();
			if (contextFile.exists()) {
				contextFile.delete();
			}
		}
		return (webAppContext != null);
	}

	public List<Server> getJettyServers() {
		return _jettyServers;
	}

	public void save(final IFolder folder, final IProgressMonitor monitor) throws IOException, CoreException {
		IPath path = null;
		String filename = null;
		byte[] data = null;
		InputStream in = null;
		IFolder newFolder = folder;
		for (final Server jettyServer : _jettyServers) {
			path = jettyServer.getPath();
			if (path.segmentCount() > 1) {
				newFolder = folder.getFolder(path.removeLastSegments(1));
				IOUtils.createFolder(newFolder, monitor);
			}

			filename = jettyServer.getFile().getName();
			data = jettyServer.getFactory().getContents();
			in = new ByteArrayInputStream(data);
			final IFile file = newFolder.getFile(filename);
			if (file.exists()) {
				// if (isServerDirty)
				file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
				// else
				// monitor.worked(200);
			} else {
				file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
			}

			Trace.trace(Trace.FINER, "ServerInstance.save() wrote " + jettyServer + " to " + file.getLocation());
		}

		final IFile adminPortFile = folder.getFile("adminPort");
		in = new ByteArrayInputStream(Integer.toString(_adminPort).getBytes());
		if (adminPortFile.exists()) {
			adminPortFile.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
		} else {
			adminPortFile.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
		}
		Trace.trace(Trace.FINER, "ServerInstance.save() wrote " + _adminPort + " to " + adminPortFile.getLocation());

		if (_webApp != null) {
			path = _webApp.getPath();
			if (path.segmentCount() > 1) {
				newFolder = folder.getFolder(path.removeLastSegments(1));
				IOUtils.createFolder(newFolder, monitor);
			}

			filename = _webApp.getFile().getName();
			data = _webApp.getFactory().getContents();
			in = new ByteArrayInputStream(data);
			final IFile file = newFolder.getFile(filename);
			if (file.exists()) {
				// if (isServerDirty)
				file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
				// else
				// monitor.worked(200);
			} else {
				file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
			}
			Trace.trace(Trace.FINER, "ServerInstance.save() wrote " + _webApp + " to " + file.getLocation());
		}

	}

	public WebAppContext createContext(final String documentBase, final String memento, final String path) throws IOException, SAXException {
		loadContextsIfNeeded();
		String pathWithoutSlash = path;
		if (pathWithoutSlash.startsWith("/")) {
			pathWithoutSlash = pathWithoutSlash.substring(1, pathWithoutSlash.length());
		}
		final WebAppContext context = createContext(WebAppContext.class.getResourceAsStream("WebAppContext.xml"));
		context.setContextPath(pathWithoutSlash);

		final File f = new File(documentBase);
		if (f.exists()) {
			context.setWar(documentBase, true);
		} else {
			context.setWar("/" + JettyServer.DEFAULT_DEPLOYDIR + "/" + pathWithoutSlash, false);
		}

		final IPath contextFilePath = getXMLContextFilePath(pathWithoutSlash);
		context.setSaveFile(contextFilePath.toFile());
		context.save();
		return context;
	}

	private IPath getXMLContextFilePath(final String path) {
		String pathWithoutSlash = path;
		if (pathWithoutSlash.startsWith("/")) {
			pathWithoutSlash = pathWithoutSlash.substring(1, pathWithoutSlash.length());
		}
		// Save it as file in the WTP /contexts
		final String fileName = pathWithoutSlash + ".xml";
		final IPath contextFolderPath = getXMLContextFolderPath();
		final File folder = contextFolderPath.toFile();
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return contextFolderPath.append(fileName);
	}

	private WebAppContext createContext(final InputStream stream) throws IOException, SAXException {
		final Factory webAppContextFactory = new Factory();
		webAppContextFactory.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetty7.webapp");
		final WebAppContext context = (WebAppContext) webAppContextFactory.loadDocument(stream);
		_webAppContexts.add(context);
		return context;
	}

	private void loadContextsIfNeeded() {
		if (_contextsLoaded) {
			return;
		}
		try {
			WebAppContext context = null;
			final IPath contexts = getXMLContextFolderPath();
			final File contextsFolder = contexts.toFile();
			if (contextsFolder.exists()) {
				InputStream stream = null;
				File f = null;
				final File[] files = contextsFolder.listFiles();
				for (int i = 0; i < files.length; i++) {
					f = files[i];
					try {
						stream = new FileInputStream(f);
						context = createContext(stream);
						context.setSaveFile(f);
					} catch (final Throwable e) {
						e.printStackTrace();
					} finally {
						if (stream != null) {
							try {
								stream.close();
							} catch (final IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		} finally {
			_contextsLoaded = true;
		}
	}

	public Collection<WebAppContext> getContexts() {
		loadContextsIfNeeded();
		return _webAppContexts;
	}

	public WebAppContext getContext(final int index) {
		return _webAppContexts.get(index);
	}

	public void setAdminPort(final int port) {
		_adminPort = port;
	}

	public int getAdminPort() {
		return _adminPort;
	}

	protected IPath getXMLContextFolderPath() {
		return _runtimeBaseDirectory.append("contexts");
	}
}
