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

package org.jetrs;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.libj.lang.Numbers;
import org.libj.lang.Strings;
import org.libj.util.CollectionUtil;

/**
 * Utility class with convenience functions for the {@link MediaType} class.
 *
 * @see <a href= "https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7">rfc2616</a>
 * @see <a href= "https://datatracker.ietf.org/doc/html/rfc6838#section-4.2.8">Structured Syntax Name Suffixes</a>
 * @see <a href= "https://datatracker.ietf.org/doc/html/draft-ietf-appsawg-media-type-regs-14#section-6">Structured Syntax Suffix
 *      Registration Procedures</a>
 * @see <a href= "https://datatracker.ietf.org/doc/html/draft-ietf-appsawg-media-type-suffix-regs-02#section-3.1">The +json
 *      Structured Syntax Suffix</a>
 */
public final class MediaTypes {
  static final ServerMediaType[] OCTET_SERVER_TYPE = {ServerMediaType.APPLICATION_OCTET_STREAM_TYPE};
  static final CompatibleMediaType[] WILDCARD_COMPATIBLE_TYPE = {CompatibleMediaType.WILDCARD_TYPE};
  static final ServerMediaType[] WILDCARD_SERVER_TYPE = {ServerMediaType.WILDCARD_TYPE};
  static final MediaType[] WILDCARD_TYPE = {MediaType.WILDCARD_TYPE};

  static final Comparator<MediaType> QUALITY_COMPARATOR = (final MediaType o1, final MediaType o2) -> {
    if (o1 == null)
      return o2 == null ? 0 : 1;

    if (o2 == null)
      return -1;

    if (o1.isWildcardType()) {
      if (!o2.isWildcardType())
        return 1;

      if (o1.isWildcardSubtype()) {
        if (!o2.isWildcardSubtype())
          return 1;
      }
      else if (o2.isWildcardSubtype()) {
        return -1;
      }
    }
    else if (o2.isWildcardType()) {
      return -1;
    }

    final double q1 = getQuality("q", o1);
    final double q2 = getQuality("q", o2);
    if (q1 > q2)
      return -1;

    if (q1 < q2)
      return 1;

    final double qs1 = getQuality("qs", o1);
    final double qs2 = getQuality("qs", o2);
    return qs1 > qs2 ? -1 : qs1 < qs2 ? 1 : 0;
  };

  private static double getQuality(final String key, final MediaType mediaType) {
    final String q = mediaType.getParameters().get(key);
    return q == null ? 1 : Numbers.parseDouble(q, 1);
  }

  private static HashMap<String,String> mergeParameters(final ServerMediaType serverType, final MediaType clientType) {
    final HashMap<String,String> parameters = new HashMap<>();
    parameters.putAll(serverType.getParameters());
    parameters.putAll(clientType.getParameters());
    return parameters;
  }

  private static boolean isCharsetMatched(final ServerMediaType serverType, final MediaType clientType, final List<String> acceptCharsets) {
    final String serverCharset = serverType.getParameters().get("charset");
    if (serverCharset == null)
      return true;

    final String clientCharset = clientType.getParameters().get("charset");
    if (clientCharset != null)
      return serverCharset.equals(clientCharset);

    final int i$;
    if (acceptCharsets == null || (i$ = acceptCharsets.size()) == 0)
      return true;

    if (CollectionUtil.isRandomAccess(acceptCharsets)) {
      int i = 0; do // [RA]
        if (acceptCharsets.get(i).equalsIgnoreCase(serverCharset))
          return true;
      while (++i < i$);
    }
    else {
      final Iterator<String> it = acceptCharsets.iterator(); do // [I]
        if (it.next().equalsIgnoreCase(serverCharset))
          return true;
      while (it.hasNext());
    }

    return false;
  }

  /**
   * Returns an array of {@link CompatibleMediaType}s by evaluating the provided {@link ServerMediaType}s, {@link MediaType}s and
   * "Accept-Charset" header values for compatibility.
   *
   * @implNote The "Accept-Charset" header values are only considered if a {@code charset} parameter is not present on the provided
   *           {@link ServerMediaType}s.
   * @param serverTypes The {@link ServerMediaType}s sorted on the "qs" parameter in descending order.
   * @param clientTypes The client {@link MediaType}s sorted on the "q" parameter in descending order.
   * @param acceptCharsets Value of "Accept-Charsets" header, or {@code null} if no such header was provided.
   * @return An array of {@link CompatibleMediaType}s by evaluating the provided {@link ServerMediaType}s, {@link MediaType}s and
   *         "Accept-Charset" header values for compatibility.
   * @throws NullPointerException If {@code serverTypes}, any member of {@code serverTypes}, {@code clientTypes}, or any member
   *           of {@code clientTypes} is null.
   */
  static CompatibleMediaType[] getCompatible(final ServerMediaType[] serverTypes, final MediaType[] clientTypes, final List<String> acceptCharsets) {
    final CompatibleMediaType[] mediaTypes = getCompatible(serverTypes, clientTypes, acceptCharsets, 0, 0, 0);
    Arrays.sort(mediaTypes, QUALITY_COMPARATOR);
    return mediaTypes;
  }

