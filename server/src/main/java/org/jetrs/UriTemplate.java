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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;

import org.libj.net.URIComponent;
import org.libj.util.Patterns;

class UriTemplate implements Comparable<UriTemplate> {
  static final String DEL = "ZZZ";
  private static final char ESCAPE = 'Z';
  private static final char NO_ESCAPE = '\0';
  private static final char[] ESCAPE_MAP = {
    '_', 'W',
    '.', 'X',
    '-', 'Y',
    ESCAPE, ESCAPE
  };

  private static boolean isValidPathStart(final char ch) {
    return Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == '_';
  }

  private static boolean isValidPathPart(final char ch) {
    return isValidPathStart(ch) || ch == '.' || ch == '-';
  }

  private static char getEscapeChar(final char ch) {
    if (Character.isDigit(ch) || Character.isAlphabetic(ch) && ch != ESCAPE)
      return NO_ESCAPE;

    for (int i = 0; i < ESCAPE_MAP.length; i += 2) // [N]
      if (ESCAPE_MAP[i] == ch)
        return ESCAPE_MAP[++i];

    throw new IllegalStateException("Unexpected char: '" + ch + "'");
  }

  private static String escapeName(final String uriTemplate, int i, final int end) {
    char ch = uriTemplate.charAt(i);
    if (!isValidPathStart(ch))
      throw new IllegalArgumentException("Invalid URI template start ('" + ch + "' at index " + i + "): " + uriTemplate);

    final StringBuilder b = new StringBuilder();
    for (char escape;;) { // [N]
      if ((escape = getEscapeChar(ch)) != NO_ESCAPE)
        b.append(ESCAPE).append(escape);
      else
        b.append(ch);

      if (++i == end)
        break;

      ch = uriTemplate.charAt(i);
      if (!isValidPathPart(ch))
        throw new IllegalArgumentException("Invalid URI template part ('" + ch + "' at index " + i + ": " + uriTemplate);
    }

    return b.append(DEL).append(i).toString();
  }

  static boolean addLeadingRemoveTrailing(final StringBuilder builder, final Path path) {
    final String value;
    final int i$;
    if (path == null || (i$ = (value = path.value()).length()) == 0)
      return false;

    final boolean leadingSlash = value.charAt(0) == '/';
    if (leadingSlash && i$ == 1)
      return false;

    final int trailingSlash = value.charAt(i$ - 1) == '/' ? i$ - 1 : i$;
    if (trailingSlash != i$) {
      if (leadingSlash) {
        if (i$ == 2)
          return false;

        builder.append(value, 0, trailingSlash);
        return true;
      }

      builder.append('/');
    }
    else if (!leadingSlash) {
      builder.append('/');
    }

    builder.append(value, 0, trailingSlash);
    return true;
  }

  // FIXME: This does URI-Encode...
  // FIXME: URL-Encode baseUri, but don't double-encode %-encoded values
  private static int appendLiteral(final StringBuilder builder, final String uriTemplate, final int i$, final int start, final int end) {
    // Append the literal path, first with URI encode, then with regex escape
    int repeatedSlashes = 0;
    char ch, prev = '\0';
    for (int i = start; i < end; ++i, prev = ch) { // [N]
      ch = uriTemplate.charAt(i);
      if (ch != '/') {
        final String en = URIComponent.encode(ch);
        if (en.length() > 1) {
          builder.append(en);
        }
        else {
          if (Patterns.isMetaCharacter(ch))
            builder.append('\\');

          builder.append(ch);
        }
      }
      else if (prev == '/') {
        ++repeatedSlashes;
      }
      else {
        builder.append('/');
      }
    }

    final int builderLen = builder.length() - 1;
    if (end == i$ && builder.charAt(builderLen) == '/') {
      builder.setLength(builderLen);
      ++repeatedSlashes;
    }

    return end - start - repeatedSlashes;
  }

  private final String uriTemplate;
  private final Pattern pattern;
  private final String[] pathSegmentParamNames;
  private int literalChars;
  private int allGroups;
  private int nonDefaultGroups;

  UriTemplate(final String baseUri, final Path classPath, final Path methodPath) {
    if (baseUri.length() > 0 && (!baseUri.startsWith("/") || baseUri.endsWith("/")))
      throw new IllegalArgumentException("baseUri (" + baseUri + ") be either \"\", or a multi-character string starting with '/' and not ending with '/'");

    if (classPath == null && methodPath == null)
      throw new IllegalArgumentException("classPath == null && methodPath == null");

    final StringBuilder builder = new StringBuilder(baseUri);
    addLeadingRemoveTrailing(builder, classPath);
    addLeadingRemoveTrailing(builder, methodPath);
    this.uriTemplate = builder.toString();

    builder.setLength(0);
    builder.append('^');

    this.pathSegmentParamNames = parsePathParams(builder, null, uriTemplate, uriTemplate.length(), -1, 0);

    builder.append("(/.*)?$");

    this.pattern = Patterns.compile(builder.toString());
    this.allGroups = allGroups + nonDefaultGroups;
  }

