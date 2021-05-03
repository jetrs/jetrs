/* Copyright (c) 2016 JetRS
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

package org.jetrs.provider.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.libj.lang.Numbers;
import org.libj.lang.Strings;

/**
 * Utility class with convenience functions for the {@link MediaType} class.
 *
 * @see <a href=
 *      "https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7">rfc2616</a>
 */
public final class MediaTypes {
  public static final MediaType TEXT_PLAIN = new MediaType("text", "plain");
  public static final MediaType TEXT_XML = new MediaType("text", "xml");
  public static final MediaType APPLICATION_XML = new MediaType("application", "xml");
  public static final MediaType APPLICATION_JSON = new MediaType("application", "json");

  private static final Comparator<MediaType> qComparator = (o1, o2) -> {
    final Double s1 = o1 == null ? null : Numbers.parseDouble(o1.getParameters().get("q"));
    final Double s2 = o2 == null ? null : Numbers.parseDouble(o2.getParameters().get("q"));
    return Double.compare(s2 == null ? 1 : s2, s1 == null ? 1 : s1);
  };

  /**
   * Returns the first compatible {@link MediaType} that is encountered by
   * evaluating the two specified arrays of {@link MediaType}s, or {@code null}
   * if there not compatible {@link MediaType} is found.
   * <p>
   * <b>Note:</b> The {@link MediaType} arrays must be sorted on the "q"
   * parameter in descending order.
   * <p>
   * <b>Note:</b> A null {@link MediaType} is considered to be equivalent to
   * {@link MediaType#WILDCARD}.
   *
   * @param mediaTypes1 The first {@link MediaType} array sorted on the "q"
   *          parameter in descending order.
   * @param mediaTypes2 The second {@link MediaType} array sorted on the "q"
   *          parameter in descending order.
   * @return The first compatible {@link MediaType} that is encountered by
   *         evaluating the two specified arrays of {@link MediaType}s, or
   *         {@code null} if there not compatible {@link MediaType} is found.
   */
  public static MediaType getCompatible(final MediaType[] mediaTypes1, final MediaType[] mediaTypes2) {
    for (final MediaType mediaType2 : mediaTypes2) {
      final MediaType mediaType = getCompatible(mediaType2, mediaTypes1);
      if (mediaType != null)
        return mediaType;
    }

    return null;
  }

  /**
   * Returns the first compatible {@link MediaType} that is encountered by
   * evaluating the specified {@link MediaType} and provided arrays of
   * {@link MediaType}s, or {@code null} if there not compatible
   * {@link MediaType} is found.
   * <p>
   * <b>Note:</b> The provided {@link MediaType} array must be sorted on the "q"
   * parameter in descending order.
   * <p>
   * <b>Note:</b> A null {@link MediaType} is considered to be equivalent to
   * {@link MediaType#WILDCARD}.
   *
   * @param mediaType1 The first {@link MediaType}.
   * @param mediaTypes2 The second {@link MediaType} array sorted on the "q"
   *          parameter in descending order.
   * @return The first compatible {@link MediaType} that is encountered by
   *         evaluating the specified {@link MediaType} and provided arrays of
   *         {@link MediaType}s, or {@code null} if there not compatible
   *         {@link MediaType} is found.
   */
  public static MediaType getCompatible(final MediaType mediaType1, final MediaType[] mediaTypes2) {
    Arrays.sort(mediaTypes2, qComparator);
    for (final MediaType mediaType2 : mediaTypes2) {
      final MediaType mediaType = getCompatible(mediaType1, mediaType2);
      if (mediaType != null)
        return mediaType;
    }

    return null;
  }

