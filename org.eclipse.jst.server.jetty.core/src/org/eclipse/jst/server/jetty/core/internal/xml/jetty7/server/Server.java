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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.server.jetty.core.internal.xml.XMLElement;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Server extends XMLElement {

	private File _file;
	private IPath _path;

	public List<Connector> getConnectors() {
		List<Connector> connectors = null;
		final NodeList callNodes = getElementNode().getElementsByTagName("Call");
		final int length = callNodes.getLength();
		Element node = null;
		for (int i = 0; i < length; i++) {
			node = (Element) callNodes.item(i);
			if (hasAttribute(node, "addConnector")) {
				final Element portElement = super.findElement(node, "Set", "port");
				final NodeList typeElements = node.getElementsByTagName("New");
				if (portElement != null) {
					final Connector connector = new Connector(portElement, (Element) typeElements.item(0));
					if (connectors == null) {
						connectors = new ArrayList<Connector>();
					}
					connectors.add(connector);
				}
			}
		}
		return connectors;
	}

	public void setFile(final File jettyXMLFile) {
		this._file = jettyXMLFile;
	}

	public File getFile() {
		return _file;
	}

	public IPath getPath() {
		return _path;
	}

	public void setPath(final IPath path) {
		this._path = path;
	}

	@Override
	public String toString() {
		return "Server [_file=" + _file + "]";
	}

}
