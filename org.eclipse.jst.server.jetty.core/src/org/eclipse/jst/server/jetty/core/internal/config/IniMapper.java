package org.eclipse.jst.server.jetty.core.internal.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.server.jetty.core.internal.util.IOUtils;

public class IniMapper {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private IniMapper() {
	}

	public static Map<String, List<String>> deserialize(final File serialized) throws IOException {
		return deserialize(new FileInputStream(serialized));
	}

	public static Map<String, List<String>> deserialize(final InputStream serialized) throws IOException {
		return deserialize(new InputStreamReader(serialized, CHARSET));
	}

	public static Map<String, List<String>> deserialize(final Reader serialized) throws IOException {
		final Map<String, List<String>> deserialized = new LinkedHashMap<String, List<String>>();
		final BufferedReader bufferedReader = new BufferedReader(serialized);
		try {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty()) {
					// blank line, skip
				} else if (line.startsWith("#") || line.startsWith(";")) {
					// comment, skip
				} else {
					final String[] split = line.split("\s*=\s*", 2);
					if (split.length >= 2) {
						final String key = split[0];
						final String value = split[1];
						List<String> bucket = deserialized.get(key);
						if (bucket == null) {
							bucket = new ArrayList<String>();
							deserialized.put(key, bucket);
						}
						bucket.add(value);
					}
				}
			}
			return deserialized;
		} finally {
			IOUtils.closeQuietly(bufferedReader);
		}
	}

	public static String serialize(final Map<String, List<String>> deserialized) throws IOException {
		final StringWriter stringWriter = new StringWriter();
		serialize(deserialized, stringWriter);
		return stringWriter.toString();
	}

	/**
	 * @deprecated If you create a file with this method, Eclipse will not add it to the workspace until you refresh the workspace.
	 * This has the unfortunate effect of not copying a newly-created {@code start.ini} from the workspace Servers folder to the metadata tmp0 directory on publish.
	 * Use {@link #serialize(Map, IFile)} instead.
	 */
	@Deprecated
	public static void serialize(final Map<String, List<String>> deserialized, final File serialized) throws IOException {
		serialize(deserialized, new FileOutputStream(serialized, false));
	}

	public static void serialize(final Map<String, List<String>> deserialized, final OutputStream serialized) throws IOException {
		serialize(deserialized, new OutputStreamWriter(serialized, CHARSET));
	}

	public static void serialize(final Map<String, List<String>> deserialized, final Writer serialized) throws IOException {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new BufferedWriter(serialized));
			for (final Entry<String, List<String>> bucket : deserialized.entrySet()) {
				final String key = bucket.getKey();
				for (final String value : bucket.getValue()) {
					final String line = key + "=" + value;
					writer.println(line);
				}
			}
			writer.flush();
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public static void serialize(final Map<String, List<String>> jettyBaseStartIniContents, final IFile startIniFile) throws IOException, CoreException {
		final PipedOutputStream fromString = new PipedOutputStream();
		final PipedInputStream toFile = new PipedInputStream(fromString);
		serialize(jettyBaseStartIniContents, fromString);

		if (startIniFile.exists()) {
			startIniFile.setContents(toFile, true, true, null);
		} else {
			startIniFile.create(toFile, true, null);
		}
	}

}
