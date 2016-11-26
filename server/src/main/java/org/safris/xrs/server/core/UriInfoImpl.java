/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server.core;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class UriInfoImpl implements UriInfo {
  private final HttpServletRequest request;

  public UriInfoImpl(final HttpServletRequest request) {
    this.request = request;
  }

  @Override
  public String getPath() {
    return request.getPathInfo();
  }

  @Override
  public String getPath(final boolean decode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<PathSegment> getPathSegments() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<PathSegment> getPathSegments(final boolean decode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI getRequestUri() {
    return URI.create(request.getRequestURI());
  }

  @Override
  public UriBuilder getRequestUriBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI getAbsolutePath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public UriBuilder getAbsolutePathBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI getBaseUri() {
    throw new UnsupportedOperationException();
  }

  @Override
  public UriBuilder getBaseUriBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String,String> getPathParameters() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String,String> getPathParameters(final boolean decode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String,String> getQueryParameters() {
    final MultivaluedMap<String,String> parameters = new MultivaluedHashMap<String,String>();
    for (final Map.Entry<String,String[]> entry : request.getParameterMap().entrySet())
      parameters.addAll(entry.getKey(), entry.getValue());

    return parameters;
  }

  @Override
  public MultivaluedMap<String,String> getQueryParameters(final boolean decode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getMatchedURIs() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getMatchedURIs(final boolean decode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Object> getMatchedResources() {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI resolve(final URI uri) {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI relativize(final URI uri) {
    throw new UnsupportedOperationException();
  }
}