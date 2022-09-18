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

import static org.libj.lang.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.libj.lang.Strings;
import org.libj.net.URIComponent;
import org.libj.net.URIs;
import org.libj.net.URLs;

class UriInfoImpl implements UriInfo {
  static String getAbsoluteUri(final HttpServletRequest request) {
    final StringBuilder uri = new StringBuilder(request.getRequestURL().toString());
    final String queryString = request.getQueryString();
    if (queryString != null)
      uri.append('?').append(queryString);

    return uri.toString();
  }

  private final HttpServletRequest httpServletRequest;

  private final String absoluteUri;
  private final String contextPath;
  private final int pathIndex;
  private final int queryIndex;

  private ContainerRequestContextImpl requestContext;

  UriInfoImpl(final HttpServletRequest httpServletRequest, final ContainerRequestContextImpl requestContext) {
    this.httpServletRequest = assertNotNull(httpServletRequest, "httpServletRequest is null");
    this.requestContext = requestContext;
    this.absoluteUri = getAbsoluteUri(httpServletRequest);
    this.contextPath = httpServletRequest.getContextPath() + httpServletRequest.getServletPath();

    int pathIndex = absoluteUri.indexOf('/');
    if (pathIndex < 3)
      throw new IllegalStateException();

    if (absoluteUri.charAt(pathIndex - 1) != ':')
      throw new IllegalStateException();

    if (absoluteUri.charAt(pathIndex + 1) != '/')
      throw new IllegalStateException();

    pathIndex = absoluteUri.indexOf('/', pathIndex + 2);
    if (pathIndex < 0)
      throw new IllegalStateException();

    this.pathIndex = pathIndex += contextPath.length() + 1;
    this.queryIndex = absoluteUri.indexOf('?', pathIndex);

    if (absoluteUri.length() < pathIndex)
      pathEncoded = pathDecoded = "";
  }

  // Must not have leading '/'
  private String pathDecoded;
  private String pathEncoded;

  @Override
  public String getPath() {
    return getPath(true);
  }

  @Override
  public String getPath(final boolean decode) {
    if (decode) {
      if (pathDecoded != null)
        return pathDecoded;

      if (pathEncoded != null)
        return pathDecoded = URLs.decodePath(pathEncoded);
    }
    else if (pathEncoded != null)
      return pathEncoded;

    pathEncoded = queryIndex < 0 ? absoluteUri.substring(pathIndex) : absoluteUri.substring(pathIndex, queryIndex);
    return decode ? pathDecoded = URLs.decodePath(pathEncoded) : pathEncoded;
  }

  private PathSegmentList pathSegmentsDecoded;
  private PathSegmentList pathSegmentsEncoded;

  @Override
  public List<PathSegment> getPathSegments() {
    return getPathSegments(true);
  }

  @Override
  public List<PathSegment> getPathSegments(final boolean decode) {
    if (decode) {
      if (pathSegmentsDecoded != null)
        return pathSegmentsDecoded;

      if (pathSegmentsEncoded != null)
        return this.pathSegmentsDecoded = new PathSegmentList(pathSegmentsEncoded, decode);

      return this.pathSegmentsDecoded = new PathSegmentList(pathEncoded, decode);
    }

    if (pathSegmentsEncoded != null)
      return pathSegmentsEncoded;

    if (pathSegmentsDecoded != null)
      return this.pathSegmentsEncoded = new PathSegmentList(pathSegmentsDecoded, decode);

    return this.pathSegmentsEncoded = new PathSegmentList(pathEncoded, decode);
  }

  private URI requestUri;

  @Override
  public URI getRequestUri() {
    return requestUri == null ? requestUri = URI.create(absoluteUri) : requestUri;
  }

  private URI absolutePath;

  @Override
  public URI getAbsolutePath() {
    return absolutePath == null ? absolutePath = queryIndex < 0 ? getRequestUri() : URI.create(absoluteUri.substring(0, queryIndex)) : absolutePath;
  }

  private URI baseUri;

  @Override
  public URI getBaseUri() {
    if (baseUri != null)
      return baseUri;

    if (absoluteUri.length() < pathIndex) // this can happen if the absoluteUri is the basePath without the trailing '/'
      return baseUri = URI.create(absoluteUri + "/");

    final StringBuilder basePath = new StringBuilder();
    basePath.append(absoluteUri, 0, pathIndex);
    if (!Strings.endsWith(basePath, '/'))
      basePath.append('/');

    return baseUri = URI.create(basePath.toString());
  }

  @Override
  public UriBuilder getRequestUriBuilder() {
    return UriBuilder.fromUri(getRequestUri());
  }

  @Override
  public UriBuilder getAbsolutePathBuilder() {
    return UriBuilder.fromUri(getAbsolutePath());
  }

