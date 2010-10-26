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

import org.eclipse.core.resources.IFolder;
import org.eclipse.jst.server.jetty.core.IJettyConfiguration;
import org.eclipse.jst.server.jetty.core.internal.IJettyVersionHandler;
import org.eclipse.jst.server.jetty.core.internal.IJettyVersionProvider;

public class Jetty70Provider implements IJettyVersionProvider {

	public static final IJettyVersionProvider INSTANCE = new Jetty70Provider();

	private IJettyVersionHandler versionHandler = new Jetty70Handler();
	
	public IJettyVersionHandler getJettyVersionHandler() {
		return versionHandler;
	}
	public IJettyConfiguration createJettyConfiguration(IFolder path) {
		return new Jetty70Configuration(path);
	}
}
