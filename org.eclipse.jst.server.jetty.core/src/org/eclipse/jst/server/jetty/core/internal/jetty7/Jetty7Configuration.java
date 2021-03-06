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
package org.eclipse.jst.server.jetty.core.internal.jetty7;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.internal.ProgressUtil;
import org.eclipse.jst.server.jetty.core.JettyPlugin;
import org.eclipse.jst.server.jetty.core.WebModule;
import org.eclipse.jst.server.jetty.core.internal.IJettyWebModule;
import org.eclipse.jst.server.jetty.core.internal.JettyConfiguration;
import org.eclipse.jst.server.jetty.core.internal.JettyConstants;
import org.eclipse.jst.server.jetty.core.internal.Messages;
import org.eclipse.jst.server.jetty.core.internal.Trace;
import org.eclipse.jst.server.jetty.core.internal.config.JettyXMLConfig;
import org.eclipse.jst.server.jetty.core.internal.config.PathFileConfig;
import org.eclipse.jst.server.jetty.core.internal.config.StartConfig;
import org.eclipse.jst.server.jetty.core.internal.config.StartIni;
import org.eclipse.jst.server.jetty.core.internal.config.WebdefaultXMLConfig;
import org.eclipse.jst.server.jetty.core.internal.util.IOUtils;
import org.eclipse.jst.server.jetty.core.internal.util.StringUtils;
import org.eclipse.jst.server.jetty.core.internal.xml.Factory;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.ServerInstance;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.Connector;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.Server;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.WebApp;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.webapp.WebAppContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.ServerPort;

public class Jetty7Configuration extends JettyConfiguration implements JettyConstants {
	private StartIni _startIniConfig;

	protected ServerInstance _serverInstance;

	private boolean _isServerDirty;

	// property change listeners
	private transient List<PropertyChangeListener> _propertyListeners;

	public Jetty7Configuration(final IFolder path) {
		super(path);
		Trace.trace(Trace.CONFIG, "Created Jetty7Configuration with path " + path.getFullPath());
	}

	public Collection<ServerPort> getServerPorts() {
		final List<ServerPort> ports = new ArrayList<ServerPort>();

		// first add server port
		try {
			final int port = _serverInstance.getAdminPort();
			ports.add(new ServerPort("server", Messages.portServer, port, "TCPIP"));
		} catch (final Exception e) {
			// ignore
		}

		// add connectors
		try {

			final Collection<Connector> connectors = _serverInstance.getConnectors();
			if (connectors != null) {
				int portId = 0;
				for (final Connector connector : connectors) {
					Trace.trace(Trace.FINEST, "Found server connector with port=" + connector.getPort() + " and class=" + connector.getType());
					int port = -1;
					try {
						port = Integer.parseInt(connector.getPort());
					} catch (final Exception e) {
						// ignore
					}

					final String id = Integer.toString(portId++);
					final String type = connector.getType();
					String name = "HTTP";
					final String className = type.substring(type.lastIndexOf('.') + 1);
					if ("SelectChannelConnector".equals(className) ||
					    "SocketConnector".equals(className)) {
						name = "HTTP";
					} else if ("SslSelectChannelConnector".equals(className) ||
					    "SslSocketConnector".equals(className)) {
						name = "SSL";
					} else if ("Ajp13SocketConnector".equals(className)) {
						name = "AJP";
					}

					ports.add(new ServerPort(id, name, port, name));

					// ports.add(new ServerPort(portId, name, port, protocol2,
					// contentTypes, advanced));
				}
			}

		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error getting server ports", e);
		}

		return ports;

		// String instanceServiceName = serverInstance.getService().getName();
		// int size = server.getServiceCount();
		// for (int i = 0; i < size; i++) {
		// Service service = server.getService(i);
		// int size2 = service.getConnectorCount();
		// for (int j = 0; j < size2; j++) {
		// Connector connector = service.getConnector(j);
		// String name = "HTTP/1.1";
		// String protocol2 = "HTTP";
		// boolean advanced = true;
		// String[] contentTypes = null;
		// int port = -1;
		// try {
		// port = Integer.parseInt(connector.getPort());
		// } catch (Exception e) {
		// // ignore
		// }
		// String protocol = connector.getProtocol();
		// if (protocol != null && protocol.length() > 0) {
		// if (protocol.startsWith("HTTP")) {
		// name = protocol;
		// }
		// else if (protocol.startsWith("AJP")) {
		// name = protocol;
		// protocol2 = "AJP";
		// }
		// else {
		// // Get Jetty equivalent name if protocol handler class specified
		// name = (String)protocolHandlerMap.get(protocol);
		// if (name != null) {
		// // Prepare simple protocol string for ServerPort protocol
		// int index = name.indexOf('/');
		// if (index > 0)
		// protocol2 = name.substring(0, index);
		// else
		// protocol2 = name;
		// }
		// // Specified protocol is unknown, just use as is
		// else {
		// name = protocol;
		// protocol2 = protocol;
		// }
		// }
		// }
		// if (protocol2.toLowerCase().equals("http"))
		// contentTypes = new String[] { "web", "webservices" };
		// String secure = connector.getSecure();
		// if (secure != null && secure.length() > 0) {
		// name = "SSL";
		// protocol2 = "SSL";
		// } else
		// advanced = false;
		// String portId;
		// if (instanceServiceName != null &&
		// instanceServiceName.equals(service.getName()))
		// portId = Integer.toString(j);
		// else
		// portId = i +"/" + j;
		// ports.add(new ServerPort(portId, name, port, protocol2, contentTypes,
		// advanced));
		// }

	}