  @Override
  public UriBuilder getBaseUriBuilder() {
    return UriBuilder.fromUri(getBaseUri());
  }

  private MultivaluedMap<String,String> pathParametersDecoded;
  private MultivaluedMap<String,String> pathParametersEncoded;

  private static final MultivaluedMap<String,String> EMPTY_MAP = new MultivaluedHashMap<>(0); // FIXME: Make this unmodifiable

  @Override
  public MultivaluedMap<String,String> getPathParameters() {
    return getPathParameters(true);
  }

  @Override
  public MultivaluedMap<String,String> getPathParameters(final boolean decode) {
    if (requestContext.getResourceMatches() == null)
      return EMPTY_MAP;

    // FIXME: Note that this code always picks the 1st ResourceMatch to obtain the PathParameters.
    // FIXME: This is done under the assumption that it is not possible to have a situation where
    // FIXME: any other ResourceMatch would be retrieved. Is this truly the case?!
    if (pathParametersEncoded == null)
      pathParametersEncoded = requestContext.getResourceMatches().get(0).getPathParameters();

    if (!decode)
      return pathParametersEncoded;

    if (pathParametersDecoded == null) {
      pathParametersDecoded = new MultivaluedHashMap<>(pathParametersEncoded.size());
      for (final Map.Entry<String,List<String>> entry : pathParametersEncoded.entrySet()) { // [S]
        final List<String> values = entry.getValue();
        if (values instanceof RandomAccess) {
          for (int i = 0, i$ = values.size(); i < i$; ++i) // [RA]
            pathParametersDecoded.add(entry.getKey(), URIComponent.decode(values.get(i)));
        }
        else {
          for (final String value : values) // [L]
            pathParametersDecoded.add(entry.getKey(), URIComponent.decode(value));
        }
      }
    }

    return pathParametersDecoded;
  }

  private MultivaluedMap<String,String> queryParametersDecoded;
  private MultivaluedMap<String,String> queryParametersEncoded;

  @Override
  public MultivaluedMap<String,String> getQueryParameters() {
    return getQueryParameters(true); // FIXME: Make this unmodifiable
  }

  @Override
  public MultivaluedMap<String,String> getQueryParameters(final boolean decode) {
    try {
      if (decode) {
        if (queryParametersDecoded != null)
          return queryParametersDecoded;

        if (queryParametersEncoded != null) {
          queryParametersDecoded = new MultivaluedHashMap<>();
          for (final Map.Entry<String,List<String>> entry : queryParametersEncoded.entrySet()) // [S]
            for (final String value : entry.getValue()) // [L]
              queryParametersDecoded.add(URLDecoder.decode(entry.getKey(), "UTF-8"), URLDecoder.decode(value, "UTF-8"));
        }
      }
      else if (queryParametersEncoded != null) {
        return queryParametersEncoded;
      }

      final MultivaluedMap<String,String> parameters = new MultivaluedHashMap<>();
      if (decode) {
        final String queryString = httpServletRequest.getQueryString();
        if (queryString != null)
          URIs.decodeParameters(parameters, queryString, "UTF-8");
      }
      else {
        final String queryString = httpServletRequest.getQueryString();
        if (queryString != null)
          URIs.decodeParameters(parameters, queryString, null);
      }

      // FIXME: Make `parameters` unmodifiable, as per the spec.
      return decode ? queryParametersDecoded = parameters : (queryParametersEncoded = parameters);
    }
    catch (final UnsupportedEncodingException e) {
      throw new ProcessingException(e);
    }
  }

  @Override
  public List<String> getMatchedURIs() {
    return getMatchedURIs(true);
  }

  @Override
  public List<String> getMatchedURIs(final boolean decode) {
    return requestContext.getResourceMatches() != null ? requestContext.getResourceMatches().getMatchedURIs(decode) : Collections.EMPTY_LIST;
  }

  @Override
  public List<Object> getMatchedResources() {
    // FIXME: Not tested
    return requestContext.getResourceMatches() != null ? requestContext.getResourceMatches().getMatchedResources(requestContext) : Collections.EMPTY_LIST;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException If {@code uri} is null.
   */
  @Override
  public URI resolve(final URI uri) {
    return getBaseUri().resolve(assertNotNull(uri));
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException If {@code uri} is null.
   */
  @Override
  public URI relativize(final URI uri) {
    assertNotNull(uri);
    return URIs.relativize(getRequestUri(), uri.getScheme() != null || uri.getHost() != null ? uri : getBaseUriBuilder()
      .replaceQuery(null)
      .path(uri.getPath())
      .replaceQuery(uri.getQuery())
      .fragment(uri.getFragment())
      .build());
  }

  @Override
  public String toString() {
    return getPath(false);
  }
}