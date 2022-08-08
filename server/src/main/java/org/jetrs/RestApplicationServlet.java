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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class RestApplicationServlet extends RestHttpServlet {
  private static final Logger logger = LoggerFactory.getLogger(RestApplicationServlet.class);

  private enum Stage {
    FILTER_REQUEST_PRE_MATCH,
    MATCH,
    FILTER_REQUEST,
    SERVICE,
    FILTER_RESPONSE,
    WRITE_RESPONSE
  }

  RestApplicationServlet(final Application application) {
    super(application);
    if (application != null && getClass().getAnnotation(WebServlet.class) == null)
      throw new UnsupportedOperationException("@WebServlet annotation is missing");
  }

  @Override
  protected final void service(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException, ServletException {
    try {
      final ServerRequestContext requestContext = getRuntimeContext().newRequestContext(new RequestImpl(httpServletRequest.getMethod()));
      requestContext.init(new HttpServletRequestWrapper(httpServletRequest) {
        // NOTE: Check for the existence of the @Consumes header, and subsequently the Content-Type header in the request,
        // NOTE: only if data is expected (i.e. GET, HEAD, DELETE, OPTIONS methods will not have a body and should thus not
        // NOTE: expect a Content-Type header from the request)
        private void checkContentType() {
          final ResourceInfoImpl resourceInfo = requestContext.getResourceMatch().getResourceInfo();
          if (resourceInfo != null && !resourceInfo.checkContentHeader(HttpHeaders.CONTENT_TYPE, Consumes.class, requestContext.getContainerRequestContext()))
            throw new BadRequestException("Request has data yet missing Content-Type header");
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
          checkContentType();
          return httpServletRequest.getInputStream();
        }

        @Override
        public BufferedReader getReader() throws IOException {
          checkContentType();
          return httpServletRequest.getReader();
        }
      }, httpServletResponse);

      Stage stage = null;

      try {
        // (1) Filter Request (Pre-Match)
        stage = Stage.FILTER_REQUEST_PRE_MATCH;
        requestContext.filterPreMatchContainerRequest();

        // (2) Match
        stage = Stage.MATCH;
        if (!requestContext.filterAndMatch())
          throw new NotFoundException();

        // (3) Filter Request
        stage = Stage.FILTER_REQUEST;
        requestContext.filterContainerRequest();

        // (4a) Service
        stage = Stage.SERVICE;
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
              requestContext.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            e1.addSuppressed(e);
            throw e1;
          }

          if (response == null) {
            requestContext.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
            requestContext.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

          e.addSuppressed(e1);
          throw e;
        }
      }
      finally {
        // (7) Commit Response
        requestContext.commitResponse();
      }
    }
    catch (final ServletException | RuntimeException t) {
      if (t.getCause() instanceof ServletException)
        throw (ServletException)t.getCause();

      throw t;
    }
  }
}