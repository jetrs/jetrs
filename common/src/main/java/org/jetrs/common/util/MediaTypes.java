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

package org.jetrs.common.util;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.libj.util.Numbers;
import org.libj.util.Strings;

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
   * <i><b>Note:</b> The {@link MediaType} arrays must be sorted on the "q"
   * parameter in descending order.</i>
   * <p>
   * <i><b>Note:</b> A null {@link MediaType} is considered to be equivalent to
   * {@link MediaType#WILDCARD}.</i>
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
   * <i><b>Note:</b> The provided {@link MediaType} array must be sorted on the
   * "q" parameter in descending order.</i>
   * <p>
   * <i><b>Note:</b> A null {@link MediaType} is considered to be equivalent to
   * {@link MediaType#WILDCARD}.</i>
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
   * <i><b>Note:</b> A null {@link MediaType} is considered to be equivalent to
   * {@link MediaType#WILDCARD}.</i>
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

    final Map<String,String> parameters = new HashMap<>();
    final Iterator<Map.Entry<String,String>> iterator1 = mediaType1.getParameters().entrySet().iterator();
    final Iterator<Map.Entry<String,String>> iterator2 = mediaType2.getParameters().entrySet().iterator();

    boolean next1 = true;
    boolean next2 = true;
    Map.Entry<String,String> entry1 = null;
    Map.Entry<String,String> entry2 = null;
    while (iterator1.hasNext() || iterator2.hasNext()) {
      if (next1)
        entry1 = iterator1.hasNext() ? iterator1.next() : null;

      if (entry1 != null && "q".equalsIgnoreCase(entry1.getKey())) {
        entry1 = null;
        continue;
      }

      next1 = false;
      if (next2)
        entry2 = iterator2.hasNext() ? iterator2.next() : null;

      if (entry2 != null && "q".equalsIgnoreCase(entry2.getKey())) {
        entry2 = null;
        continue;
      }

      next2 = false;
      final int comparison = entry1 == null ? 1 : entry2 == null ? -1 : entry1.getKey().toLowerCase().compareTo(entry2.getKey().toLowerCase());
      if (comparison < 0) {
        if (entry1 != null)
          parameters.put(entry1.getKey(), entry1.getValue());

        next1 = true;
        continue;
      }

      if (entry2 != null)
        parameters.put(entry2.getKey(), entry2.getValue());

      if (comparison > 0) {
        next2 = true;
        continue;
      }

      if (!entry1.getValue().equalsIgnoreCase(entry2.getValue()))
        return null;

      next1 = true;
      next2 = true;
    }

    if (entry1 != null)
      parameters.put(entry1.getKey(), entry1.getValue());

    if (entry2 != null)
      parameters.put(entry2.getKey(), entry2.getValue());

    return new MediaType(type, subType, parameters);
  }

  public static MediaType[] parse(final Collection<String> strings) throws ParseException {
    if (strings == null)
      return null;

    final MediaType[] mediaTypes = parse(strings.iterator(), 0);
    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  private static MediaType[] parse(final Iterator<String> iterator, final int depth) throws ParseException {
    if (!iterator.hasNext())
      return new MediaType[depth];

    final String[] parts = iterator.next().split(",");
    final MediaType[] mediaTypes = parse(iterator, depth + parts.length);
    for (int i = 0; i < parts.length; ++i)
      mediaTypes[depth + i] = parse(parts[i]);

    return mediaTypes;
  }

  /**
   * Parses the specified enumeration of strings and returns an array of
   * {@link MediaType} objects.
   *
   * @param enumeration The enumeration of strings.
   * @return An array of {@link MediaType} objects.
   * @throws ParseException If a parse error has occurred.
   * @throws NullPointerException If {@code enumeration} is null.
   */
  public static MediaType[] parse(final Enumeration<String> enumeration) throws ParseException {
    final MediaType[] mediaTypes = parse(enumeration, 0);
    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  private static MediaType[] parse(final Enumeration<String> enumeration, final int depth) throws ParseException {
    if (!enumeration.hasMoreElements())
      return new MediaType[depth];

    final String[] parts = enumeration.nextElement().split(",");
    final MediaType[] mediaTypes = parse(enumeration, depth + parts.length);
    for (int i = 0; i < parts.length; ++i)
      mediaTypes[depth + i] = parse(parts[i]);

    return mediaTypes;
  }

  /**
   * Parses the specified array of strings and returns an array of
   * {@link MediaType} objects.
   *
   * @param strings The the strings array.
   * @return An array of {@link MediaType} objects.
   * @throws ParseException If a parse error has occurred.
   * @throws NullPointerException If {@code strings} is null.
   */
  public static MediaType[] parse(final String ... strings) throws ParseException {
    final MediaType[] mediaTypes = parse(strings, 0, 0);
    Arrays.sort(mediaTypes, qComparator);
    return mediaTypes;
  }

  private static MediaType[] parse(final String[] strings, int index, final int depth) throws ParseException {
    if (index == strings.length)
      return new MediaType[depth];

    final String[] parts = strings[index].split(",");
    final MediaType[] mediaTypes = parse(strings, index + 1, depth + parts.length);
    for (int i = 0; i < parts.length; ++i)
      mediaTypes[depth + i] = parse(parts[i]);

    return mediaTypes;
  }

  /**
   * Parses the specified string and returns an the corresponding
   * {@link MediaType} objects, or {@code null} if the specified string is null.
   *
   * @param string The the string.
   * @return The corresponding {@link MediaType} object, or {@code null} if the
   *         specified string is null.
   * @throws ParseException If a parse error has occurred.
   */
  public static MediaType parse(String string) throws ParseException {
    if (string == null)
      return null;

    string = string.trim();
    int start = string.indexOf('/');
    if (start == -1)
      return new MediaType(string.trim(), null);

    int end = string.indexOf(';', start + 2);
    final String type = string.substring(0, start).trim();
    final String subtype = string.substring(start + 1, end > -1 ? end : string.length()).trim();
    if (end < 0)
      return new MediaType(type, subtype);

    final int len = string.length();
    start = end;
    final Map<String,String> parameters = new HashMap<>();
    do {
      final int eq = string.indexOf('=', start + 2);
      if (eq == -1)
        throw new ParseException("Unable to parse parameter: " + string, start);

      final String key = string.substring(start + 1, eq).trim();
      end = Strings.indexOfUnQuoted(string, ';', eq + 1);
      if (end == -1)
        end = len;

      final String value = string.substring(eq + 1, end).trim();
      parameters.put(key, value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"' ? value.substring(1, value.length() - 1) : value);
      start = end;
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