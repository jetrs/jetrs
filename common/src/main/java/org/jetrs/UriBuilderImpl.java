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

package org.jetrs;

import static org.jetrs.UriBuilderUtil.*;
import static org.libj.lang.Assertions.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.libj.lang.Strings;
import org.libj.util.ArrayUtil;

class UriBuilderImpl extends UriBuilder implements Cloneable {
  static final Pattern opaqueUri = Pattern.compile("^([^:/?#{]+):([^/].*)");
  private static final Pattern hierarchicalUri = Pattern.compile("^(([^:/?#{]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");
  private static final Pattern squareHostBrackets = Pattern.compile("(\\[(([0-9A-Fa-f]{0,4}:){2,7})([0-9A-Fa-f]{0,4})%?.*\\]):(\\d+)");
  private static final Pattern hostPortPattern = Pattern.compile("([^/:]+):(\\d+)");

  private static final class TemplateParametersMap extends HashMap<String,Object> {
    private final Object[] parameterValues;
    private int index;

    private TemplateParametersMap(final Object[] parameterValues) {
      super(parameterValues.length);
      this.parameterValues = parameterValues;
    }

    @Override
    public Object get(final Object key) {
      if (super.containsKey(key))
        return super.get(key);

      if (index == parameterValues.length)
        return null;

      final Object value = parameterValues[index++];
      put((String)key, assertNotNull(value));
      return value;
    }

    @Override
    public boolean containsKey(final Object key) {
      get(key);
      return super.containsKey(key);
    }

    @Override
    public TemplateParametersMap clone() {
      return (TemplateParametersMap)super.clone();
    }
  }

  private String host;
  private String scheme;
  private int port = -1;

  private String userInfo;
  private String path;
  private String query;
  private String fragment;
  private String ssp;
  private String authority;

  UriBuilderImpl() {
  }

  private UriBuilderImpl(final UriBuilderImpl copy) {
    this.host = copy.host;
    this.scheme = copy.scheme;
    this.port = copy.port;
    this.userInfo = copy.userInfo;
    this.path = copy.path;
    this.query = copy.query;
    this.fragment = copy.fragment;
    this.ssp = copy.ssp;
    this.authority = copy.authority;
  }

  private UriBuilder parseHierarchicalUri(final CharSequence uriTemplate, final Matcher match) {
    final boolean hasScheme = match.group(2) != null;
    if (hasScheme)
      this.scheme = match.group(2);

    final String authority = match.group(4);
    if (authority != null) {
      this.authority = null;
      String host = authority;
      final int at = host.indexOf('@');
      if (at > -1) {
        final String user = host.substring(0, at);
        host = host.substring(at + 1);
        this.userInfo = user;
      }

      try {
        final Matcher hostPortMatch = hostPortPattern.matcher(host);
        if (hostPortMatch.matches()) {
          this.host = hostPortMatch.group(1);
          this.port = Integer.parseInt(hostPortMatch.group(2));
        }
        else {
          if (host.startsWith("[")) {
            // Must support an IPv6 hostname of format "[::1]" or [0:0:0:0:0:0:0:0]
            // and IPv6 link-local format [fe80::1234%1] [ff08::9abc%interface10]
            final Matcher bracketsMatch = squareHostBrackets.matcher(host);
            if (bracketsMatch.matches()) {
              host = bracketsMatch.group(1);
              this.port = Integer.parseInt(bracketsMatch.group(5));
            }
          }

          this.host = host;
        }
      }
      catch (final NumberFormatException e) {
        throw new IllegalArgumentException(invalid("uri template", uriTemplate), e);
      }
    }

    final String group = match.group(5);
    if (group != null) {
      final int length = group.length();
      if (!hasScheme && length > 0 && !group.startsWith("/") && group.indexOf(':') > -1 && group.indexOf('/') > -1 && group.indexOf(':') < group.indexOf('/'))
        throw new IllegalArgumentException(invalid("uri template", uriTemplate));

      if (length > 0)
        replacePath(group);
    }

    if (match.group(7) != null)
      replaceQuery(match.group(7));

    if (match.group(9) != null)
      fragment(match.group(9));

    return this;
  }

