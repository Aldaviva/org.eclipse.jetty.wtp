package org.eclipse.jst.server.jetty.core.internal.jetty9;

import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.jetty.core.JettyPlugin;
import org.eclipse.jst.server.jetty.core.WebModule;
import org.eclipse.jst.server.jetty.core.internal.IJettyWebModule;
import org.eclipse.jst.server.jetty.core.internal.JettyConfiguration;
import org.eclipse.jst.server.jetty.core.internal.JettyConstants;
import org.eclipse.jst.server.jetty.core.internal.JettyServer;
import org.eclipse.jst.server.jetty.core.internal.Messages;
import org.eclipse.jst.server.jetty.core.internal.Trace;
import org.eclipse.jst.server.jetty.core.internal.config.Jetty9StartIni;
import org.eclipse.jst.server.jetty.core.internal.util.IOUtils;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.ServerInstance;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty7.server.WebApp;
import org.eclipse.jst.server.jetty.core.internal.xml.jetty9.Jetty9ServerInstance;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.ServerPort;

/**
 * Deploy to Jetty 9.4.
 * 
 * <p>Jetty 9 is different from Jetty 8 and earlier because this version supports separate installation and data directories (like Tomcat).
 * This means Jetty now has first-class support for a concept that WTP has been hacking together for years, where Eclipse can scribble in its own set of Jetty folders
 * without modifying the Jetty installation directory, providing isolation of configuration.</p>
 * 
 * <h2>Notable differences in Jetty 9</h2>
 * <ul><li>{@code jetty.home} is the Jetty installation directory, which Eclipse does not modify</li>
 * <li>{@code jetty.base} is the workspace metadata deployment directory, which Eclipse creates in {@code .metadata/.plugins/org.eclipse.wst.server.core/tmp0}</li>
 * <li>Webapp context XML files go in the <code>{jetty.base}/webapps</code> directory, not the separate <code>{jetty.base}/contexts</code> directory</li></ul>
 * 
 * @author Ben Hutchison
 */
public class Jetty9Configuration extends JettyConfiguration {

	protected Jetty9ServerInstance serverInstance;

	/**
	 * @param path The workspace server directory, relative to the workspace root; e.g. {@code /Servers/Jetty v9.4 Server at localhost-config}.
	 */
	public Jetty9Configuration(final IFolder path) {
		super(path);
		Trace.trace(Trace.CONFIG, "Created Jetty9Configuration with path " + path.getFullPath());
	}

	/**
	 * This gets called when you make a new Jetty server in Eclipse.
	 * @param runtimeBaseDirectory {@code jetty.base}; e.g. {@code C:/Users/Ben/Documents/runtime-EclipseApplication/.metadata/.plugins/org.eclipse.wst.server.core/tmp0}
	 * @param servers for Jetty 7, all the {@code etc\*.xml} files are detected in {@code jetty.home} by {@code load()}, based on {@code StartIni.getJettyXMLFiles()}. Probably unused for Jetty 9 because we don't need to copy a shit-ton of XML files around, we just put things in {@code start.ini} and Jetty adapts them into immutable XML files using its module system.
	 * @param webapp e.g. {@code C:\Programs\Development\Jetty\9.4\etc\webdefault.xml} when the server is first created in Eclipse, or {@code null} before launching Jetty even if a webapp is added to the server in Eclipse
	 */
	protected Jetty9ServerInstance createServerInstance(final IPath runtimeBaseDirectory) {
		Trace.trace(Trace.CONFIG, "Created new Jetty 9 server instance for runtime directory " + runtimeBaseDirectory);
		return new Jetty9ServerInstance(runtimeBaseDirectory);
	}

	public List<WebModule> getWebModules() {
		//TODO
		return Collections.emptyList();
	}

	public void addWebModule(final int i, final IJettyWebModule module) {
		//TODO
	}

	public void removeWebModule(final int index) {
		//TODO
	}

