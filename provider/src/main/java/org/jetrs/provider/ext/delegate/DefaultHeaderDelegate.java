/* Copyright (c) 2021 JetRS
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

package org.jetrs.provider.ext.delegate;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.RuntimeDelegate;

public class DefaultHeaderDelegate implements RuntimeDelegate.HeaderDelegate<Object> {
  @Override
  public Object fromString(final String value) {
    return value;
  }

  @Override
  public String toString(final Object value) {
    if (value == null)
      return null;

    if (value instanceof String)
      return (String)value;

    if (value instanceof MediaType)
      return value.toString();

    if (value instanceof Locale)
      return value.toString();

    if (value instanceof Charset)
      return value.toString();

    if (value instanceof Date)
      return DateHeaderDelegate.format((Date)value);

    if (value instanceof URI)
      return value.toString();

    if (value instanceof CacheControl)
      return value.toString();

    if (value instanceof NewCookie)
      return value.toString();

    // NOTE: It is assumed that the only Map in here is a Map of cookies
    if (value instanceof Map) {
      final StringBuilder builder = new StringBuilder();
      final Iterator<?> iterator = ((Map<?,?>)value).values().iterator();
      for (int i = 0; iterator.hasNext(); ++i) {
        if (i > 0)
          builder.append(';');

        builder.append(iterator.next());
      }

      return builder.toString();
    }

    throw new UnsupportedOperationException("Unsupported type: " + value.getClass().getName());
  }
}