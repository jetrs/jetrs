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

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class RestApplicationServlet extends RestHttpServlet {
  private static final Logger logger = LoggerFactory.getLogger(RestApplicationServlet.class);

  RestApplicationServlet(final Application application) {
    super(application);
    if (application != null && getClass().getAnnotation(WebServlet.class) == null)
      throw new UnsupportedOperationException("@WebServlet annotation is missing");
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

  private enum Stage {
    FILTER_REQUEST_PRE_MATCH,
    MATCH,
    FILTER_REQUEST,
    SERVICE,
    FILTER_RESPONSE,
    WRITE_RESPONSE
  }

  private void service(final HttpServletRequestImpl httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException, ServletException {
    final ServerRuntimeContext serverContext = getServerContext();
    final HttpHeadersImpl requestHeaders = new HttpHeadersImpl(httpServletRequest);

    final ServerRequestContext requestContext = serverContext.newRequestContext(new RequestImpl(httpServletRequest.getMethod()));
    requestContext.init(requestHeaders, serverContext.getConfiguration(), serverContext.getApplication(), getServletConfig(), getServletContext(), httpServletRequest, httpServletResponse);

    final ContainerResponseContextImpl containerResponseContext = new ContainerResponseContextImpl(httpServletRequest, httpServletResponse, requestContext);
    requestContext.setContainerResponseContext(containerResponseContext);

    final ContainerRequestContextImpl containerRequestContext; // NOTE: This weird construct is done this way to at least somehow make the two objects cohesive
    httpServletRequest.setRequestContext(containerRequestContext = new ContainerRequestContextImpl(httpServletRequest, requestContext));
    requestContext.setContainerRequestContext(containerRequestContext);

    ResourceMatch resourceMatch = null;
    Stage stage = null;

    try {
      // (1) Filter Request (Pre-Match)
      stage = Stage.FILTER_REQUEST_PRE_MATCH;
      requestContext.filterPreMatchContainerRequest();

      // (2) Match
      stage = Stage.MATCH;
      resourceMatch = requestContext.filterAndMatch();
      requestContext.setResourceMatch(resourceMatch);

      final ResourceManifest resourceManifest = resourceMatch.getManifest();
      requestContext.setResourceInfo(resourceManifest);

      // (3) Filter Request
      stage = Stage.FILTER_REQUEST;
      requestContext.filterContainerRequest();

      // (4a) Service
      stage = Stage.SERVICE;
      httpServletRequest.setResourceManifest(resourceManifest);
      requestContext.service();

      // (5a) Filter Response
      stage = Stage.FILTER_RESPONSE;
      requestContext.filterContainerResponse();

      // (6a) Write Response
      stage = Stage.WRITE_RESPONSE;
      requestContext.writeResponse();
    }
    catch (final IOException | RuntimeException | ServletException e) {
      if (!(e instanceof AbortFilterChainException)) {
        // FIXME: Review [JAX-RS 2.1 3.3.4 2,3]
        // (4b) Error
        final Response response;
        try {
          response = requestContext.setErrorResponse(e);
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

        logger.info(e.getMessage(), e);
      }
      else if (stage == Stage.FILTER_RESPONSE) {
        throw new IllegalStateException("ContainerRequestContext.abortWith(Response) cannot be called from response filter chain");
      }
      else {
        requestContext.setAbortResponse((AbortFilterChainException)e);
      }

      try {
        // (5b) Filter Response
        stage = Stage.FILTER_RESPONSE;
        requestContext.filterContainerResponse();
      }
      catch (final IOException | RuntimeException e1) {
        e.addSuppressed(e1);
      }

      try {
        // (6b) Write Response
        stage = Stage.WRITE_RESPONSE;
        requestContext.writeResponse();
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
      requestContext.commitResponse();
    }
  }
}