	/**
	 * This gets called when you make a new Jetty server in Eclipse. Calling this function is supposed to mutate this {@code Jetty9Instance} instance to load data from disk into memory.
	 * It's this class's responsibility to set the local {@code serverInstance} field (using {@link #createServerInstance(IPath, List, WebApp)}) and call {@link ServerInstance#setAdminPort(String)} on it.
	 * Older implementations do this by reading a ton of server and webapp XML and INI files, but I am not yet sure that is necessary.
	 * I think the only relevant information that should be in those server or webapp variables are the listening ports (and protocol names) and maybe the jetty.base and jetty.home directories.
	 * When the server is first created in Eclipse, the adminPort file doesn't exist (since it can't come from jetty.home), so the serverInstance's adminPort value starts at a default value.
	 * @param path Absolute directory of Jetty installation, also known as {@code jetty.home}; e.g. {@code C:/Programs/Development/Jetty/9.4}
	 * @param runtimeBaseDirectory Absolute directory of the workspace metadata temp where deployed files go, also known as {@code jetty.base}; e.g. {@code C:/Users/Ben/Documents/runtime-EclipseApplication/.metadata/.plugins/org.eclipse.wst.server.core/tmp0}
	 */
	public void load(final IPath path, final IPath runtimeBaseDirectory, final IProgressMonitor monitor) throws CoreException {
		Trace.trace(Trace.CONFIG, "Loading Jetty 9 configuration from path " + path + " and runtime directory " + runtimeBaseDirectory);

		try {
			serverInstance = createServerInstance(runtimeBaseDirectory);
			Trace.trace(Trace.CONFIG, "Created and assigned _serverInstance");

			BufferedReader adminPortReader = null;
			try {
				adminPortReader = new BufferedReader(new FileReader(path.append(JettyConstants.ADMIN_PORT).toFile()));
				serverInstance.setAdminPort(Integer.parseInt(adminPortReader.readLine()));
			} catch (final FileNotFoundException e) {
				//leave admin port set to the default value
			} finally {
				IOUtils.closeQuietly(adminPortReader);
			}
		} catch (final NumberFormatException e) {
			Trace.trace(Trace.WARNING, "Could not load Jetty v9.x configuration from " + path.toOSString() + ": " + e.getMessage());
			throw new CoreException(
			    new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorCouldNotLoadConfiguration, path.toOSString()), e));
		} catch (final IOException e) {
			Trace.trace(Trace.WARNING, "Could not load Jetty v9.x configuration from " + path.toOSString() + ": " + e.getMessage());
			throw new CoreException(
			    new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0, NLS.bind(Messages.errorCouldNotLoadConfiguration, path.toOSString()), e));
		}
	}

	// this is just loading an existing server from the workspace metadata folder
	public void load(final IFolder folder, final IPath runtimeBaseDirectory, final IProgressMonitor monitor) throws CoreException {
		Trace.trace(Trace.CONFIG, "Loading Jetty 9 configuration from folder " + folder + " and runtime directory " + runtimeBaseDirectory);
		this.load(folder.getFullPath(), runtimeBaseDirectory, monitor);
	}

	/**
	 * This method is responsible for writing {@code start.ini} and the {@code adminPort} file, presumably to the workspace server folder.
	 * Previous versions also seem to have written the server and webapp XML files, {@code start.config}, {@code realm.properties}, {@code webdefault.xml}, and {@code start.jar}, but I'm not sure Jetty 9 needs to use those.
	 * @param folder Path to server directory in workspace; e.g. {@code F/Servers/Jetty v9.4 Server at localhost-config}
	 */
	public void save(final IFolder folder, final IProgressMonitor monitor) throws CoreException {
		Trace.trace(Trace.CONFIG, "Saving Jetty 9 configuration to folder " + folder);

		ensureRequiredConfigurationExists();

		try {
			serverInstance.save(folder, monitor);
		} catch (final IOException e) {
			Trace.trace(Trace.SEVERE, "Could not save Jetty v9.x configuration to " + folder.toString(), e);
			throw new CoreException(new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0,
			    NLS.bind(Messages.errorCouldNotSaveConfiguration, new String[] { e.getLocalizedMessage() }), e));
		} catch (final CoreException e) {
			Trace.trace(Trace.SEVERE, "Could not save Jetty v9.x configuration to " + folder.toString(), e);
			throw new CoreException(new Status(IStatus.ERROR, JettyPlugin.PLUGIN_ID, 0,
			    NLS.bind(Messages.errorCouldNotSaveConfiguration, new String[] { e.getLocalizedMessage() }), e));
		}
	}

	/**
	 * Update the in-memory view of the start.ini file to make sure it has some important default values.
	 */
	private void ensureRequiredConfigurationExists() {
		final Jetty9StartIni startIni = serverInstance.getStartIni();
		startIni.ensureContains("--module", "annotations");
		startIni.ensureContains("--module", "http");
		startIni.ensureContains("--module", "deploy");
		startIni.ensureDefaultValue("jetty.http.port", "8080");
		startIni.ensureDefaultValue("jetty.deploy.monitoredDir", JettyServer.DEFAULT_DEPLOYDIR);
	}

	/**
	 * This is really just publish, there's no backing up going on.
	 */
	@Override
	public IStatus backupAndPublish(final IPath jettyDir, final boolean doBackup, final IProgressMonitor monitor) {
		Trace.trace(Trace.CONFIG, "Publishing to Jetty 9 path " + jettyDir + ", backup = " + doBackup);
		return super.backupAndPublish(jettyDir, doBackup, monitor);
	}

	/**
	 * Change the in-memory value for the Stop port (aka admin port or server port).
	 * This will get written to start.ini in the workspace Servers folder on save(), and copied to tmp0 on publish.
	 */
	public void modifyServerPort(final String id, final int port) {
		Trace.trace(Trace.CONFIG, "Setting Jetty 9 service " + id + " to use port " + port);
		serverInstance.getStartIni().setStartIniValue("jetty." + id + ".port", String.valueOf(port));
	}

	public void importFromPath(final IPath path, final IPath runtimeBaseDirectory, final boolean isTestEnv, final IProgressMonitor monitor)
	    throws CoreException {
		load(path, runtimeBaseDirectory, monitor);
	}

	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		//do nothing
	}

	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		//do nothing
	}

	public void modifyWebModule(final int index, final String docBase, final String path, final boolean reloadable) {
		//TODO
	}

	/**
	 * Figure out which port to use for opening a web browser or sending health check pings or something.
	 */
	@Override
	public ServerPort getMainPort() {
		ServerPort httpPort = null;
		for (final ServerPort serverPort : getServerPorts()) {
			if ("ssl".equals(serverPort.getId())) {
				return serverPort;
			} else if ("http".equals(serverPort.getId())) {
				httpPort = serverPort;
			}
		}
		return httpPort;
	}

	public ServerPort getAdminPort() {
		for (final ServerPort serverPort : getServerPorts()) {
			if (JettyConstants.STOP_PORT_ID.equals(serverPort.getId())) {
				return serverPort;
			}
		}
		return null;
	}

	public Collection<ServerPort> getServerPorts() {
		final List<ServerPort> ports = new ArrayList<ServerPort>();

		ports.add(new ServerPort(JettyConstants.STOP_PORT_ID, Messages.portServer, serverInstance.getAdminPort(), "TCPIP"));

		String port = serverInstance.getStartIni().getLastStartIniValue("jetty.ssl.port");
		if (port != null) {
			ports.add(new ServerPort("ssl", "SSL", Integer.parseInt(port), "SSL"));
		}

		port = serverInstance.getStartIni().getLastStartIniValue("jetty.http.port", "8080");
		ports.add(new ServerPort("http", "HTTP", Integer.parseInt(port), "HTTP"));

		return ports;
	}

}
