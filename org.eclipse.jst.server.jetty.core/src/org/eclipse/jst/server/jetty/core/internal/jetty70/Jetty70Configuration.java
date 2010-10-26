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
package org.eclipse.jst.server.jetty.core.internal.jetty70;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.jst.server.jetty.core.internal.xml.Factory;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.ServerInstance;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server.Connector;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server.Server;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server.WebApp;
import org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.webapp.WebAppContext;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.ServerPort;

public class Jetty70Configuration extends JettyConfiguration implements
		JettyConstants {

	public Jetty70Configuration(IFolder path) {
		super(path);
	}

	private StartIni startIniConfig;

	protected ServerInstance serverInstance;
	private boolean isServerDirty;
	// property change listeners
	private transient List<PropertyChangeListener> propertyListeners;

	public Collection<ServerPort> getServerPorts() {
		List<ServerPort> ports = new ArrayList<ServerPort>();

		// first add server port
		// try {
		// int port = Integer.parseInt(server.getPort());
		// ports.add(new ServerPort("server", Messages.portServer, port,
		// "TCPIP"));
		// } catch (Exception e) {
		// // ignore
		// }

		// add connectors
		try {

			Collection<Connector> connectors = serverInstance.getConnectors();
			if (connectors != null) {
				for (Connector connector : connectors) {
					int port = -1;
					try {
						port = Integer.parseInt(connector.getPort());
					} catch (Exception e) {
						// ignore
					}
					ports.add(new ServerPort("server", Messages.portServer,
							port, HTTP));
					// TODO : how get HTTP type port???

					// ports.add(new ServerPort(portId, name, port, protocol2,
					// contentTypes, advanced));
				}
			}

		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error getting server ports", e);
		}
		if (ports.size() < 1) {
			ports.add(new ServerPort("server", Messages.portServer, 8080, HTTP));
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
	 * Return a list of the web modules in this server.
	 * 
	 * @return java.util.List
	 */
	public List<WebModule> getWebModules() {
		List list = new ArrayList();

		try {
			Collection<WebAppContext> contexts = serverInstance.getContexts();
			if (contexts != null) {
				for (WebAppContext context : contexts) {
					String documentBase = context.getDocumentBase();
					String path = context.getContextPath();
					String memento = context.getMemento();
					WebModule module = new WebModule(path, documentBase,
							memento, true);
					list.add(module);
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error getting project refs", e);
		}
		return list;
	}

	public void addWebModule(int i, IJettyWebModule module) {
		try {
			WebAppContext context = serverInstance.createContext(
					module.getDocumentBase(), module.getMemento(),
					module.getPath());
			if (context != null) {
				// context.setDocBase(module.getDocumentBase());
				// context.setPath(module.getPath());
				// context.setReloadable(module.isReloadable() ? "true" :
				// "false");
				// if (module.getMemento() != null &&
				// module.getMemento().length() > 0)
				// context.setSource(module.getMemento());
				isServerDirty = true;
				firePropertyChangeEvent(ADD_WEB_MODULE_PROPERTY, null, module);
			}
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE,
					"Error adding web module " + module.getPath(), e);
		}

	}

	/**
	 * Removes a web module.
	 * 
	 * @param index
	 *            int
	 */
	public void removeWebModule(int index) {
		try {
			serverInstance.removeContext(index);
			isServerDirty = true;
			firePropertyChangeEvent(REMOVE_WEB_MODULE_PROPERTY, null, index);
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error removing module ref " + index, e);
		}
	}

	protected void firePropertyChangeEvent(String propertyName,
			Object oldValue, Object newValue) {
		if (propertyListeners == null)
			return;

		PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName,
				oldValue, newValue);
		try {
			Iterator<PropertyChangeListener> iterator = propertyListeners
					.iterator();
			while (iterator.hasNext()) {
				try {
					PropertyChangeListener listener = iterator.next();
					listener.propertyChange(event);
				} catch (Exception e) {
					Trace.trace(Trace.SEVERE,
							"Error firing property change event", e);
				}
			}
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error in property event", e);
		}
	}

	/**
	 * Adds a property change listener to this server.
	 * 
	 * @param listener
	 *            java.beans.PropertyChangeListener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (propertyListeners == null)
			propertyListeners = new ArrayList<PropertyChangeListener>();
		propertyListeners.add(listener);
	}

	/**
	 * Removes a property change listener from this server.
	 * 
	 * @param listener
	 *            java.beans.PropertyChangeListener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (propertyListeners != null)
			propertyListeners.remove(listener);
	}

	/**
	 * @see JettyConfiguration#load(IPath, IProgressMonitor)
	 */
	public void load(IPath path, IPath runtimeBaseDirectory,
			IProgressMonitor monitor) throws CoreException {
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);
			monitor.beginTask(Messages.loadingTask, 5);

			Factory serverFactory = null;

			// Load config.ini
			this.startIniConfig = new StartIni(path);

			// Load jetty.xml files
			List<PathFileConfig> jettyXMLConfiFiles = startIniConfig
					.getJettyXMLFiles();
			List<Server> servers = new ArrayList<Server>();
			Server server = null;
			File file = null;
			IPath jettyPath = null;
			if (jettyXMLConfiFiles.size() > 0) {
				for (PathFileConfig jettyXMLConfig : jettyXMLConfiFiles) {
					file = jettyXMLConfig.getFile();

					jettyPath = jettyXMLConfig.getPath();
					serverFactory = new Factory();
					serverFactory
							.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server");
					server = (Server) serverFactory.loadDocument(JettyXMLConfig
							.getInputStream(file));
					server.setFile(file);
					server.setPath(jettyPath);
					servers.add(server);
				}
			}
			
			WebApp webApp = null;
			PathFileConfig pathFileConfig = startIniConfig.getWebdefaultXMLConfig();
			if (pathFileConfig != null) {
				File webAppFile = pathFileConfig.getFile();
				IPath webAppPath = pathFileConfig.getPath();
				
				Factory webdefaultFactory = new Factory();
				webdefaultFactory
						.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server");
				webApp = (WebApp) webdefaultFactory.loadDocument(WebdefaultXMLConfig
						.getInputStream(webAppFile));	
				webApp.setFile(webAppFile);
				webApp.setPath(webAppPath);
			}
			// check for catalina.policy to verify that this is a v4.0 config
			// InputStream in = new
			// FileInputStream(path.append("catalina.policy").toFile());
			// in.read();
			// in.close();
			monitor.worked(1);

			// server = (Server) serverFactory.loadDocument(new FileInputStream(
			// path.append("jetty.xml").toFile()));
			serverInstance = new ServerInstance(servers, webApp, runtimeBaseDirectory);
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
			monitor.worked(1);

			if (monitor.isCanceled())
				return;
			monitor.done();
		} catch (Exception e) {
			Trace.trace(
					Trace.WARNING,
					"Could not load Jetty v7.0 configuration from "
							+ path.toOSString() + ": " + e.getMessage());
			throw new CoreException(new Status(IStatus.ERROR,
					JettyPlugin.PLUGIN_ID, 0, NLS.bind(
							Messages.errorCouldNotLoadConfiguration,
							path.toOSString()), e));
		}
	}

	public void load(IFolder folder, IPath runtimeBaseDirectory,
			IProgressMonitor monitor) throws CoreException {
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);
			monitor.beginTask(Messages.loadingTask, 800);

			Factory serverFactory = null;

			// Load config.ini
			this.startIniConfig = new StartIni(folder);

			// Load jetty.xml files
			List<PathFileConfig> jettyXMLConfiFiles = startIniConfig
					.getJettyXMLFiles();
			List<Server> servers = new ArrayList<Server>();
			Server server = null;
			File file = null;
			IPath jettyPath = null;
			if (jettyXMLConfiFiles.size() > 0) {
				for (PathFileConfig jettyXMLConfig : jettyXMLConfiFiles) {
					file = jettyXMLConfig.getFile();
					jettyPath = jettyXMLConfig.getPath();
					serverFactory = new Factory();
					serverFactory
							.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server");
					server = (Server) serverFactory
							.loadDocument(new FileInputStream(file));
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
			PathFileConfig pathFileConfig = startIniConfig.getWebdefaultXMLConfig();
			if (pathFileConfig != null) {
				File webAppFile = pathFileConfig.getFile();
				IPath webAppPath = pathFileConfig.getPath();
				
				Factory webdefaultFactory = new Factory();
				webdefaultFactory
						.setPackageName("org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server");
				webApp = (WebApp) webdefaultFactory.loadDocument(new FileInputStream(webAppFile));	
				webApp.setFile(webAppFile);
				webApp.setPath(webAppPath);
			}
			// server = (Server) serverFactory.loadDocument(new FileInputStream(
			// path.append("jetty.xml").toFile()));
			serverInstance = new ServerInstance(servers, webApp, runtimeBaseDirectory);
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
			monitor.worked(200);

			// load catalina.policy
			// file = folder.getFile("catalina.policy");
			// in = file.getContents();
			// policyFile = JettyVersionHelper.getFileContents(in);
			monitor.worked(200);

			if (monitor.isCanceled())
				throw new Exception("Cancelled");
			monitor.done();
		} catch (Exception e) {
			Trace.trace(
					Trace.WARNING,
					"Could not reload Jetty v7.0 configuration from: "
							+ folder.getFullPath() + ": " + e.getMessage());
			throw new CoreException(new Status(IStatus.ERROR,
					JettyPlugin.PLUGIN_ID, 0, NLS.bind(
							Messages.errorCouldNotLoadConfiguration, folder
									.getFullPath().toOSString()), e));
		}

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
	public void save(IFolder folder, IProgressMonitor monitor)
			throws CoreException {
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);
			monitor.beginTask(Messages.savingTask, 1200);
			if (monitor.isCanceled())
				return;

			startIniConfig.save(folder.getFile(START_INI), monitor);
			serverInstance.save(folder, monitor);

			// get etc/realm.properties
			// get etc/webdefault.xml

			InputStream in = null;
			IFolder newFolder = folder;
			IPath path = null;
			String filename = null;
			List<PathFileConfig> otherConfigs = startIniConfig
					.getOtherConfigs();
			for (PathFileConfig pathFileConfig : otherConfigs) {
				path = pathFileConfig.getPath();
				if (path.segmentCount() > 1) {
					newFolder = folder.getFolder(path.removeLastSegments(1));
					IOUtils.createFolder(newFolder, monitor);
				}
				filename = pathFileConfig.getFile().getName();
				in = new FileInputStream(pathFileConfig.getFile());
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

			// start.config from start.jar
			PathFileConfig startConfig = startIniConfig.getStartConfig();
			if (startConfig != null) {
				File startJARFile = startConfig.getFile();
				InputStream stream = StartConfig.getInputStream(startJARFile);
				IFile file = folder.getFile("start.config");
				if (file.exists()) {
					// if (isServerDirty)
					file.setContents(stream, true, true,
							ProgressUtil.getSubMonitorFor(monitor, 200));
					// else
					// monitor.worked(200);
				} else
					file.create(stream, true,
							ProgressUtil.getSubMonitorFor(monitor, 200));

			}

			monitor.done();
		} catch (Exception e) {
			Trace.trace(
					Trace.SEVERE,
					"Could not save Jetty v7.0 configuration to "
							+ folder.toString(), e);
			throw new CoreException(new Status(IStatus.ERROR,
					JettyPlugin.PLUGIN_ID, 0, NLS.bind(
							Messages.errorCouldNotSaveConfiguration,
							new String[] { e.getLocalizedMessage() }), e));
		}
	}

	public void importFromPath(IPath path, IPath runtimeBaseDirectory,
			boolean isTestEnv, IProgressMonitor monitor) throws CoreException {
		load(path, runtimeBaseDirectory, monitor);

		// for test environment, remove existing contexts since a separate
		// catalina.base will be used
		if (isTestEnv) {
			while (serverInstance.removeContext(0)) {
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
	public void modifyServerPort(String id, int port) {
		try {
			if ("server".equals(id)) {
				serverInstance.setPort(port + "");
				isServerDirty = true;
				firePropertyChangeEvent(MODIFY_PORT_PROPERTY, id, new Integer(
						port));
				return;
			}

			// int i = id.indexOf("/");
			// // If a connector in the instance Service
			// if (i < 0) {
			// int connNum = Integer.parseInt(id);
			// Connector connector = serverInstance.getConnector(connNum);
			// if (connector != null) {
			// connector.setPort(port + "");
			// isServerDirty = true;
			// firePropertyChangeEvent(MODIFY_PORT_PROPERTY, id, new
			// Integer(port));
			// }
			// }
			// // Else a connector in another Service
			// else {
			// int servNum = Integer.parseInt(id.substring(0, i));
			// int connNum = Integer.parseInt(id.substring(i + 1));
			//
			// Service service = server.getService(servNum);
			// Connector connector = service.getConnector(connNum);
			// connector.setPort(port + "");
			// isServerDirty = true;
			// firePropertyChangeEvent(MODIFY_PORT_PROPERTY, id, new
			// Integer(port));
			// }
		} catch (Exception e) {
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
	public void modifyWebModule(int index, String docBase, String path,
			boolean reloadable) {
		try {
			WebAppContext context = serverInstance.getContext(index);
			if (context != null) {
				context.setContextPath(path);
				context.save();
				isServerDirty = true;
				WebModule module = new WebModule(path, docBase, null,
						reloadable);
				firePropertyChangeEvent(MODIFY_WEB_MODULE_PROPERTY,
						new Integer(index), module);
			}
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error modifying web module " + index, e);
		}
	}

}
