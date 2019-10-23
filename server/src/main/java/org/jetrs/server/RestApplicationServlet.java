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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Providers;

import org.jetrs.common.core.AnnotationInjector;
import org.jetrs.common.core.HttpHeadersImpl;
import org.jetrs.server.container.ContainerRequestContextImpl;
import org.jetrs.server.container.ContainerResponseContextImpl;
import org.jetrs.server.core.RequestImpl;
import org.libj.util.Classes;

@WebServlet(name="javax.ws.rs.core.Application", urlPatterns="/*")
public class RestApplicationServlet extends RestHttpServlet {
  private static final long serialVersionUID = 3700080355780006441L;

  private static String getApplicationClassName(final WebServlet webServlet) {
    final WebInitParam[] webInitParams = webServlet.initParams();
    if (webInitParams != null)
      for (final WebInitParam webInitParam : webInitParams)
        if ("javax.ws.rs.Application".equals(webInitParam.name()))
          return webInitParam.value();

    return null;
  }

  public RestApplicationServlet() {
    final WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
    if (webServlet == null)
      return;

    final String applicationClassName = getApplicationClassName(webServlet);
    if (applicationClassName == null)
      return;

    Classes.setAnnotationValue(RestApplicationServlet.class.getAnnotation(WebServlet.class), "urlPatterns", new String[0]);
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

  public static AnnotationInjector createAnnotationInjector(final ContainerRequestContext containerRequestContext, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final HttpHeaders headers, final ResourceContext resourceContext) {
    final AnnotationInjector annotationInjector = new AnnotationInjector(containerRequestContext, new RequestImpl(httpServletRequest.getMethod()), httpServletRequest, httpServletResponse, headers, resourceContext.getApplication());
    annotationInjector.setProviders(resourceContext.getProviders(annotationInjector));
    return annotationInjector;
  }

  @SuppressWarnings("unchecked")
  private void service(final HttpServletRequestContext httpServletRequestContext, final HttpServletResponse httpServletResponse) throws IOException {
    final ContainerResponseContextImpl containerResponseContext = new ContainerResponseContextImpl(httpServletResponse, getResourceContext().getWriterInterceptors());
    final HttpHeaders requestHeaders = new HttpHeadersImpl(httpServletRequestContext);
    final StringBuilder accessLogDebug = new StringBuilder();
    accessLogDebug.append("Headers: ").append(requestHeaders);
    final ExecutionContext executionContext = new ExecutionContext(requestHeaders, httpServletResponse, containerResponseContext, getResourceContext());

    final ContainerRequestContextImpl containerRequestContext; // NOTE: This weird construct is done this way to at least somehow make the two objects cohesive
    httpServletRequestContext.setRequestContext(containerRequestContext = new ContainerRequestContextImpl(httpServletRequestContext, containerResponseContext, executionContext, getResourceContext().getReaderInterceptors()));

    final AnnotationInjector annotationInjector = createAnnotationInjector(containerRequestContext, httpServletRequestContext, httpServletResponse, requestHeaders, getResourceContext());
    final Providers providers = getResourceContext().getProviders(annotationInjector);
    try {
      getResourceContext().getContainerFilters().filterPreMatchContainerRequest(containerRequestContext, annotationInjector);
      final ResourceMatch resource = executionContext.filterAndMatch(containerRequestContext, annotationInjector);
      accessLogDebug.append(", Matched: ");
      if (resource == null) {
        accessLogDebug.append("null");
        throw new NotFoundException();
      }

      accessLogDebug.append(resource.getManifest());
      httpServletRequestContext.setResourceManifest(resource.getManifest());
      getResourceContext().getContainerFilters().filterContainerRequest(containerRequestContext, annotationInjector);

      final MediaType[] mediaTypes = resource.getManifest().getResourceAnnotationProcessor(Produces.class).getMediaTypes();
      if (mediaTypes != null)
        containerResponseContext.getStringHeaders().putSingle(HttpHeaders.CONTENT_TYPE, resource.getAccept().toString());
      else
        containerResponseContext.setStatus(204);

      final Object content = resource.getManifest().service(executionContext, containerRequestContext, annotationInjector, getResourceContext().getParamConverterProviders());
      if (content instanceof Response)
        executionContext.setResponse((Response)content);
      else if (content != null)
        containerResponseContext.setEntity(content);

      getResourceContext().getContainerFilters().filterContainerResponse(containerRequestContext, containerResponseContext, annotationInjector);
      executionContext.writeResponse(containerRequestContext, providers);
      // FIXME: There's kind of a mixup of when to use the ContainerResponseContext vs Response.
    }
    catch (final IOException | RuntimeException | ServletException e) {
      final WebApplicationException e1 = e instanceof WebApplicationException ? (WebApplicationException)e : new InternalServerErrorException(e);

      final ExceptionMapper<WebApplicationException> exceptionMapper = providers.getExceptionMapper((Class<WebApplicationException>)e1.getClass());
      final Response response = exceptionMapper != null ? exceptionMapper.toResponse(e1) : e1.getResponse();

      try {
        executionContext.setResponse(response);
        getResourceContext().getContainerFilters().filterContainerResponse(containerRequestContext, containerResponseContext, annotationInjector);
        executionContext.writeResponse(containerRequestContext, providers);
      }
      catch (final WebApplicationException e2) {
        e2.addSuppressed(e1);
        throw e2;
      }
      catch (final IOException | RuntimeException e2) {
        httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        e2.addSuppressed(e1);
        throw e2;
      }

      if (response.getStatusInfo().getFamily() == Response.Status.Family.SERVER_ERROR)
        throw e1;
    }
    finally {
      executionContext.commit();
    }
  }

  @Override
  protected final void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
    try {
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
    catch (final IOException | RuntimeException e) {
      if (e.getCause() instanceof ServletException)
        throw (ServletException)e.getCause();

      throw e;
    }
  }
}