	/**
	 * Return the port number.
	 * 
	 * @return int
	 */
	public ServerPort getAdminPort() {
		final Collection<ServerPort> serverPorts = getServerPorts();

		for (final ServerPort serverPort : serverPorts) {
			// Return only an HTTP port from the selected Service
			if (serverPort.getId().equals("server")) {
				return serverPort;
			}
		}

		return null;
	}

	/**
	 * Return a list of the web modules in this server.
	 * 
	 * @return java.util.List
	 */
	public List<WebModule> getWebModules() {
		final List<WebModule> list = new ArrayList<WebModule>();

		try {
			final Collection<WebAppContext> contexts = _serverInstance.getContexts();
			if (contexts != null) {
				for (final WebAppContext context : contexts) {
					final String documentBase = context.getDocumentBase();
					final String path = context.getContextPath();
					final String memento = context.getMemento();
					final WebModule module = new WebModule(path, documentBase, memento, true);
					list.add(module);
				}
			}
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error getting project refs", e);
		}
		return list;
	}

	public void addWebModule(final int i, final IJettyWebModule module) {
		try {
			final WebAppContext context = _serverInstance.createContext(module.getDocumentBase(), module.getMemento(), module.getPath());
			if (context != null) {
				// context.setDocBase(module.getDocumentBase());
				// context.setPath(module.getPath());
				// context.setReloadable(module.isReloadable() ? "true" :
				// "false");
				// if (module.getMemento() != null &&
				// module.getMemento().length() > 0)
				// context.setSource(module.getMemento());
				_isServerDirty = true;
				firePropertyChangeEvent(__ADD_WEB_MODULE_PROPERTY, null, module);
			}
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error adding web module " + module.getPath(), e);
		}

	}

	/**
	 * Removes a web module.
	 * 
	 * @param index
	 *            int
	 */
	public void removeWebModule(final int index) {
		try {
			_serverInstance.removeContext(index);
			_isServerDirty = true;
			firePropertyChangeEvent(__REMOVE_WEB_MODULE_PROPERTY, null, index);
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error removing module ref " + index, e);
		}
	}

	protected void firePropertyChangeEvent(final String propertyName, final Object oldValue, final Object newValue) {
		if (_propertyListeners == null) {
			return;
		}

		final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
		try {
			final Iterator<PropertyChangeListener> iterator = _propertyListeners.iterator();
			while (iterator.hasNext()) {
				try {
					final PropertyChangeListener listener = iterator.next();
					listener.propertyChange(event);
				} catch (final Exception e) {
					Trace.trace(Trace.SEVERE, "Error firing property change event", e);
				}
			}
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error in property event", e);
		}
	}

