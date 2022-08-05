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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.WriterInterceptor;

abstract class RestHttpServlet extends HttpServlet {
  private ServerRuntimeContext serverContext;

  ServerRuntimeContext getServerContext() {
    return serverContext;
  }

  private final Application application;

  RestHttpServlet(final Application application) {
    this.application = application;
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    String servletPath;
    // FIXME: URL-Encode baseUri, but don't double-encode %-encoded values
    final ApplicationPath applicationPath = application.getClass().getAnnotation(ApplicationPath.class);
    if (applicationPath != null) {
      servletPath = applicationPath.value();
      if (servletPath.endsWith("/"))
        servletPath = servletPath.substring(0, servletPath.length() - 1);
    }
    else {
      final WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
      if (webServlet != null) {
        servletPath = webServlet.urlPatterns()[0];
        if (servletPath.endsWith("/*"))
          servletPath = servletPath.substring(0, servletPath.length() - 2);
        else if (servletPath.endsWith("/"))
          servletPath = servletPath.substring(0, servletPath.length() - 1);
      }
      else {
        servletPath = config.getServletContext().getContextPath();
      }
    }

    final List<ResourceManifest> resourceManifests = new ArrayList<>();
    final List<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorEntityProviderFactories = new ArrayList<>();
    final List<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorEntityProviderFactories = new ArrayList<>();
    final List<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderEntityProviderFactories = new ArrayList<>();
    final List<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterEntityProviderFactories = new ArrayList<>();
    final List<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperEntityProviderFactories = new ArrayList<>();
    final List<ProviderFactory<ParamConverterProvider>> paramConverterEntityProviderFactories = new ArrayList<>();
    final List<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterEntityProviderFactories = new ArrayList<>();
    final List<ProviderFactory<ContainerRequestFilter>> containerRequestFilterEntityProviderFactories = new ArrayList<>();
    final List<ProviderFactory<ContainerResponseFilter>> containerResponseFilterEntityProviderFactories = new ArrayList<>();

    final ServerBootstrap bootstrap = new ServerBootstrap(servletPath,
      readerInterceptorEntityProviderFactories,
      writerInterceptorEntityProviderFactories,
      messageBodyReaderEntityProviderFactories,
      messageBodyWriterEntityProviderFactories,
      exceptionMapperEntityProviderFactories,
      paramConverterEntityProviderFactories,
      preMatchContainerRequestFilterEntityProviderFactories,
      containerRequestFilterEntityProviderFactories,
      containerResponseFilterEntityProviderFactories
    );

    try {
      final Application application;
      if (this.application != null) {
        application = this.application;
      }
      else {
        final String applicationSpec = getInitParameter("javax.ws.rs.Application");
        application = applicationSpec == null ? null : (Application)Class.forName(applicationSpec).getDeclaredConstructor().newInstance();
      }

      final Set<?> singletons;
      final Set<Class<?>> classes;
      if (application != null) {
        singletons = application.getSingletons();
        classes = application.getClasses();
      }
      else {
        singletons = null;
        classes = null;
      }

      readerInterceptorEntityProviderFactories.sort(Bootstrap.providerResourceComparator);
      writerInterceptorEntityProviderFactories.sort(Bootstrap.providerResourceComparator);

      bootstrap.init(singletons, classes, resourceManifests);

      serverContext = new ServerRuntimeContext(
        readerInterceptorEntityProviderFactories,
        writerInterceptorEntityProviderFactories,
        messageBodyReaderEntityProviderFactories,
        messageBodyWriterEntityProviderFactories,
        exceptionMapperEntityProviderFactories,
        paramConverterEntityProviderFactories,
        preMatchContainerRequestFilterEntityProviderFactories,
        containerRequestFilterEntityProviderFactories,
        containerResponseFilterEntityProviderFactories,
        resourceManifests,
        application
      );

      RuntimeDelegate.setInstance(new RuntimeDelegateImpl(serverContext));
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Throwable t) {
      throw new ServletException(t);
    }
  }
}