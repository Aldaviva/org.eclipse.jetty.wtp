/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - Initial API and implementation
 *    Angelo Zerr <angelo.zerr@gmail.com> - Jetty packages
 *******************************************************************************/
package org.eclipse.jst.server.jetty.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.internal.ProgressUtil;
import org.eclipse.jst.server.jetty.core.IJettyConfiguration;
import org.eclipse.jst.server.jetty.core.JettyPlugin;
import org.eclipse.jst.server.jetty.core.WebModule;
import org.eclipse.jst.server.jetty.core.internal.util.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.internal.IModulePublishHelper;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;
import org.eclipse.wst.server.core.util.SocketUtil;

public class JettyServerBehaviour extends ServerBehaviourDelegate implements IJettyServerBehaviour, IModulePublishHelper {

	private static final String __ATTR_STOP = "stop-server";

	private static final String[] __JMX_EXCLUDE_ARGS = new String[] {
	    "-Dcom.sun.management.jmxremote",
	    "-Dcom.sun.management.jmxremote.port=",
	    "-Dcom.sun.management.jmxremote.ssl=",
	    "-Dcom.sun.management.jmxremote.authenticate=" };

	// the thread used to ping the server to check for startup
	protected transient PingThread _ping = null;
	protected transient IDebugEventSetListener _processListener;

	/**
	 * JettyServerBehaviour.
	 */
	public JettyServerBehaviour() {
		super();
	}

	@Override
	public void initialize(final IProgressMonitor monitor) {
		// do nothing
	}

	public JettyRuntime getJettyRuntime() {
		if (getServer().getRuntime() == null) {
			return null;
		}

		return (JettyRuntime) getServer().getRuntime().loadAdapter(JettyRuntime.class, null);
	}

	public IJettyVersionHandler getJettyVersionHandler() {
		return getJettyServer().getJettyVersionHandler();
	}

	public IJettyConfiguration getJettyConfiguration() throws CoreException {
		return getJettyServer().getJettyConfiguration();
	}

	public JettyServer getJettyServer() {
		return (JettyServer) getServer().loadAdapter(JettyServer.class, null);
	}

	/**
	 * Return the runtime class name.
	 * 
	 * @return the class name
	 */
	public String getRuntimeClass() {
		return getJettyVersionHandler().getRuntimeClass();
	}

	/**
	 * Returns the runtime base path for relative paths in the server configuration.
	 * 
	 * @return the base path
	 */
	public IPath getRuntimeBaseDirectory() {
		return getJettyServer().getRuntimeBaseDirectory();
	}

	/**
	 * Return the program's runtime arguments to start or stop.
	 * 
	 * @param starting
	 *            true if starting
	 * @return an array of runtime program arguments
	 */
	protected String[] getRuntimeProgramArguments(final boolean starting) {
		IPath configPath = null;
		if (getJettyServer().isTestEnvironment()) {
			configPath = getRuntimeBaseDirectory();
		}
		return getJettyVersionHandler().getRuntimeProgramArguments(configPath, getJettyServer().isDebug(), starting);
	}

	protected String[] getExcludedRuntimeProgramArguments(final boolean starting) {
		return getJettyVersionHandler().getExcludedRuntimeProgramArguments(getJettyServer().isDebug(), starting);
	}

	/**
	 * Return the runtime (VM) arguments.
	 * 
	 * @return an array of runtime arguments
	 */
	protected List<String> getRuntimeVMArguments() {
		IPath installPath = getServer().getRuntime().getLocation();
		// If installPath is relative, convert to canonical path and hope for
		// the best
		if (!installPath.isAbsolute()) {
			try {
				final String installLoc = (new File(installPath.toOSString())).getCanonicalPath();
				installPath = new Path(installLoc);
			} catch (final IOException e) {
				// Ignore if there is a problem
			}
		}
		final IPath configPath = getRuntimeBaseDirectory();
		IPath deployPath;
		// If serving modules without publishing, use workspace path as the
		// deploy path
		if (getJettyServer().isServeModulesWithoutPublish()) {
			deployPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		}
		// Else normal publishing for modules
		else {
			deployPath = getServerDeployDirectory();
			// If deployPath is relative, convert to canonical path and hope for
			// the best
			if (!deployPath.isAbsolute()) {
				try {
					final String deployLoc = (new File(deployPath.toOSString())).getCanonicalPath();
					deployPath = new Path(deployLoc);
				} catch (final IOException e) {
					// Ignore if there is a problem
				}
			}
		}

		int mainPort = 8080;
		int adminPort = 8082;
		try {
			final IJettyConfiguration config = getJettyConfiguration();
			mainPort = config.getMainPort().getPort(); //FIXME either getJettyConfiguration() or getMainPort() returns null, probably because Jetty 9 does not have a start.config file from which to parse the port numbers. New, Jetty 9 specific behavior should probably be added to Jetty9Configuration
			adminPort = config.getAdminPort().getPort();
		} catch (final CoreException ex) {
			// ignore exception and use the defaults;
		}

		return getJettyVersionHandler().getRuntimeVMArguments(installPath, configPath, deployPath, mainPort, adminPort, getJettyServer().isTestEnvironment());
	}

