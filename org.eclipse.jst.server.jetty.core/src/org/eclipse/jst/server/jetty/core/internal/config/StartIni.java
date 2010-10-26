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
package org.eclipse.jst.server.jetty.core.internal.config;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.server.core.internal.ProgressUtil;
import org.eclipse.jst.server.jetty.core.internal.JettyConstants;
import org.eclipse.jst.server.jetty.core.internal.util.IOUtils;

public class StartIni implements JettyConstants {

	private List<PathFileConfig> jettyXMLFiles = new ArrayList<PathFileConfig>();
	private List<PathFileConfig> otherConfigs = new ArrayList<PathFileConfig>();
	private PathFileConfig startConfig = null;
	private PathFileConfig webdefaultXMLConfig = null;
	private File startIniFile;

	private boolean isStartIniDirty;

	public StartIni(IPath baseDirPath) {
		loadStartIni(baseDirPath, null);
		loadOtherConfigs(baseDirPath);
	}

	public StartIni(IFolder baseDirFolder) {
		loadStartIni(null, baseDirFolder);
		//loadOtherConfigs(null, baseDirFolder);
	}

	private List<String> loadStartIni(IPath baseDirPath, IFolder baseDirFolder) {
		List<String> args = new ArrayList<String>();
		if (baseDirPath != null) {
			IPath startIniPath = baseDirPath.append(START_INI);
			this.startIniFile = startIniPath.toFile();
		} else {
			try {
				this.startIniFile = IOUtils.toLocalFile(
						baseDirFolder.getFile(START_INI), null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		if (startIniFile.exists() && startIniFile.canRead()) {
			FileReader reader = null;
			BufferedReader buf = null;
			try {
				reader = new FileReader(startIniFile);
				buf = new BufferedReader(reader);

				File jettyXMLFile = null;
				String arg;
				while ((arg = buf.readLine()) != null) {
					arg = arg.trim();
					if (arg.length() == 0 || arg.startsWith("#")) {
						continue;
					}
					if (arg.indexOf('=') == -1) {
						if (baseDirPath != null) {
							jettyXMLFile = baseDirPath.append(arg).toFile();
						} else {
							try {
								jettyXMLFile = IOUtils.toLocalFile(
										baseDirFolder.getFile(arg), null);
							} catch (CoreException e) {
								e.printStackTrace();
							}
						}
						if (jettyXMLFile != null && jettyXMLFile.exists()
								&& jettyXMLFile.canRead()) {
							jettyXMLFiles.add(new PathFileConfig(jettyXMLFile,
									new Path(arg)));
						}
					}
					args.add(arg);
				}
			} catch (IOException e) {
			} finally {
				close(buf);
				close(reader);
			}
		}
		return args;
	}

	private void close(Closeable c) {
		if (c == null) {
			return;
		}
		try {
			c.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	private void loadOtherConfigs(IPath baseDirPath) {
		IPath realmPropertiesPath = baseDirPath.append("etc/realm.properties");
		File realmPropertiesFile = realmPropertiesPath.toFile();
		if (realmPropertiesFile.exists()) {
			otherConfigs.add(new PathFileConfig(realmPropertiesFile, new Path(
					"etc/realm.properties")));
		}

		IPath webdefaultPath = baseDirPath.append("etc/webdefault.xml");
		File webdefaultFile = webdefaultPath.toFile();
		if (webdefaultFile.exists()) {
			webdefaultXMLConfig = new PathFileConfig(webdefaultFile, new Path(
					"etc/webdefault.xml"));
		}

		IPath startJARPath = baseDirPath.append(START_JAR);
		File startConfigFile = startJARPath.toFile();
		if (startConfigFile.exists()) {
			startConfig = new PathFileConfig(startConfigFile, new Path(
					START_JAR));
		}
	}

	public List<PathFileConfig> getJettyXMLFiles() {
		return jettyXMLFiles;
	}

	public PathFileConfig getWebdefaultXMLConfig() {
		return webdefaultXMLConfig;
	}
	/**
	 * Saves the Web app document.
	 * 
	 * @param path
	 *            a path
	 * @param forceDirty
	 *            true to force a save
	 * @throws IOException
	 *             if anything goes wrong
	 */
	// public void save(String path, boolean forceDirty) throws IOException {
	// if (forceDirty || isWebAppDirty)
	// //XMLUtil.save(path, webAppDocument);
	// }

	/**
	 * Saves the Web app document.
	 * 
	 * @param file
	 *            a file
	 * @param monitor
	 *            a progress monitor
	 * @throws Exception
	 *             if anything goes wrong
	 */
	public void save(IFile file, IProgressMonitor monitor) throws Exception {
		if (file.exists() && !isStartIniDirty)
			return;
		if (startIniFile == null
				|| !(startIniFile.exists() && startIniFile.canRead()))
			return;

		InputStream in = null;
		try {
			in = new FileInputStream(startIniFile);
			if (file.exists())
				file.setContents(in, true, true,
						ProgressUtil.getSubMonitorFor(monitor, 200));
			else
				file.create(in, true,
						ProgressUtil.getSubMonitorFor(monitor, 200));
		} catch (Exception e) {
			// ignore
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				// ignore
			}
		}
		isStartIniDirty = false;
	}

	public List<PathFileConfig> getOtherConfigs() {
		return otherConfigs;
	}

	public PathFileConfig getStartConfig() {
		return startConfig;
	}
}