  private static CompatibleMediaType[] getCompatible(final ServerMediaType[] serverTypes, final MediaType[] clientTypes, final List<String> acceptCharsets, int index1, final int index2, final int depth) {
    if (index2 == clientTypes.length) {
      if (++index1 == serverTypes.length)
        return depth == 0 ? WILDCARD_COMPATIBLE_TYPE : new CompatibleMediaType[depth];

      return getCompatible(serverTypes, clientTypes, acceptCharsets, index1, 0, depth);
    }

    final ServerMediaType serverType = serverTypes[index1];
    final MediaType clientType = clientTypes[index2];
    final CompatibleMediaType compatibleType = getCompatible(serverType, clientType, acceptCharsets);
    if (compatibleType == null)
      return getCompatible(serverTypes, clientTypes, acceptCharsets, index1, index2 + 1, depth);

    final CompatibleMediaType[] compatibleTypes = getCompatible(serverTypes, clientTypes, acceptCharsets, index1, index2 + 1, depth + 1);
    compatibleTypes[depth] = compatibleType;
    return compatibleTypes;
  }

  /**
   * Returns an array of {@link CompatibleMediaType}s by evaluating the provided {@link ServerMediaType}s, {@link MediaType}s and
   * "Accept-Charset" header values for compatibility.
   *
   * @implNote The "Accept-Charset" header values are only considered if a {@code charset} parameter is not present on the provided
   *           {@link ServerMediaType}s.
   * @param serverTypes The {@link ServerMediaType}s sorted on the "qs" parameter in descending order.
   * @param clientTypes The client {@link MediaType}s sorted on the "q" parameter in descending order.
   * @param acceptCharsets Value of "Accept-Charsets" header, or {@code null} if no such header was provided.
   * @return An array of {@link CompatibleMediaType}s by evaluating the provided {@link ServerMediaType}s, {@link MediaType}s and
   *         "Accept-Charset" header values for compatibility.
   * @throws NullPointerException If {@code serverTypes}, any member of {@code serverTypes}, {@code clientTypes}, or any member
   *           of {@code clientTypes} is null.
   */
  static CompatibleMediaType[] getCompatible(final ServerMediaType[] serverTypes, final List<MediaType> clientTypes, final List<String> acceptCharsets) {
    final CompatibleMediaType[] mediaTypes = getCompatible(serverTypes, clientTypes, acceptCharsets, 0, 0, 0);
    Arrays.sort(mediaTypes, QUALITY_COMPARATOR);
    return mediaTypes;
  }

  private static CompatibleMediaType[] getCompatible(final ServerMediaType[] serverTypes, final List<MediaType> clientTypes, final List<String> acceptCharsets, int index1, final int index2, final int depth) {
    if (index2 == clientTypes.size()) {
      if (++index1 == serverTypes.length)
        return depth == 0 ? WILDCARD_COMPATIBLE_TYPE : new CompatibleMediaType[depth];

      return getCompatible(serverTypes, clientTypes, acceptCharsets, index1, 0, depth);
    }

    final ServerMediaType serverType = serverTypes[index1];
    final MediaType clientType = clientTypes.get(index2);
    final CompatibleMediaType compatibleType = getCompatible(serverType, clientType, acceptCharsets);
    if (compatibleType == null)
      return getCompatible(serverTypes, clientTypes, acceptCharsets, index1, index2 + 1, depth);

    final CompatibleMediaType[] compatibleTypes = getCompatible(serverTypes, clientTypes, acceptCharsets, index1, index2 + 1, depth + 1);
    compatibleTypes[depth] = compatibleType;
    return compatibleTypes;
  }