  private UriBuilder uri(final CharSequence uriTemplate) {
    assertNotNull(uriTemplate, "uriTemplate is null");

    final Matcher opaque = opaqueUri.matcher(uriTemplate);
    if (opaque.matches()) {
      this.authority = null;
      this.host = null;
      this.port = -1;
      this.userInfo = null;
      this.query = null;
      this.scheme = opaque.group(1);
      this.ssp = opaque.group(2);
      return this;
    }

    final Matcher matcher = hierarchicalUri.matcher(uriTemplate);
    if (!matcher.matches())
      throw new IllegalArgumentException(invalid("uri template", uriTemplate));

    ssp = null;
    return parseHierarchicalUri(uriTemplate, matcher);
  }

  private StringBuilder buildFromParameters(final Map<String,?> parameters, final boolean fromEncodedMap, final boolean isTemplate, final boolean encodeSlash) {
    final UriEncoder uriEncoder = encodeSlash ? UriEncoder.PATH_SEGMENT : UriEncoder.PATH;
    final StringBuilder builder = new StringBuilder();
    if (scheme != null) {
      replaceParameter(parameters, fromEncodedMap, isTemplate, scheme, builder, uriEncoder);
      builder.append(':');
    }

    if (ssp != null) {
      builder.append(ssp);
    }
    else if (userInfo != null || host != null || port != -1) {
      builder.append("//");
      if (userInfo != null) {
        replaceParameter(parameters, fromEncodedMap, isTemplate, userInfo, builder, uriEncoder);
        builder.append('@');
      }

      if (host != null) {
        if (host.length() == 0)
          throw new UriBuilderException(invalid("host", "\"\""));

        replaceParameter(parameters, fromEncodedMap, isTemplate, host, builder, uriEncoder);
      }

      if (port != -1)
        builder.append(':').append(port);
    }
    else if (authority != null) {
      builder.append("//");
      replaceParameter(parameters, fromEncodedMap, isTemplate, authority, builder, uriEncoder);
    }

    if (path != null) {
      final StringBuilder pathBuilder = new StringBuilder();
      replaceParameter(parameters, fromEncodedMap, isTemplate, path, pathBuilder, uriEncoder);
      if ((userInfo != null || host != null) && pathBuilder.length() > 0 && pathBuilder.charAt(0) != '/')
        builder.append('/');

      builder.append(pathBuilder);
    }

    if (query != null) {
      builder.append('?');
      replaceParameter(parameters, fromEncodedMap, isTemplate, query, builder, UriEncoder.QUERY_PARAM);
    }

    if (fragment != null) {
      builder.append('#');
      replaceParameter(parameters, fromEncodedMap, isTemplate, fragment, builder, uriEncoder);
    }

    return builder;
  }

  @Override
  public UriBuilder uri(final String uriTemplate) {
    return uri((CharSequence)uriTemplate);
  }

  @Override
  public UriBuilder uri(final URI uri) {
    assertNotNull(uri, "uri is null");

    final String rawFragment = uri.getRawFragment();
    if (rawFragment != null)
      fragment = rawFragment;

    final String scheme = uri.getScheme();
    final String rawSchemeSpecificPart = uri.getRawSchemeSpecificPart();
    if (uri.isOpaque()) {
      this.scheme = scheme;
      ssp = rawSchemeSpecificPart;
      return this;
    }

    if (scheme != null) {
      this.scheme = scheme;
    }
    else if (ssp != null && rawSchemeSpecificPart != null) {
      ssp = rawSchemeSpecificPart;
      return this;
    }

    ssp = null;
    final String rawAuthority = uri.getRawAuthority();
    if (rawAuthority != null) {
      final String rawUserInfo = uri.getRawUserInfo();
      final String host$ = uri.getHost();
      final int port$ = uri.getPort();
      if (rawUserInfo == null && host$ == null && port$ == -1) {
        authority = rawAuthority;
        userInfo = null;
        host = null;
        port = -1;
      }
      else {
        authority = null;
        if (rawUserInfo != null)
          userInfo = rawUserInfo;

        if (host$ != null)
          host = host$;

        if (port$ != -1)
          port = port$;
      }
    }

    final String rawPath = uri.getRawPath();
    if (rawPath != null && rawPath.length() > 0)
      path = rawPath;

    final String rawQuery = uri.getRawQuery();
    if (rawQuery != null && rawQuery.length() > 0)
      query = rawQuery;

    return this;
  }

