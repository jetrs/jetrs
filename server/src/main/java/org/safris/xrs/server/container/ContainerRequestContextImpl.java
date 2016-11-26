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

package org.safris.xrs.server.container;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.safris.commons.util.Locales;
import org.safris.xrs.server.ResponseContext;
import org.safris.xrs.server.core.DefaultSecurityContext;
import org.safris.xrs.server.core.UriInfoImpl;
import org.safris.xrs.server.util.MediaTypes;

public class ContainerRequestContextImpl extends ContainerContextImpl implements ContainerRequestContext {
  private final Map<String,Object> properties = new HashMap<String,Object>();
  private final HttpServletRequest httpServletRequest;

  private final ResponseContext response;
  private final HttpHeaders headers;
  private final UriInfo uriInfo;
  private final List<MediaType> accept;
  private final List<Locale> acceptLanguages;
  private InputStream entityStream;

  public ContainerRequestContextImpl(final HttpServletRequest httpServletRequest, final ResponseContext response) {
    super(httpServletRequest.getLocale());
    final Enumeration<String> attributes = httpServletRequest.getAttributeNames();
    String attribute;
    while (attributes.hasMoreElements())
      properties.put(attribute = attributes.nextElement(), httpServletRequest.getAttribute(attribute));

    this.httpServletRequest = httpServletRequest;
    this.response = response;
    this.accept = Collections.unmodifiableList(Arrays.asList(MediaTypes.parse(httpServletRequest.getHeaders(HttpHeaders.ACCEPT))));
    this.acceptLanguages = Collections.unmodifiableList(Arrays.asList(Locales.parse(httpServletRequest.getHeaders(HttpHeaders.ACCEPT_LANGUAGE))));
    this.headers = response.getHttpHeaders();
    this.uriInfo = new UriInfoImpl(httpServletRequest);
  }

  @Override
  protected MultivaluedMap<String,String> getStringHeaders() {
    return headers.getRequestHeaders();
  }

  @Override
  public Object getProperty(final String name) {
    return properties.get(name);
  }

  @Override
  public Collection<String> getPropertyNames() {
    return properties.keySet();
  }

  @Override
  public void setProperty(final String name, final Object object) {
    properties.put(name, object);
  }

  @Override
  public void removeProperty(final String name) {
    properties.remove(name);
  }

  @Override
  public UriInfo getUriInfo() {
    return uriInfo;
  }

  @Override
  public void setRequestUri(final URI requestUri) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setRequestUri(final URI baseUri, final URI requestUri) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Request getRequest() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMethod() {
    return httpServletRequest.getMethod();
  }

  @Override
  public void setMethod(final String method) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String,String> getHeaders() {
    return headers.getRequestHeaders();
  }

  @Override
  public List<MediaType> getAcceptableMediaTypes() {
    return accept;
  }

  @Override
  public List<Locale> getAcceptableLanguages() {
    return acceptLanguages;
  }

  @Override
  public Map<String,Cookie> getCookies() {
    return headers.getCookies();
  }

  @Override
  public int getLength() {
    return headers.getLength();
  }

  @Override
  public MediaType getMediaType() {
    return headers.getMediaType();
  }

  @Override
  public boolean hasEntity() {
    return headers.getLength() > 0;
  }

  @Override
  public InputStream getEntityStream() {
    if (entityStream != null)
      return entityStream;

    try {
      return httpServletRequest.getInputStream();
    }
    catch (final IOException e) {
      throw new WebApplicationException(e);
    }
  }

  @Override
  public void setEntityStream(final InputStream input) {
    this.entityStream = input;
  }

  private SecurityContext defaultSecurityContext;
  private SecurityContext securityContext;

  @Override
  public SecurityContext getSecurityContext() {
    return securityContext != null ? securityContext : defaultSecurityContext == null ? defaultSecurityContext = new DefaultSecurityContext(httpServletRequest) : defaultSecurityContext;
  }

  @Override
  public void setSecurityContext(final SecurityContext context) {
    this.securityContext = context;
  }

  @Override
  public void abortWith(final Response response) {
    this.response.setResponse(response);
  }
}