  /**
   * Returns an array of {@link CompatibleMediaType}s by evaluating the provided {@link ServerMediaType}s, {@link MediaType} and
   * "Accept-Charset" header values for compatibility.
   *
   * @implNote The "Accept-Charset" header values are only considered if a {@code charset} parameter is not present on the provided
   *           {@link ServerMediaType}.
   * @param serverTypes The {@link ServerMediaType}s sorted on the "qs" parameter in descending order.
   * @param clientType The client {@link MediaType}.
   * @param acceptCharsets Value of "Accept-Charsets" header, or {@code null} if no such header was provided.
   * @return An array of {@link CompatibleMediaType}s by evaluating the provided {@link ServerMediaType}s, {@link MediaType} and
   *         "Accept-Charset" header values for compatibility.
   * @throws NullPointerException If {@code serverTypes}, any member of {@code serverTypes}, or {@code clientType} is null.
   */
  static CompatibleMediaType[] getCompatible(final ServerMediaType[] serverTypes, final MediaType clientType, final List<String> acceptCharsets) {
    final CompatibleMediaType[] mediaTypes = getCompatible(serverTypes, clientType, acceptCharsets, serverTypes.length, 0, 0);
    Arrays.sort(mediaTypes, QUALITY_COMPARATOR);
    return mediaTypes;
  }

  private static CompatibleMediaType[] getCompatible(final ServerMediaType[] serverTypes, final MediaType clientType, final List<String> acceptCharsets, final int length, final int index, final int depth) {
    if (index == length)
      return depth == 0 ? WILDCARD_COMPATIBLE_TYPE : new CompatibleMediaType[depth];

    final ServerMediaType serverType = serverTypes[index];
    final CompatibleMediaType compatibleType = getCompatible(serverType, clientType, acceptCharsets);
    if (compatibleType == null)
      return getCompatible(serverTypes, clientType, acceptCharsets, length, index + 1, depth);

    final CompatibleMediaType[] compatibleTypes = getCompatible(serverTypes, clientType, acceptCharsets, length, index + 1, depth + 1);
    compatibleTypes[depth] = compatibleType;
    return compatibleTypes;
  }

  /**
   * Returns an array of {@link CompatibleMediaType}s by evaluating the provided {@link ServerMediaType}s, {@link MediaType} and
   * "Accept-Charset" header values for compatibility.
   *
   * @implNote The "Accept-Charset" header values are only considered if a {@code charset} parameter is not present on the provided
   *           {@link ServerMediaType}.
   * @param serverTypes The {@link ServerMediaType}s sorted on the "qs" parameter in descending order.
   * @param clientType The client {@link MediaType}.
   * @param acceptCharsets Value of "Accept-Charsets" header, or {@code null} if no such header was provided.
   * @return An array of {@link CompatibleMediaType}s by evaluating the provided {@link ServerMediaType}s, {@link MediaType} and
   *         "Accept-Charset" header values for compatibility.
   * @throws NullPointerException If {@code serverTypes}, any member of {@code serverTypes}, or {@code clientType} is null.
   */
  static CompatibleMediaType[] getCompatible(final List<ServerMediaType> serverTypes, final MediaType clientType, final List<String> acceptCharsets) {
    final CompatibleMediaType[] mediaTypes = getCompatible(serverTypes, clientType, acceptCharsets, serverTypes.size(), 0, 0);
    Arrays.sort(mediaTypes, QUALITY_COMPARATOR);
    return mediaTypes;
  }

  private static CompatibleMediaType[] getCompatible(final List<ServerMediaType> serverTypes, final MediaType clientType, final List<String> acceptCharsets, final int length, final int index, final int depth) {
    if (index == length)
      return depth == 0 ? WILDCARD_COMPATIBLE_TYPE : new CompatibleMediaType[depth];

    final ServerMediaType serverType = serverTypes.get(index);
    final CompatibleMediaType compatibleType = getCompatible(serverType, clientType, acceptCharsets);
    if (compatibleType == null)
      return getCompatible(serverTypes, clientType, acceptCharsets, length, index + 1, depth);

    final CompatibleMediaType[] compatibleTypes = getCompatible(serverTypes, clientType, acceptCharsets, length, index + 1, depth + 1);
    compatibleTypes[depth] = compatibleType;
    return compatibleTypes;
  }

  private static String getCompatibleSubType(final String subType1, final String subType2) {
    if (subType1.equals(subType2))
      return subType1;

    final int p1 = subType1.indexOf('+');
    final int p2 = subType2.indexOf('+');
    if (p1 < 1 && p2 < 1)
      return null;

    if (p1 == 1 && subType1.charAt(0) == '*')
      return subType2;

    if (p2 == 1 && subType2.charAt(0) == '*')
      return subType1;

    if (p1 > 0 && subType2.equals(subType1.substring(p1 + 1)))
      return subType1;

    if (p2 > 0 && subType1.equals(subType2.substring(p2 + 1)))
      return subType2;

    return null;
  }

