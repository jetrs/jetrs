/* Copyright (c) 2018 lib4j
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

package org.libx4j.xrs.server.ext;

import javax.ws.rs.ext.RuntimeDelegate;

public class StringArrayHeaderDelegate implements RuntimeDelegate.HeaderDelegate<String[]> {
  private static String[] fromString(final String value, final int start, final int depth) {
    if (start >= value.length() - 1)
      return new String[depth];

    int end = value.indexOf(',', start);
    if (start == end)
      return fromString(value, end + 1, depth);

    if (end == -1)
      end = value.length();

    final String token = value.substring(start, end).trim();
    if (token.length() == 0)
      return fromString(value, end + 1, depth);

    final String[] array = fromString(value, end + 1, depth + 1);
    array[depth] = token;
    return array;
  }

  public static String[] parse(final String value) {
    return value == null ? null : value.length() == 0 ? new String[0] : fromString(value, 0, 0);
  }

  public static String format(final String[] value) {
    if (value == null)
      return null;

    if (value.length == 0)
     return "";

    final StringBuilder builder = new StringBuilder();
    builder.append(value[0]);
    for (int i = 1; i < value.length; i++)
      builder.append(',').append(value[i]);

    return builder.toString();
  }

  @Override
  public String[] fromString(final String value) {
    return parse(value);
  }

  @Override
  public String toString(final String[] value) {
    return format(value);
  }
}