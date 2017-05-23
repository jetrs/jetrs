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

package org.libx4j.xrs.server;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.lib4j.util.Patterns;

public class PathPattern {
  private static final Pattern pathExpressionPattern = Pattern.compile("(\\w+)\\s*(:\\s*\\(([^\\)]+)\\))?");

  private static String pathExpressionToRegex(final String pathExpression) {
    final Matcher matcher = pathExpressionPattern.matcher(pathExpression);
    if (!matcher.find())
      throw new IllegalArgumentException("Expression \"" + pathExpression + "\" does not match expected format");

    final String name = matcher.group(1);
    final String regex = matcher.group(3);
    if (regex != null)
      return "(?<" + name + ">" + regex + ")";

    return "(?<" + name + ">[^\\/]+)";
  }

  private static Pattern createPattern(final String path) {
    int start = -1;
    int end = -1;
    final StringBuilder builder = new StringBuilder();
    while ((start = path.indexOf("{", end + 1)) > -1) {
      builder.append(path.substring(end + 1, start++));
      end = path.indexOf("}", start);
      final String token = path.substring(start, end);
      builder.append(pathExpressionToRegex(token));
    }

    return Pattern.compile(builder.length() != 0 ? builder.toString() : path);
  }

  private static String prependSlash(final Path path) {
    return path == null ? null : path.value().startsWith("/") ? path.value() : "/" + path.value();
  }

  private final Pattern pattern;

  public PathPattern(final Method method) {
    this(method.getDeclaringClass().getAnnotation(Path.class), method.getAnnotation(Path.class));
  }

  protected PathPattern(final Path path, final Path methodPath) {
    if (path == null && methodPath == null)
      throw new IllegalArgumentException("path == null && methodPath == null");

    this.pattern = methodPath == null ? createPattern(prependSlash(path)) : path != null ? createPattern(prependSlash(path) + prependSlash(methodPath)) : createPattern(prependSlash(methodPath));
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

    final MultivaluedMap<String,String> parameters = new MultivaluedHashMap<String,String>();
    for (final String groupName : groupNames) {
      final String[] values = matcher.group(groupName).replace("%3B", ";").split(";");
      for (final String value : values)
        parameters.add(groupName, value);
    }

    return parameters;
  }
}