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

import java.util.Collection;

public class StringUtils {

	public static final String TRUE = "true";
	public static final String FALSE = "false";

	public static boolean isTrue(final String s) {
		return TRUE.equals(s);
	}

	public static String join(final Collection<?> collection, final String separator) {
		final StringBuilder stringBuilder = new StringBuilder();
		if (collection != null) {
			for (final Object item : collection) {
				if (stringBuilder.length() > 0) {
					stringBuilder.append(separator);
				}
				stringBuilder.append(item);
			}
		}
		return stringBuilder.toString();
	}
}
