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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetrs.ContainerRequestContextImpl.Stage;
import org.libj.net.BufferedServletInputStream;
import org.libj.util.ArrayUtil;
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
  @SuppressWarnings("resource")
  protected final void service(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException, ServletException {
    try (final ContainerRequestContextImpl requestContext = getRuntimeContext().newRequestContext(new RequestImpl(httpServletRequest.getMethod()))) {
      requestContext.init(new HttpServletRequestWrapper(httpServletRequest) {
        private String _queryEncoding;
        private boolean contentTypeChecked = false;
        private Boolean isFormUrlEncoded;
        private ServletInputStream in;
        private Map<String,String[]> parameterMap;
        private Map<String,String[]> queryParameterMap;
        private Map<String,String[]> formParameterMap;

        // NOTE: Check for the existence of the @Consumes header, and subsequently the Content-Type header in the request,
        // NOTE: only if data is expected (i.e. GET, HEAD, DELETE, OPTIONS methods will not have a body and should thus not
        // NOTE: expect a Content-Type header from the request)
        private void checkContentType() {
          if (contentTypeChecked)
            return;

          if (requestContext.getStage() != Stage.FILTER_REQUEST_PRE_MATCH)
            requestContext.getResourceMatch().getResourceInfo().checkContentHeader(HttpHeader.CONTENT_TYPE, Consumes.class, requestContext);

          contentTypeChecked = true;
        }

        @Override
        public String getParameter(final String name) {
          final String[] values = getParameterMap().get(name);
          return values == null ? null : values[0];
        }

        @Override
        public void setAttribute(final String name, final Object o) {
          // NOTE: This it copy+pasted from jetty-server `Request`
          if ("org.eclipse.jetty.server.Request.queryEncoding".equals(name))
            _queryEncoding = o == null ? null : o.toString();

          super.setAttribute(name, o);
        }

        private Map<String,String[]> getQueryParameterMap() {
          if (this.queryParameterMap == null) {
            final String queryString = httpServletRequest.getQueryString();
            this.queryParameterMap = queryString != null ? EntityUtil.toStringArrayMap(EntityUtil.readQueryString(queryString, _queryEncoding)) : Collections.EMPTY_MAP;
          }

          return this.queryParameterMap;
        }

        private Map<String,String[]> getFormParameterMap() {
          if (this.formParameterMap == null) {
            if (isFormUrlEncoded() && httpServletRequest.getContentLength() != 0) {
              try {
                this.formParameterMap = EntityUtil.toStringArrayMap(EntityUtil.readFormParams(getInputStream(), Charset.forName(getCharacterEncoding()), true));
              }
              catch (final IOException e) {
                throw new UncheckedIOException(e);
              }
            }
            else {
              this.formParameterMap = Collections.EMPTY_MAP;
            }
          }

          return this.formParameterMap;
        }

        @Override
        public Map<String,String[]> getParameterMap() {
          if (this.parameterMap == null) {
            final Map<String,String[]> parameterMap = getFormParameterMap();
            final Map<String,String[]> queryParameterMap = getQueryParameterMap();
            if (queryParameterMap.size() > 0) {
              for (final Map.Entry<String,String[]> entry : parameterMap.entrySet()) {
                final String[] value = queryParameterMap.get(entry.getKey());
                if (value != null)
                  entry.setValue(ArrayUtil.concat(entry.getValue(), value));
              }
            }

            this.parameterMap = parameterMap;
          }

          return this.parameterMap;
        }

        @Override
        public Enumeration<String> getParameterNames() {
          return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public String[] getParameterValues(final String name) {
          return getParameterMap().get(name);
        }

        private boolean isFormUrlEncoded() {
          return isFormUrlEncoded == null ? isFormUrlEncoded = MediaType.APPLICATION_FORM_URLENCODED.equals(httpServletRequest.getContentType()) : isFormUrlEncoded;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
          if (this.in == null) {
            if (getContentLengthLong() != -1)
              checkContentType();

            ServletInputStream in = httpServletRequest.getInputStream();
            if (isFormUrlEncoded() && httpServletRequest.getContentLength() != 0) {
              final int maxSize = EntityUtil.getMaxFormContentSize();
              in = new BufferedServletInputStream(in, maxSize);
              in.mark(maxSize);
            }

            this.in = in;
          }

          return this.in;
        }

        @Override
        public BufferedReader getReader() throws IOException {
          return httpServletRequest.getReader();
        }
      }, httpServletResponse);

      ByteArrayOutputStream entityStream = null;

      try {
        // (1) Filter Request (Pre-Match)
        requestContext.setStage(Stage.FILTER_REQUEST_PRE_MATCH);
        requestContext.filterPreMatchContainerRequest();

        // (2) Match
        requestContext.setStage(Stage.MATCH);
        if (!requestContext.filterAndMatch())
          throw new NotFoundException();

        // (3) Filter Request
        requestContext.setStage(Stage.FILTER_REQUEST);
        requestContext.filterContainerRequest();

        // (4a) Service
        requestContext.setStage(Stage.SERVICE);
        requestContext.service();

        // (5a) Filter Response
        requestContext.setStage(Stage.FILTER_RESPONSE);
        requestContext.filterContainerResponse();

        // (6a) Write Response
        requestContext.setStage(Stage.WRITE_RESPONSE);
        entityStream = requestContext.writeResponse();
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
        else if (requestContext.getStage() == Stage.FILTER_RESPONSE) {
          throw new IllegalStateException("ContainerRequestContext.abortWith(Response) cannot be called from response filter chain");
        }
        else {
          requestContext.setAbortResponse((AbortFilterChainException)e);
        }

        try {
          // (5b) Filter Response
          requestContext.setStage(Stage.FILTER_RESPONSE);
          requestContext.filterContainerResponse();
        }
        catch (final IOException | RuntimeException e1) {
          e.addSuppressed(e1);
        }

        try {
          // (6b) Write Response
          requestContext.setStage(Stage.WRITE_RESPONSE);
          entityStream = requestContext.writeResponse();
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
        requestContext.commitResponse(entityStream);
      }
    }
    catch (final ServletException | RuntimeException t) {
      if (t.getCause() instanceof ServletException)
        throw (ServletException)t.getCause();

      throw t;
    }
  }
}