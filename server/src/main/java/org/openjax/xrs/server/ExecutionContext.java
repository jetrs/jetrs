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

package org.openjax.xrs.server;

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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.fastjax.util.FastArrays;
import org.fastjax.util.ObservableList;
import org.openjax.xrs.server.core.AnnotationInjector;

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

  protected Response getResponse() {
    return response;
  }

  protected void setResponse(final Response response) {
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

  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  private ByteArrayOutputStream outputStream = null;

  private ByteArrayOutputStream getOutputStream() {
    return outputStream == null ? outputStream = new ByteArrayOutputStream() : outputStream;
  }

  private void writeHeader() {
    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();

    for (final Map.Entry<String,List<String>> entry : containerResponseHeaders.entrySet())
      for (final String header : entry.getValue())
        httpServletResponse.addHeader(entry.getKey(), header);

    httpServletResponse.setStatus(containerResponseContext.getStatus());
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void writeBody(final Providers providers) throws IOException {
    final Object entity = containerResponseContext.getEntity();
    if (entity == null)
      return;

    final Annotation[] annotations = containerResponseContext.getEntityAnnotations() != null ? FastArrays.concat(containerResponseContext.getEntityAnnotations(), entity.getClass().getAnnotations()) : entity.getClass().getAnnotations();
    final MessageBodyWriter messageBodyWriter = providers.getMessageBodyWriter(containerResponseContext.getEntityClass(), containerResponseContext.getEntityType(), annotations, containerResponseContext.getMediaType());
    if (messageBodyWriter != null)
      messageBodyWriter.writeTo(entity, containerResponseContext.getEntityClass(), entity.getClass().getGenericSuperclass(), annotations, httpHeaders.getMediaType(), httpHeaders.getRequestHeaders(), getOutputStream());
    else
      throw new WebApplicationException("Could not find MessageBodyWriter for type: " + entity.getClass().getName());
  }

  protected void writeResponse(final Providers providers) throws IOException {
    writeHeader();
    writeBody(providers);
  }

  protected void commit() throws IOException {
    if (httpServletResponse.isCommitted())
      return;

    if (outputStream != null)
      httpServletResponse.getOutputStream().write(outputStream.toByteArray());

    // @see ServletResponse#getOutputStream :: "Calling flush() on the ServletOutputStream commits the response."
    httpServletResponse.getOutputStream().flush();
  }
}