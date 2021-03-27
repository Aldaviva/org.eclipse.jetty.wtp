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
package org.eclipse.jst.server.jetty.core.internal.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.jetty.core.internal.JettyConstants;
import org.eclipse.jst.server.jetty.core.internal.JettyServer;
import org.eclipse.jst.server.jetty.core.internal.JettyServerBehaviour;
import org.eclipse.jst.server.jetty.core.internal.Trace;

public class JettyVersionHelper implements JettyConstants
{

    public static IStatus checkJettyVersion(IPath installPath)
    {
        // Search start.jar
        IPath startJarPath = installPath.append(__START_JAR);
        File jarFile = null;
        jarFile = startJarPath.toFile();
        // If jar is not at expected location, try alternate location
        if (!jarFile.exists())
        {
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

    /**
     * Gets the base directory for this server. This directory is used as the "base" property for the server.
     * 
     * @param ts
     *            JettyServer from which to derive the base directory directory. Only used to get the temp directory if needed.
     * @return path to base directory
     */
    public static IPath getStandardBaseDirectory(JettyServer ts)
    {
        if (ts.isTestEnvironment())
        {
            String baseDir = ts.getInstanceDirectory();
            // If test mode and no instance directory specified, use temporary
            // directory
            if (baseDir == null)
            {
                JettyServerBehaviour tsb = (JettyServerBehaviour)ts.getServer().loadAdapter(JettyServerBehaviour.class,null);
                if (tsb == null)
                {
                    return null;
                }
                return tsb.getTempDirectory();
            }
            IPath path = new Path(baseDir);
            if (!path.isAbsolute())
            {
                IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
                path = rootPath.append(path);
            }
            // Return specified instance directory
            return path;
        }
        // Return runtime path
        return ts.getServer().getRuntime().getLocation();
    }

    public static String[] getJettyProgramArguments(IPath configPath, boolean debug, boolean starting)
    {
        List<String> list = new ArrayList<String>();

        if (starting)
        {
            // list.add(configPath.toOSString() + "/etc/jetty.xml");
            // list.add(configPath.toOSString() + "/etc/jetty-deploy.xml");
        }
        else
            list.add("--stop");

        String[] temp = new String[list.size()];
        list.toArray(temp);
        return temp;
    }

    /**
     * Creates a Jetty instance directory at the specified path. This involves creating the set of subdirectories uses by a Jetty instance.
     * 
     * @param baseDir
     *            directory at which to create Jetty instance directories.
     * @return result status of the operation
     */
    public static IStatus createJettyInstanceDirectory(IPath baseDir)
    {
        if (Trace.isTraceEnabled())
            Trace.trace(Trace.CONFIG,"Creating runtime directory at " + baseDir.toOSString());
        File temp = baseDir.append("contexts").toFile();
        if (!temp.exists())
            temp.mkdirs();
        temp = baseDir.append("etc").toFile();
        if (!temp.exists())
            temp.mkdirs();
        temp = baseDir.append("resources").toFile();
        if (!temp.exists())
            temp.mkdirs();
        temp = baseDir.append("webapps").toFile();
        if (!temp.exists())
            temp.mkdirs();

        return Status.OK_STATUS;
    }

}
