package org.eclipse.jst.server.jetty.core.internal.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.server.jetty.core.internal.Trace;

/**
 * Represents the {@code start.ini} file that controls Jetty 9's settings.
 * Contains an in-memory view of the file contents (ignoring comments and blank lines).
 * Also exposes convenience methods to get and set values on that in-memory view. 
 * This class doesn't inherit from {@link StartIni} because they have no implementation specifics in common.
 */
public class Jetty9StartIni {
	private Map<String, List<String>> jettyBaseStartIniContents;

	public Jetty9StartIni(final File startIniFileToLoad) {
		try {
			jettyBaseStartIniContents = IniMapper.deserialize(startIniFileToLoad);
		} catch (final IOException e) {
			Trace.trace(Trace.WARNING, "Could not load Jetty v9.x configuration from " + startIniFileToLoad + ", using empty configuration: " + e.getMessage());
			jettyBaseStartIniContents = new LinkedHashMap<String, List<String>>();
		}
	}

	public void save(final IFile startIniFile) throws IOException, CoreException {
		IniMapper.serialize(jettyBaseStartIniContents, startIniFile);
	}

	public String getLastStartIniValue(final String key) {
		return getLastStartIniValue(key, null);
	}

	public String getLastStartIniValue(final String key, final String defaultIfMissing) {
		if (jettyBaseStartIniContents != null) {
			final List<String> values = jettyBaseStartIniContents.get(key);
			if (values != null && !values.isEmpty()) {
				values.get(values.size() - 1);
			}
		}

		return defaultIfMissing;
	}

	public List<String> getStartIniValues(final String key) {
		if (jettyBaseStartIniContents != null) {
			return jettyBaseStartIniContents.get(key);
		}

		return null;
	}

	public void setStartIniValue(final String key, final String value) {
		initializeBaseStartIniIfNecessary();
		jettyBaseStartIniContents.put(key, new ArrayList<String>(Arrays.asList(value)));
	}

	public void appendStartIniValue(final String key, final String value) {
		initializeBaseStartIniIfNecessary();
		final List<String> bucket = jettyBaseStartIniContents.get(key);
		if (bucket == null) {
			jettyBaseStartIniContents.put(key, new ArrayList<String>(Arrays.asList(value)));
		} else {
			bucket.add(value);
		}
	}

	public void ensureDefaultValue(final String key, final String value) {
		initializeBaseStartIniIfNecessary();
		if (!jettyBaseStartIniContents.containsKey(key)) {
			jettyBaseStartIniContents.put(key, new ArrayList<String>(Arrays.asList(value)));
		}
	}

	public void ensureContains(final String key, final String value) {
		initializeBaseStartIniIfNecessary();
		final List<String> bucket = jettyBaseStartIniContents.get(key);
		if (bucket == null) {
			jettyBaseStartIniContents.put(key, new ArrayList<String>(Arrays.asList(value)));
		} else if (!bucket.contains(value)) {
			bucket.add(value);
		}
	}

	private void initializeBaseStartIniIfNecessary() {
		if (jettyBaseStartIniContents == null) {
			jettyBaseStartIniContents = new LinkedHashMap<String, List<String>>();
		}
	}

}
