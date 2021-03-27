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
package org.eclipse.jst.server.jetty.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.internal.ProgressUtil;
import org.eclipse.jst.server.jetty.core.IJettyConfiguration;
import org.eclipse.jst.server.jetty.core.IJettyConfigurationWorkingCopy;
import org.eclipse.jst.server.jetty.core.IJettyServer;
import org.eclipse.jst.server.jetty.core.JettyPlugin;
import org.eclipse.jst.server.jetty.core.WebModule;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerPort;

public abstract class JettyConfiguration implements IJettyConfiguration, IJettyConfigurationWorkingCopy {

	private final IFolder _configPath;

	/**
	 * JettyConfiguration constructor.
	 * 
	 * @param path
	 *            a path
	 */
	public JettyConfiguration(final IFolder path) {
		super();
		this._configPath = path;
	}

	protected IFolder getFolder() {
		return _configPath;
	}

	/**
	 * Return the port number.
	 * 
	 * @return int
	 */
	public ServerPort getMainPort() {
		final Collection<ServerPort> serverPorts = getServerPorts();

		for (final ServerPort serverPort : serverPorts) {
			// Return only an HTTP port from the selected Service
			if (serverPort.getProtocol().toLowerCase().equals("http") && serverPort.getId().indexOf('/') < 0) {
				return serverPort;
			}
		}

		return null;
	}

	/**
	 * Returns the partial URL applicable to this module.
	 * 
	 * @param webModule
	 *            a web module
	 * @return the partial URL
	 */
	public String getWebModuleURL(final IModule webModule) {
		final WebModule module = getWebModule(webModule);

		if (module != null) {
			return module.getPath();
		}

		final IWebModule webModule2 = (IWebModule) webModule.loadAdapter(IWebModule.class, null);

		return "/" + webModule2.getContextRoot();
	}

	/**
	 * Returns the given module from the config.
	 * 
	 * @param module
	 *            a web module
	 * @return a web module
	 */
	public WebModule getWebModule(final IModule module) {
		if (module == null) {
			return null;
		}

		final String memento = module.getId();

		final List<WebModule> modules = getWebModules();
		final int size = modules.size();
		for (int i = 0; i < size; i++) {
			final WebModule webModule = modules.get(i);
			if (memento.equals(webModule.getMemento())) {
				return webModule;
			}
		}
		return null;
	}

	/**
	 * Returns the prefix that is used in front of the web module path property. (e.g. "webapps")
	 * 
	 * @return java.lang.String
	 */
	public String getDocBasePrefix() {
		return "";
	}

