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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.lib4j.lang.Arrays;

public class ExecutionContext {
  private final HttpHeaders httpHeaders;
  private final HttpServletResponse httpServletResponse;
  private final ContainerResponseContext containerResponseContext;
  private final ResourceContext resourceContext;

  public ExecutionContext(final HttpHeaders httpHeaders, final HttpServletResponse httpServletResponse, final ContainerResponseContext containerResponseContext, final ResourceContext resourceContext) {
    this.httpHeaders = httpHeaders;
    this.httpServletResponse = httpServletResponse;
    this.containerResponseContext = containerResponseContext;
    this.resourceContext = resourceContext;
  }

  private List<String> matchedURIs;
  private List<String> decodedMatchedURIs;
  private List<Object> matchedResources;

  public ResourceMatch filterAndMatch(final ContainerRequestContext containerRequestContext) {
    final ResourceMatch[] resources = resourceContext.filterAndMatch(containerRequestContext);
    if (resources == null)
      return null;

    final List<String> matchedURIs = new ArrayList<String>(resources.length);
    final List<String> decodedMatchedURIs = new ArrayList<String>(resources.length);
    final List<Object> matchedResources = new ArrayList<Object>(resources.length);

    try {
      for (final ResourceMatch resource : resources) {
        matchedURIs.add(resource.getManifest().getPathPattern().getURI(false));
        decodedMatchedURIs.add(resource.getManifest().getPathPattern().getURI(true));
        // FIXME: This is not efficient if there are multiple resource matches, and only one is needed.
        // FIXME: Need to finish implementing the beforeGet and afterGet methods on ObservableList, and
        // FIXME: have it translate a Class<?> into an Object instance on get operation.
        matchedResources.add(resource.getManifest().getServiceClass().getDeclaredConstructor().newInstance());
      }
    }
    catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
      throw new WebApplicationException(e);
    }

    this.matchedURIs = Collections.unmodifiableList(matchedURIs);
    this.decodedMatchedURIs = Collections.unmodifiableList(decodedMatchedURIs);
    this.matchedResources = Collections.unmodifiableList(matchedResources);

    return resources[0];
  }

  public List<String> getMatchedURIs(final boolean decode) {
    return decode ? this.decodedMatchedURIs : this.matchedURIs;
  }

  public List<Object> getMatchedResources() {
    return this.matchedResources;
  }

  private Response response;

  public Response getResponse() {
    return response;
  }

  public void setResponse(final Response response) {
    this.response = response;
  }

  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  protected void writeHeader() {
    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    if (getResponse() != null) {
      if (getResponse().hasEntity())
        containerResponseContext.setEntity(getResponse().getEntity());

      containerResponseContext.setStatus(getResponse().getStatus());
      containerResponseContext.setStatusInfo(getResponse().getStatusInfo());

      final MultivaluedMap<String,String> responseHeaders = getResponse().getStringHeaders();
      for (final Map.Entry<String,List<String>> entry : responseHeaders.entrySet())
        for (final String header : entry.getValue())
          containerResponseHeaders.add(entry.getKey(), header);
    }

    for (final Map.Entry<String,List<String>> entry : containerResponseHeaders.entrySet())
      for (final String header : entry.getValue())
        httpServletResponse.addHeader(entry.getKey(), header);

    httpServletResponse.setStatus(containerResponseContext.getStatus());
  }

  private ByteArrayOutputStream outputStream = null;

  private ByteArrayOutputStream getOutputStream() {
    return outputStream == null ? outputStream = new ByteArrayOutputStream() : outputStream;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected void writeBody(final Providers providers) throws IOException {
    final Object entity = containerResponseContext.getEntity();
    if (entity == null)
      return;

    if (entity instanceof Response) {
      final Response response = (Response)entity;
      containerResponseContext.setStatus(response.getStatus());
      containerResponseContext.setStatusInfo(response.getStatusInfo());
      containerResponseContext.setEntity(response.getEntity());
      writeBody(providers);
    }
    else {
      final Annotation[] annotations = containerResponseContext.getEntityAnnotations() != null ? Arrays.concat(containerResponseContext.getEntityAnnotations(), entity.getClass().getAnnotations()) : entity.getClass().getAnnotations();
      final MessageBodyWriter messageBodyWriter = providers.getMessageBodyWriter(containerResponseContext.getEntityClass(), containerResponseContext.getEntityType(), annotations, containerResponseContext.getMediaType());
      if (messageBodyWriter != null) {
        messageBodyWriter.writeTo(entity, containerResponseContext.getEntityClass(), entity.getClass().getGenericSuperclass(), annotations, httpHeaders.getMediaType(), httpHeaders.getRequestHeaders(), getOutputStream());
      }
      else {
        throw new WebApplicationException("Could not find MessageBodyWriter for type: " + entity.getClass().getName());
      }
    }
  }

  protected void commit() throws IOException {
    if (outputStream != null)
      httpServletResponse.getOutputStream().write(outputStream.toByteArray());

    // @see ServletResponse#getOutputStream :: "Calling flush() on the ServletOutputStream commits the response."
    httpServletResponse.getOutputStream().flush();
  }
}