  /**
   * Returns a {@link CompatibleMediaType} by evaluating the provided {@link ServerMediaType}, {@link MediaType} and
   * "Accept-Charset" header values for compatibility, or {@code null} if the arguments are not compatible.
   *
   * @implNote The "Accept-Charset" header values are only considered if a {@code charset} parameter is not present on the provided
   *           {@link ServerMediaType}.
   * @param serverType The {@link ServerMediaType}.
   * @param clientType The client {@link MediaType}.
   * @param acceptCharsets Value of "Accept-Charsets" header, or {@code null} if no such header was provided.
   * @return A {@link CompatibleMediaType} by evaluating the provided {@link ServerMediaType}, {@link MediaType} and
   *         "Accept-Charset" header values for compatibility, or {@code null} if the arguments are not compatible.
   * @throws NullPointerException If {@code serverType} or {@code clientType} is null.
   */
  static CompatibleMediaType getCompatible(final ServerMediaType serverType, final MediaType clientType, final List<String> acceptCharsets) {
    if (serverType.isWildcardType()) {
      // {*/?}+{?/?}
      if (clientType.isWildcardType()) {
        // {*/?}+{*/?}
        if (serverType.isWildcardSubtype()) {
          // {*/*}+{*/?}
          if (clientType.isWildcardSubtype())
            // {*/*}+{*/*}
            return CompatibleMediaType.WILDCARD_TYPE;

          // {*/*}+{*/s}
          return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(clientType, mergeParameters(serverType, clientType), 1);
        }
        else if (clientType.isWildcardSubtype()) {
          // {*/s}+{*/*}
          return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType, mergeParameters(serverType, clientType), 1);
        }

        // {*/s}+{*/s}
        final String subType = getCompatibleSubType(serverType.getSubtype(), clientType.getSubtype());
        if (subType != null)
          return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType.getType(), subType, mergeParameters(serverType, clientType), 0);

        return null;
      }
      else if (serverType.isWildcardSubtype()) {
        // {*/*}+{s/?}
        if (clientType.isWildcardSubtype())
          // {*/*}+{s/*}
          return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(clientType.getType(), serverType.getSubtype(), 1);

        // {*/*}+{s/s}
        return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(clientType, mergeParameters(serverType, clientType), 2);
      }

