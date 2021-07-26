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

package org.jetrs.common.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.libj.lang.ArrayCharSequence;
import org.libj.net.URLs;

final class UriEncoder {
  static final UriEncoder QUERY;
  static final UriEncoder QUERY_PARAM;
  static final UriEncoder MATRIX;
  static final UriEncoder PATH;
  static final UriEncoder PATH_SEGMENT;

  private static final String REPLACEMENT_PARAM = "_uri_parameter";
  static final Pattern PARAM_REPLACEMENT = Pattern.compile(REPLACEMENT_PARAM);

  static {
    final String[] pathEncoding = new String[128];
    final String[] pathSegmentEncoding = new String[128];
    final String[] matrixParameterEncoding = new String[128];
    final String[] queryNameValueEncoding = new String[128];
    final String[] queryStringEncoding = new String[128];

    /*
     * Encode via <a href="http://ietf.org/rfc/rfc3986.txt">RFC 3986</a>. PCHAR
     * is allowed along with '/' unreserved = ALPHA / DIGIT / "-" / "." / "_" /
     * "~" sub-delims = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," /
     * ";" / "=" pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
     */
    for (int i = 0; i < 128; ++i) {
      if ('a' <= i && i <= 'z' || 'A' <= i && i <= 'Z' || '0' <= i && i <= '9')
        continue;

      switch (i) {
        case '-':
        case '.':
        case '_':
        case '~':
        case '!':
        case '$':
        case '&':
        case '\'':
        case '(':
        case ')':
        case '*':
        case '+':
        case ',':
        case '/':
        case ';':
        case '=':
        case ':':
        case '@':
          continue;
      }

      pathEncoding[i] = URLs.encode(String.valueOf((char)i));
    }

    pathEncoding[' '] = "%20";
    System.arraycopy(pathEncoding, 0, matrixParameterEncoding, 0, pathEncoding.length);
    matrixParameterEncoding[';'] = "%3B";
    matrixParameterEncoding['='] = "%3D";
    matrixParameterEncoding['/'] = "%2F";
    System.arraycopy(pathEncoding, 0, pathSegmentEncoding, 0, pathEncoding.length);
    pathSegmentEncoding['/'] = "%2F";
    /*
     * Encode via <a href="http://ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~" space encoded as '+'
     */
    for (int i = 0; i < 128; ++i) {
      if ('a' <= i && i <= 'z' || 'A' <= i && i <= 'Z' || '0' <= i && i <= '9')
        continue;

      switch (i) {
        case '-':
        case '.':
        case '_':
        case '~':
        case '?':
          continue;
        case ' ':
          queryNameValueEncoding[i] = "+";
          continue;
      }

      queryNameValueEncoding[i] = URLs.encode(String.valueOf((char)i));
    }

    // query = *( pchar / "/" / "?" )
    for (int i = 0; i < 128; ++i) {
      if ('a' <= i && i <= 'z' || 'A' <= i && i <= 'Z' || '0' <= i && i <= '9')
        continue;

      switch ((char)i) {
        case '-':
        case '.':
        case '_':
        case '~':
        case '!':
        case '$':
        case '&':
        case '\'':
        case '(':
        case ')':
        case '*':
        case '+':
        case ',':
        case ';':
        case '=':
        case ':':
        case '@':
        case '?':
        case '/':
          continue;
        case ' ':
          queryStringEncoding[i] = "%20";
          continue;
      }

      queryStringEncoding[i] = URLs.encode(String.valueOf((char)i));
    }

    PATH = new UriEncoder(pathEncoding);
    MATRIX = new UriEncoder(matrixParameterEncoding);
    QUERY = new UriEncoder(queryStringEncoding);
    QUERY_PARAM = new UriEncoder(queryNameValueEncoding);
    PATH_SEGMENT = new UriEncoder(pathSegmentEncoding);
  }

  // private static final Pattern nonCodes = Pattern.compile("%([^a-fA-F0-9]|$)");
  private static final Pattern nonCodes = Pattern.compile("%([^a-fA-F0-9]|[a-fA-F0-9]$|$|[a-fA-F0-9][^a-fA-F0-9])");

  /**
   * Encode '%' if it is not an encoding sequence
   *
   * @param string value to encode
   * @return encoded value
   */
  private static String encodeNonCodes(final String string) {
    final Matcher matcher = nonCodes.matcher(string);
    final StringBuilder builder = new StringBuilder();

    // FYI: we do not use the no-arg matcher.find()
    // coupled with matcher.appendReplacement()
    // because the matched text may contain
    // a second % and we must make sure we
    // encode it (if necessary).
    int index = 0;
    for (int start; matcher.find(index); index = start + 1)
      builder.append(string, index, start = matcher.start()).append("%25");

    builder.append(string.substring(index));
    return builder.toString();
  }

