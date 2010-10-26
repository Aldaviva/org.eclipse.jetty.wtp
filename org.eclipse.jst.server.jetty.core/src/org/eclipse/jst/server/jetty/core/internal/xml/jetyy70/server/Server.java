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
package org.eclipse.jst.server.jetty.core.internal.xml.jetyy70.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.server.jetty.core.internal.xml.XMLElement;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Server extends XMLElement {

	private File file;
	private IPath path;
	
	public List<Connector> getConnectors() {
		List<Connector> connectors = null;
		NodeList callNodes = getElementNode().getElementsByTagName("Call");
		int length = callNodes.getLength();
		Element node = null;
		for (int i = 0; i < length; i++) {
			node = (Element)callNodes.item(i);
			if (hasAttribute(node, "addConnector")) {
				Element portElement = super.findElement(node, "Set", "port");
				if (portElement != null) {
					Connector connector = new Connector(portElement);
					if (connectors == null) {
						connectors = new ArrayList<Connector>();
					}
					connectors.add(connector);					
				}
			}
		}
		return connectors;
	}

	public void setFile(File jettyXMLFile) {
		this.file = jettyXMLFile;		
	}
	
	public File getFile() {
		return file;
	}

	public IPath getPath() {
		return path;
	}

	public void setPath(IPath path) {
		this.path = path;
	}

	
	
}
