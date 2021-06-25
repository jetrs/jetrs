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

package org.jetrs.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.libj.lang.Strings;
import org.libj.net.URLs;
import org.libj.util.Patterns;

public class PathPattern implements Comparable<PathPattern> {
  private static final Pattern pathExpressionPattern = Pattern.compile("(\\w+)\\s*(:\\s*(.+))?");

  private static String prependSlash(final Path path) {
    return path == null ? null : path.value().startsWith("/") ? path.value() : "/" + path.value();
  }

  private final String uri;
  private String decodedUri;

  private final Pattern pattern;
  private final int literalChars;
  private final int defaultGroups;
  private final int nonDefaultGroups;

  PathPattern(final Path classPath, final Path methodPath) {
    if (classPath == null && methodPath == null)
      throw new IllegalArgumentException("classPath == null && methodPath == null");

    final String pathString = methodPath == null ? prependSlash(classPath) : classPath == null ? prependSlash(methodPath) : prependSlash(classPath) + prependSlash(methodPath);
    final int index = pathString.indexOf('{');
    this.uri = index < 0 ? pathString : pathString.substring(0, index);

    final StringBuilder builder = new StringBuilder();
    int literalChars = 0;
    int defaultGroups = 0;
    int nonDefaultgroups = 0;
    int end = -1;
    for (int start; (start = pathString.indexOf('{', ++end)) > -1;) {
      literalChars += start - end;
      builder.append(pathString, end, start++);

      end = Strings.indexOfScopeClose(pathString, '{', '}', start);
      final String pathExpression = pathString.substring(start, end);
      final Matcher matcher = pathExpressionPattern.matcher(pathExpression);
      if (!matcher.find())
        throw new IllegalArgumentException("Expression \"" + pathExpression + "\" does not match expected format");

      final String name = matcher.group(1);
      final String regex = matcher.group(3);
      builder.append("(?<" + name + ">");
      if (regex != null) {
        ++nonDefaultgroups;
        builder.append(regex);
      }
      else {
        ++defaultGroups;
        builder.append("[^/]+");
      }

      builder.append(')');
    }

    builder.append(pathString.substring(end));

    this.pattern = Patterns.compile(builder.length() != 0 ? builder.toString() : pathString);
    this.literalChars = literalChars;
    this.defaultGroups = defaultGroups;
    this.nonDefaultGroups = nonDefaultgroups;
  }

  String getURI(final boolean decode) {
    return !decode ? uri : decodedUri == null ? decodedUri = URLs.decodePath(uri) : decodedUri;
  }

  private Matcher matcher(final String path) {
    return pattern.matcher(path);
  }

  boolean matches(final String path) {
    return matcher(path).matches();
  }

  public MultivaluedMap<String,String> getParameters(final String path) {
    final Matcher matcher = matcher(path);
    if (!matcher.find())
      return null;

    final String[] groupNames = Patterns.getGroupNames(pattern);
    if (groupNames == null)
      return null;

    final MultivaluedMap<String,String> parameters = new MultivaluedHashMap<>();
    for (final String groupName : groupNames) {
      final String values = matcher.group(groupName).replace("%3B", ";");
      int start = 0;
      for (int end = -1; (end = values.indexOf(';', end + 1)) != -1; start = end + 1)
        if (start != end)
          parameters.add(groupName, values.substring(start, end));

      parameters.add(groupName, values.substring(start));
    }

    return parameters;
  }

  // [JAX-RS 2.1 6.7.2 1.e]
  @Override
  public int compareTo(final PathPattern o) {
    if (literalChars > o.literalChars)
      return -1;

    if (literalChars < o.literalChars)
      return 1;

    if (defaultGroups > o.defaultGroups)
      return -1;

    if (defaultGroups < o.defaultGroups)
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

    if (!(obj instanceof PathPattern))
      return false;

    return pattern.equals(((PathPattern)obj).pattern);
  }

  @Override
  public int hashCode() {
    return 31 + pattern.hashCode();
  }

  @Override
  public String toString() {
    return pattern.toString();
  }
}