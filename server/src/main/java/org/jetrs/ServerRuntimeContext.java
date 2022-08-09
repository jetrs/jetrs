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
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
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

import org.libj.lang.Enumerations;

class ServerRuntimeContext extends RuntimeContext {
  private final List<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories;
  private final List<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories;
  private final List<ProviderFactory<ContainerRequestFilter>> containerRequestFilterProviderFactories;
  private final List<ProviderFactory<ContainerResponseFilter>> containerResponseFilterProviderFactories;

  private final ServletConfig servletConfig;
  private final ServletContext servletContext;
  private final Application application;
  private final List<ResourceInfoImpl> resourceInfos;

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

  private static final PropertiesAdapter<HttpServletRequest> propertiesAdapter = new PropertiesAdapter<HttpServletRequest>() {
    @Override
    Object getProperty(final HttpServletRequest properties, final String name) {
      return properties.getAttribute(name);
    }

    @Override
    Enumeration<String> getPropertyNames(final HttpServletRequest properties) {
      return properties.getAttributeNames();
    }

    @Override
    void setProperty(final HttpServletRequest properties, final String name, final Object value) {
      properties.setAttribute(name, value);
    }

    @Override
    void removeProperty(final HttpServletRequest properties, final String name) {
      properties.removeAttribute(name);
    }

    @Override
    int size(final HttpServletRequest properties) {
      return Enumerations.getSize(properties.getAttributeNames());
    }
  };

  private final ThreadLocal<ContainerRequestContextImpl> threadLocalRequestContext = new ThreadLocal<>();

  @Override
  ContainerRequestContextImpl localRequestContext() {
    return threadLocalRequestContext.get();
  }

  @Override
  ContainerRequestContextImpl newRequestContext(final Request request) {
    final ContainerRequestContextImpl requestContext = new ContainerRequestContextImpl(propertiesAdapter, this, request) {
      @Override
      public void close() throws IOException {
        threadLocalRequestContext.remove();
      }
    };

    threadLocalRequestContext.set(requestContext);
    return requestContext;
  }
}