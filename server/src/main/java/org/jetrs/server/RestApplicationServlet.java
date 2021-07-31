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

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;

import org.jetrs.common.core.AnnotationInjector;
import org.jetrs.common.core.HttpHeadersImpl;
import org.jetrs.common.core.RequestImpl;
import org.jetrs.server.container.ContainerRequestContextImpl;
import org.jetrs.server.container.ContainerResponseContextImpl;
import org.libj.lang.Classes;

abstract class RestApplicationServlet extends RestHttpServlet {
  private static final long serialVersionUID = 3700080355780006441L;

  private static String getApplicationClassName(final WebServlet webServlet) {
    final WebInitParam[] webInitParams = webServlet.initParams();
    for (final WebInitParam webInitParam : webInitParams)
      if ("javax.ws.rs.Application".equals(webInitParam.name()))
        return webInitParam.value();

    return null;
  }

  RestApplicationServlet(final Application application) {
    super(application);
    final Class<?> applicationClass;
    final ApplicationPath applicationPath;
    final WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
    if (application != null) {
      applicationClass = application.getClass();
      if (webServlet == null)
        throw new UnsupportedOperationException("@WebServlet annotation is missing");
    }
    else {
      if (webServlet == null)
        return;

      final String applicationClassName = getApplicationClassName(webServlet);
      if (applicationClassName == null || webServlet.urlPatterns().length > 0 || webServlet.value().length > 0)
        return;

      try {
        applicationClass = Class.forName(applicationClassName);
      }
      catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }

    applicationPath = applicationClass.getAnnotation(ApplicationPath.class);
    if (applicationPath != null && (webServlet.urlPatterns().length != 1 || !applicationPath.value().equals(webServlet.urlPatterns()[0])))
      Classes.setAnnotationValue(webServlet, "urlPatterns", new String[] {applicationPath.value()});
  }

  @Override
  protected final void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    try {
      service(new HttpServletRequestImpl(request) {
        // NOTE: Check for the existence of the @Consumes header, and subsequently the Content-Type header in the request,
        // NOTE: only if data is expected (i.e. GET, HEAD, DELETE, OPTIONS methods will not have a body and should thus not
        // NOTE: expect a Content-Type header from the request)
        private void checkContentType() {
          if (getResourceManifest() != null && !getResourceManifest().checkContentHeader(HttpHeaders.CONTENT_TYPE, Consumes.class, getRequestContext()))
            throw new BadRequestException("Request has data yet missing Content-Type header");
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
          checkContentType();
          return request.getInputStream();
        }

        @Override
        public BufferedReader getReader() throws IOException {
          checkContentType();
          return request.getReader();
        }
      }, response);
    }
    catch (final ServletException | RuntimeException t) {
      if (t.getCause() instanceof ServletException)
        throw (ServletException)t.getCause();

      throw t;
    }
  }

  private AnnotationInjector createAnnotationInjector(final ContainerRequestContext containerRequestContext, final ContainerResponseContextImpl containerResponseContext, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final HttpHeaders headers, final ServerContext serverContext) {
    final AnnotationInjector annotationInjector = new AnnotationInjector(containerRequestContext, containerResponseContext, new RequestImpl(httpServletRequest.getMethod()), getServletConfig(), getServletContext(), httpServletRequest, httpServletResponse, headers, serverContext.getConfiguration(), serverContext.getApplication());
    annotationInjector.setProviders(serverContext.getProviders(annotationInjector));
    return annotationInjector;
  }

  private enum Stage {
    FILTER_REQUEST_PRE_MATCH,
    MATCH,
    FILTER_REQUEST,
    SERVICE,
    FILTER_RESPONSE,
    WRITE_RESPONSE
  }

  private void service(final HttpServletRequestImpl httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException, ServletException {
    final ServerContext serverContext = getServerContext();
    final ContainerResponseContextImpl containerResponseContext = new ContainerResponseContextImpl(httpServletRequest, httpServletResponse, serverContext.getWriterInterceptors());
    final HttpHeadersImpl requestHeaders = new HttpHeadersImpl(httpServletRequest);
    final ExecutionContext executionContext = new ExecutionContext(requestHeaders, httpServletResponse, containerResponseContext, serverContext);

    final ContainerRequestContextImpl containerRequestContext; // NOTE: This weird construct is done this way to at least somehow make the two objects cohesive
    httpServletRequest.setRequestContext(containerRequestContext = new ContainerRequestContextImpl(httpServletRequest, executionContext, serverContext.getReaderInterceptors()));

    final AnnotationInjector annotationInjector = createAnnotationInjector(containerRequestContext, containerResponseContext, httpServletRequest, httpServletResponse, requestHeaders, serverContext);
    final Providers providers = serverContext.getProviders(annotationInjector);

    ResourceMatch resource = null;
    Stage stage = null;

    try {
      // (1) Filter Request (Pre-Match)
      stage = Stage.FILTER_REQUEST_PRE_MATCH;
      executionContext.filterPreMatchContainerRequest(containerRequestContext, annotationInjector);

      // (2) Match
      stage = Stage.MATCH;
      resource = executionContext.filterAndMatch(containerRequestContext, annotationInjector);

      // (3) Filter Request
      stage = Stage.FILTER_REQUEST;
      executionContext.filterContainerRequest(containerRequestContext, annotationInjector);

      // (4a) Service
      stage = Stage.SERVICE;
      httpServletRequest.setResourceManifest(resource.getManifest());
      executionContext.service(resource, containerRequestContext, annotationInjector);

      // (5a) Filter Response
      stage = Stage.FILTER_RESPONSE;
      executionContext.filterContainerResponse(containerRequestContext, annotationInjector);

      // (6a) Write Response
      stage = Stage.WRITE_RESPONSE;
      executionContext.writeResponse(resource, containerRequestContext, providers);
    }
    catch (final IOException | RuntimeException | ServletException e) {
      if (!(e instanceof AbortFilterChainException)) {
        // FIXME: Review [JAX-RS 2.1 3.3.4 2,3]
        // (4b) Error
        final Response response;
        try {
          response = executionContext.setErrorResponse(providers, e);
        }
        catch (final RuntimeException e1) {
          if (!(e1 instanceof WebApplicationException))
            httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

          e1.addSuppressed(e);
          throw e1;
        }

        if (response == null) {
          httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          throw e;
        }
      }
      else if (stage == Stage.FILTER_RESPONSE) {
        throw new IllegalStateException("ContainerRequestContext.abortWith(Response) cannot be called from response filter chain");
      }
      else {
        executionContext.setAbortResponse((AbortFilterChainException)e);
      }

      try {
        // (5b) Filter Response
        stage = Stage.FILTER_RESPONSE;
        executionContext.filterContainerResponse(containerRequestContext, annotationInjector);
      }
      catch (final IOException | RuntimeException e1) {
        e.addSuppressed(e1);
      }

      try {
        // (6b) Write Response
        stage = Stage.WRITE_RESPONSE;
        executionContext.writeResponse(resource, containerRequestContext, providers);
      }
      catch (final IOException | RuntimeException e1) {
        if (!(e1 instanceof WebApplicationException))
          httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        e.addSuppressed(e1);
        throw e;
      }
    }
    finally {
      // (7) Commit Response
      executionContext.commitResponse(containerRequestContext);
    }
  }
}