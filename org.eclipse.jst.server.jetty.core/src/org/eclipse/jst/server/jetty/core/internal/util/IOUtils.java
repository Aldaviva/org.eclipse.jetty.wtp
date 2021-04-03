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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.server.jetty.core.internal.Trace;

/**
 * I/O Utilities.
 * 
 */
public class IOUtils {

	private static final int __DEFAULT_BUFFER_SIZE = 1024 * 4;

	/**
	 * Converts the given URI to a local file. Use the existing file if the uri is on the local file system. Otherwise fetch it. Returns null if unable to fetch
	 * it.
	 * 
	 * @param uri
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public static File toLocalFile(final URI uri, final IProgressMonitor monitor) throws CoreException {
		final IFileStore fileStore = EFS.getStore(uri);
		File localFile = fileStore.toLocalFile(EFS.NONE, monitor);
		if (localFile == null) {
			// non local file system
			localFile = fileStore.toLocalFile(EFS.CACHE, monitor);
		}
		return localFile;
	}

	public static File toLocalFile(final IFile file, final IProgressMonitor monitor) throws CoreException {
		return toLocalFile(file.getLocationURI(), monitor);
	}

	/**
	 * Create Eclipse {@link IFolder}.
	 * 
	 * @param folderHandle
	 * @param monitor
	 * @throws CoreException
	 */
	public static void createFolder(final IFolder folderHandle, final IProgressMonitor monitor) throws CoreException {
		if (folderHandle.exists()) {
			return;
		}

		final IPath path = folderHandle.getFullPath();
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final int numSegments = path.segmentCount();
		if (numSegments > 2 && !root.getFolder(path.removeLastSegments(1)).exists()) {
			// If the direct parent of the path doesn't exist, try
			// to create the
			// necessary directories.
			for (int i = numSegments - 2; i > 0; i--) {
				final IFolder folder = root.getFolder(path.removeLastSegments(i));
				if (!folder.exists()) {
					folder.create(false, true, monitor);
				}
			}
		}
		folderHandle.create(false, true, monitor);
	}

	public static InputStream getInputStream(final File jarFile, final String fileName) throws ZipException, IOException {
		Trace.trace(Trace.FINER, "Reading file " + fileName + " from inside ZIP file " + jarFile);
		final ZipFile zipFile = new ZipFile(jarFile);
		final ZipEntry zipEntry = zipFile.getEntry(fileName);
		return zipFile.getInputStream(zipEntry);
	}

	/**
	 * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @return the number of bytes copied
	 * @throws IOException
	 *             In case of an I/O problem
	 */
	public static int copy(final InputStream input, final OutputStream output) throws IOException {
		final byte[] buffer = new byte[__DEFAULT_BUFFER_SIZE];
		int count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	public static InputStream toInputStream(final String input, final String encoding) throws IOException {
		final byte bytes[] = encoding == null ? input.getBytes() : input.getBytes(encoding);
		return new ByteArrayInputStream(bytes);
	}

	public static void closeQuietly(final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final IOException e) {
			}
		}
	}
}