  @Override
  public UriBuilder scheme(final String scheme) {
    this.scheme = scheme;
    return this;
  }

  @Override
  public UriBuilder schemeSpecificPart(final String ssp) {
    assertNotNull(ssp, "ssp is null");

    final StringBuilder builder = new StringBuilder();
    if (scheme != null)
      builder.append(scheme).append(':');

    builder.append(ssp);
    if (fragment != null && fragment.length() > 0)
      builder.append('#').append(fragment);

    final URI uri = newURI(builder.toString());
    final String rawSchemeSpecificPart = uri.getRawSchemeSpecificPart();
    final String rawPath = uri.getRawPath();
    if (rawSchemeSpecificPart != null && rawPath == null) {
      this.ssp = rawSchemeSpecificPart;
    }
    else {
      this.ssp = null;
      userInfo = uri.getRawUserInfo();
      host = uri.getHost();
      port = uri.getPort();
      path = rawPath;
      query = uri.getRawQuery();
    }

    return this;
  }

  @Override
  public UriBuilder userInfo(final String ui) {
    this.userInfo = ui;
    return this;
  }

  @Override
  public UriBuilder host(final String host) {
    if (host != null && host.length() == 0)
      throw new IllegalArgumentException(invalidParam("host", "\"\""));

    this.host = host;
    return this;
  }

  @Override
  public UriBuilder port(final int port) {
    if (port < -1)
      throw new IllegalArgumentException(invalidParam("port", port));

    this.port = port;
    return this;
  }

  @Override
  public UriBuilder path(final String segment) {
    if (segment == null)
      throw new IllegalArgumentException(invalidParam("segment", null));

    path = appendPath(path, true, segment);
    return this;
  }

  @Override
  public UriBuilder path(final Class resource) {
    assertNotNull(resource, "resource is null");

    final Path annotation = AnnotationUtil.getAnnotation(resource, Path.class);
    if (annotation == null)
      throw new IllegalArgumentException("Path resource not annotated with @Path: " + resource.getName());

    path = appendPath(path, true, annotation.value());
    return this;
  }

  @Override
  public UriBuilder path(final Class resource, final String method) {
    assertNotNull(resource, "resource is null");
    assertNotNull(method, "method is null");

    Method theMethod = null;
    for (final Method m : resource.getMethods()) { // [A]
      if (m.getName().equals(method) && AnnotationUtil.isAnnotationPresent(m, Path.class)) {
        if (theMethod != null)
          throw new IllegalArgumentException("Multiple public @Path annotated methods with name \"" + method + "\"");

        theMethod = m;
      }
    }

    if (theMethod == null)
      throw new IllegalArgumentException("No public @Path annotated method for " + resource.getName() + "." + method);

    return path(theMethod);
  }

  @Override
  public UriBuilder path(final Method method) {
    assertNotNull(method, "method is null");

    final Path annotation = AnnotationUtil.getAnnotation(method, Path.class);
    if (annotation == null)
      throw new IllegalArgumentException("Method " + method.getDeclaringClass().getName() + "." + method.getName() + "(" + ArrayUtil.toString(method.getParameterTypes(), ',', Class::getName) + ") is not annotated with @Path");

    path = appendPath(path, true, annotation.value());
    return this;
  }

