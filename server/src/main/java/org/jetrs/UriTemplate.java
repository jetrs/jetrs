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

import org.libj.lang.Strings;
import org.libj.net.URIComponent;
import org.libj.util.Patterns;

class UriTemplate implements Comparable<UriTemplate> {
  private static final Pattern pathExpressionPattern = Pattern.compile("(\\w+)\\s*(:\\s*(.+))?");

  static boolean addLeadingRemoveTrailing(final StringBuilder builder, final Path path) {
    final String value;
    final int len;
    if (path == null || (len = (value = path.value()).length()) == 0)
      return false;

    final boolean leadingSlash = value.charAt(0) == '/';
    if (leadingSlash && len == 1)
      return false;

    final int trailingSlash = value.charAt(len - 1) == '/' ? len - 1 : len;
    if (trailingSlash != len) {
      if (leadingSlash) {
        if (len == 2)
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
  private static int append(final StringBuilder builder, final char[] chars, final int start, final int end) {
    // Append the literal path, first with URI encode, then with regex escape
    for (int i = start; i < end; ++i) {
      final char ch = chars[i];
      if (ch == '/') {
        builder.append('/');
      }
      else {
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
    }

    return end - start;
  }

  private final String uriTemplate;
  private final String regex;
  private final Pattern pattern;
  private final String[] groupNames;
  private final int literalChars;
  private final int allGroups;
  private final int nonDefaultGroups;

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

    int literalChars = 0;
    int allGroups = 0;
    int nonDefaultGroups = 0;
    int end = -1;
    final char[] chars = uriTemplate.toCharArray();
    for (int start; (start = uriTemplate.indexOf('{', ++end)) > -1;) {
      literalChars += append(builder, chars, end, start);
      end = Strings.indexOfScopeClose(uriTemplate, '{', '}', ++start);

      final String pathExpression = uriTemplate.substring(start, end);
      final Matcher matcher = pathExpressionPattern.matcher(pathExpression);
      if (!matcher.find())
        throw new IllegalArgumentException("Expression \"" + pathExpression + "\" does not match expected format");

      final String name = matcher.group(1);
      final String regex = matcher.group(3);
      builder.append("(?<" + name + ">");
      if (regex != null) {
        ++nonDefaultGroups;
        builder.append(regex);
      }
      else {
        ++allGroups;
        builder.append("[^/]+?");
      }

      builder.append(')');
    }

    literalChars += append(builder, chars, end, chars.length);
    builder.append("(/.*)?$");

    this.regex = builder.toString();
    this.pattern = Patterns.compile(regex);
    this.groupNames = Patterns.getGroupNames(regex);
    this.literalChars = literalChars;
    this.allGroups = allGroups + nonDefaultGroups;
    this.nonDefaultGroups = nonDefaultGroups;
  }

  Matcher matcher(final String path) {
    return pattern.matcher(path);
  }

  String[] getGroupNames() {
    return this.groupNames;
  }

  String getRegex() {
    return this.regex;
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
    return literalChars == that.literalChars && allGroups == that.allGroups && nonDefaultGroups == that.nonDefaultGroups &&  regex.equals(that.regex);
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    hashCode = 31 * hashCode + literalChars;
    hashCode = 31 * hashCode + allGroups;
    hashCode = 31 * hashCode + nonDefaultGroups;
    return 31 * hashCode + regex.hashCode();
  }

  @Override
  public String toString() {
    return uriTemplate;
  }
}