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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

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
  private static final String applicationClassName = "javax.ws.rs.Application";
  private final Application application;
  private ServerRuntimeContext runtimeContext;

  RestHttpServlet(final Application application) {
    if (application != null) {
      this.application = application;
    }
    else {
      final String applicationSpec = getInitParameter(applicationClassName);
      if (applicationSpec == null)
        throw new IllegalStateException("Could not find " + applicationClassName);

      try {
        this.application = (Application)Class.forName(applicationSpec).getDeclaredConstructor().newInstance();
      }
      catch (final ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
        throw new IllegalStateException("Could not find " + applicationClassName, e);
      }
    }
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
        final int length = servletPath.length();
        if (servletPath.endsWith("/*"))
          servletPath = servletPath.substring(0, length - 2);
        else if (servletPath.endsWith("/"))
          servletPath = servletPath.substring(0, length - 1);
      }
      else {
        servletPath = config.getServletContext().getContextPath();
      }
    }

    final ResourceInfos resourceInfos = new ResourceInfos();
    final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorProviderFactories = new ArrayList<>();
    final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorProviderFactories = new ArrayList<>();
    final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderProviderFactories = new ArrayList<>();
    final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterProviderFactories = new ArrayList<>();
    final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperProviderFactories = new ArrayList<>();
    final ArrayList<Component<ParamConverterProvider>> paramConverterProviderFactories = new ArrayList<>();
    final ArrayList<Component<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories = new ArrayList<>();
    final ArrayList<Component<ContainerRequestFilter>> containerRequestFilterProviderFactories = new ArrayList<>();
    final ArrayList<Component<ContainerResponseFilter>> containerResponseFilterProviderFactories = new ArrayList<>();

    final ServerBootstrap bootstrap = new ServerBootstrap(servletPath,
      readerInterceptorProviderFactories,
      writerInterceptorProviderFactories,
      messageBodyReaderProviderFactories,
      messageBodyWriterProviderFactories,
      exceptionMapperProviderFactories,
      paramConverterProviderFactories,
      preMatchContainerRequestFilterProviderFactories,
      containerRequestFilterProviderFactories,
      containerResponseFilterProviderFactories);

    try {
      readerInterceptorProviderFactories.sort(Bootstrap.providerResourceComparator);
      writerInterceptorProviderFactories.sort(Bootstrap.providerResourceComparator);

      bootstrap.init(application.getSingletons(), application.getClasses(), resourceInfos);

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
        config,
        getServletContext(),
        application,
        resourceInfos);

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