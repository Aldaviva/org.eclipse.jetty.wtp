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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Connector {

	private Element portElement;
	
	public Connector(Element portElement) {
		this.portElement = portElement;
	}
	
	public String getPort() {
		Node firstChild = portElement.getFirstChild();
		if (firstChild.getNodeType() == Node.ELEMENT_NODE) {
			// SystemProperty default=""
			return ((Element)firstChild).getAttribute("default");
			
		}
		else {
			return portElement.getTextContent();
		}
	}
	
	public void setPort(String port) {		
		portElement.setTextContent(port);		
	}

}
