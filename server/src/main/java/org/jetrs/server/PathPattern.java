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

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.libj.net.URLs;
import org.libj.util.Patterns;

public class PathPattern {
  private static final Pattern pathExpressionPattern = Pattern.compile("(\\w+)\\s*(:\\s*\\(([^)]+)\\))?");

  private static String pathExpressionToRegex(final String pathExpression) {
    final Matcher matcher = pathExpressionPattern.matcher(pathExpression);
    if (!matcher.find())
      throw new IllegalArgumentException("Expression \"" + pathExpression + "\" does not match expected format");

    final String name = matcher.group(1);
    final String regex = matcher.group(3);
    return regex != null ? "(?<" + name + ">" + regex + ")" : "(?<" + name + ">[^\\/]+)";
  }

  private static Pattern createPattern(final String path) {
    int start = -1;
    int end = -1;
    final StringBuilder builder = new StringBuilder();
    while ((start = path.indexOf('{', end + 1)) > -1) {
      builder.append(path.substring(end + 1, start++));
      end = path.indexOf('}', start);
      builder.append(pathExpressionToRegex(path.substring(start, end)));
    }

    return Pattern.compile(builder.length() != 0 ? builder.toString() : path);
  }

  private static String prependSlash(final Path path) {
    return path == null ? null : path.value().startsWith("/") ? path.value() : "/" + path.value();
  }

  private final String uri;
  private final String decodedUri;
  private final Pattern pattern;

  public PathPattern(final Method method) {
    this(method.getDeclaringClass().getAnnotation(Path.class), method.getAnnotation(Path.class));
  }

  protected PathPattern(final Path path, final Path methodPath) {
    if (path == null && methodPath == null)
      throw new IllegalArgumentException("path == null && methodPath == null");

    final String pathString = methodPath == null ? prependSlash(path) : path == null ? prependSlash(methodPath) : prependSlash(path) + prependSlash(methodPath);
    final int index = pathString.indexOf('{');
    this.uri = index < 0 ? pathString : pathString.substring(0, index);
    this.decodedUri = URLs.decodePath(uri);
    this.pattern = createPattern(pathString);
  }

  public String getURI(final boolean decode) {
    return decode ? this.decodedUri : this.uri;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public boolean matches(final String path) {
    final Matcher matcher = pattern.matcher(path);
    return matcher.matches();
  }

  public MultivaluedMap<String,String> getParameters(final String path) {
    final Matcher matcher = pattern.matcher(path);
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
}