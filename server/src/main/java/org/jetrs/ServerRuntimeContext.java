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
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
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

import org.libj.lang.Enumerations;

class ServerRuntimeContext extends RuntimeContext {
  private final ArrayList<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories;
  private final ArrayList<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories;
  private final ArrayList<ProviderFactory<ContainerRequestFilter>> containerRequestFilterProviderFactories;
  private final ArrayList<ProviderFactory<ContainerResponseFilter>> containerResponseFilterProviderFactories;

  private final ServletConfig servletConfig;
  private final ServletContext servletContext;
  private final Application application;
  private final ArrayList<ResourceInfoImpl> resourceInfos;

  ServerRuntimeContext(
    final ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories,
    final ArrayList<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories,
    final ArrayList<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final ArrayList<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final ArrayList<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories,
    final ArrayList<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories,
    final ArrayList<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterProviderFactories,
    final ArrayList<ProviderFactory<ContainerRequestFilter>> containerRequestFilterProviderFactories,
    final ArrayList<ProviderFactory<ContainerResponseFilter>> containerResponseFilterProviderFactories,
    final ServletConfig servletConfig, final ServletContext servletContext,
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

  ArrayList<ProviderFactory<ParamConverterProvider>> getParamConverterProviderFactories() {
    return paramConverterProviderFactories;
  }

  ArrayList<ProviderFactory<ContainerRequestFilter>> getPreMatchContainerRequestFilterProviderFactories() {
    return preMatchContainerRequestFilterProviderFactories;
  }

  ArrayList<ProviderFactory<ContainerRequestFilter>> getContainerRequestFilterProviderFactories() {
    return containerRequestFilterProviderFactories;
  }

  ArrayList<ProviderFactory<ContainerResponseFilter>> getContainerResponseFilterProviderFactories() {
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

  ContainerRequestContextImpl newRequestContext(final Request request) {
    final ContainerRequestContextImpl requestContext = new ContainerRequestContextImpl(propertiesAdapter, this, request) {
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