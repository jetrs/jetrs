/* Copyright (c) 2019 JetRS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.jetrs.common.ext.delegate;

import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.ext.RuntimeDelegate;

import org.libj.lang.Strings;

public class CacheControlHeaderDelegate implements RuntimeDelegate.HeaderDelegate<CacheControl> {
  private static String parseField(final String value) {
    final int index = value.indexOf('=');
    if (index == -1)
      return null;

    final String field = value.substring(index + 1).trim();
    return field.startsWith("\"") && field.endsWith("\"") ? field.substring(1, field.length() - 1).trim() : field;
  }

  private static String fieldToString(String field) {
    if (field == null)
      return null;

    field = field.trim();
    return field.length() == 0 ? null : "\"" + field + "\"";
  }

  private static boolean parseValue(final CacheControl cacheControl, final String value) {
    if (value.startsWith("max-age")) {
      final int index = value.indexOf('=');
      if (index == -1)
        throw new IllegalArgumentException(value);

      cacheControl.setMaxAge(Integer.parseInt(value.substring(index + 1).trim()));
    }
    else if (value.startsWith("s-maxage")) {
      final int index = value.indexOf('=');
      if (index == -1)
        throw new IllegalArgumentException(value);

      cacheControl.setSMaxAge(Integer.parseInt(value.substring(index + 1).trim()));
    }
    else if (value.startsWith("no-cache")) {
      final String field = parseField(value);
      if (field == null)
        cacheControl.setNoCache(true);
      else
        cacheControl.getNoCacheFields().add(field);
    }
    else if (value.equals("no-store")) {
      cacheControl.setNoStore(true);
    }
    else if (value.equals("no-transform")) {
      cacheControl.setNoTransform(true);
    }
    else if (value.equals("must-revalidate")) {
      cacheControl.setMustRevalidate(true);
    }
    else if (value.equals("private")) {
      final String field = parseField(value);
      if (field == null)
        cacheControl.setPrivate(true);
      else
        cacheControl.getPrivateFields().add(field);
    }
    else if (value.equals("proxy-revalidate")) {
      cacheControl.setProxyRevalidate(true);
    }
    else {
      final int index = value.indexOf('=');
      if (index == -1)
        cacheControl.getCacheExtension().put(value, null);
      else
        cacheControl.getCacheExtension().put(Strings.requireLettersOrDigits(value.substring(0, index).trim()), value.substring(index + 1).trim());

      return false;
    }

    return true;
  }

  @Override
  public CacheControl fromString(final String value) {
    final String[] values = value.split(",");
    for (int i = 0; i < values.length; ++i)
      values[i] = values[i].trim();

    boolean valid = false;
    final CacheControl cacheControl = new CacheControl();
    for (int i = 0; i < values.length; ++i)
      valid |= parseValue(cacheControl, values[i]);

    if (!valid)
      throw new IllegalArgumentException(value);

    return cacheControl;
  }

  @Override
  public String toString(final CacheControl value) {
    final StringBuilder builder = new StringBuilder();

    if (value.isMustRevalidate())
      builder.append("must-revalidate,");

    if (value.isNoStore())
      builder.append("no-store,");

    if (value.isNoTransform())
      builder.append("no-transform,");

    if (value.isNoTransform())
      builder.append("no-transform,");

    if (value.getPrivateFields().size() > 0) {
      for (String field : value.getPrivateFields()) {
        field = fieldToString(field);
        if (field != null)
          builder.append("private=").append(field).append(',');
      }
    }
    else if (value.isPrivate()) {
      builder.append("private,");
    }

    if (value.getNoCacheFields().size() > 0) {
      for (String field : value.getNoCacheFields()) {
        field = fieldToString(field);
        if (field != null)
          builder.append("no-cache=").append(field).append(',');
      }
    }
    else if (value.isNoCache()) {
      builder.append("no-cache,");
    }

    if (value.getMaxAge() == Integer.MAX_VALUE)
      builder.append("max-age,");
    else if (value.getMaxAge() != -1)
      builder.append("max-age=").append(value.getMaxAge());

    if (value.getSMaxAge() == Integer.MAX_VALUE)
      builder.append("s-maxage,");
    else if (value.getMaxAge() != -1)
      builder.append("s-maxage=").append(value.getMaxAge());

    final Iterator<Map.Entry<String,String>> iterator = value.getCacheExtension().entrySet().iterator();
    for (int i = 0; iterator.hasNext(); ++i) {
      if (i > 0)
        builder.append(',');

      final Map.Entry<String,String> entry = iterator.next();
      builder.append(entry.getKey());
      if (entry.getValue() != null)
        builder.append('=').append(entry.getValue());
    }

    return builder.toString();
  }
}