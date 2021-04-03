package org.eclipse.jst.server.jetty.core.internal.jetty9;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jst.server.jetty.core.IJettyConfiguration;
import org.eclipse.jst.server.jetty.core.internal.IJettyVersionHandler;
import org.eclipse.jst.server.jetty.core.internal.IJettyVersionProvider;
import org.eclipse.jst.server.jetty.core.internal.jetty8.Jetty8Provider;

public class Jetty9Provider extends Jetty8Provider {

	public static final IJettyVersionProvider __INSTANCE = new Jetty9Provider();

	private final IJettyVersionHandler _versionHandler = new Jetty9Handler();

	@Override
	public IJettyVersionHandler getJettyVersionHandler() {
		return _versionHandler;
	}

	@Override
	public IJettyConfiguration createJettyConfiguration(final IFolder path) {
		return new Jetty9Configuration(path);
	}

}