	/**
	 * Copies all files from the given directory in the workbench to the given location. Can be overridden by version specific class to modify or enhance what
	 * publish does.
	 * 
	 * @param jettyDir
	 *            Destination Jetty directory. Equivalent to catalina.base for Jetty 4.x and up.
	 * @param doBackup
	 *            Backup existing configuration files (true if not test mode).
	 * @param monitor
	 *            Progress monitor to use
	 * @return result of operation
	 */
	public IStatus backupAndPublish(final IPath jettyDir, final boolean doBackup, IProgressMonitor monitor) {
		final MultiStatus ms = new MultiStatus(JettyPlugin.PLUGIN_ID, 0, Messages.publishConfigurationTask, null);

		if (Trace.isTraceEnabled()) {
			Trace.trace(Trace.FINEST, "Backup and publish");
		}

		monitor = ProgressUtil.getMonitorFor(monitor);

		try {
			IPath backup = null;
			if (doBackup) {
				// create backup directory
				backup = jettyDir.append("backup");
				if (!backup.toFile().exists()) {
					backup.toFile().mkdir();
				}
			}
			backupFolder(getFolder(), jettyDir, backup, ms, monitor);
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "backupAndPublish() error", e);
			final IStatus s = new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0,
			    NLS.bind(Messages.errorPublishConfiguration, new String[] { e.getLocalizedMessage() }), e);
			ms.add(s);
		}

		monitor.done();
		return ms;
	}

	protected void backupFolder(final IFolder folder, final IPath confDir, final IPath backup, final MultiStatus ms, final IProgressMonitor monitor)
	    throws CoreException {
		final IResource[] children = folder.members();
		if (children == null) {
			return;
		}

		IResource resource = null;
		final int size = children.length;
		monitor.beginTask(Messages.publishConfigurationTask, size * 100);
		for (int i = 0; i < size; i++) {
			resource = children[i];
			switch (resource.getType()) {
				case IResource.FILE:
					try {
						final IFile file = (IFile) resource;
						final String name = file.getName();
						monitor.subTask(NLS.bind(Messages.publisherPublishTask, new String[] { name }));
						if (Trace.isTraceEnabled()) {
							Trace.trace(Trace.FINEST, "Publishing " + name);
						}

						// backup and copy file
						boolean copy = true;
						if (backup != null && !(backup.append(name).toFile().exists())) {
							final IStatus status = FileUtil.copyFile(confDir.append(name).toOSString(), backup + File.separator + name);
							ms.add(status);
							if (!status.isOK()) {
								copy = false;
							}
						}

						if (copy) {
							final String destPath = confDir.append(name).toOSString();
							String destContents = null;
							String srcContents = null;
							final File dest = new File(destPath);
							if (dest.exists()) {
								InputStream fis = new FileInputStream(destPath);
								destContents = FileUtil.getFileContents(fis);
								if (destContents != null) {
									fis = file.getContents();
									srcContents = FileUtil.getFileContents(fis);
								}
							}
							if (destContents == null || srcContents == null || !srcContents.equals(destContents)) {
								final InputStream in = file.getContents();
								ms.add(FileUtil.copyFile(in, destPath));
							}
						}
					} catch (final Exception e) {
						Trace.trace(Trace.SEVERE, "backupAndPublish() error", e);
						ms.add(new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0,
						    NLS.bind(Messages.errorPublishConfiguration, new String[] { e.getLocalizedMessage() }), e));
					}
					break;
				case IResource.FOLDER:
					final IFolder childFolder = (IFolder) resource;
					backupFolder(childFolder, confDir.append(childFolder.getName()), (backup != null ? backup.append(childFolder.getName()) : null), ms,
					    monitor);
					break;
			}
			monitor.worked(100);
		}
	}

	protected void backupPath(final IPath path, final IPath confDir, final IPath backup, final MultiStatus ms, final IProgressMonitor monitor) {
		final File[] files = path.toFile().listFiles();
		if (files == null) {
			return;
		}

		final int size = files.length;
		monitor.beginTask(Messages.publishConfigurationTask, size * 100);
		for (int i = 0; i < size; i++) {
			try {
				final File file = files[i];
				final String name = file.getName();
				monitor.subTask(NLS.bind(Messages.publisherPublishTask, new String[] { name }));
				if (Trace.isTraceEnabled()) {
					Trace.trace(Trace.FINEST, "Publishing " + name);
				}

				// backup and copy file
				boolean copy = true;
				if (backup != null && !(backup.append(name).toFile().exists())) {
					final IStatus status = FileUtil.copyFile(confDir.append(name).toOSString(), backup + File.separator + name);
					ms.add(status);
					if (!status.isOK()) {
						copy = false;
					}
				}

				if (copy) {
					ms.add(FileUtil.copyFile(file.getAbsolutePath(), confDir.append(name).toOSString()));
				}
			} catch (final Exception e) {
				Trace.trace(Trace.SEVERE, "backupAndPublish() error", e);
				ms.add(new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0,
				    NLS.bind(Messages.errorPublishConfiguration, new String[] { e.getLocalizedMessage() }), e));
			}
			monitor.worked(100);
		}
	}

	public IStatus cleanupServer(final IPath confDir, final IPath installDir, final IProgressMonitor monitor) {
		// Default implementation assumes nothing to clean
		return Status.OK_STATUS;
	}

	public IStatus localizeConfiguration(
	    final IPath confDir, final IPath serverDeployDirectory, final IJettyServer jettyServer, final IProgressMonitor subMonitorFor) {
		return Status.OK_STATUS;
	}
}
