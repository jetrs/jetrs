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

package org.safris.xrs.server;

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
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.RuntimeDelegate;

import org.safris.commons.lang.reflect.Classes;
import org.safris.xrs.server.container.ContainerRequestContextImpl;
import org.safris.xrs.server.container.ContainerResponseContextImpl;
import org.safris.xrs.server.core.ContextInjector;
import org.safris.xrs.server.core.HttpHeadersImpl;
import org.safris.xrs.server.core.RequestImpl;
import org.safris.xrs.server.ext.RuntimeDelegateImpl;

@WebServlet(name="javax.ws.rs.core.Application", urlPatterns="/*")
public class DefaultRESTServlet extends StartupServlet {
  private static final long serialVersionUID = 3700080355780006441L;

  static {
    System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, RuntimeDelegateImpl.class.getName());
  }

  private static String getApplicationClassName(final WebServlet webServlet) {
    final WebInitParam[] webInitParams = webServlet.initParams();
    if (webInitParams != null)
      for (final WebInitParam webInitParam : webInitParams)
        if ("javax.ws.rs.Application".equals(webInitParam.name()))
          return webInitParam.value();

    return null;
  }

  public DefaultRESTServlet() {
    final WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
    if (webServlet == null)
      return;

    final String applicationClassName = getApplicationClassName(webServlet);
    if (applicationClassName == null)
      return;

    Classes.setAnnotationValue(DefaultRESTServlet.class.getAnnotation(WebServlet.class), "urlPatterns", new String[0]);
    Classes.setAnnotationValue(webServlet, "name", applicationClassName);
    if (webServlet.urlPatterns().length > 0 || webServlet.value().length > 0)
      return;

    try {
      final Class<?> applicationClass = Class.forName(applicationClassName);
      final ApplicationPath applicationPath = applicationClass.getAnnotation(ApplicationPath.class);
      if (applicationPath != null)
        Classes.setAnnotationValue(webServlet, "urlPatterns", new String[] {applicationPath.value()});
    }
    catch (final ClassNotFoundException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private void service(final HttpServletRequestContext request, final HttpServletResponse response) throws IOException, ServletException {
    final ContainerResponseContext containerResponseContext = new ContainerResponseContextImpl(response);
    final HttpHeaders httpHeaders = new HttpHeadersImpl(request);
    final ResponseContext responseContext = new ResponseContext(httpHeaders, response, containerResponseContext);

    try {
      final ContainerRequestContext containerRequestContext; // NOTE: This weird construct is done this way to at least somehow make the two object cohesive
      request.setRequestContext(containerRequestContext = new ContainerRequestContextImpl(getExecutionContext(), request, responseContext));

      final ContextInjector injectionContext = ContextInjector.createInjectionContext(containerRequestContext, new RequestImpl(request.getMethod()), httpHeaders, getExecutionContext().getProviders());

      try {
        getExecutionContext().getContainerFilters().filterPreMatchRequest(containerRequestContext, injectionContext);
        getExecutionContext().getContainerFilters().filterPreMatchResponse(containerRequestContext, containerResponseContext, injectionContext);

        if (responseContext.getResponse() != null) {
          responseContext.writeHeader();
          responseContext.writeBody(getExecutionContext().getProviders());
          responseContext.commit();
          return;
        }

        final ResourceManifest manifest; // NOTE: This weird construct is done this way to at least somehow make the two object cohesive
        request.setResourceManifest(manifest = getExecutionContext().filterAndMatch(RequestMatchParams.forContext(containerRequestContext)));

        getExecutionContext().getContainerFilters().filterPostMatchRequest(containerRequestContext, injectionContext);

        if (manifest == null)
          throw new NotFoundException();

        final Produces produces = manifest.getMatcher(Produces.class).getAnnotation();
        if (produces != null)
          containerResponseContext.getStringHeaders().addAll(HttpHeaders.CONTENT_TYPE, produces.value());

        final Object content = manifest.service(containerRequestContext, injectionContext, getExecutionContext().getParamConverterProviders());
        if (content != null)
          containerResponseContext.setEntity(content);

        responseContext.writeBody(getExecutionContext().getProviders());
        getExecutionContext().getContainerFilters().filterPostMatchResponse(containerRequestContext, containerResponseContext, injectionContext);
      }
      catch (final Throwable t) {
        throw t;
      }
      finally {
        responseContext.writeHeader();
      }
    }
    catch (final IOException | ServletException e) {
      throw e;
    }
    catch (final Throwable t) {
      if (t.getCause() instanceof IOException) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        throw (IOException)t.getCause();
      }
      else if (t.getCause() instanceof ServletException) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        throw (ServletException)t.getCause();
      }
      else {
        final StringBuilder message = new StringBuilder(t.getMessage() != null ? t.getMessage() : "");
        if (t.getCause() != null)
          message.append(" ").append(t.getCause().getMessage() != null ? t.getCause().getMessage() : "");

        if (t instanceof ClientErrorException) {
          final ClientErrorException e = (ClientErrorException)t;
          response.sendError(e.getResponse().getStatus(), message.toString());
          return;
        }
        else if (t instanceof WebApplicationException) {
          final WebApplicationException e = (WebApplicationException)t;
          response.sendError(e.getResponse().getStatus(), message.toString());
          if (e.getResponse().getStatus() == HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            throw t;
        }
        else {
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          throw t;
        }
      }

      return;
    }

    responseContext.commit();
  }

  @Override
  protected final void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    service(new HttpServletRequestContext(request) {
      // NOTE: Check for the existence of the @Consumes header, and subsequently the Content-Type header in the request,
      // NOTE: only if data is expected (i.e. GET, HEAD, DELETE, OPTIONS methods will not have a body and should thus not
      // NOTE: expect a Content-Type header from the request)
      private void checkContentType() {
        if (getResourceManifest() != null && !getResourceManifest().checkHeader(HttpHeaders.CONTENT_TYPE, Consumes.class, getRequestContext()))
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
}