  static boolean savePathParams(final String segmentString, final StringBuilder newSegment, final List<? super String> params) {
    boolean foundParam = false;
    // Regular expressions can have '{' and '}' characters. Replace them to do
    // match
    final CharSequence segment = replaceBraces(segmentString);
    final Matcher matcher = URI_TEMPLATE_PATTERN.matcher(segment);
    int start = 0;
    for (; matcher.find(); start = matcher.end()) {
      newSegment.append(segment, start, matcher.start());
      foundParam = true;
      // Regular expressions can have '{' and '}' characters. Recover earlier
      // replacement
      params.add(recoverBraces(matcher.group()));
      newSegment.append(REPLACEMENT_PARAM);
    }

    newSegment.append(segment, start, segment.length());
    return foundParam;
  }

  /**
   * Keep encoded values "%..." and template parameters intact i.e. "{x}"
   *
   * @param segment value to encode
   * @param encoding encoding
   * @return encoded value
   */
  private static String encodeValue(String segment, final String[] encoding) {
    final ArrayList<String> params = new ArrayList<>();
    boolean foundParam = false;
    final StringBuilder newSegment = new StringBuilder();
    if (savePathParams(segment, newSegment, params)) {
      foundParam = true;
      segment = newSegment.toString();
    }

    segment = encodeNonCodes(encodeFromArray(segment, encoding, false));
    if (foundParam)
      segment = pathParamReplacement(segment, params);

    return segment;
  }

  private static String encodeFromArray(final String segment, final String[] encodingMap, final boolean encodePercent) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < segment.length(); ++i) {
      final char ch = segment.charAt(i);
      if (encodePercent || ch != '%') {
        final String encoding = encode(ch, encodingMap);
        builder.append(encoding != null ? encoding : ch);
      }
      else {
        builder.append(ch);
      }
    }

    return builder.toString();
  }

  /**
   * Returns the URL-encoded representation of the specified character, via the
   * provided encoding map.
   *
   * @param zhar Integer representation of character.
   * @param encodingMap Encoding map.
   * @return The URL-encoded character.
   */
  private static String encode(final int zhar, final String[] encodingMap) {
    return zhar < encodingMap.length ? encodingMap[zhar] : URLs.encode(String.valueOf((char)zhar));
  }

  private static String pathParamReplacement(final String segment, final List<String> params) {
    final StringBuilder builder = new StringBuilder();
    final Matcher matcher = PARAM_REPLACEMENT.matcher(segment);
    int start = 0;
    for (int i = 0; matcher.find(); ++i) {
      builder.append(segment, start, matcher.start());
      final String replacement = params.get(i);
      builder.append(replacement);
      start = matcher.end();
    }

    builder.append(segment, start, segment.length());
    return builder.toString();
  }

  /**
   * A regex pattern that searches for a URI template parameter in the form of
   * {*}
   */
  private static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile("(\\{([^}]+)\\})");

  private static final char openCurlyReplacement = 6;
  private static final char closeCurlyReplacement = 7;

  /**
   * A cheaper (memory-wise) version of replaceEnclosedCurlyBraces(String str).
   *
   * @param string input string
   * @return replaced output
   */
  @SuppressWarnings("null")
  static CharSequence replaceBraces(final String string) {
    CharSequence sequence = string;
    char[] chars = null;
    for (int i = 0, open = 0, len = string.length(); i < len; ++i) {
      if (sequence.charAt(i) == '{') {
        if (open++ != 0) {
          if (sequence == string) {
            chars = string.toCharArray();
            sequence = new ArrayCharSequence(chars);
          }

          chars[i] = openCurlyReplacement;
        }
      }
      else if (sequence.charAt(i) == '}') {
        if (--open != 0) {
          if (sequence == string) {
            chars = string.toCharArray();
            sequence = new ArrayCharSequence(chars);
          }

          chars[i] = closeCurlyReplacement;
        }
      }
    }

    return sequence;
  }

  private static String recoverBraces(final String str) {
    return str.replace(openCurlyReplacement, '{').replace(closeCurlyReplacement, '}');
  }

  private final String[] data;

  private UriEncoder(final String[] data) {
    this.data = data;
  }

  /**
   * Keep encoded values "%..." and template parameters intact.
   *
   * @param value query string
   * @return encoded query string
   */
  String encode(final String value) {
    return encodeValue(value, data);
  }

  String encodeAsIs(final String nameOrValue) {
    return encodeFromArray(nameOrValue, data, true);
  }

  /**
   * Keep any valid encodings from string i.e. keep "%2D" but don't keep "%p"
   *
   * @param segment value to encode
   * @return encoded value
   */
  String encodeSaveEncodings(final String segment) {
    return encodeNonCodes(encodeFromArray(segment, data, false));
  }
}