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
package org.eclipse.jst.server.jetty.core.internal.xml.jetty7.webapp;

import java.io.File;
import java.io.IOException;
import org.eclipse.jst.server.jetty.core.internal.xml.XMLElement;
import org.eclipse.jst.server.jetty.core.internal.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WebAppContext extends XMLElement {

	private File _saveFile;
	private String _memento;
	private String _documentBase;

	public void setContextPath(final String contextPath) {
		final Element element = super.findElement("Set", "contextPath");
		if (contextPath.startsWith("/")) {
			element.setTextContent(contextPath);
		} else {
			element.setTextContent("/" + contextPath);
		}

	}

	public void setWar(final String war, final boolean isExternal) {
		final Element element = super.findElement("Set", "war");
		element.setTextContent(war);
		if (!isExternal) {
			final Document document = element.getOwnerDocument();
			final Element systemProperty = document.createElement("SystemProperty");
			systemProperty.setAttribute("name", "jetty.home");
			systemProperty.setAttribute("default", ".");
			final Node firstChild = element.getFirstChild();
			element.insertBefore(systemProperty, firstChild);
		}
	}

	public String getContextPath() {
		final Element element = super.findElement("Set", "contextPath");
		return element.getTextContent();
	}

	public String getWar() {
		final Element element = super.findElement("Set", "war");
		return element.getTextContent();
	}

	public void save() throws IOException {
		XMLUtil.save(_saveFile.getCanonicalPath(), getElementNode().getOwnerDocument());
	}

	public void setSaveFile(final File saveFile) {
		this._saveFile = saveFile;
		final String war = getWar();
		final File warFile = new File(war);
		if (war != null && warFile.exists()) {
			try {
				this._documentBase = warFile.getCanonicalPath();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		} else {
			this._documentBase = saveFile.getName();
			final int index = _documentBase.lastIndexOf('.');
			if (index != -1) {
				_documentBase = _documentBase.substring(0, index);
			}
			this._memento = "org.eclipse.jst.jee.server:" + _documentBase;
		}
	}

	public String getMemento() {
		return _memento;
	}

	// public void setMemento(String memento) {
	// this.memento = memento;
	// }

	public String getDocumentBase() {
		return _documentBase;
	}

	// public void setDocumentBase(String documentBase) {
	// this.documentBase = documentBase;
	// }
}
