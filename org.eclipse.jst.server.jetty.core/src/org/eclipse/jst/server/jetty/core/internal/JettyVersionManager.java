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

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jst.server.jetty.core.IJettyConfiguration;
import org.eclipse.jst.server.jetty.core.internal.jetty70.Jetty70Provider;
import org.eclipse.jst.server.jetty.core.internal.jetty80.Jetty80Provider;

public class JettyVersionManager {

	public static final JettyVersionManager INSTANCE = new JettyVersionManager();

	private Map<String, IJettyVersionProvider> versionProviders = new HashMap<String, IJettyVersionProvider>();

	private List<String> runtimeTypes = new ArrayList<String>();

	public enum JettyVersion {
		V70, V80
	}

	private JettyVersionManager() {
		// Jetty 7.0
		register(JettyVersion.V70, Jetty70Provider.INSTANCE);
		// Jetty 8.0, same than Jetty7.0
		register(JettyVersion.V80, Jetty80Provider.INSTANCE);
	}

	public void register(JettyVersion version,
			IJettyVersionProvider versionProvider) {
		versionProviders.put(version.name(), versionProvider);

		String versionNumber = version.name().substring(1,
				version.name().length());
		runtimeTypes.add("org.eclipse.jst.server.jetty.runtime."
				+ versionNumber);
	}

	public IJettyVersionHandler getJettyVersionHandler(String id) {
		String version = getVersion(id);
		IJettyVersionProvider versionProvider = versionProviders.get(version);
		if (versionProvider == null) {
			throw new JettyVersionHandlerNotFoundException(version);
		}
		return versionProvider.getJettyVersionHandler();
	}

	public IJettyConfiguration getJettyConfiguration(String id, IFolder path) {
		String version = getVersion(id);
		IJettyVersionProvider versionProvider = versionProviders.get(version);
		if (versionProvider == null) {
			throw new JettyVersionHandlerNotFoundException(version);
		}
		return versionProvider.createJettyConfiguration(path);
	}

	private String getVersion(String id) {
		String version = id;
		int index = version.lastIndexOf('.');
		if (index != -1) {
			version = version.substring(index + 1, version.length());
		}
		if (!version.startsWith("v")) {
			version = "v" + version;
		}
		version = version.toUpperCase();
		return version;
	}

	private static class JettyVersionHandlerNotFoundException extends
			RuntimeException {
		private static final long serialVersionUID = 1L;

		private static final String MESSAGE = "Version Handler not founded with serverType={0}.";

		public JettyVersionHandlerNotFoundException(String serverType) {
			super(format(MESSAGE, serverType));
		}
	}

	public Collection<String> getRuntimeTypes() {
		return runtimeTypes;
	}

}