  /**
   * @param regex The {@link StringBuilder} to be used for construction of the matching regex for this {@link UriTemplate}.
   * @param value The {@link StringBuilder} to be used for construction of the regex values of path parameters.
   * @param uriTemplate The URI template to parse.
   * @param i$ The length of the {@code uriTemplate} string.
   * @param i The character index of iteration into the {@code uriTemplate} string.
   * @param depth The depth of regex name groups.
   * @return An array of regex name groups corresponding to path parameter names.
   * @see Path#value()
   */
  String[] parsePathParams(final StringBuilder regex, StringBuilder value, final String uriTemplate, final int i$, int i, final int depth) {
    int start = uriTemplate.indexOf('{', ++i);
    if (start < 0) {
      literalChars += appendLiteral(regex, uriTemplate, i$, i, i$);
      return new String[depth];
    }

    literalChars += appendLiteral(regex, uriTemplate, i$, i, start);

    char ch;
    i = ++start;
    String name = null;
    for (int mark = -1; i < i$; ++i) { // [N]
      ch = uriTemplate.charAt(i);
      if (Character.isWhitespace(ch)) {
        if (name == null) {
          if (i - start != 0) {
            name = escapeName(uriTemplate, start, i);
            start = -1;
            continue;
          }

          start = i + 1;
        }
      }
      else if (ch == ':') {
        if (mark == -1) {
          mark = i;
          if (name == null) {
            if (i - start == 0)
              throw new IllegalArgumentException("Expression \"" + uriTemplate + "\" does not match expected format: \"(\\w+)\\s*(:\\s*(.+))?\" at index 0");

            name = escapeName(uriTemplate, start, i);
            start = -1;
          }

          continue;
        }
      }
      else if (ch == '}') {
        if (i - start == 0)
          throw new IllegalArgumentException("Expression \"" + uriTemplate + "\" does not match expected format: \"(\\w+)\\s*(:\\s*(.+))?\" at index 0");

        if (name == null) {
          name = escapeName(uriTemplate, start, i);
          start = -1;
        }

        break;
      }
      else if (mark != -1) {
        if (value == null)
          value = new StringBuilder();

        start = i;
        boolean escaped = false;
        boolean wsp = false;
        for (int j = 0, noWsp = 0, scope = 1; i < i$; ch = uriTemplate.charAt(++i)) { // [N]
          if (escaped) {
            escaped = false;
            if (ch == 'k')
              j = -1;
          }
          else if (wsp = Character.isWhitespace(ch)) {
            ++noWsp;
          }
          else if (ch == '{') {
            ++scope;
          }
          else if (ch == '}' && --scope == 0) {
            if (noWsp > 0)
              value.setLength(value.length() - noWsp);

            break;
          }
          else if (ch == '\\') {
            escaped = true;
          }
          else {
            if (j == 0)
              j = ch == '(' ? 1 : 0;
            else if (j == 1)
              j = ch == '?' ? 2 : 0;
            else if (j == 2)
              j = ch == '<' ? 3 : 0;
            else if (j == 3) {
              value.append(ESCAPE); // prepend all named groups with ESCAPE char so they may never collide with generated named groups
              j = 0;
            }
            else if (j == -1)
              j = ch == '<' ? -2 : 0;
            else if (j == -2) {
              value.append(ESCAPE); // prepend all named group back references with ESCAPE char so they may never collide with generated named groups
              j = 0;
            }
          }

          if (!wsp && noWsp > 0) {
            if (noWsp == value.length())
              value.setLength(0);

            noWsp = 0;
          }

          value.append(ch);
        }

        break;
      }
      else if (name != null) {
        throw new IllegalArgumentException("Expression \"" + uriTemplate + "\" does not match expected format at index " + i);
      }
    }

    if (name == null)
      throw new IllegalArgumentException("Expression \"" + uriTemplate + "\" does not match expected format: \"(\\w+)\\s*(:\\s*(.+))?\" at index 0");

    if (i == -1)
      throw new IllegalArgumentException("Expression \"" + uriTemplate + "\" does not match expected format at index " + start);

    regex.append("(?<").append(name).append(">");

    if (value != null && value.length() > 0) {
      regex.append(value);
      value.setLength(0);
      regex.append(')');
      ++nonDefaultGroups;
    }
    else {
      regex.append("[^/]+?)");
      ++allGroups;
    }

    final String[] pathParamNames = parsePathParams(regex, value, uriTemplate, i$, i, depth + 1);
    pathParamNames[depth] = name;
    return pathParamNames;
  }

  Matcher matcher(final String path) {
    return pattern.matcher(path);
  }

  String[] getPathParamNames() {
    return pathSegmentParamNames;
  }

  // [JAX-RS 2.1 3.7.2 2.f]
  @Override
  public int compareTo(final UriTemplate o) {
    if (literalChars > o.literalChars)
      return -1;

    if (literalChars < o.literalChars)
      return 1;

    if (allGroups > o.allGroups)
      return -1;

    if (allGroups < o.allGroups)
      return 1;

    if (nonDefaultGroups > o.nonDefaultGroups)
      return -1;

    if (nonDefaultGroups < o.nonDefaultGroups)
      return 1;

    return 0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof UriTemplate))
      return false;

    final UriTemplate that = (UriTemplate)obj;
    return literalChars == that.literalChars && allGroups == that.allGroups && nonDefaultGroups == that.nonDefaultGroups && uriTemplate.equals(that.uriTemplate);
  }

  @Override
  public int hashCode() {
    return uriTemplate.hashCode();
  }

  @Override
  public String toString() {
    return uriTemplate;
  }
}