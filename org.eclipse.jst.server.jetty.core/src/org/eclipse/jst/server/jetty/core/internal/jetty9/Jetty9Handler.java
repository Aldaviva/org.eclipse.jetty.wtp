package org.eclipse.jst.server.jetty.core.internal.jetty9;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jst.server.jetty.core.internal.jetty8.Jetty8Handler;
import org.eclipse.wst.server.core.IModule;

public class Jetty9Handler extends Jetty8Handler {

	@Override
	public IStatus canAddModule(final IModule module) {
		final String version = module.getModuleType().getVersion();

		if ("3.1".equals(version)) {
			// Jetty 9.1–9.4 support Servlet 3.1
			// Jetty 9.0 supports Servlet 3.1-beta
			return Status.OK_STATUS;
		} else {
			return super.canAddModule(module);
		}
	}

	/**
	 * Make the classpath only contain {@code start.jar}. Jetty 9 is smart enough to load its own additional JARs based on the contents of {@code start.ini}.
	 */
	@Override
	public Collection<IRuntimeClasspathEntry> getRuntimeClasspath(final IPath installPath, final IPath configPath) {
		final Collection<IRuntimeClasspathEntry> classpath = new ArrayList<IRuntimeClasspathEntry>();

		final IPath startJAR = installPath.append(START_JAR);
		classpath.add(JavaRuntime.newArchiveRuntimeClasspathEntry(startJAR));

		return classpath;
	}

	/**
	 * These are JVM options, such as {@code -Da=b}, that appear before the main class, not after.
	 * Therefore, arguments to Jetty passed from here must be prefixed with {@code -D}, or else the JVM will mistake them for the name of a main class.
	 */
	@Override
	public List<String> getRuntimeVMArguments(
	    final IPath installPath, final IPath configPath, final IPath deployPath, final int mainPort, final int adminPort, final boolean isTestEnv) {
		final List<String> vmArguments = new ArrayList<String>();
		vmArguments.add("-Djetty.home=\"" + installPath.toOSString() + "\""); //not sure if these quotation marks are needed, since I think the launch configuration adds its own around the entire string
		vmArguments.add("-Djetty.base=\"" + (isTestEnv ? configPath : installPath).toOSString() + "\"");
		vmArguments.add("-DSTOP.PORT=" + adminPort);
		vmArguments.add("-DSTOP.KEY=secret");
		return vmArguments;
	}

}
