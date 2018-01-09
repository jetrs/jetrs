/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.MediaType;

public final class MediaTypes {
  private static final Comparator<MediaType> qComparator = new Comparator<MediaType>() {
    @Override
    public int compare(final MediaType o1, final MediaType o2) {
      if (o1.getParameters().isEmpty())
        return -1;

      if (o2.getParameters().isEmpty())
        return 1;

      final String q1 = o1.getParameters().get("q");
      if (q1 == null)
        return -1;

      final String q2 = o2.getParameters().get("q");
      if (q2 == null)
        return 1;

      return Double.valueOf(q1) > Double.valueOf(q2) ? -1 : 1;
    }
  };

  public static MediaType matches(final MediaType[] required, final MediaType[] tests) {
    for (final MediaType test : tests)
      if (matches(test, required))
        return test;

    return null;
  }

  public static boolean matches(final MediaType required, final MediaType[] tests) {
    for (final MediaType test : tests)
      if (matches(required, test))
        return true;

    return false;
  }

  public static boolean matches(final MediaType required, final MediaType test) {
    if (required == null || test == null)
      return true;

    if (!required.isCompatible(test))
      return false;

    for (final Map.Entry<String,String> entry : required.getParameters().entrySet()) {
      final String value = test.getParameters().get(entry.getKey());
      if (value != null && !"q".equals(value) && !value.equals(entry.getValue()))
        return false;
    }

    return true;
  }

  public static MediaType[] parse(final Collection<String> strings) {
    if (strings == null)
      return null;

    final MediaType[] mediaTypes = parse(strings.iterator(), 0);
    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  private static MediaType[] parse(final Iterator<String> iterator, final int depth) {
    if (!iterator.hasNext())
      return new MediaType[depth];

    final String[] parts = iterator.next().split(",");
    final MediaType[] mediaTypes = parse(iterator, depth + parts.length);
    for (int i = 0; i < parts.length; i++)
      mediaTypes[depth + i] = parse(parts[i]);

    return mediaTypes;
  }

  public static MediaType[] parse(final Enumeration<String> enumeration) {
    final MediaType[] mediaTypes = parse(enumeration, 0);
    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  private static MediaType[] parse(final Enumeration<String> enumeration, final int depth) {
    if (!enumeration.hasMoreElements())
      return new MediaType[depth];

    final String[] parts = enumeration.nextElement().split(",");
    final MediaType[] mediaTypes = parse(enumeration, depth + parts.length);
    for (int i = 0; i < parts.length; i++)
      mediaTypes[depth + i] = parse(parts[i]);

    return mediaTypes;
  }

  public static MediaType[] parse(final String ... strings) {
    final MediaType[] mediaTypes = parse(strings, 0, 0);
    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  private static MediaType[] parse(final String[] strings, int index, final int depth) {
    if (index == strings.length)
      return new MediaType[depth];

    final String[] parts = strings[index].split(",");
    final MediaType[] mediaTypes = parse(strings, index + 1, depth + parts.length);
    for (int i = 0; i < parts.length; i++)
      mediaTypes[depth + i] = parse(parts[i]);

    return mediaTypes;
  }

  public static MediaType parse(final String string) {
    if (string == null)
      return null;

    int start = string.indexOf("/");
    if (start == -1)
      return new MediaType(string.trim(), null);

    int semicolon = string.indexOf(";", start + 1);
    final String type = string.substring(0, start).trim();
    final String subtype = string.substring(start + 1, semicolon > -1 ? semicolon : string.length()).trim();
    if (semicolon < 0)
      return new MediaType(type, subtype);

    start = semicolon;
    final Map<String,String> parameters = new HashMap<String,String>();
    do {
      semicolon = string.indexOf(";", semicolon + 1);
      final String token = string.substring(start + 1, semicolon > 0 ? semicolon : string.length());
      final int eq = token.indexOf('=');
      if (eq >= 0)
        parameters.put(token.substring(0, eq).trim(), token.substring(eq + 1).trim());
    }
    while (semicolon > 0);

    return new MediaType(type, subtype, parameters);
  }

  public static String toString(final MediaType value) {
    if (value == null)
      throw new NullPointerException();

    final StringBuilder builder = new StringBuilder();
    builder.append(value.getType());
    builder.append('/');
    builder.append(value.getSubtype());
    if (value.getParameters() != null)
      for (final Map.Entry<String,String> entry : value.getParameters().entrySet())
        builder.append(';').append(entry.getKey()).append('=').append(entry.getValue());

    return builder.toString();
  }

  private MediaTypes() {
  }
}