  // NOTE: It is not clear whether the first match for a subtype with a suffix should be
  // NOTE: for the prefix+suffix, or the prefix?
  private static String getCompatibleSubtype(final String subType1, final String subType2, final boolean checkPlus) {
    if (MediaType.MEDIA_TYPE_WILDCARD.equals(subType1))
      return subType2;

    if (MediaType.MEDIA_TYPE_WILDCARD.equals(subType2))
      return subType1;

    if (subType1.equalsIgnoreCase(subType2))
      return subType1;

    if (!checkPlus)
      return null;

    final int index1 = subType1.indexOf('+');
    final int index2 = subType2.indexOf('+');
    if (index1 == -1) {
      if (index2 == -1)
        return null;

      return getCompatibleSubtype(subType1, subType2.substring(index2 + 1), false);
    }

    final String ret1 = getCompatibleSubtype(subType1.substring(index1 + 1), subType2, false);
    if (ret1 != null)
      return ret1;

    if (index2 == -1)
      return null;

    final String ret2 = getCompatibleSubtype(subType1, subType2.substring(index2 + 1), false);
    if (ret2 != null)
      return ret2;

    if (!MediaType.MEDIA_TYPE_WILDCARD.equals(subType1.substring(0, index1)) && !MediaType.MEDIA_TYPE_WILDCARD.equals(subType2.substring(0, index2)))
      return null;

    return getCompatibleSubtype(subType1.substring(index1 + 1), subType2.substring(index2 + 1), false);
  }

  private static Map<String,String> mergeParameters(final MediaType mediaType1, final MediaType mediaType2) {
    final Map<String,String> parameters = new HashMap<>();
    parameters.putAll(mediaType1.getParameters());
    parameters.putAll(mediaType2.getParameters());
    parameters.remove("q");
    return parameters;
  }

  private static MediaType filter(final MediaType mediaType) {
    if (!mediaType.getParameters().containsKey("q") && !mediaType.getParameters().containsKey("Q"))
      return mediaType;

    final Map<String,String> parameters = new HashMap<>(mediaType.getParameters());
    parameters.remove("q");
    return new MediaType(mediaType.getType(), mediaType.getSubtype(), parameters);
  }

  /**
   * Returns the compatible {@link MediaType} by evaluating the two specified
   * {@link MediaType}s, or {@code null} if the {@link MediaType}s are not
   * compatible.
   * <p>
   * <b>Note:</b> A null {@link MediaType} is considered to be equivalent to
   * {@link MediaType#WILDCARD}.
   *
   * @param mediaType1 The first {@link MediaType}.
   * @param mediaType2 The second {@link MediaType}.
   * @return The compatible {@link MediaType} by evaluating the two specified
   *         {@link MediaType}s, or {@code null} if the {@link MediaType}s are
   *         not compatible.
   */
  public static MediaType getCompatible(final MediaType mediaType1, final MediaType mediaType2) {
    if (mediaType1 == null)
      return mediaType2;

    if (mediaType2 == null)
      return mediaType1;

    final String type;
    if (mediaType1.isWildcardType())
      type = mediaType2.getType();
    else if (mediaType2.isWildcardType() || mediaType1.getType().equalsIgnoreCase(mediaType2.getType()))
      type = mediaType1.getType();
    else
      return null;

    final String subType = getCompatibleSubtype(mediaType1.getSubtype(), mediaType2.getSubtype(), true);
    if (subType == null)
      return null;

    if (mediaType1.getParameters().isEmpty() || mediaType2.getParameters().isEmpty()) {
      if (type.equalsIgnoreCase(mediaType1.getType()) && subType.equalsIgnoreCase(mediaType1.getSubtype()) && mediaType2.getParameters().isEmpty())
        return filter(mediaType1);

      if (type.equalsIgnoreCase(mediaType2.getType()) && subType.equalsIgnoreCase(mediaType2.getSubtype()) && mediaType1.getParameters().isEmpty())
        return filter(mediaType2);

      return new MediaType(type, subType, mergeParameters(mediaType1, mediaType2));
    }

    // FIXME: Need to compare parameters for compatibility.
    final Map<String,String> parameters = mergeParameters(mediaType1, mediaType2);
    return new MediaType(type, subType, parameters);
  }