      // {*/s}+{s/s}
      return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(clientType.getType(), serverType.getSubtype(), mergeParameters(serverType, clientType), 2);
    }
    else if (clientType.isWildcardType()) {
      // {s/?}+{*/?}
      if (serverType.isWildcardSubtype()) {
        // {s/*}+{*/?}
        if (clientType.isWildcardSubtype())
          // {s/*}+{*/*}
          return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType, mergeParameters(serverType, clientType), 1);

        // {s/*}+{*/s}
        return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType.getType(), clientType.getSubtype(), mergeParameters(serverType, clientType), 2);
      }
      else if (clientType.isWildcardSubtype()) {
        // {s/s}+{*/*}
        return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType, mergeParameters(serverType, clientType), 2);
      }

      // {s/s}+{*/s}
      final String subType = getCompatibleSubType(serverType.getSubtype(), clientType.getSubtype());
      if (subType != null)
        return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType.getType(), subType, mergeParameters(serverType, clientType), 1);

      return null;
    }

    if (!serverType.getType().equals(clientType.getType()))
      return null;

    // {s/?}+{s/?}
    if (serverType.isWildcardSubtype()) {
      // {s/*}+{s/?}
      if (clientType.isWildcardSubtype())
        // {s/*}+{s/*}
        return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType, mergeParameters(serverType, clientType), 0);

      // {s/*}+{s/s}
      return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType.getType(), clientType.getSubtype(), mergeParameters(serverType, clientType), 1);
    }
    else if (clientType.isWildcardSubtype()) {
      // {s/s}+{s/*}
      return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType, mergeParameters(serverType, clientType), 1);
    }

    // {s/s}+{s/s}
    final String subType = getCompatibleSubType(serverType.getSubtype(), clientType.getSubtype());
    if (subType != null)
      return !isCharsetMatched(serverType, clientType, acceptCharsets) ? null : new CompatibleMediaType(serverType.getType(), subType, mergeParameters(serverType, clientType), 0);

    return null;
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

  @SuppressWarnings("unchecked")
  private static <T extends QualifiedMediaType,S>T[] parse(final Class<T> cls, final Adapter<S> adapter, final S source, int index, final int depth, StringBuilder builder, String header, int start) {
    T mediaType = null;
    do {
      if (header == null) {
        if (adapter.hasNext(source, index)) {
          header = adapter.next(source, index++);
          start = 0;
          if (header == null || header.length() == 0)
            return parse(cls, adapter, source, index, depth, builder, null, 0);
        }
        else {
          return (T[])(depth == 0 ? null : cls == ServerMediaType.class ? new ServerMediaType[depth] : cls == QualifiedMediaType.class ? new QualifiedMediaType[depth] : new MediaType[depth]);
        }
      }

      for (int i = start, i$ = header.length(); i <= i$; ++i) { // [N]
        final char ch;
        if (i == i$ || (ch = header.charAt(i)) == ',') {
          if ((start = i + 1) >= i$)
            header = null;

          if (builder != null && builder.length() != 0) {
            mediaType = parse(cls, builder.toString());
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

    final T[] mediaTypes = parse(cls, adapter, source, index, depth + 1, builder, header, start);
    mediaTypes[depth] = mediaType;

    return mediaTypes;
  }

  static final ServerMediaType[] EMPTY_SERVER_TYPE = {};
  private static final QualifiedMediaType[] EMPTY_QUALIFIED_TYPE = {};
  private static final MediaType[] EMPTY_MEDIA_TYPE = {};

  @SuppressWarnings("unchecked")
  private static final <T extends MediaType>T[] empty(final Class<T> cls) {
    return (T[])(cls == ServerMediaType.class ? EMPTY_SERVER_TYPE : cls == QualifiedMediaType.class ? EMPTY_QUALIFIED_TYPE : EMPTY_MEDIA_TYPE);
  }

  /**
   * Parses the specified collection of strings and returns an array of {@link MediaType} objects.
   *
   * @param strings The collection of strings.
   * @return An array of {@link MediaType} objects.
   */
  static QualifiedMediaType[] parse(final Collection<String> strings) {
    return parse(QualifiedMediaType.class, strings);
  }

  /**
   * Parses the specified collection of strings and returns an array of {@link MediaType} objects.
   *
   * @param <T> The type of the {@link MediaType} to be returned.
   * @param cls The {@link Class} of the {@link MediaType} to be returned.
   * @param strings The collection of strings.
   * @return An array of {@link MediaType} objects.
   */
  static <T extends QualifiedMediaType>T[] parse(final Class<T> cls, final Collection<String> strings) {
    if (strings == null)
      return null;

    if (strings.size() == 0)
      return empty(cls);

    final T[] mediaTypes = parse(cls, iteratorAdapter, strings.iterator(), -1, 0, null, null, 0);
    if (mediaTypes == null)
      return empty(cls);

    Arrays.sort(mediaTypes, QUALITY_COMPARATOR);
    return mediaTypes;
  }

  /**
   * Parses the specified enumeration of strings and returns an array of {@link MediaType} objects.
   *
   * @param enumeration The enumeration of strings.
   * @return An array of {@link MediaType} objects.
   */
  static MediaType[] parse(final Enumeration<String> enumeration) {
    return parse(QualifiedMediaType.class, enumeration);
  }

  /**
   * Parses the specified enumeration of strings and returns an array of {@link MediaType} objects.
   *
   * @param <T> The type of the {@link MediaType} to be returned.
   * @param cls The {@link Class} of the {@link MediaType} to be returned.
   * @param enumeration The enumeration of strings.
   * @return An array of {@link MediaType} objects.
   */
  static <T extends QualifiedMediaType>T[] parse(final Class<T> cls, final Enumeration<String> enumeration) {
    if (enumeration == null)
      return null;

    if (!enumeration.hasMoreElements())
      return empty(cls);

    final T[] mediaTypes = parse(cls, enumerationAdapter, enumeration, -1, 0, null, null, 0);
    if (mediaTypes == null)
      return empty(cls);

    Arrays.sort(mediaTypes, QUALITY_COMPARATOR);
    return mediaTypes;
  }

  /**
   * Parses the specified array of strings and returns an array of {@link MediaType} objects.
   *
   * @param strings The the strings array.
   * @return An array of {@link MediaType} objects.
   */
  static MediaType[] parse(final String ... strings) {
    return parse(QualifiedMediaType.class, strings);
  }

  /**
   * Parses the specified array of strings and returns an array of {@link MediaType} objects.
   *
   * @param <T> The type of the {@link MediaType} to be returned.
   * @param cls The {@link Class} of the {@link MediaType} to be returned.
   * @param strings The the strings array.
   * @return An array of {@link MediaType} objects.
   */
  static <T extends QualifiedMediaType>T[] parse(final Class<T> cls, final String ... strings) {
    if (strings == null)
      return null;

    if (strings.length == 0)
      return empty(cls);

    final T[] mediaTypes = parse(cls, arrayAdapter, strings, 0, 0, null, null, 0);
    if (mediaTypes == null)
      return empty(cls);

    Arrays.sort(mediaTypes, QUALITY_COMPARATOR);
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
   * Parses the specified string and returns the corresponding {@link MediaType} objects, or {@code null} if the specified string is
   * null or is empty.
   *
   * @param string The the string.
   * @return The corresponding {@link MediaType} object, or {@code null} if the specified string is null.
   */
  static QualifiedMediaType parse(final String string) {
    return parse(QualifiedMediaType.class, string);
  }

  /**
   * Parses the specified string and returns the corresponding {@link MediaType} objects, or {@code null} if the specified string is
   * null or is empty.
   *
   * @param <T> The type of the {@link MediaType} to be returned.
   * @param cls The {@link Class} of the {@link MediaType} to be returned.
   * @param string The the string.
   * @return The corresponding {@link MediaType} object, or {@code null} if the specified string is null.
   */
  // FIXME: Reimplement with char-by-char algorithm
  // FIXME: What are the legal name and sub-name spec? Need to properly throw IllegalArgumentException!
  @SuppressWarnings("unchecked")
  static <T extends QualifiedMediaType>T parse(final Class<T> cls, String string) {
    string = string.trim();
    if (string.length() == 0)
      throw new IllegalArgumentException(string);

    for (int i = 0, i$ = string.length(); i < i$; ++i) { // [N]
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
        return (T)(cls == ServerMediaType.class ? new ServerMediaType(type, subtype) : cls == QualifiedMediaType.class ? new QualifiedMediaType(type, subtype) : new MediaType(type, subtype));
    }
    else if (end == -1) {
      return (T)(cls == ServerMediaType.class ? new ServerMediaType(string, null) : cls == QualifiedMediaType.class ? new QualifiedMediaType(string, null) : new MediaType(string, null));
    }
    else {
      type = string.substring(0, end).trim();
      subtype = null;
    }

    final int len = string.length();
    start = end;
    final HashMap<String,String> parameters = new HashMap<>();
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

    return (T)(cls == ServerMediaType.class ? new ServerMediaType(type, subtype, parameters) : cls == QualifiedMediaType.class ? new QualifiedMediaType(type, subtype, parameters) : new QualifiedMediaType(type, subtype, parameters));
  }

  static MediaType cloneWithoutParameters(final MediaType mediaType) {
    return new MediaType(mediaType.getType(), mediaType.getSubtype());
  }

  /**
   * Returns the string representation of the specified {@link MediaType}.
   *
   * @param mediaType The {@link MediaType}.
   * @return The string representation of the specified {@link MediaType}.
   * @throws NullPointerException If {@code mediaType} is null.
   */
  static String toString(final MediaType mediaType) {
    final StringBuilder builder = new StringBuilder();
    builder.append(mediaType.getType()).append('/').append(mediaType.getSubtype());
    if (mediaType.getParameters().size() > 0) {
      for (final Map.Entry<String,String> entry : mediaType.getParameters().entrySet()) { // [S]
        final String value = entry.getValue();
        boolean quoted = false;
        for (int i = 0, i$ = value.length(); i < i$; ++i) { // [N]
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

  /**
   * Returns the {@link Charset} specified in the provided {@link MediaType}, set by the media type parameter "charset".
   * <p>
   * If the provided {@link MediaType} is null, or the parameter is not present, the {@link StandardCharsets#UTF_8} charset is
   * returned.
   *
   * @param mediaType The {@link MediaType}.
   * @return The {@link Charset} specified in the provided {@link MediaType}.
   */
  public static Charset getCharset(final MediaType mediaType) {
    if (mediaType == null)
      return StandardCharsets.UTF_8;

    final String name = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
    return name == null ? StandardCharsets.UTF_8 : Charset.forName(name);
  }

  private MediaTypes() {
  }
}