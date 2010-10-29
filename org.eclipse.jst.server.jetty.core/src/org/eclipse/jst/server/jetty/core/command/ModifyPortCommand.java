/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *     Angelo Zerr <angelo.zerr@gmail.com> - Jetty packages
 *******************************************************************************/
package org.eclipse.jst.server.jetty.core.command;

import java.util.Iterator;

import org.eclipse.jst.server.jetty.core.IJettyConfigurationWorkingCopy;
import org.eclipse.jst.server.jetty.core.internal.Messages;
import org.eclipse.wst.server.core.ServerPort;

/**
 * Command to change the configuration port.
 */
public class ModifyPortCommand extends ConfigurationCommand {
	protected String id;
	protected int port;
	protected int oldPort;

	/**
	 * ModifyPortCommand constructor.
	 * 
	 * @param configuration
	 *            a Jetty configuration
	 * @param id
	 *            a port id
	 * @param port
	 *            new port number
	 */
	public ModifyPortCommand(IJettyConfigurationWorkingCopy configuration,
			String id, int port) {
		super(configuration, Messages.configurationEditorActionModifyPort);
		this.id = id;
		this.port = port;
	}

	/**
	 * Execute the command.
	 */
	public void execute() {
		// find old port number
		Iterator iterator = configuration.getServerPorts().iterator();
		while (iterator.hasNext()) {
			ServerPort temp = (ServerPort) iterator.next();
			if (id.equals(temp.getId()))
				oldPort = temp.getPort();
		}

		// make the change
		configuration.modifyServerPort(id, port);
	}

	/**
	 * Undo the command.
	 */
	public void undo() {
		configuration.modifyServerPort(id, oldPort);
	}
}