  private interface Adapter<T> {
    boolean hasNext(T obj, int index);
    String next(T obj, int index);
  }

  private static final Adapter<Iterator<String>> iteratorAdapter = new Adapter<Iterator<String>>() {
    @Override
    public boolean hasNext(final Iterator<String> obj, final int index) {
      return obj.hasNext();
    }

    @Override
    public String next(final Iterator<String> obj, final int index) {
      return obj.next();
    }
  };

  private static final Adapter<Enumeration<String>> enumerationAdapter = new Adapter<Enumeration<String>>() {
    @Override
    public boolean hasNext(final Enumeration<String> obj, final int index) {
      return obj.hasMoreElements();
    }

    @Override
    public String next(final Enumeration<String> obj, final int index) {
      return obj.nextElement();
    }
  };

  private static final Adapter<String[]> arrayAdapter = new Adapter<String[]>() {
    @Override
    public boolean hasNext(final String[] obj, final int index) {
      return index < obj.length;
    }

    @Override
    public String next(final String[] obj, final int index) {
      return obj[index];
    }
  };

  private static <T>MediaType[] parse(final Adapter<T> adapter, final T source, int index, final int depth, StringBuilder builder, String header, int start) {
    MediaType mediaType = null;
    do {
      if (header == null) {
        if (adapter.hasNext(source, index)) {
          header = adapter.next(source, index++);
          start = 0;
          if (header == null || header.length() == 0)
            return parse(adapter, source, index, depth, builder, null, 0);
        }
        else {
          return depth == 0 ? null : new MediaType[depth];
        }
      }

      final int len = header.length();
      for (int i = start; i <= len; ++i) {
        final char ch;
        if (i == len || (ch = header.charAt(i)) == ',') {
          if ((start = i + 1) >= len)
            header = null;

          if (builder != null && builder.length() != 0) {
            mediaType = parse(builder.toString());
            break;
          }

          if (header == null)
            break;
        }
        else if (ch != ' ') {
          if (!isValidChar(ch)) {
            i = header.indexOf(',', i + 2) - 1;
            if (i < 0) {
              if (builder != null)
                builder.setLength(0);

              header = null;
              break;
            }
          }

          if (builder == null)
            builder = new StringBuilder();

          builder.append(ch);
        }
      }
    }
    while (mediaType == null);
    if (builder != null)
      builder.setLength(0);

    final MediaType[] mediaTypes = parse(adapter, source, index, depth + 1, builder, header, start);
    mediaTypes[depth] = mediaType;

    return mediaTypes;
  }

