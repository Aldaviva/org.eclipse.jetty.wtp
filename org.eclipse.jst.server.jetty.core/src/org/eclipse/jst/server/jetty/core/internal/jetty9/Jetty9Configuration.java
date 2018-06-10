package org.eclipse.jst.server.jetty.core.internal.jetty9;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jst.server.jetty.core.IJettyConfiguration;
import org.eclipse.jst.server.jetty.core.IJettyServer;
import org.eclipse.jst.server.jetty.core.WebModule;
import org.eclipse.jst.server.jetty.core.internal.IJettyWebModule;
import org.eclipse.jst.server.jetty.core.internal.jetty7.Jetty7Configuration;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerPort;

public class Jetty9Configuration extends Jetty7Configuration {

	public Jetty9Configuration(IFolder path) {
		super(path);
	}

}
