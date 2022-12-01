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
  private final Application application;
  private ServerRuntimeContext runtimeContext;

  RestHttpServlet(final Application application) {
    this.application = application;
  }

  final ServerRuntimeContext getRuntimeContext() {
    return runtimeContext;
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    super.init(config);
    String servletPath;
    // FIXME: URL-Encode baseUri, but don't double-encode %-encoded values
    final ApplicationPath applicationPath = AnnotationUtil.getAnnotation(application.getClass(), ApplicationPath.class);
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

    final ResourceInfos resourceInfos = new ResourceInfos();
    final ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories = new ArrayList<>();
    final ArrayList<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories = new ArrayList<>();
    final ArrayList<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories = new ArrayList<>();
    final ArrayList<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories = new ArrayList<>();
    final ArrayList<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories = new ArrayList<>();
    final ArrayList<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories = new ArrayList<>();
    final ArrayList<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories = new ArrayList<>();
    final ArrayList<ProviderFactory<ContainerRequestFilter>> containerRequestFilterProviderFactories = new ArrayList<>();
    final ArrayList<ProviderFactory<ContainerResponseFilter>> containerResponseFilterProviderFactories = new ArrayList<>();

    final ServerBootstrap bootstrap = new ServerBootstrap(servletPath,
      readerInterceptorProviderFactories,
      writerInterceptorProviderFactories,
      messageBodyReaderProviderFactories,
      messageBodyWriterProviderFactories,
      exceptionMapperProviderFactories,
      paramConverterProviderFactories,
      preMatchContainerRequestFilterProviderFactories,
      containerRequestFilterProviderFactories,
      containerResponseFilterProviderFactories
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

      final Set<Object> singletons;
      final Set<Class<?>> classes;
      if (application != null) {
        singletons = application.getSingletons();
        classes = application.getClasses();
      }
      else {
        singletons = null;
        classes = null;
      }

      readerInterceptorProviderFactories.sort(Bootstrap.providerResourceComparator);
      writerInterceptorProviderFactories.sort(Bootstrap.providerResourceComparator);

      bootstrap.init(singletons, classes, resourceInfos);

      runtimeContext = new ServerRuntimeContext(
        readerInterceptorProviderFactories,
        writerInterceptorProviderFactories,
        messageBodyReaderProviderFactories,
        messageBodyWriterProviderFactories,
        exceptionMapperProviderFactories,
        paramConverterProviderFactories,
        preMatchContainerRequestFilterProviderFactories,
        containerRequestFilterProviderFactories,
        containerResponseFilterProviderFactories,
        config, getServletContext(),
        application,
        resourceInfos
      );

      final RuntimeDelegate runtimeDelegate = RuntimeDelegate.getInstance();
      if (!(runtimeDelegate instanceof RuntimeDelegateImpl))
        throw new IllegalStateException("Unsupported RuntimeDelegate implementation: " + runtimeDelegate.getClass().getName());

      ((RuntimeDelegateImpl)runtimeDelegate).setRuntimeContext(runtimeContext);
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Throwable t) {
      throw new ServletException(t);
    }
  }
}