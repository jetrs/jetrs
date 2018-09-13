/* Copyright (c) 2016 OpenJAX
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

package org.openjax.xrs.server.core;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.fastjax.net.URIComponent;
import org.fastjax.net.URLs;
import org.openjax.xrs.server.ExecutionContext;
import org.openjax.xrs.server.ResourceMatch;

public class UriInfoImpl implements UriInfo {
  private final ContainerRequestContext containerRequestContext;
  private final HttpServletRequest httpServletRequest;
  private final ExecutionContext executionContext;

  private MultivaluedMap<String,String> decodedParameters;
  private MultivaluedMap<String,String> encodedParameters;

  public UriInfoImpl(final ContainerRequestContext containerRequestContext, final HttpServletRequest httpServletRequest, final ExecutionContext executionContext) {
    this.containerRequestContext = containerRequestContext;
    this.httpServletRequest = httpServletRequest;
    this.executionContext = executionContext;
  }

  @Override
  public String getPath() {
    return getPath(true);
  }

  private String decodedPath;

  @Override
  public String getPath(final boolean decode) {
    return !decode ? httpServletRequest.getPathInfo() : decodedPath == null ? decodedPath = URLs.pathDecode(httpServletRequest.getPathInfo()) : decodedPath;
  }

  @Override
  public List<PathSegment> getPathSegments() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public List<PathSegment> getPathSegments(final boolean decode) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public URI getRequestUri() {
    return URI.create(httpServletRequest.getRequestURI());
  }

  @Override
  public UriBuilder getRequestUriBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public URI getAbsolutePath() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public UriBuilder getAbsolutePathBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public URI getBaseUri() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public UriBuilder getBaseUriBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String,String> getPathParameters() {
    return getPathParameters(true);
  }

  private MultivaluedMap<String,String> filterPathParameters(final boolean decode) {
    final ResourceMatch[] resourceMatches = executionContext.filterAndMatch(containerRequestContext);
    return resourceMatches == null ? null : resourceMatches[0].getManifest().getPathPattern().getParameters(getPath(decode));
  }

  @Override
  public MultivaluedMap<String,String> getPathParameters(final boolean decode) {
    return decode ? (decodedParameters == null ? decodedParameters = filterPathParameters(decode) : decodedParameters) : encodedParameters == null ? encodedParameters = filterPathParameters(decode) : encodedParameters;
  }

  @Override
  public MultivaluedMap<String,String> getQueryParameters() {
    return getQueryParameters(true);
  }

  @Override
  public MultivaluedMap<String,String> getQueryParameters(final boolean decode) {
    final MultivaluedMap<String,String> parameters = new MultivaluedHashMap<>();
    if (decode) {
      for (final Map.Entry<String,String[]> entry : httpServletRequest.getParameterMap().entrySet())
        for (final String value : entry.getValue())
          parameters.add(entry.getKey(), URIComponent.decode(value));
    }
    else {
      for (final Map.Entry<String,String[]> entry : httpServletRequest.getParameterMap().entrySet())
        parameters.addAll(entry.getKey(), entry.getValue());
    }

    return parameters;
  }

  @Override
  public List<String> getMatchedURIs() {
    return executionContext.getMatchedURIs(true);
  }

  @Override
  public List<String> getMatchedURIs(final boolean decode) {
    return executionContext.getMatchedURIs(decode);
  }

  @Override
  public List<Object> getMatchedResources() {
    return executionContext.getMatchedResources();
  }

  @Override
  public URI resolve(final URI uri) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public URI relativize(final URI uri) {
    // TODO:
    throw new UnsupportedOperationException();
  }
}