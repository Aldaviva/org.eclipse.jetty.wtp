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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;

public class JettyRuntimeClasspathProvider extends RuntimeClasspathProviderDelegate implements JettyConstants {
	private static final IClasspathEntry[] EMPTY_CLASSPATH_ENTRY = new IClasspathEntry[0];

	@Override
	public IClasspathEntry[] resolveClasspathContainer(final IProject project, final IRuntime runtime) {
		final IPath installPath = runtime.getLocation();
		if (installPath == null) {
			return EMPTY_CLASSPATH_ENTRY;
			// String runtimeId = runtime.getRuntimeType().getId();
		}

		final List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		final IPath libPath = installPath.append(LIB_FOLDER);
		final File libDir = libPath.toFile();
		if (libDir.exists()) {
			// add all jars from the Jetty ${jetty.home}/lib
			RuntimeClasspathProviderDelegate.addLibraryEntries(entries, libDir, true);
			// add all jars from the Jetty ${jetty.home}/lib/jsp directory
			final IPath jspLibPath = libPath.append(JSP_FOLDER);
			final File jspLibDir = jspLibPath.toFile();
			if (jspLibDir.exists()) {
				RuntimeClasspathProviderDelegate.addLibraryEntries(entries, jspLibDir, true);
			}

			//TODO also add the lib/annotations directory so servlet 3 annotation scanning works 
		}
		return entries.toArray(new IClasspathEntry[entries.size()]);
	}

}