	/**
	 * Adds a property change listener to this server.
	 * 
	 * @param listener
	 *            java.beans.PropertyChangeListener
	 */
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		if (_propertyListeners == null) {
			_propertyListeners = new ArrayList<PropertyChangeListener>();
		}
		_propertyListeners.add(listener);
	}

	/**
	 * Removes a property change listener from this server.
	 * 
	 * @param listener
	 *            java.beans.PropertyChangeListener
	 */
	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		if (_propertyListeners != null) {
			_propertyListeners.remove(listener);
		}
	}

	/**
	 * @see JettyConfiguration#load(IPath, IProgressMonitor)
	 */
	public void load(final IPath path, final IPath runtimeBaseDirectory, IProgressMonitor monitor) throws CoreException {
		Trace.trace(Trace.CONFIG, "Loading Jetty 7 configuration from path " + path + " and runtime directory " + runtimeBaseDirectory);
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);
			monitor.beginTask(Messages.loadingTask, 5);

			Factory serverFactory = null;

			// Load config.ini
			this._startIniConfig = new StartIni(path);

			// Load jetty.xml files
			final List<PathFileConfig> jettyXMLConfiFiles = _startIniConfig.getJettyXMLFiles();
			final List<Server> servers = new ArrayList<Server>();
			Server server = null;
			File file = null;
			IPath jettyPath = null;
			if (jettyXMLConfiFiles.size() > 0) {
				for (final PathFileConfig jettyXMLConfig : jettyXMLConfiFiles) {
					file = jettyXMLConfig.getFile();

					jettyPath = jettyXMLConfig.getPath();
					serverFactory = new Factory();
					serverFactory.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server");
					server = (Server) serverFactory.loadDocument(JettyXMLConfig.getInputStream(file));
					server.setFile(file);
					server.setPath(jettyPath);
					servers.add(server);
				}
			}

			WebApp webApp = null;
			final PathFileConfig pathFileConfig = _startIniConfig.getWebdefaultXMLConfig();
			if (pathFileConfig != null) {
				final File webAppFile = pathFileConfig.getFile();
				final IPath webAppPath = pathFileConfig.getPath();

				final Factory webdefaultFactory = new Factory();
				webdefaultFactory.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server");
				webApp = (WebApp) webdefaultFactory.loadDocument(WebdefaultXMLConfig.getInputStream(webAppFile));
				webApp.setFile(webAppFile);
				webApp.setPath(webAppPath);
			}

			final File adminPortFile = _startIniConfig.getAdminPortFile();
			String adminPort = null;
			if (adminPortFile != null && adminPortFile.exists()) {
				final BufferedReader reader = new BufferedReader(new FileReader(adminPortFile));
				adminPort = reader.readLine();
				reader.close();
			}
			// check for catalina.policy to verify that this is a v4.0 config
			// InputStream in = new
			// FileInputStream(path.append("catalina.policy").toFile());
			// in.read();
			// in.close();
			monitor.worked(1);

			// server = (Server) serverFactory.loadDocument(new FileInputStream(
			// path.append("jetty.xml").toFile()));
			_serverInstance = createServerInstance(runtimeBaseDirectory, servers, webApp);
			if (adminPort != null) {
				_serverInstance.setAdminPort(Integer.parseInt(adminPort));
			}
			// monitor.worked(1);
			//
			// webAppDocument = new
			// WebAppDocument(path.append("webdefault.xml"));
			// monitor.worked(1);

			// jettyUsersDocument = XMLUtil.getDocumentBuilder().parse(new
			// InputSource(new
			// FileInputStream(path.append("jetty-users.xml").toFile())));
			monitor.worked(1);

			// load policy file
			// policyFile = JettyVersionHelper.getFileContents(new
			// FileInputStream(path.append("catalina.policy").toFile()));
			//monitor.worked(1);

			if (monitor.isCanceled()) {
				return;
			}
			monitor.done();
		} catch (final Exception e) {
			Trace.trace(Trace.WARNING, "Could not load Jetty v7.x configuration from " + path.toOSString() + ": " + e.getMessage());
			throw new CoreException(
			    new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorCouldNotLoadConfiguration, path.toOSString()), e));
		}
	}

	public void load(final IFolder folder, final IPath runtimeBaseDirectory, IProgressMonitor monitor) throws CoreException {
		Trace.trace(Trace.CONFIG, "Loading Jetty 7 configuration from folder " + folder + " and runtime directory " + runtimeBaseDirectory);
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);
			monitor.beginTask(Messages.loadingTask, 800);

			Factory serverFactory = null;

			// Load config.ini
			this._startIniConfig = new StartIni(folder);

			// Load jetty.xml files
			final List<PathFileConfig> jettyXMLConfiFiles = _startIniConfig.getJettyXMLFiles();
			final List<Server> servers = new ArrayList<Server>();
			Server server = null;
			File file = null;
			IPath jettyPath = null;
			if (jettyXMLConfiFiles.size() > 0) {
				for (final PathFileConfig jettyXMLConfig : jettyXMLConfiFiles) {
					file = jettyXMLConfig.getFile();
					jettyPath = jettyXMLConfig.getPath();
					serverFactory = new Factory();
					serverFactory.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server");
					server = (Server) serverFactory.loadDocument(new FileInputStream(file));
					server.setFile(file);
					server.setPath(jettyPath);
					servers.add(server);
				}
			}
			// check for catalina.policy to verify that this is a v4.0 config
			// InputStream in = new
			// FileInputStream(path.append("catalina.policy").toFile());
			// in.read();
			// in.close();
			monitor.worked(1);

			WebApp webApp = null;
			final PathFileConfig pathFileConfig = _startIniConfig.getWebdefaultXMLConfig();
			if (pathFileConfig != null) {
				final File webAppFile = pathFileConfig.getFile();
				final IPath webAppPath = pathFileConfig.getPath();

				final Factory webdefaultFactory = new Factory();
				webdefaultFactory.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server");
				webApp = (WebApp) webdefaultFactory.loadDocument(new FileInputStream(webAppFile));
				webApp.setFile(webAppFile);
				webApp.setPath(webAppPath);
			}
			final File adminPortFile = _startIniConfig.getAdminPortFile();
			String adminPort = null;
			if (adminPortFile != null && adminPortFile.exists()) {
				final BufferedReader reader = new BufferedReader(new FileReader(adminPortFile));
				adminPort = reader.readLine();
				reader.close();
			}
			// server = (Server) serverFactory.loadDocument(new FileInputStream(
			// path.append("jetty.xml").toFile()));
			_serverInstance = createServerInstance(runtimeBaseDirectory, servers, webApp);
			if (adminPort != null) {
				_serverInstance.setAdminPort(Integer.parseInt(adminPort));
			}
			// check for catalina.policy to verify that this is a v4.0 config
			// IFile file = folder.getFile("catalina.policy");
			// if (!file.exists())
			// throw new CoreException(new Status(IStatus.WARNING,
			// JettyPlugin.PLUGIN_ID, 0,
			// NLS.bind(Messages.errorCouldNotLoadConfiguration,
			// folder.getFullPath().toOSString()), null));

			// load server.xml
			// IFile file = folder.getFile("jetty.xml");
			// InputStream in = file.getContents();
			// serverFactory = new Factory();
			// serverFactory.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.server70");
			// server = (Server) serverFactory.loadDocument(in);
			// serverInstance = new ServerInstance(server);
			// monitor.worked(200);
			//
			// // load web.xml
			// file = folder.getFile("webdefault.xml");
			// webAppDocument = new WebAppDocument(file);
			// monitor.worked(200);

			// load jetty-users.xml
			// file = folder.getFile("jetty-users.xml");
			// in = file.getContents();

			// jettyUsersDocument = XMLUtil.getDocumentBuilder().parse(new
			// InputSource(in));
			//monitor.worked(200);

			// load catalina.policy
			// file = folder.getFile("catalina.policy");
			// in = file.getContents();
			// policyFile = JettyVersionHelper.getFileContents(in);
			monitor.worked(200);

			if (monitor.isCanceled()) {
				throw new Exception("Cancelled");
			}
			monitor.done();
		} catch (final Exception e) {
			Trace.trace(Trace.WARNING, "Could not reload Jetty v7.x configuration from: " + folder.getFullPath() + ": " + e.getMessage());
			throw new CoreException(new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorCouldNotLoadConfiguration, folder.getFullPath()
			    .toOSString()), e));
		}

	}

	protected ServerInstance createServerInstance(final IPath runtimeBaseDirectory, final List<Server> servers, final WebApp webApp) {
		Trace.trace(Trace.CONFIG, "Created new Jetty 7 server instance for runtime directory " + runtimeBaseDirectory + ", servers "
		    + StringUtils.join(servers, ", ") + ", and webapp " + (webApp != null ? webApp.getFile() : null));
		return new ServerInstance(servers, webApp, runtimeBaseDirectory);
	}

	/**
	 * Save the information held by this object to the given directory.
	 * 
	 * @param folder
	 *            a folder
	 * @param monitor
	 *            a progress monitor
	 * @throws CoreException
	 */
	public void save(final IFolder folder, IProgressMonitor monitor) throws CoreException {
		Trace.trace(Trace.CONFIG, "Saving Jetty 7 configuration to folder " + folder);
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);
			monitor.beginTask(Messages.savingTask, 1200);
			if (monitor.isCanceled()) {
				return;
			}

			_startIniConfig.save(folder.getFile(START_INI), monitor);
			_serverInstance.save(folder, monitor);

			// get etc/realm.properties
			// get etc/webdefault.xml

			InputStream in = null;
			IFolder newFolder = folder;
			IPath path = null;
			String filename = null;
			final List<PathFileConfig> otherConfigs = _startIniConfig.getOtherConfigs();
			for (final PathFileConfig pathFileConfig : otherConfigs) {
				path = pathFileConfig.getPath();
				if (path.segmentCount() > 1) {
					newFolder = folder.getFolder(path.removeLastSegments(1));
					IOUtils.createFolder(newFolder, monitor);
				}
				filename = pathFileConfig.getFile().getName();
				in = new FileInputStream(pathFileConfig.getFile());
				final IFile file = newFolder.getFile(filename);
				if (file.exists()) {
					// if (isServerDirty)
					file.setContents(in, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
					// else
					// monitor.worked(200);
				} else {
					file.create(in, true, ProgressUtil.getSubMonitorFor(monitor, 200));
				}
				Trace.trace(Trace.FINER, "Jetty7Configuration.save() copied " + pathFileConfig.getFile().getAbsolutePath() + " to " + file.getLocation());
			}

			writeStartConfigFile(folder, monitor);

			monitor.done();
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Could not save Jetty v7.x configuration to " + folder.toString(), e);
			throw new CoreException(new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0,
			    NLS.bind(Messages.errorCouldNotSaveConfiguration, new String[] { e.getLocalizedMessage() }), e));
		}
	}

	protected void writeStartConfigFile(final IFolder folder, final IProgressMonitor monitor) throws ZipException, IOException, CoreException {
		// start.config from start.jar
		final PathFileConfig startConfig = _startIniConfig.getStartConfig();
		if (startConfig != null) {
			final File startJARFile = startConfig.getFile();
			final InputStream stream = StartConfig.getInputStream(startJARFile);
			final IFile file = folder.getFile("start.config");
			if (file.exists()) {
				// if (isServerDirty)
				file.setContents(stream, true, true, ProgressUtil.getSubMonitorFor(monitor, 200));
				// else
				// monitor.worked(200);
			} else {
				file.create(stream, true, ProgressUtil.getSubMonitorFor(monitor, 200));
			}
			Trace.trace(Trace.FINER, "Jetty7Configuration.writeStartConfigFile() wrote " + file.getLocation());
		}
	}

	public void importFromPath(final IPath path, final IPath runtimeBaseDirectory, final boolean isTestEnv, final IProgressMonitor monitor)
	    throws CoreException {
		load(path, runtimeBaseDirectory, monitor);

		// for test environment, remove existing contexts since a separate
		// catalina.base will be used
		if (isTestEnv) {
			while (_serverInstance.removeContext(0)) {
				// no-op
			}
		}
	}

	/**
	 * Modify the port with the given id.
	 * 
	 * @param id
	 *            java.lang.String
	 * @param port
	 *            int
	 */
	public void modifyServerPort(final String id, final int port) {
		Trace.trace(Trace.CONFIG, "Setting Jetty 7 service " + id + " to use port " + port);
		try {
			if ("server".equals(id)) {
				_serverInstance.setAdminPort(port);
				_isServerDirty = true;
				firePropertyChangeEvent(__MODIFY_PORT_PROPERTY, id, Integer.valueOf(port));
				return;
			}

			final int connNum = Integer.parseInt(id);
			final List<Connector> connectors = _serverInstance.getConnectors();
			final Connector connector = connectors.get(connNum);
			connector.setPort(port + "");
			_isServerDirty = true;
			firePropertyChangeEvent(__MODIFY_PORT_PROPERTY, id, new Integer(port));
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error modifying server port " + id, e);
		}
	}

	/**
	 * Change a web module.
	 * 
	 * @param index
	 *            int
	 * @param docBase
	 *            java.lang.String
	 * @param path
	 *            java.lang.String
	 * @param reloadable
	 *            boolean
	 */
	public void modifyWebModule(final int index, final String docBase, final String path, final boolean reloadable) {
		try {
			final WebAppContext context = _serverInstance.getContext(index);
			if (context != null) {
				context.setContextPath(path);
				context.save();
				_isServerDirty = true;
				final WebModule module = new WebModule(path, docBase, null, reloadable);
				firePropertyChangeEvent(__MODIFY_WEB_MODULE_PROPERTY, Integer.valueOf(index), module);
			}
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error modifying web module " + index, e);
		}
	}

}
