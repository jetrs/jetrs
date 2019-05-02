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

package org.jetrs.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.jetrs.server.container.ContainerResponseContextImpl;
import org.jetrs.server.core.AnnotationInjector;
import org.openjax.util.FastArrays;
import org.openjax.util.ObservableList;

public class ExecutionContext {
  private final HttpHeaders requestHeaders;
  private final HttpServletResponse httpServletResponse;
  private final ContainerResponseContextImpl containerResponseContext;
  private final ResourceContext resourceContext;

  public ExecutionContext(final HttpHeaders requestHeaders, final HttpServletResponse httpServletResponse, final ContainerResponseContextImpl containerResponseContext, final ResourceContext resourceContext) {
    this.requestHeaders = requestHeaders;
    this.httpServletResponse = httpServletResponse;
    this.containerResponseContext = containerResponseContext;
    this.resourceContext = resourceContext;
  }

  private List<String> matchedURIs;
  private List<String> decodedMatchedURIs;
  private List<Object> matchedResources;
  private ByteArrayOutputStream entityStream;

  public ResourceMatch[] filterAndMatch(final ContainerRequestContext containerRequestContext) {
    return resourceContext.filterAndMatch(containerRequestContext);
  }

  public ResourceMatch filterAndMatch(final ContainerRequestContext containerRequestContext, final AnnotationInjector annotationInjector) {
    final ResourceMatch[] resources = filterAndMatch(containerRequestContext);
    if (resources == null)
      return null;

    final List<String> matchedURIs = new ArrayList<>(resources.length);
    final List<String> decodedMatchedURIs = new ArrayList<>(resources.length);
    final List<Object> matchedResources = new ObservableList<Object>(new ArrayList<>(resources.length)) {
      @Override
      protected void beforeGet(final int index, final ListIterator<Object> iterator) {
        final Object object = this.target.get(index);
        if (object instanceof Class) {
          try {
            final Object instance = annotationInjector.newResourceInstance((Class<?>)object);
            if (iterator != null)
              iterator.set(instance);
            else
              this.target.set(index, instance);
          }
          catch (final IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new InternalServerErrorException(e);
          }
        }
      }
    };

    for (final ResourceMatch resource : resources) {
      matchedURIs.add(resource.getManifest().getPathPattern().getURI(false));
      decodedMatchedURIs.add(resource.getManifest().getPathPattern().getURI(true));
      matchedResources.add(resource.getManifest().getSingleton() != null ? resource.getManifest().getSingleton() : resource.getManifest().getServiceClass());
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

  Response getResponse() {
    return response;
  }

  void setResponse(final Response response) {
    if (this.response != null)
      throw new IllegalStateException("Response has already been set");

    this.response = response;
    containerResponseContext.setEntityStream(null);
    containerResponseContext.setEntity(response.hasEntity() ? response.getEntity() : null);

    if (response.getStatusInfo() != null) {
      containerResponseContext.setStatusInfo(response.getStatusInfo());
    }
    else {
      containerResponseContext.setStatusInfo(null);
      containerResponseContext.setStatus(response.getStatus());
    }

    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    containerResponseHeaders.clear();

    final MultivaluedMap<String,String> responseHeaders = response.getStringHeaders();
    for (final Map.Entry<String,List<String>> entry : responseHeaders.entrySet())
      for (final String header : entry.getValue())
        containerResponseHeaders.add(entry.getKey(), header);
  }

  public HttpHeaders getRequestHeaders() {
    return requestHeaders;
  }

  private void writeHeader() {
    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    for (final Map.Entry<String,List<String>> entry : containerResponseHeaders.entrySet())
      for (final String header : entry.getValue())
        httpServletResponse.addHeader(entry.getKey(), header);

    httpServletResponse.setStatus(containerResponseContext.getStatus());
  }

  @SuppressWarnings("rawtypes")
  private void writeBody(final Providers providers) throws IOException {
    final Object entity = containerResponseContext.getEntity();
    if (entity == null)
      return;

    final Annotation[] annotations = containerResponseContext.getEntityAnnotations() != null ? FastArrays.concat(containerResponseContext.getEntityAnnotations(), entity.getClass().getAnnotations()) : entity.getClass().getAnnotations();
    containerResponseContext.setAnnotations(annotations);

    final MessageBodyWriter messageBodyWriter = providers.getMessageBodyWriter(containerResponseContext.getEntityClass(), containerResponseContext.getEntityType(), containerResponseContext.getAnnotations(), containerResponseContext.getMediaType());
    if (messageBodyWriter == null)
      throw new WebApplicationException("Could not find MessageBodyWriter for type: " + entity.getClass().getName());

    entityStream = new ByteArrayOutputStream();
    containerResponseContext.setEntityStream(entityStream);
    // Start WriterInterceptor process chain
    containerResponseContext.writeBody(messageBodyWriter);
  }

  void writeResponse(final ContainerRequestContext requestContext, final Providers providers) throws IOException {
    writeHeader();
    if (!HttpMethod.HEAD.equals(requestContext.getMethod()))
      writeBody(providers);
  }

  void commit() throws IOException {
    if (httpServletResponse.isCommitted())
      return;

    if (entityStream != null) {
      final byte[] bytes = entityStream.toByteArray();
      httpServletResponse.addHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length));
      httpServletResponse.getOutputStream().write(bytes);
    }

    // @see ServletResponse#getOutputStream :: "Calling flush() on the ServletOutputStream commits the response."
    httpServletResponse.getOutputStream().flush();
  }
}