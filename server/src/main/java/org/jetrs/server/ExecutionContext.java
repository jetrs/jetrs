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

package org.jetrs.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.jetrs.common.core.AnnotationInjector;
import org.jetrs.common.core.HttpHeadersImpl;
import org.jetrs.common.core.ResponseImpl;
import org.jetrs.provider.ext.header.CompatibleMediaType;
import org.jetrs.provider.ext.header.ServerMediaType;
import org.jetrs.server.container.ContainerRequestContextImpl;
import org.jetrs.server.container.ContainerResponseContextImpl;
import org.libj.util.ArrayUtil;
import org.libj.util.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionContext {
  private static final Logger logger = LoggerFactory.getLogger(ExecutionContext.class);

  private final HttpHeadersImpl requestHeaders;
  private final HttpServletResponse httpServletResponse;
  private final ContainerResponseContextImpl containerResponseContext;
  private final ServerContext serverContext;

  public ExecutionContext(final HttpHeadersImpl requestHeaders, final HttpServletResponse httpServletResponse, final ContainerResponseContextImpl containerResponseContext, final ServerContext serverContext) {
    this.requestHeaders = requestHeaders;
    this.httpServletResponse = httpServletResponse;
    this.containerResponseContext = containerResponseContext;
    this.serverContext = serverContext;
  }

  private boolean filterAndMatchWasCalled;
  private ResourceMatch resourceMatch;
  private List<String> matchedURIs;
  private List<String> decodedMatchedURIs;
  private List<Object> matchedResources;

  ResourceMatch filterAndMatch(final ContainerRequestContext containerRequestContext, final AnnotationInjector annotationInjector) {
    if (filterAndMatchWasCalled)
      throw new IllegalStateException(getClass().getSimpleName() + ".filterAndMatch(" + ResourceMatch.class.getSimpleName() + "," + AnnotationInjector.class.getSimpleName() + ") has already been called");

    filterAndMatchWasCalled = true;
    final List<ResourceMatch> resourceMatches = serverContext.filterAndMatch(containerRequestContext);
    if (resourceMatches == null)
      throw new NotFoundException();

    if (resourceMatches.size() > 1) {
      final StringBuilder builder = new StringBuilder("Multiple resources match for [\"" + containerRequestContext.getUriInfo() + "\"]: {");
      for (final ResourceMatch resource : resourceMatches)
        builder.append('"').append(resource).append("\",");

      builder.setCharAt(builder.length() - 1, '}');
      logger.warn(builder.toString());
    }

    final List<String> matchedURIs = new ArrayList<>(resourceMatches.size());
    final List<String> decodedMatchedURIs = new ArrayList<>(resourceMatches.size());
    final List<Object> matchedResources = new ObservableList<Object>(new ArrayList<>(resourceMatches.size())) {
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
          catch (final IllegalAccessException | InstantiationException e) {
            throw new InternalServerErrorException(e);
          }
          catch (final InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException)
              throw (RuntimeException)e.getCause();

            throw new InternalServerErrorException(e.getCause());
          }
        }
      }
    };

    for (final ResourceMatch resource : resourceMatches) {
      final ResourceManifest manifest = resource.getManifest();
      matchedURIs.add(manifest.getPathPattern().getURI(false));
      decodedMatchedURIs.add(manifest.getPathPattern().getURI(true));
      matchedResources.add(manifest.getResource());
    }

    this.matchedURIs = Collections.unmodifiableList(matchedURIs);
    this.decodedMatchedURIs = Collections.unmodifiableList(decodedMatchedURIs);
    this.matchedResources = Collections.unmodifiableList(matchedResources);

    return this.resourceMatch = resourceMatches.get(0);
  }

  public ResourceMatch getResourceMatch() {
    return resourceMatch;
  }

  public List<String> getMatchedURIs(final boolean decode) {
    return decode ? this.decodedMatchedURIs : this.matchedURIs;
  }

  public List<Object> getMatchedResources() {
    return this.matchedResources;
  }

  public HttpHeadersImpl getRequestHeaders() {
    return requestHeaders;
  }

  void filterPreMatchContainerRequest(final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector) throws IOException {
    serverContext.getContainerFilters().filterPreMatchContainerRequest(containerRequestContext, annotationInjector);
  }

  void filterContainerRequest(final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector) throws IOException {
    serverContext.getContainerFilters().filterContainerRequest(containerRequestContext, annotationInjector);
  }

  void filterContainerResponse(final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector) throws IOException {
    serverContext.getContainerFilters().filterContainerResponse(containerRequestContext, containerResponseContext, annotationInjector);
  }

  private void setContentType(final ResourceMatch resource) {
    final ServerMediaType[] mediaTypes = resource.getManifest().getResourceAnnotationProcessor(Produces.class).getMediaTypes();
    if (mediaTypes != null)
      containerResponseContext.getStringHeaders().putSingle(HttpHeaders.CONTENT_TYPE, resource.getAccept().toString());
    else
      containerResponseContext.setStatus(Response.Status.NO_CONTENT.getStatusCode());
  }

  void service(final ResourceMatch resource, final ContainerRequestContextImpl containerRequestContext, final AnnotationInjector annotationInjector) throws IOException, ServletException {
    setContentType(resource);

    final Object content = resource.service(containerRequestContext, annotationInjector, serverContext.getParamConverterProviders());
    if (content instanceof Response)
      setResponse((Response)content, resource.getManifest().getMethodAnnotations(), resource.getAccept());
    else if (content != null)
      setEntity(content, resource.getManifest().getMethodAnnotations(), resource.getAccept());
  }

  void setAbortResponse(final AbortFilterChainException e) {
    setResponse(e.getResponse(), null, null);
  }

  // [JAX-RS 2.1 3.3.4 1]
  @SuppressWarnings({"rawtypes", "unchecked"})
  Response setErrorResponse(final Providers providers, final Throwable t) {
    Class cls = t.getClass();
    do {
      final ExceptionMapper exceptionMapper = providers.getExceptionMapper(cls);
      if (exceptionMapper != null) {
        final Response response = exceptionMapper.toResponse(t);
        if (response != null)
          return setResponse(response, null, null);
      }
    }
    while ((cls = cls.getSuperclass()) != null);

    if (t instanceof WebApplicationException)
      return setResponse(((WebApplicationException)t).getResponse(), null, null);

    return null;
  }

  private Response setResponse(final Response response, final Annotation[] annotations, final CompatibleMediaType mediaType) {
    containerResponseContext.setEntityStream(null);

    // FIXME: Have to hack getting the annotations out of the Response
    final Annotation[] responseAnnotations = ((ResponseImpl)response).annotations;
    final Annotation[] entityAnnotations = annotations == null ? responseAnnotations : responseAnnotations == null ? annotations : ArrayUtil.concat(responseAnnotations, annotations);

    // FIXME: Assuming that if the response has a MediaType that's set, it overrides the method's Produces annotation?!
    containerResponseContext.setEntity(response.hasEntity() ? response.getEntity() : null, entityAnnotations, response.getMediaType() != null ? response.getMediaType() : mediaType);

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

    return response;
  }

  private void setEntity(final Object entity, final Annotation[] annotations, final CompatibleMediaType mediaType) {
    containerResponseContext.setEntity(entity, annotations, mediaType);
  }

  private void writeHeader() {
    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    for (final Map.Entry<String,List<String>> entry : containerResponseHeaders.entrySet())
      for (final String header : entry.getValue())
        httpServletResponse.addHeader(entry.getKey(), header);

    httpServletResponse.setStatus(containerResponseContext.getStatus());
  }

  private ByteArrayOutputStream entityStream;

  @SuppressWarnings("rawtypes")
  private void writeBody(final ResourceMatch resource, final Providers providers) throws IOException {
    final Object entity = containerResponseContext.getEntity();
    if (entity == null)
      return;

    final Type methodReturnType;
    final Annotation[] methodAnnotations;
    if (resource != null) {
      final ResourceManifest manifest = resource.getManifest();
      methodReturnType = manifest.getMethodReturnType();
      methodAnnotations = manifest.getMethodAnnotations();
    }
    else {
      methodReturnType = null;
      methodAnnotations = null;
    }

    final MessageBodyWriter messageBodyWriter = providers.getMessageBodyWriter(containerResponseContext.getEntityClass(), methodReturnType, methodAnnotations, containerResponseContext.getMediaType());
    if (messageBodyWriter == null)
      throw new WebApplicationException("Could not find MessageBodyWriter for type: " + entity.getClass().getName());

    if (containerResponseContext.getOutputStream() == null)
      containerResponseContext.setEntityStream(entityStream = new ByteArrayOutputStream(1024));

    // Start WriterInterceptor process chain
    containerResponseContext.writeBody(messageBodyWriter);
  }

  void writeResponse(final ResourceMatch resource, final ContainerRequestContext containerRequestContext, final Providers providers) throws IOException {
    writeHeader();
    if (!HttpMethod.HEAD.equals(containerRequestContext.getMethod()))
      writeBody(resource, providers);
  }

  void commitResponse(final ContainerRequestContext containerRequestContext) throws IOException {
    if (httpServletResponse.isCommitted())
      return;

    try {
      if (entityStream != null) {
        final byte[] bytes = entityStream.toByteArray();
        httpServletResponse.addHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length));
        httpServletResponse.getOutputStream().write(bytes);
      }

      // @see ServletResponse#getOutputStream :: "Calling flush() on the ServletOutputStream commits the response."
      httpServletResponse.getOutputStream().flush();
    }
    finally {
      // Absolutely positively assert that the streams are closed
      if (containerRequestContext.getEntityStream() != null) {
        try {
          containerRequestContext.getEntityStream().close();
        }
        catch (final Throwable t) {
          logger.error(t.getMessage(), t);
        }
      }

      if (containerResponseContext.getOutputStream() != null) {
        try {
          containerResponseContext.getOutputStream().close();
        }
        catch (final Throwable t) {
          logger.error(t.getMessage(), t);
        }
      }
    }
  }
}