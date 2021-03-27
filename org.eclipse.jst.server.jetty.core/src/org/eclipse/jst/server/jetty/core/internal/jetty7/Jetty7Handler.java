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

import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jst.server.jetty.core.JettyPlugin;
import org.eclipse.jst.server.jetty.core.internal.IJettyVersionHandler;
import org.eclipse.jst.server.jetty.core.internal.JettyHandler;
import org.eclipse.jst.server.jetty.core.internal.JettyServer;
import org.eclipse.jst.server.jetty.core.internal.Messages;
import org.eclipse.jst.server.jetty.core.internal.util.JettyVersionHelper;
import org.eclipse.wst.server.core.IModule;

public class Jetty7Handler extends JettyHandler {

	protected static final IStatus __START_JAR_REQUIRED_STATUS = new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0,
	    Messages.startJarRequiredInstallDirStatus, null);

	public IStatus verifyInstallPath(final IPath installPath) {
		final IStatus result = JettyVersionHelper.checkJettyVersion(installPath);

		if (result.getSeverity() == IStatus.CANCEL) {
			return __START_JAR_REQUIRED_STATUS;
		}

		return result;
	}

	public IStatus validate(final IPath path, final IVMInstall vmInstall) {
		// validate JVM
		return null;
	}

	/**
	 * @see IJettyVersionHandler#canAddModule(IModule)
	 */
	public IStatus canAddModule(final IModule module) {
		final String version = module.getModuleType().getVersion();

		if ("2.2".equals(version) ||
		    "2.3".equals(version) ||
		    "2.4".equals(version) ||
		    "2.5".equals(version)) {
			return Status.OK_STATUS;
		}

		return new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0, Messages.errorSpec70, null);
	}

	public IPath getRuntimeBaseDirectory(final JettyServer server) {
		return JettyVersionHelper.getStandardBaseDirectory(server);
	}

	/**
	 * @see IJettyVersionHandler#getRuntimeVMArguments(IPath, IPath, IPath, boolean)
	 */
	public List<String> getRuntimeVMArguments(
	    final IPath installPath, final IPath configPath, final IPath deployPath, final int mainPort, final int adminPort, final boolean isTestEnv) {
		final List<String> vmArguments = getCommonRuntimeVMArguments(installPath, configPath, deployPath, mainPort, adminPort, isTestEnv);

		if (isTestEnv) {
			vmArguments.add("-Djetty.home=\"" + configPath.toOSString() + "\"");
			vmArguments.add("-Dinstall.jetty.home=\"" + installPath.toOSString() + "\"");
			vmArguments.add("-DSTART=\"" + configPath.toOSString() + "/start.config\"");
		} else {
			vmArguments.add("-Djetty.home=\"" + installPath.toOSString() + "\"");
		}

		return vmArguments;
	}

	public String getEndorsedDirectories(final IPath installPath) {
		return installPath.append("endorsed").toOSString();
	}

	public String getRuntimePolicyFile(final IPath configPath) {
		return configPath.append("lib").append("policy").append("jetty.policy").toOSString();
	}

	public String[] getRuntimeProgramArguments(final IPath configPath, final boolean debug, final boolean starting) {
		return JettyVersionHelper.getJettyProgramArguments(configPath, debug, starting);
	}

	public String[] getExcludedRuntimeProgramArguments(final boolean debug, final boolean starting) {
		return null;
	}

	public boolean supportsServeModulesWithoutPublish() {
		return true;
	}
}