  @Override
  public UriBuilder replaceMatrix(String matrix) {
    if (matrix == null || matrix.length() == 0)
      matrix = ";";
    else if (matrix.charAt(0) != ';')
      matrix = ";" + UriEncoder.PATH.encode(matrix);
    else
      matrix = UriEncoder.PATH.encode(matrix);

    if (path == null) {
      path = matrix;
    }
    else {
      final int matrixIndex = path.indexOf(';', path.lastIndexOf('/') + 1);
      if (matrixIndex > -1)
        path = path.substring(0, matrixIndex);

      path += matrix;
    }

    return this;
  }

  @Override
  public UriBuilder replaceQuery(final String query) {
    this.query = query == null || query.length() == 0 ? null : UriEncoder.QUERY.encode(query);
    return this;
  }

  @Override
  public UriBuilder fragment(final String fragment) {
    this.fragment = fragment == null ? null : UriEncoder.QUERY.encode(fragment);
    return this;
  }

  @Override
  public URI build(final Object ... values) {
    return build(values, true);
  }

  @Override
  public URI build(final Object[] values, final boolean encodeSlashInPath) {
    assertNotNull(values, "values is null");
    return newURI(buildFromParameters(new TemplateParametersMap(values), false, false, encodeSlashInPath).toString());
  }

  @Override
  public URI buildFromMap(final Map<String,?> values) {
    return buildFromMap(values, true);
  }

  @Override
  public URI buildFromMap(final Map<String,?> values, final boolean encodeSlashInPath) {
    assertNotNull(values, "values is null");
    return newURI(buildFromParameters(values, false, false, encodeSlashInPath).toString());
  }

  @Override
  public URI buildFromEncodedMap(final Map<String,?> values) {
    assertNotNull(values, "values is null");
    return newURI(buildFromParameters(values, true, false, false).toString());
  }

  @Override
  public UriBuilder matrixParam(final String name, final Object ... values) {
    assertNotNull(name, "name is null");
    assertNotNull(values, "values is null");

    if (path == null)
      path = "";

    if (values.length != 0) {
      final StringBuilder b = new StringBuilder();
      for (final Object value : values) { // [A]
        assertNotNull(value, "value is null");
        b.append(';').append(UriEncoder.MATRIX.encode(name)).append('=').append(UriEncoder.MATRIX.encode(value.toString()));
      }

      path += b;
    }

    return this;
  }

  @Override
  public UriBuilder replaceMatrixParam(final String name, final Object ... values) {
    assertNotNull(name, "name is null");

    if (path == null)
      return values != null && values.length > 0 ? matrixParam(name, values) : this;

    // remove all path param expressions so we don't accidentally start replacing within a regular expression
    final StringBuilder segment = new StringBuilder();
    final ArrayList<String> pathParams = UriEncoder.savePathParams(path, segment);
    path = segment.toString();

    // Find last path segment
    int start = path.lastIndexOf('/');
    if (start < 0)
      start = 0;

    final int matrixIndex = path.indexOf(';', start);
    if (matrixIndex > -1) {
      final MultivaluedArrayHashMap<String,String> params = new MultivaluedArrayHashMap<>();
      PathSegmentImpl.parseMatrixParams(params, path, matrixIndex);
      params.remove(name);

      final StringBuilder newPath = new StringBuilder(path);
      newPath.delete(matrixIndex, newPath.length());
      if (params.size() > 0) {
        for (final String paramName : params.keySet()) { // [S]
          final List<String> paramValues = params.get(paramName);
          for (int i = 0, i$ = paramValues.size(); i < i$; ++i) // [RA]
            appendParam(newPath, paramName, paramValues.get(i));
        }
      }

      path = newPath.toString();
    }

    if (values != null && values.length > 0)
      matrixParam(name, values);

    // put back all path param expressions
    if (pathParams != null) {
      final Matcher matcher = UriEncoder.PARAM_REPLACEMENT.matcher(path);
      final StringBuilder b = new StringBuilder();
      int from = 0;
      for (int i = 0; matcher.find(); ++i, from = matcher.end()) { // [RA]
        b.append(path, from, matcher.start());
        b.append(pathParams.get(i));
      }

      b.append(path, from, path.length());
      path = b.toString();
    }

    return this;
  }

