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
  private final ArrayList<Component<ParamConverterProvider>> paramConverterComponents;
  private final ArrayList<Component<ContainerRequestFilter>> preMatchContainerRequestFilterComponents;
  private final ArrayList<Component<ContainerRequestFilter>> containerRequestFilterComponents;
  private final ArrayList<Component<ContainerResponseFilter>> containerResponseFilterComponents;

  private final ServletConfig servletConfig;
  private final ServletContext servletContext;
  private final Application application;
  private final ArrayList<ResourceInfoImpl> resourceInfos;

  ServerRuntimeContext(
    final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorComponents,
    final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorComponents,
    final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderComponents,
    final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterComponents,
    final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperComponents,
    final ArrayList<Component<ParamConverterProvider>> paramConverterComponents,
    final ArrayList<Component<ContainerRequestFilter>> preMatchContainerRequestFilterComponents,
    final ArrayList<Component<ContainerRequestFilter>> containerRequestFilterComponents,
    final ArrayList<Component<ContainerResponseFilter>> containerResponseFilterComponents,
    final ServletConfig servletConfig,
    final ServletContext servletContext,
    final Application application,
    final ArrayList<ResourceInfoImpl> resourceInfos
  ) {
    super(new ConfigurationImpl(application), readerInterceptorComponents, writerInterceptorComponents, messageBodyReaderComponents, messageBodyWriterComponents, exceptionMapperComponents);
    this.paramConverterComponents = paramConverterComponents;
    this.preMatchContainerRequestFilterComponents = preMatchContainerRequestFilterComponents;
    this.containerRequestFilterComponents = containerRequestFilterComponents;
    this.containerResponseFilterComponents = containerResponseFilterComponents;
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

  ArrayList<Component<ParamConverterProvider>> getParamConverterComponents() {
    return paramConverterComponents;
  }

  ArrayList<Component<ContainerRequestFilter>> getPreMatchContainerRequestFilterComponents() {
    return preMatchContainerRequestFilterComponents;
  }

  ArrayList<Component<ContainerRequestFilter>> getContainerRequestFilterComponents() {
    return containerRequestFilterComponents;
  }

  ArrayList<Component<ContainerResponseFilter>> getContainerResponseFilterComponents() {
    return containerResponseFilterComponents;
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