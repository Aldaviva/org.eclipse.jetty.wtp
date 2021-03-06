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
package org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server;

import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.server.jetty.core.internal.xml.XMLElement;

public class WebApp extends XMLElement {

	private File _file;
	private IPath _path;

	public File getFile() {
		return _file;
	}

	public void setFile(final File file) {
		this._file = file;
	}

	public IPath getPath() {
		return _path;
	}

	public void setPath(final IPath path) {
		this._path = path;
	}

	@Override
	public String toString() {
		return "WebApp [_file=" + _file + "]";
	}

}