  public static MediaType[] parse(final Collection<String> strings) {
    if (strings == null)
      return null;

    if (strings.size() == 0)
      return EMPTY_ARRAY;

    final MediaType[] mediaTypes = parse(iteratorAdapter, strings.iterator(), -1, 0, null, null, 0);
    if (mediaTypes == null)
      return EMPTY_ARRAY;

    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  private static final MediaType[] EMPTY_ARRAY = {};

  /**
   * Parses the specified enumeration of strings and returns an array of
   * {@link MediaType} objects.
   *
   * @param enumeration The enumeration of strings.
   * @return An array of {@link MediaType} objects.
   * @throws NullPointerException If {@code enumeration} is null.
   */
  public static MediaType[] parse(final Enumeration<String> enumeration) {
    if (enumeration == null)
      return null;

    if (!enumeration.hasMoreElements())
      return EMPTY_ARRAY;

    final MediaType[] mediaTypes = parse(enumerationAdapter, enumeration, -1, 0, null, null, 0);
    if (mediaTypes == null)
      return EMPTY_ARRAY;

    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  /**
   * Parses the specified array of strings and returns an array of
   * {@link MediaType} objects.
   *
   * @param strings The the strings array.
   * @return An array of {@link MediaType} objects.
   * @throws NullPointerException If {@code strings} is null.
   */
  public static MediaType[] parse(final String ... strings) {
    if (strings == null)
      return null;

    if (strings.length == 0)
      return EMPTY_ARRAY;

    final MediaType[] mediaTypes = parse(arrayAdapter, strings, 0, 0, null, null, 0);
    if (mediaTypes == null)
      return EMPTY_ARRAY;

    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  private static boolean isValidChar(final char ch) {
    if ('0' <= ch && ch <= '9')
      return true;

    if ('a' <= ch && ch <= 'z')
      return true;

    if ('A' <= ch && ch <= 'Z')
      return true;

    if (ch == '*' || ch == '/' || ch == '_' || ch == '+' || ch == '-' || ch == '.' || ch == ' ' || ch == ';' || ch == '=' || ch == '"')
      return true;

    return false;
  }

  /**
   * Parses the specified string and returns an the corresponding
   * {@link MediaType} objects, or {@code null} if the specified string is null
   * or is empty.
   *
   * @param string The the string.
   * @return The corresponding {@link MediaType} object, or {@code null} if the
   *         specified string is null.
   */
  // FIXME: Reimplement with char-by-char algorithm
  // FIXME: What are the legal name and sub-name spec? Need to properly throw IllegalArgumentException!
  public static MediaType parse(String string) {
    if (string == null)
      throw new IllegalArgumentException(string);

    string = string.trim();
    if (string.length() == 0)
      throw new IllegalArgumentException(string);

    for (int i = 0; i < string.length(); ++i) {
      final char ch = string.charAt(i);
      if (!isValidChar(ch))
        throw new IllegalArgumentException("Illegal character '" + ch + "' at pos=" + i + " in: " + string);
    }

    int start = string.indexOf('/');
    int end = string.indexOf(';', start + 1);
    final String type;
    final String subtype;
    if (start > -1) {
      type = string.substring(0, start).trim();
      subtype = string.substring(start + 1, end > -1 ? end : string.length()).trim();
      if (subtype.chars().anyMatch(c -> c == '/'))
        throw new IllegalArgumentException(string);

      if (end == -1)
        return new MediaType(type, subtype);
    }
    else if (end == -1) {
      return new MediaType(string, null);
    }
    else {
      type = string.substring(0, end).trim();
      subtype = null;
    }

    final int len = string.length();
    start = end;
    final Map<String,String> parameters = new HashMap<>();
    do {
      int eq = string.indexOf('=', start + 1);
      final boolean hasEq = eq != -1;
      if (!hasEq)
        eq = start;

      end = Strings.indexOfUnQuoted(string, ';', eq + 1);
      if (end == -1)
        end = len;

      if (hasEq) {
        final String key = string.substring(start + 1, eq).trim();
        final String value = string.substring(eq + 1, end).trim();
        if (value.length() > 0)
          parameters.put(key, value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"' ? value.substring(1, value.length() - 1) : value);
      }
    }
    while ((start = end) < len - 1);
    return new MediaType(type, subtype, parameters);
  }

  /**
   * Returns the string representation of the specified {@link MediaType}.
   *
   * @param mediaType The {@link MediaType}.
   * @return The string representation of the specified {@link MediaType}.
   * @throws NullPointerException If {@code mediaType} is null.
   */
  public static String toString(final MediaType mediaType) {
    final StringBuilder builder = new StringBuilder();
    builder.append(mediaType.getType()).append('/').append(mediaType.getSubtype());
    if (mediaType.getParameters() != null) {
      for (final Map.Entry<String,String> entry : mediaType.getParameters().entrySet()) {
        final String value = entry.getValue();
        boolean quoted = false;
        for (int i = 0, len = value.length(); i < len; ++i) {
          final char ch = value.charAt(i);
          if (ch == ' ' || ch == ';' || ch == '"') {
            quoted = true;
            break;
          }
        }

        builder.append(';').append(entry.getKey()).append('=');
        if (quoted)
          builder.append('"').append(entry.getValue()).append('"');
        else
          builder.append(entry.getValue());
      }
    }

    return builder.toString();
  }

  private MediaTypes() {
  }
}