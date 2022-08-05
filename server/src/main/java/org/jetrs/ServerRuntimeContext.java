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

import java.util.Collections;
import java.util.List;

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
  private final List<ProviderFactory<ParamConverterProvider>> paramConverterEntityProviderFactories;
  private final List<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterEntityProviderFactories;
  private final List<ProviderFactory<ContainerRequestFilter>> containerRequestFilterEntityProviderFactories;
  private final List<ProviderFactory<ContainerResponseFilter>> containerResponseFilterEntityProviderFactories;

  private final List<ResourceManifest> resourceManifests;
  private final Application application;
  private final Configuration configuration;

  ServerRuntimeContext(final Application application) {
    this(
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      null,
      application
    );
  }

  ServerRuntimeContext(
    final List<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorEntityProviderFactories,
    final List<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorEntityProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderEntityProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterEntityProviderFactories,
    final List<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperEntityProviderFactories,
    final List<ProviderFactory<ParamConverterProvider>> paramConverterEntityProviderFactories,
    final List<ProviderFactory<ContainerRequestFilter>> preMatchContainerRequestFilterEntityProviderFactories,
    final List<ProviderFactory<ContainerRequestFilter>> containerRequestFilterEntityProviderFactories,
    final List<ProviderFactory<ContainerResponseFilter>> containerResponseFilterEntityProviderFactories,
    final List<ResourceManifest> resourceManifests,
    final Application application
  ) {
    super(readerInterceptorEntityProviderFactories, writerInterceptorEntityProviderFactories, messageBodyReaderEntityProviderFactories, messageBodyWriterEntityProviderFactories, exceptionMapperEntityProviderFactories);
    this.paramConverterEntityProviderFactories = paramConverterEntityProviderFactories;
    this.preMatchContainerRequestFilterEntityProviderFactories = preMatchContainerRequestFilterEntityProviderFactories;
    this.containerRequestFilterEntityProviderFactories = containerRequestFilterEntityProviderFactories;
    this.containerResponseFilterEntityProviderFactories = containerResponseFilterEntityProviderFactories;
    this.resourceManifests = resourceManifests;
    this.application = application;
    this.configuration = new ConfigurationImpl(application);
  }

  Application getApplication() {
    return application;
  }

  Configuration getConfiguration() {
    return configuration;
  }

  @Override
  ServerRequestContext newRequestContext(final Request request) {
    return new ServerRequestContext(
      request,
      readerInterceptorEntityProviderFactories,
      writerInterceptorEntityProviderFactories,
      messageBodyReaderEntityProviderFactories,
      messageBodyWriterEntityProviderFactories,
      exceptionMapperEntityProviderFactories,
      paramConverterEntityProviderFactories,
      preMatchContainerRequestFilterEntityProviderFactories,
      containerRequestFilterEntityProviderFactories,
      containerResponseFilterEntityProviderFactories,
      resourceManifests
    );
  }
}