	protected String getRuntimePolicyFile() {
		final IPath configPath = getRuntimeBaseDirectory();
		return getJettyVersionHandler().getRuntimePolicyFile(configPath);
	}

	protected static String renderCommandLine(final String[] commandLine, final String separator) {
		if (commandLine == null || commandLine.length < 1) {
			return "";
		}

		final StringBuffer buf = new StringBuffer(commandLine[0]);

		for (int i = 1; i < commandLine.length; i++) {
			buf.append(separator);
			buf.append(commandLine[i]);
		}

		return buf.toString();
	}

	/**
	 * Setup for starting the server.
	 * 
	 * @param launch
	 *            ILaunch
	 * @param launchMode
	 *            String
	 * @param monitor
	 *            IProgressMonitor
	 * @throws CoreException
	 *             if anything goes wrong
	 */
	public void setupLaunch(final ILaunch launch, final String launchMode, final IProgressMonitor monitor) throws CoreException {
		if (StringUtils.isTrue(launch.getLaunchConfiguration().getAttribute(__ATTR_STOP, StringUtils.FALSE))) {
			return;
		}
		// if (getJettyRuntime() == null)
		// throw new CoreException();

		final IStatus status = getJettyRuntime().validate();

		if (status != null && status.getSeverity() == IStatus.ERROR) {
			throw new CoreException(status);
		}

		// setRestartNeeded(false);
		final IJettyConfiguration configuration = getJettyConfiguration();

		// check that ports are free
		Iterator<ServerPort> iterator = configuration.getServerPorts().iterator();
		final List<ServerPort> usedPorts = new ArrayList<ServerPort>();
		while (iterator.hasNext()) {
			final ServerPort sp = iterator.next();
			if (sp.getPort() < 0) {
				throw new CoreException(new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0, Messages.errorPortInvalid, null));
			}
			if (SocketUtil.isPortInUse(sp.getPort(), 5)) {
				usedPorts.add(sp);
			}
		}
		if (usedPorts.size() == 1) {
			final ServerPort port = usedPorts.get(0);
			throw new CoreException(new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0,
			    NLS.bind(Messages.errorPortInUse, new String[] { port.getPort() + "", getServer().getName() }), null));
		} else if (usedPorts.size() > 1) {
			String portStr = "";
			iterator = usedPorts.iterator();
			boolean first = true;
			while (iterator.hasNext()) {
				if (!first) {
					portStr += ", ";
				}
				first = false;
				final ServerPort sp = iterator.next();
				portStr += "" + sp.getPort();
			}
			throw new CoreException(
			    new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorPortsInUse, new String[] { portStr, getServer().getName() }), null));
		}

		// check that there is only one app for each context root
		final Iterator<WebModule> wmIterator = configuration.getWebModules().iterator();
		final List<String> contextRoots = new ArrayList<String>();
		while (iterator.hasNext()) {
			final WebModule module = wmIterator.next();
			final String contextRoot = module.getPath();
			if (contextRoots.contains(contextRoot)) {
				throw new CoreException(
				    new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorDuplicateContextRoot, new String[] { contextRoot }), null));
			}

			contextRoots.add(contextRoot);
		}

		setServerRestartState(false);
		setServerState(IServer.STATE_STARTING);
		setMode(launchMode);

		// ping server to check for startup
		try {
			String url = "http://" + getServer().getHost();
			final int port = configuration.getMainPort().getPort();
			if (port != 80) {
				url += ":" + port;
			}
			_ping = new PingThread(getServer(), url, -1, this);
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Can't ping for Jetty startup.");
		}
	}

	/**
	 * Cleanly shuts down and terminates the server.
	 * 
	 * @param force
	 *            <code>true</code> to kill the server
	 */
	@Override
	public void stop(final boolean force) {
		if (force) {
			terminate();
			return;
		}
		final int state = getServer().getServerState();
		// If stopped or stopping, no need to run stop command again
		if (state == IServer.STATE_STOPPED || state == IServer.STATE_STOPPING) {
			return;
		} else if (state == IServer.STATE_STARTING) {
			terminate();
			return;
		}

		try {
			if (Trace.isTraceEnabled()) {
				Trace.trace(Trace.FINEST, "Stopping Jetty");
			}
			if (state != IServer.STATE_STOPPED) {
				setServerState(IServer.STATE_STOPPING);
			}

			final ILaunchConfiguration launchConfig = ((Server) getServer()).getLaunchConfiguration(true, null);
			final ILaunchConfigurationWorkingCopy wc = launchConfig.getWorkingCopy();

			final String args = renderCommandLine(getRuntimeProgramArguments(false), " ");
			// Remove JMX arguments if present
			final String existingVMArgs = wc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
			if (existingVMArgs.indexOf(__JMX_EXCLUDE_ARGS[0]) >= 0) {
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
				    mergeArguments(existingVMArgs, new String[] {}, __JMX_EXCLUDE_ARGS, false));
			}
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, args);
			wc.setAttribute("org.eclipse.debug.ui.private", true);
			wc.setAttribute(__ATTR_STOP, "true");
			wc.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error stopping Jetty", e);
		}
	}

	/**
	 * Terminates the server.
	 */
	protected void terminate() {
		if (getServer().getServerState() == IServer.STATE_STOPPED) {
			return;
		}

		try {
			setServerState(IServer.STATE_STOPPING);
			if (Trace.isTraceEnabled()) {
				Trace.trace(Trace.FINEST, "Killing the Jetty process");
			}
			final ILaunch launch = getServer().getLaunch();
			if (launch != null) {
				launch.terminate();
				stopImpl();
			}
		} catch (final Exception e) {
			Trace.trace(Trace.SEVERE, "Error killing the process", e);
		}
	}

	@Override
	public IPath getTempDirectory() {
		return super.getTempDirectory(false);
	}

	protected static int getNextToken(final String s, final int start) {
		int i = start;
		final int length = s.length();
		char lookFor = ' ';

		while (i < length) {
			final char c = s.charAt(i);
			if (lookFor == c) {
				if (lookFor == '"') {
					return i + 1;
				}
				return i;
			}
			if (c == '"') {
				lookFor = '"';
			}
			i++;
		}
		return -1;
	}

	/**
	 * Merge the given arguments into the original argument string, replacing invalid values if they have been changed. Special handling is provided if the
	 * keepActionLast argument is true and the last vmArg is a simple string. The vmArgs will be merged such that the last vmArg is guaranteed to be the last
	 * argument in the merged string.
	 * 
	 * @param originalArg
	 *            String of original arguments.
	 * @param vmArgs
	 *            Arguments to merge into the original arguments string
	 * @param excludeArgs
	 *            Arguments to exclude from the original arguments string
	 * @param keepActionLast
	 *            If <b>true</b> the vmArguments are assumed to be Jetty program arguments, the last of which is the action to perform which must remain the
	 *            last argument. This only has an impact if the last vmArg is a simple string argument, like &quot;start&quot;.
	 * @return merged argument string
	 */
	public static String mergeArguments(String originalArg, final String[] vmArgs, final String[] excludeArgs, final boolean keepActionLast) {
		if (vmArgs == null) {
			return originalArg;
		}

		if (originalArg == null) {
			originalArg = "";
		}

		// replace and null out all vmargs that already exist
		final int size = vmArgs.length;
		for (int i = 0; i < size; i++) {
			final int ind = vmArgs[i].indexOf(" ");
			final int ind2 = vmArgs[i].indexOf("=");
			if (ind >= 0 && (ind2 == -1 || ind < ind2)) { // -a bc style
				final int index = originalArg.indexOf(vmArgs[i].substring(0, ind + 1));
				if (index == 0 || (index > 0 && Character.isWhitespace(originalArg.charAt(index - 1)))) {
					// replace
					final String s = originalArg.substring(0, index);
					final int index2 = getNextToken(originalArg, index + ind + 1);
					if (index2 >= 0) {
						originalArg = s + vmArgs[i] + originalArg.substring(index2);
					} else {
						originalArg = s + vmArgs[i];
					}
					vmArgs[i] = null;
				}
			} else if (ind2 >= 0) { // a=b style
				final int index = originalArg.indexOf(vmArgs[i].substring(0, ind2 + 1));
				if (index == 0 || (index > 0 && Character.isWhitespace(originalArg.charAt(index - 1)))) {
					// replace
					final String s = originalArg.substring(0, index);
					final int index2 = getNextToken(originalArg, index);
					if (index2 >= 0) {
						originalArg = s + vmArgs[i] + originalArg.substring(index2);
					} else {
						originalArg = s + vmArgs[i];
					}
					vmArgs[i] = null;
				}
			} else { // abc style
				final int index = originalArg.indexOf(vmArgs[i]);
				if (index == 0 || (index > 0 && Character.isWhitespace(originalArg.charAt(index - 1)))) {
					// replace
					final String s = originalArg.substring(0, index);
					final int index2 = getNextToken(originalArg, index);
					if (!keepActionLast || i < (size - 1)) {
						if (index2 >= 0) {
							originalArg = s + vmArgs[i] + originalArg.substring(index2);
						} else {
							originalArg = s + vmArgs[i];
						}
						vmArgs[i] = null;
					} else {
						// The last VM argument needs to remain last,
						// remove original arg and append the vmArg later
						if (index2 >= 0) {
							originalArg = s + originalArg.substring(index2);
						} else {
							originalArg = s;
						}
					}
				}
			}
		}

		// remove excluded arguments
		if (excludeArgs != null && excludeArgs.length > 0) {
			for (int i = 0; i < excludeArgs.length; i++) {
				final int ind = excludeArgs[i].indexOf(" ");
				final int ind2 = excludeArgs[i].indexOf("=");
				if (ind >= 0 && (ind2 == -1 || ind < ind2)) { // -a bc style
					final int index = originalArg.indexOf(excludeArgs[i].substring(0, ind + 1));
					if (index == 0 || (index > 0 && Character.isWhitespace(originalArg.charAt(index - 1)))) {
						// remove
						final String s = originalArg.substring(0, index);
						int index2 = getNextToken(originalArg, index + ind + 1);
						if (index2 >= 0) {
							// If remainder will become the first argument,
							// remove leading blanks
							while (index2 < originalArg.length() && Character.isWhitespace(originalArg.charAt(index2))) {
								index2 += 1;
							}
							originalArg = s + originalArg.substring(index2);
						} else {
							originalArg = s;
						}
					}
				} else if (ind2 >= 0) { // a=b style
					final int index = originalArg.indexOf(excludeArgs[i].substring(0, ind2 + 1));
					if (index == 0 || (index > 0 && Character.isWhitespace(originalArg.charAt(index - 1)))) {
						// remove
						final String s = originalArg.substring(0, index);
						int index2 = getNextToken(originalArg, index);
						if (index2 >= 0) {
							// If remainder will become the first argument,
							// remove leading blanks
							while (index2 < originalArg.length() && Character.isWhitespace(originalArg.charAt(index2))) {
								index2 += 1;
							}
							originalArg = s + originalArg.substring(index2);
						} else {
							originalArg = s;
						}
					}
				} else { // abc style
					final int index = originalArg.indexOf(excludeArgs[i]);
					if (index == 0 || (index > 0 && Character.isWhitespace(originalArg.charAt(index - 1)))) {
						// remove
						final String s = originalArg.substring(0, index);
						int index2 = getNextToken(originalArg, index);
						if (index2 >= 0) {
							// Remove leading blanks
							while (index2 < originalArg.length() && Character.isWhitespace(originalArg.charAt(index2))) {
								index2 += 1;
							}
							originalArg = s + originalArg.substring(index2);
						} else {
							originalArg = s;
						}
					}
				}
			}
		}

		// add remaining vmargs to the end
		for (int i = 0; i < size; i++) {
			if (vmArgs[i] != null) {
				if (originalArg.length() > 0 && !originalArg.endsWith(" ")) {
					originalArg += " ";
				}
				originalArg += vmArgs[i];
			}
		}

		return originalArg;
	}

	/**
	 * Replace the current JRE container classpath with the given entry.
	 * 
	 * @param cp
	 * @param entry
	 */
	public static void replaceJREContainer(final List<IRuntimeClasspathEntry> cp, final IRuntimeClasspathEntry entry) {
		final int size = cp.size();

		for (int i = 0; i < size; i++) {
			final IRuntimeClasspathEntry entry2 = cp.get(i);

			if (entry2.getPath().uptoSegment(2).isPrefixOf(entry.getPath())) {
				cp.set(i, entry);
				return;
			}
		}

		cp.add(0, entry);
	}

	/**
	 * Merge a single classpath entry into the classpath list.
	 * 
	 * @param cp
	 * @param entry
	 */
	public static void mergeClasspath(final List<IRuntimeClasspathEntry> cp, final IRuntimeClasspathEntry entry) {
		final Iterator<IRuntimeClasspathEntry> iterator = cp.iterator();
		while (iterator.hasNext()) {
			final IRuntimeClasspathEntry entry2 = iterator.next();

			if (entry2.getPath().equals(entry.getPath())) {
				return;
			}
		}

		cp.add(entry);
	}

	@Override
	public void setupLaunchConfiguration(final ILaunchConfigurationWorkingCopy workingCopy, final IProgressMonitor monitor) throws CoreException {
		try {
			Trace.trace(Trace.FINEST, "Setting up launch configuration");
			final String existingProgArgs = workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String) null);
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
			    mergeArguments(existingProgArgs, getRuntimeProgramArguments(true), getExcludedRuntimeProgramArguments(true), true));

			String existingVMArgs = workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
			String[] parsedVMArgs = null;
			if (null != existingVMArgs) {
				parsedVMArgs = DebugPlugin.parseArguments(existingVMArgs);
			}
			final List<String> configVMArgs = getRuntimeVMArguments();
			if (getJettyServer().isSecure()) {
				boolean addSecurityArgs = true;
				if (null != parsedVMArgs) {
					for (int i = 0; i < parsedVMArgs.length; i++) {
						if (parsedVMArgs[i].startsWith("wtp.configured.security")) {
							addSecurityArgs = false;
							break;
						}
					}
				}
				if (addSecurityArgs) {
					configVMArgs.add("-Djava.security.manager");
					configVMArgs.add("-Djava.security.policy=\"" + getRuntimePolicyFile() + "\"");
					configVMArgs.add("-Dwtp.configured.security=true");
				}
			} else if (null != parsedVMArgs) {
				boolean removeSecurityArgs = false;
				for (int i = 0; i < parsedVMArgs.length; i++) {
					if (parsedVMArgs[i].startsWith("-Dwtp.configured.security")) {
						removeSecurityArgs = true;
						break;
					}
				}
				if (removeSecurityArgs) {
					final StringBuffer filteredVMArgs = new StringBuffer();
					for (int i = 0; i < parsedVMArgs.length; i++) {
						final String arg = parsedVMArgs[i];
						if (!arg.startsWith("-Djava.security.manager") && !arg.startsWith("-Djava.security.policy=")
						    && !arg.startsWith("-Dwtp.configured.security=")) {
							if (filteredVMArgs.length() > 0) {
								filteredVMArgs.append(' ');
							}
							filteredVMArgs.append(arg);
						}
					}
					existingVMArgs = filteredVMArgs.toString();
				}
			}
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
			    mergeArguments(existingVMArgs, configVMArgs.toArray(new String[0]), null, false));

			final IJettyRuntime runtime = getJettyRuntime();
			final IVMInstall vmInstall = runtime.getVMInstall();
			if (vmInstall != null) {
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
				    JavaRuntime.newJREContainerPath(vmInstall).toPortableString());
			}

			// update classpath
			final IRuntimeClasspathEntry[] originalClasspath = JavaRuntime.computeUnresolvedRuntimeClasspath(workingCopy);
			final int size = originalClasspath.length;
			final List<IRuntimeClasspathEntry> oldCp = new ArrayList<IRuntimeClasspathEntry>(originalClasspath.length + 2);

			for (int i = 0; i < size; i++) {
				oldCp.add(originalClasspath[i]);
			}

			final Collection<IRuntimeClasspathEntry> cp2 = runtime.getRuntimeClasspath(getRuntimeBaseDirectory());
			Iterator<IRuntimeClasspathEntry> iterator = cp2.iterator();

			while (iterator.hasNext()) {
				final IRuntimeClasspathEntry entry = iterator.next();
				mergeClasspath(oldCp, entry);
			}

			if (vmInstall != null) {
				try {
					final String typeId = vmInstall.getVMInstallType().getId();

					replaceJREContainer(oldCp, JavaRuntime.newRuntimeContainerClasspathEntry(
					    new Path(JavaRuntime.JRE_CONTAINER).append(typeId).append(vmInstall.getName()), IRuntimeClasspathEntry.BOOTSTRAP_CLASSES));

				} catch (final Exception e) {
					JettyPlugin.log(e);
				}

				final IPath jrePath = new Path(vmInstall.getInstallLocation().getAbsolutePath());
				if (jrePath != null) {
					final IPath toolsPath = jrePath.append("lib").append("tools.jar");
					if (toolsPath.toFile().exists()) {
						final IRuntimeClasspathEntry toolsJar = JavaRuntime.newArchiveRuntimeClasspathEntry(toolsPath);
						// Search for index to any existing tools.jar entry
						int toolsIndex;

						for (toolsIndex = 0; toolsIndex < oldCp.size(); toolsIndex++) {
							final IRuntimeClasspathEntry entry = oldCp.get(toolsIndex);
							if (entry.getType() == IRuntimeClasspathEntry.ARCHIVE && entry.getPath().lastSegment().equals("tools.jar")) {
								break;
							}
						}
						// If existing tools.jar found, replace in case it's
						// different. Otherwise add.
						if (toolsIndex < oldCp.size()) {
							oldCp.set(toolsIndex, toolsJar);
						} else {
							mergeClasspath(oldCp, toolsJar);
						}
					}
				}
			}

			iterator = oldCp.iterator();
			final List<String> list = new ArrayList<String>();

			while (iterator.hasNext()) {
				final IRuntimeClasspathEntry entry = iterator.next();
				try {
					list.add(entry.getMemento());
				} catch (final Exception e) {
					Trace.trace(Trace.SEVERE, "Could not resolve classpath entry: " + entry, e);
				}
			}

			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, list);
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);

			Trace.trace(Trace.FINER, "VM Arguments: " + workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "(not set)"));
			Trace.trace(Trace.FINER, "JRE Container Path: " + workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, "(not set)"));
			final List<String> classpathEntries = workingCopy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, Collections.<String> emptyList());
			final StringBuilder classpathBuilder = new StringBuilder();
			for (final String classpathEntry : classpathEntries) {
				if (classpathBuilder.length() > 0) {
					classpathBuilder.append('\n');
				}
				classpathBuilder.append(classpathEntry);
			}
			Trace.trace(Trace.FINER, "Classpath: " + classpathBuilder.toString());
		} catch (final Throwable t) {
			Trace.trace(Trace.SEVERE, "Uncaught throwable in setupLaunchConfiguration: " + t.getMessage(), t);
		}
	}

	protected void addProcessListener(final IProcess newProcess) {
		if (_processListener != null || newProcess == null) {
			return;
		}

		_processListener = new IDebugEventSetListener() {
			public void handleDebugEvents(final DebugEvent[] events) {
				if (events != null) {
					final int size = events.length;
					for (int i = 0; i < size; i++) {
						if (newProcess != null && newProcess.equals(events[i].getSource()) && events[i].getKind() == DebugEvent.TERMINATE) {
							stopImpl();
						}
					}
				}
			}
		};

		DebugPlugin.getDefault().addDebugEventListener(_processListener);
	}

	protected void setServerStarted() {
		setServerState(IServer.STATE_STARTED);
	}

	protected void stopImpl() {
		if (_ping != null) {
			_ping.stop();
			_ping = null;
		}
		if (_processListener != null) {
			DebugPlugin.getDefault().removeDebugEventListener(_processListener);
			_processListener = null;
		}
		setServerState(IServer.STATE_STOPPED);
	}

	@Override
	protected void publishServer(final int kind, IProgressMonitor monitor) throws CoreException {
		if (getServer().getRuntime() == null) {
			return;
		}

		final IPath installDir = getServer().getRuntime().getLocation();
		IPath confDir = null;
		if (getJettyServer().isTestEnvironment()) {
			confDir = getRuntimeBaseDirectory();
			final IStatus status = getJettyVersionHandler().prepareRuntimeDirectory(confDir);
			if (status != null && !status.isOK()) {
				throw new CoreException(status);
			}
		} else {
			confDir = installDir;
			// IStatus status =
			// getJettyVersionHandler().prepareDeployDirectory(getServerDeployDirectory());
			// if (status != null && !status.isOK())
			// throw new CoreException(status);
		}

		monitor = ProgressUtil.getMonitorFor(monitor);
		monitor.beginTask(Messages.publishServerTask, 600);

		IStatus status = getJettyConfiguration().cleanupServer(confDir, installDir, ProgressUtil.getSubMonitorFor(monitor, 100));
		if (status != null && !status.isOK()) {
			throw new CoreException(status);
		}

		status = getJettyConfiguration().backupAndPublish(confDir, !getJettyServer().isTestEnvironment(), ProgressUtil.getSubMonitorFor(monitor, 400));
		if (status != null && !status.isOK()) {
			throw new CoreException(status);
		}

		status = getJettyConfiguration().localizeConfiguration(confDir, getServerDeployDirectory(), getJettyServer(),
		    ProgressUtil.getSubMonitorFor(monitor, 100));
		if (status != null && !status.isOK()) {
			throw new CoreException(status);
		}

		monitor.done();

		setServerPublishState(IServer.PUBLISH_STATE_NONE);
	}

	/*
	 * Publishes the given module to the server.
	 */
	@Override
	protected void publishModule(final int kind, final int deltaKind, final IModule[] moduleTree, final IProgressMonitor monitor) throws CoreException {
		if (getServer().getServerState() != IServer.STATE_STOPPED) {
			if (deltaKind == ServerBehaviourDelegate.ADDED || deltaKind == ServerBehaviourDelegate.REMOVED) {
				setServerRestartState(true);
			}
		}
		if (getJettyServer().isTestEnvironment()) {
			return;
		}

		final Properties p = loadModulePublishLocations();

		final PublishHelper helper = new PublishHelper(getRuntimeBaseDirectory().append("temp").toFile());
		// If parent web module
		if (moduleTree.length == 1) {
			publishDir(deltaKind, p, moduleTree, helper, monitor);
		}
		// Else a child module
		else {
			// Try to determine the URI for the child module
			final IWebModule webModule = (IWebModule) moduleTree[0].loadAdapter(IWebModule.class, monitor);
			String childURI = null;
			if (webModule != null) {
				childURI = webModule.getURI(moduleTree[1]);
			}
			// Try to determine if child is binary
			final IJ2EEModule childModule = (IJ2EEModule) moduleTree[1].loadAdapter(IJ2EEModule.class, monitor);
			boolean isBinary = false;
			if (childModule != null) {
				isBinary = childModule.isBinary();
			}

			if (isBinary) {
				publishArchiveModule(childURI, kind, deltaKind, p, moduleTree, helper, monitor);
			} else {
				publishJar(childURI, kind, deltaKind, p, moduleTree, helper, monitor);
			}
		}

		setModulePublishState(moduleTree, IServer.PUBLISH_STATE_NONE);

		saveModulePublishLocations(p);
	}

	/**
	 * Publish a web module.
	 * 
	 * @param deltaKind
	 * @param p
	 * @param module
	 * @param monitor
	 * @throws CoreException
	 */
	private void publishDir(final int deltaKind, final Properties p, final IModule module[], final PublishHelper helper, final IProgressMonitor monitor)
	    throws CoreException {
		final List<IStatus> status = new ArrayList<IStatus>();
		// Remove if requested or if previously published and are now serving
		// without publishing
		if (deltaKind == REMOVED || getJettyServer().isServeModulesWithoutPublish()) {
			final String publishPath = (String) p.get(module[0].getId());
			if (publishPath != null) {
				try {
					final File f = new File(publishPath);
					if (f.exists()) {
						final IStatus[] stat = PublishHelper.deleteDirectory(f, monitor);
						PublishOperation2.addArrayToList(status, stat);
					}
				} catch (final Exception e) {
					throw new CoreException(new Status(IStatus.WARNING, JettyPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorPublishCouldNotRemoveModule,
					    module[0].getName()), e));
				}
				p.remove(module[0].getId());
			}
		} else {
			final IPath path = getModuleDeployDirectory(module[0]);
			final IModuleResource[] mr = getResources(module);
			IPath[] jarPaths = null;
			final IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, monitor);
			final IModule[] childModules = getServer().getChildModules(module, monitor);
			if (childModules != null && childModules.length > 0) {
				jarPaths = new IPath[childModules.length];
				for (int i = 0; i < childModules.length; i++) {
					if (webModule != null) {
						jarPaths[i] = new Path(webModule.getURI(childModules[i]));
					} else {
						final IJ2EEModule childModule = (IJ2EEModule) childModules[i].loadAdapter(IJ2EEModule.class, monitor);
						if (childModule != null && childModule.isBinary()) {
							jarPaths[i] = new Path("WEB-INF/lib").append(childModules[i].getName());
						} else {
							jarPaths[i] = new Path("WEB-INF/lib").append(childModules[i].getName() + ".jar");
						}
					}
				}
			}
			final IStatus[] stat = helper.publishSmart(mr, path, jarPaths, monitor);
			PublishOperation2.addArrayToList(status, stat);
			p.put(module[0].getId(), path.toOSString());
		}
		PublishOperation2.throwException(status);
	}

	/**
	 * Publish a jar file.
	 * 
	 * @param deltaKind
	 * @param p
	 * @param module
	 * @param monitor
	 * @throws CoreException
	 */
	private void publishJar(
	    String jarURI, final int kind, final int deltaKind, final Properties p, final IModule[] module, final PublishHelper helper,
	    final IProgressMonitor monitor)
	    throws CoreException {
		// Remove if requested or if previously published and are now serving
		// without publishing
		if (deltaKind == REMOVED || getJettyServer().isServeModulesWithoutPublish()) {
			try {
				final String publishPath = (String) p.get(module[1].getId());
				if (publishPath != null) {
					new File(publishPath).delete();
					p.remove(module[1].getId());
				}
			} catch (final Exception e) {
				throw new CoreException(new Status(IStatus.WARNING, JettyPlugin.PLUGIN_ID, 0, "Could not remove module", e));
			}
		} else {
			IPath path = getModuleDeployDirectory(module[0]);
			if (jarURI == null) {
				jarURI = "WEB-INF/lib" + module[1].getName() + ".jar";
			}
			final IPath jarPath = path.append(jarURI);
			path = jarPath.removeLastSegments(1);
			if (!path.toFile().exists()) {
				path.toFile().mkdirs();
			} else {
				// If file still exists and we are not forcing a new one to be
				// built
				if (jarPath.toFile().exists() && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
					// avoid changes if no changes to module since last publish
					final IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
					if (delta == null || delta.length == 0) {
						return;
					}
				}
			}

			final IModuleResource[] mr = getResources(module);
			final IStatus[] stat = helper.publishZip(mr, jarPath, monitor);
			final List<IStatus> status = new ArrayList<IStatus>();
			PublishOperation2.addArrayToList(status, stat);
			PublishOperation2.throwException(status);
			p.put(module[1].getId(), jarPath.toOSString());
		}
	}

	private void publishArchiveModule(
	    String jarURI, final int kind, final int deltaKind, final Properties p, final IModule[] module, final PublishHelper helper,
	    final IProgressMonitor monitor)
	    throws CoreException {
		// Remove if requested or if previously published and are now serving
		// without publishing
		if (deltaKind == REMOVED || getJettyServer().isServeModulesWithoutPublish()) {
			try {
				final String publishPath = (String) p.get(module[1].getId());
				if (publishPath != null) {
					new File(publishPath).delete();
					p.remove(module[1].getId());
				}
			} catch (final Exception e) {
				throw new CoreException(new Status(IStatus.WARNING, JettyPlugin.PLUGIN_ID, 0, "Could not remove archive module", e));
			}
		} else {
			final List<IStatus> status = new ArrayList<IStatus>();
			IPath path = getModuleDeployDirectory(module[0]);
			if (jarURI == null) {
				jarURI = "WEB-INF/lib" + module[1].getName();
			}
			final IPath jarPath = path.append(jarURI);
			path = jarPath.removeLastSegments(1);
			if (!path.toFile().exists()) {
				path.toFile().mkdirs();
			} else {
				// If file still exists and we are not forcing a new one to be
				// built
				if (jarPath.toFile().exists() && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
					// avoid changes if no changes to module since last publish
					final IModuleResourceDelta[] delta = getPublishedResourceDelta(module);
					if (delta == null || delta.length == 0) {
						return;
					}
				}
			}

			final IModuleResource[] mr = getResources(module);
			final IStatus[] stat = helper.publishToPath(mr, jarPath, monitor);
			PublishOperation2.addArrayToList(status, stat);
			PublishOperation2.throwException(status);
			p.put(module[1].getId(), jarPath.toOSString());
		}
	}

	// @Override
	// protected void publishFinish(IProgressMonitor monitor) throws CoreException {
	// IStatus status;
	// IPath baseDir = getRuntimeBaseDirectory();
	// JettyServer ts = getJettyServer();
	// IJettyVersionHandler tvh = getJettyVersionHandler();
	// // Include or remove loader jar depending on state of serving directly
	// status = tvh.prepareForServingDirectly(baseDir, getJettyServer());
	// if (status.isOK()) {
	// // If serving modules directly, update server.xml accordingly
	// // (includes project context.xmls)
	// if (ts.isServeModulesWithoutPublish()) {
	// status = getJettyConfiguration().updateContextsToServeDirectly(
	// baseDir, tvh.getSharedLoader(baseDir), monitor);
	// }
	// // Else serving normally. Add project context.xmls to server.xml
	// else {
	// // Publish context configuration for servers that support
	// // META-INF/context.xml
	// status = getJettyConfiguration().publishContextConfig(baseDir,
	// getServerDeployDirectory(), monitor);
	// }
	// if (status.isOK() && ts.isSaveSeparateContextFiles()) {
	// // Determine if context's path attribute should be removed
	// String id = getServer().getServerType().getId();
	// boolean noPath = id.indexOf("55") > 0 || id.indexOf("60") > 0;
	// boolean serverStopped = getServer().getServerState() == IServer.STATE_STOPPED;
	// JettyVersionHelper.moveContextsToSeparateFiles(baseDir, noPath,
	// serverStopped, null);
	// }
	// }
	// if (!status.isOK())
	// throw new CoreException(status);
	// }

	/**
	 * Gets the directory to which modules should be deployed for this server.
	 * 
	 * @return full path to deployment directory for the server
	 */
	public IPath getServerDeployDirectory() {
		return getJettyServer().getServerDeployDirectory();
	}

	@Override
	protected IModuleResource[] getResources(final IModule[] module) {
		return super.getResources(module);
	}

	@Override
	protected IModuleResourceDelta[] getPublishedResourceDelta(final IModule[] module) {
		return super.getPublishedResourceDelta(module);
	}

	/**
	 * Gets the directory to which to deploy a module's web application.
	 * 
	 * @param module
	 *            a module
	 * @return full path to deployment directory for the module
	 */
	public IPath getModuleDeployDirectory(final IModule module) {
		return getServerDeployDirectory().append(module.getName());
	}

	/**
	 * Temporary method to help web services team. Returns the path that the module is published to.
	 * 
	 * @param module
	 *            a module on the server
	 * @return the path that the module is published to when in test environment mode, or null if the module is not a web module
	 */
	public IPath getPublishDirectory(final IModule[] module) {
		if (module == null || module.length != 1) {
			return null;
		}

		return getModuleDeployDirectory(module[0]);
	}

	public void setModulePublishState2(final IModule[] module, final int state) {
		setModulePublishState(module, state);
	}

	public Properties loadModulePublishLocations() {
		final Properties p = new Properties();
		final IPath path = getTempDirectory().append("publish.txt");
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(path.toFile());
			p.load(fin);
		} catch (final Exception e) {
			// ignore
		} finally {
			try {
				fin.close();
			} catch (final Exception ex) {
				// ignore
			}
		}
		return p;
	}

	public void saveModulePublishLocations(final Properties p) {
		final IPath path = getTempDirectory().append("publish.txt");
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(path.toFile());
			p.store(fout, "Jetty publish data");
		} catch (final Exception e) {
			// ignore
		} finally {
			try {
				fout.close();
			} catch (final Exception ex) {
				// ignore
			}
		}
	}

}
