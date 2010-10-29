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
package org.eclipse.jst.server.jetty.core.internal.xml.jetyy70;

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
import org.eclipse.jst.server.jetty.core.internal.util.IOUtils;
import org.eclipse.jst.server.jetty.core.internal.xml.Factory;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server.Connector;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server.Server;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server.WebApp;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.webapp.WebAppContext;
import org.xml.sax.SAXException;

public class ServerInstance {

	private List<Server> jettyServers;
	private IPath runtimeBaseDirectory;
	private boolean contextsLoaded = false;
	private List<WebAppContext> webAppContexts = new ArrayList<WebAppContext>();
	private WebApp webApp = null;

	public ServerInstance(List<Server> jettyServers, WebApp webApp,
			IPath runtimeBaseDirectory) {
		if (jettyServers == null)
			throw new IllegalArgumentException(
					"Jetty Server argument may not be null.");
		this.jettyServers = jettyServers;
		this.runtimeBaseDirectory = runtimeBaseDirectory;
		this.webApp = webApp;
	}

	public List<Connector> getConnectors() {
		List<Connector> allConnectors = null;
		List<Connector> serverConnectors = null;
		for (Server server : jettyServers) {
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

	public boolean removeContext(int index) {
		if (index >= webAppContexts.size())
			return false;
		WebAppContext webAppContext = webAppContexts.remove(index);
		if (webAppContext != null) {
			IPath contextFilePath = getXMLContextFilePath(webAppContext
					.getContextPath());
			File contextFile = contextFilePath.toFile();
			if (contextFile.exists()) {
				contextFile.delete();
			}
		}
		return (webAppContext != null);
	}

	public List<Server> getJettyServers() {
		return jettyServers;
	}

	public void save(final IFolder folder, IProgressMonitor monitor)
			throws IOException, CoreException {
		IPath path = null;
		String filename = null;
		byte[] data = null;
		InputStream in = null;
		IFolder newFolder = folder;
		for (Server jettyServer : jettyServers) {
			path = jettyServer.getPath();
			if (path.segmentCount() > 1) {
				newFolder = folder.getFolder(path.removeLastSegments(1));
				IOUtils.createFolder(newFolder, monitor);
			}

			filename = jettyServer.getFile().getName();
			data = jettyServer.getFactory().getContents();
			in = new ByteArrayInputStream(data);
			IFile file = newFolder.getFile(filename);
			if (file.exists()) {
				// if (isServerDirty)
				file.setContents(in, true, true,
						ProgressUtil.getSubMonitorFor(monitor, 200));
				// else
				// monitor.worked(200);
			} else
				file.create(in, true,
						ProgressUtil.getSubMonitorFor(monitor, 200));
		}
		if (webApp != null) {
			path = webApp.getPath();
			if (path.segmentCount() > 1) {
				newFolder = folder.getFolder(path.removeLastSegments(1));
				IOUtils.createFolder(newFolder, monitor);
			}

			filename = webApp.getFile().getName();
			data = webApp.getFactory().getContents();
			in = new ByteArrayInputStream(data);
			IFile file = newFolder.getFile(filename);
			if (file.exists()) {
				// if (isServerDirty)
				file.setContents(in, true, true,
						ProgressUtil.getSubMonitorFor(monitor, 200));
				// else
				// monitor.worked(200);
			} else
				file.create(in, true,
						ProgressUtil.getSubMonitorFor(monitor, 200));
		}

	}

	public WebAppContext createContext(String documentBase, String memento,
			String path) throws IOException, SAXException {
		loadContextsIfNeeded();
		String pathWithoutSlash = path;
		if (pathWithoutSlash.startsWith("/")) {
			pathWithoutSlash = pathWithoutSlash.substring(1,
					pathWithoutSlash.length());
		}
		WebAppContext context = createContext(WebAppContext.class
				.getResourceAsStream("WebAppContext.xml"));
		context.setContextPath(pathWithoutSlash);

		File f = new File(documentBase);
		if (f.exists()) {
			context.setWar(documentBase, true);
		} else {
			context.setWar("/wtpwebapps/" + pathWithoutSlash, false);
		}

		IPath contextFilePath = getXMLContextFilePath(pathWithoutSlash);
		context.setSaveFile(contextFilePath.toFile());
		context.save();
		return context;
	}

	private IPath getXMLContextFilePath(String path) {
		String pathWithoutSlash = path;
		if (pathWithoutSlash.startsWith("/")) {
			pathWithoutSlash = pathWithoutSlash.substring(1,
					pathWithoutSlash.length());
		}
		// Save it as file in the WTP /contexts
		String fileName = pathWithoutSlash + ".xml";
		IPath contextFolderPath = runtimeBaseDirectory.append("contexts");
		File folder = contextFolderPath.toFile();
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return contextFolderPath.append(fileName);
	}

	private WebAppContext createContext(InputStream stream) throws IOException,
			SAXException {
		Factory webAppContextFactory = new Factory();
		webAppContextFactory
				.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.webapp");
		WebAppContext context = (WebAppContext) webAppContextFactory
				.loadDocument(stream);
		webAppContexts.add(context);
		return context;
	}

	private void loadContextsIfNeeded() {
		if (contextsLoaded)
			return;
		try {
			WebAppContext context = null;
			IPath contexts = runtimeBaseDirectory.append("contexts");
			File contextsFolder = contexts.toFile();
			if (contextsFolder.exists()) {
				InputStream stream = null;
				File f = null;
				File[] files = contextsFolder.listFiles();
				for (int i = 0; i < files.length; i++) {
					f = files[i];
					try {
						stream = new FileInputStream(f);
						context = createContext(stream);
						context.setSaveFile(f);
					} catch (Throwable e) {
						e.printStackTrace();
					} finally {
						if (stream != null) {
							try {
								stream.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		} finally {
			contextsLoaded = true;
		}
	}

	public Collection<WebAppContext> getContexts() {
		loadContextsIfNeeded();
		return webAppContexts;
	}

	public void setPort(String port) {
		List<Connector> connectors = getConnectors();
		if (connectors != null && connectors.size() > 0) {
			Connector connector = connectors.get(0);
			connector.setPort(port);
		}

	}

	public WebAppContext getContext(int index) {
		return webAppContexts.get(index);
	}

}
