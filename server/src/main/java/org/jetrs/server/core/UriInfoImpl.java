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

package org.jetrs.server.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jetrs.server.ExecutionContext;
import org.jetrs.server.ResourceMatch;
import org.libj.net.URIComponent;
import org.libj.net.URLs;

public class UriInfoImpl implements UriInfo {
  private final ContainerRequestContext containerRequestContext;
  private final HttpServletRequest httpServletRequest;
  private final ExecutionContext executionContext;

  public UriInfoImpl(final ContainerRequestContext containerRequestContext, final HttpServletRequest httpServletRequest, final ExecutionContext executionContext) {
    this.containerRequestContext = containerRequestContext;
    this.httpServletRequest = httpServletRequest;
    this.executionContext = executionContext;
  }

  @Override
  public String getPath() {
    return getPath(true);
  }

  private String pathDecoded;
  private String pathEncoded;

  @Override
  public String getPath(final boolean decode) {
    if (decode)
      return pathDecoded == null ? pathDecoded = URLs.decodePath(httpServletRequest.getRequestURI()) : pathDecoded;

    return pathEncoded == null ? pathEncoded = httpServletRequest.getRequestURI() : pathEncoded;
  }

  @Override
  public List<PathSegment> getPathSegments() {
    return getPathSegments(true);
  }

  private List<PathSegment> pathSegmentsDecoded;
  private List<PathSegment> pathSegmentsEncoded;

  @Override
  public List<PathSegment> getPathSegments(final boolean decode) {
    if (decode) {
      if (pathSegmentsDecoded != null)
        return pathSegmentsDecoded;
    }
    else if (pathSegmentsEncoded != null) {
      return pathSegmentsEncoded;
    }

    final String[] parts = getPath(decode).split("/");
    final List<PathSegment> pathSegments = new ArrayList<>(parts.length);
    for (int i = 0; i < parts.length; ++i)
      pathSegments.add(new PathSegmentImpl(parts[i]));

    return decode ? pathSegmentsDecoded = pathSegments : (pathSegmentsEncoded = pathSegments);
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

  private URI absolutePath;

  @Override
  public URI getAbsolutePath() {
    if (absolutePath != null)
      return absolutePath;

    try {
      return absolutePath = new URI(httpServletRequest.getQueryString() != null ? httpServletRequest.getRequestURL() + "?" + httpServletRequest.getQueryString() : httpServletRequest.getRequestURL().toString());
    }
    catch (final URISyntaxException e) {
      throw new InternalServerErrorException(e);
    }
  }

  @Override
  public UriBuilder getAbsolutePathBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  private URI baseUri;

  @Override
  public URI getBaseUri() {
    if (baseUri != null)
      return baseUri;

    final StringBuffer requestURL = httpServletRequest.getRequestURL();
    final String requestURI = httpServletRequest.getRequestURI();
    final String contextPath = httpServletRequest.getContextPath();
    try {
      return baseUri = new URI(requestURL.substring(0, requestURL.length() - requestURI.length() + contextPath.length()) + "/");
    }
    catch (final URISyntaxException e) {
      throw new InternalServerErrorException(e);
    }
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

  private MultivaluedMap<String,String> parametersDecoded;
  private MultivaluedMap<String,String> parametersEncoded;

  @Override
  public MultivaluedMap<String,String> getPathParameters(final boolean decode) {
    return decode ? (parametersDecoded == null ? parametersDecoded = filterPathParameters(decode) : parametersDecoded) : parametersEncoded == null ? parametersEncoded = filterPathParameters(decode) : parametersEncoded;
  }

  @Override
  public MultivaluedMap<String,String> getQueryParameters() {
    return getQueryParameters(true);
  }

  private MultivaluedMap<String,String> queryParametersDecoded;
  private MultivaluedMap<String,String> queryParametersEncoded;

  @Override
  public MultivaluedMap<String,String> getQueryParameters(final boolean decode) {
    if (decode) {
      if (queryParametersDecoded != null)
        return queryParametersDecoded;
    }
    else if (queryParametersEncoded != null) {
      return queryParametersEncoded;
    }

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

    // FIXME: Make `parameters` unmodifiable, as per the spec.
    return decode ? queryParametersDecoded = parameters : (queryParametersEncoded = parameters);
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
    return getBaseUri().resolve(uri);
  }

  @Override
  public URI relativize(final URI uri) {
    return uri.isAbsolute() ? getRequestUri().relativize(uri) : getRequestUri().relativize(resolve(uri));
  }
}