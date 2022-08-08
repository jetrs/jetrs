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

import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

class ServerRuntimeContext extends RuntimeContext {
  private final List<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories;
  private final List<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories;
  private final List<ProviderFactory<ContainerRequestFilter>> containerRequestFilterProviderFactories;
  private final List<ProviderFactory<ContainerResponseFilter>> containerResponseFilterProviderFactories;

  private final ServletConfig servletConfig;
  private final ServletContext servletContext;
  private final Application application;
  private final List<ResourceInfoImpl> resourceInfos;
  private final Configuration configuration;

  ServerRuntimeContext(
    final List<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories,
    final List<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final List<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories,
    final List<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories,
    final List<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories,
    final List<ProviderFactory<ContainerRequestFilter>> containerRequestFilterProviderFactories,
    final List<ProviderFactory<ContainerResponseFilter>> containerResponseFilterProviderFactories,
    final ServletConfig servletConfig, final ServletContext servletContext,
    final Application application,
    final List<ResourceInfoImpl> resourceInfos
  ) {
    super(readerInterceptorProviderFactories, writerInterceptorProviderFactories, messageBodyReaderProviderFactories, messageBodyWriterProviderFactories, exceptionMapperProviderFactories);
    this.paramConverterProviderFactories = paramConverterProviderFactories;
    this.preMatchContainerRequestFilterProviderFactories = preMatchContainerRequestFilterProviderFactories;
    this.containerRequestFilterProviderFactories = containerRequestFilterProviderFactories;
    this.containerResponseFilterProviderFactories = containerResponseFilterProviderFactories;
    this.resourceInfos = resourceInfos;
    this.servletConfig = servletConfig;
    this.servletContext = servletContext;
    this.application = application;
    this.configuration = new ConfigurationImpl(application);
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

  Configuration getConfiguration() {
    return configuration;
  }

  List<ResourceInfoImpl> getResourceInfos() {
    return resourceInfos;
  }

  List<ProviderFactory<ParamConverterProvider>> getParamConverterProviderFactories() {
    return paramConverterProviderFactories;
  }

  List<ProviderFactory<ContainerRequestFilter>> getPreMatchContainerRequestFilterProviderFactories() {
    return preMatchContainerRequestFilterProviderFactories;
  }

  List<ProviderFactory<ContainerRequestFilter>> getContainerRequestFilterProviderFactories() {
    return containerRequestFilterProviderFactories;
  }

  List<ProviderFactory<ContainerResponseFilter>> getContainerResponseFilterProviderFactories() {
    return containerResponseFilterProviderFactories;
  }

  @Override
  ServerRequestContext newRequestContext(final Request request) {
    return new ServerRequestContext(this, request);
  }
}