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

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

class ServerRuntimeContext extends RuntimeContext {
  private final ArrayList<Component<ParamConverterProvider>> paramConverterProviderFactories;
  private final ArrayList<Component<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories;
  private final ArrayList<Component<ContainerRequestFilter>> containerRequestFilterProviderFactories;
  private final ArrayList<Component<ContainerResponseFilter>> containerResponseFilterProviderFactories;

  private final ServletConfig servletConfig;
  private final ServletContext servletContext;
  private final Application application;
  private final ArrayList<ResourceInfoImpl> resourceInfos;

  ServerRuntimeContext(
    final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorProviderFactories,
    final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorProviderFactories,
    final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperProviderFactories,
    final ArrayList<Component<ParamConverterProvider>> paramConverterProviderFactories,
    final ArrayList<Component<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories,
    final ArrayList<Component<ContainerRequestFilter>> containerRequestFilterProviderFactories,
    final ArrayList<Component<ContainerResponseFilter>> containerResponseFilterProviderFactories,
    final ServletConfig servletConfig,
    final ServletContext servletContext,
    final Application application,
    final ArrayList<ResourceInfoImpl> resourceInfos
  ) {
    super(new ConfigurationImpl(application), readerInterceptorProviderFactories, writerInterceptorProviderFactories, messageBodyReaderProviderFactories, messageBodyWriterProviderFactories, exceptionMapperProviderFactories);
    this.paramConverterProviderFactories = paramConverterProviderFactories;
    this.preMatchContainerRequestFilterProviderFactories = preMatchContainerRequestFilterProviderFactories;
    this.containerRequestFilterProviderFactories = containerRequestFilterProviderFactories;
    this.containerResponseFilterProviderFactories = containerResponseFilterProviderFactories;
    this.resourceInfos = resourceInfos;
    this.servletConfig = servletConfig;
    this.servletContext = servletContext;
    this.application = application;
  }

  ServletConfig getServletConfig() {
    return servletConfig;
  }

  ServletContext getServletContext() {
    return servletContext;
  }

  Application getApplication() {
    return application;
  }

  ArrayList<ResourceInfoImpl> getResourceInfos() {
    return resourceInfos;
  }

  ArrayList<Component<ParamConverterProvider>> getParamConverterProviderFactories() {
    return paramConverterProviderFactories;
  }

  ArrayList<Component<ContainerRequestFilter>> getPreMatchContainerRequestFilterProviderFactories() {
    return preMatchContainerRequestFilterProviderFactories;
  }

  ArrayList<Component<ContainerRequestFilter>> getContainerRequestFilterProviderFactories() {
    return containerRequestFilterProviderFactories;
  }

  ArrayList<Component<ContainerResponseFilter>> getContainerResponseFilterProviderFactories() {
    return containerResponseFilterProviderFactories;
  }

  private final ThreadLocal<ContainerRequestContextImpl> threadLocalRequestContext = new ThreadLocal<>();

  @Override
  ContainerRequestContextImpl localRequestContext() {
    return threadLocalRequestContext.get();
  }

  ContainerRequestContextImpl newRequestContext(final Request request) {
    final ContainerRequestContextImpl requestContext = new ContainerRequestContextImpl(this, request) {
      @Override
      public void close() throws IOException {
        threadLocalRequestContext.remove();
        super.close();
      }
    };

    threadLocalRequestContext.set(requestContext);
    return requestContext;
  }
}