  private static void appendParam(final StringBuilder path, final String paramName, final String paramValue) {
    path.append(';').append(paramName);
    if (paramValue != null)
      path.append('=').append(paramValue);
  }

  @Override
  public UriBuilder queryParam(final String name, final Object ... values) {
    assertNotNull(name, "name is null");
    assertNotNull(values, "values is null");

    final StringBuilder b = new StringBuilder();
    if (query != null)
      b.append(query).append('&');

    for (int i = 0, i$ = values.length; i < i$; ++i) { // [A]
      final Object value = assertNotNull(values[i], "value is null");
      if (i > 0)
        b.append('&');

      b.append(UriEncoder.QUERY_PARAM.encode(name)).append('=').append(UriEncoder.QUERY_PARAM.encode(value.toString()));
    }

    query = b.toString();
    return this;
  }

  @Override
  public UriBuilder replaceQueryParam(final String name, final Object ... values) {
    assertNotNull(name, "name is null");

    if (query == null || query.length() == 0)
      return values == null || values.length == 0 ? this : queryParam(name, values);

    final String[] params = Strings.split(query, '&');
    query = null;

    final String encodedName = UriEncoder.QUERY_PARAM.encode(name);
    for (String param : params) { // [A]
      final int eq = param.indexOf('=');
      if (eq >= 0) {
        final String paramName = param.substring(0, eq);
        if (paramName.equals(encodedName))
          continue;
      }
      else if (param.equals(encodedName)) {
        continue;
      }

      if (query == null)
        query = "";
      else
        query += "&";

      query += param;
    }

    return values == null || values.length == 0 ? this : queryParam(name, values);
  }

  @Override
  public UriBuilder segment(final String ... segments) {
    if (segments == null)
      throw new IllegalArgumentException(invalidParam("segments", null));

    for (final String segment : segments) { // [A]
      assertNotNull(segment, "segment is null");
      path(UriEncoder.PATH_SEGMENT.encode(segment));
    }

    return this;
  }

  @Override
  public URI buildFromEncoded(final Object ... values) {
    assertNotNull(values, "values is null");
    return newURI(buildFromParameters(new TemplateParametersMap(values), true, false, false).toString());
  }

  @Override
  public UriBuilder replacePath(final String path) {
    this.path = path == null ? null : UriEncoder.PATH.encode(path);
    return this;
  }

  @Override
  public String toTemplate() {
    return buildFromParameters(null, true, true, true).toString();
  }

  @Override
  public UriBuilder resolveTemplateFromEncoded(final String name, final Object value) {
    assertNotNull(name, "name is null");
    assertNotNull(value, "value is null");
    return uri(buildFromParameters(Collections.singletonMap(name, value), true, true, true));
  }

  @Override
  public UriBuilder resolveTemplates(final Map<String,Object> templateValues) {
    return resolveTemplates(templateValues, true);
  }

  @Override
  public UriBuilder resolveTemplate(final String name, final Object value) {
    return resolveTemplate(name, value, true);
  }

  @Override
  public UriBuilder resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) {
    assertNotNull(name, "name is null");
    assertNotNull(value, "value is null");
    return uri(buildFromParameters(Collections.singletonMap(name, value), false, true, encodeSlashInPath));
  }

  @Override
  public UriBuilder resolveTemplates(final Map<String,Object> templateValues, final boolean encodeSlashInPath) {
    assertNotNull(templateValues, "templateValues is null");
    if (templateValues.containsKey(null))
      throw new IllegalArgumentException(invalidParam("key in templateValues map", null));

    return uri(buildFromParameters(templateValues, false, true, encodeSlashInPath));
  }

  @Override
  public UriBuilder resolveTemplatesFromEncoded(final Map<String,Object> templateValues) {
    return resolveTemplates(templateValues, true);
  }

  @Override
  public UriBuilder clone() {
    return new UriBuilderImpl(this);
  }
}