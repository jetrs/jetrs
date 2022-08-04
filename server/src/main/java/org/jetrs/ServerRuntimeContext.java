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

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Request;

class ServerRuntimeContext extends RuntimeContext {
  private final ParamConverterProviders.FactoryList paramConverterEntityProviderFactories;
  private final ContainerRequestFilterProviders.FactoryList preMatchContainerRequestFilterEntityProviderFactories;
  private final ContainerRequestFilterProviders.FactoryList containerRequestFilterEntityProviderFactories;
  private final ContainerResponseFilterProviders.FactoryList containerResponseFilterEntityProviderFactories;

  private final List<ResourceManifest> resourceManifests;
  private final Application application;
  private final Configuration configuration;

  ServerRuntimeContext(final Application application) {
    this(
      ReaderInterceptorProviders.FactoryList.EMPTY_LIST,
      WriterInterceptorProviders.FactoryList.EMPTY_LIST,
      MessageBodyReaderProviders.FactoryList.EMPTY_LIST,
      MessageBodyWriterProviders.FactoryList.EMPTY_LIST,
      ExceptionMapperProviders.FactoryList.EMPTY_LIST,
      ParamConverterProviders.FactoryList.EMPTY_LIST,
      ContainerRequestFilterProviders.FactoryList.EMPTY_LIST,
      ContainerRequestFilterProviders.FactoryList.EMPTY_LIST,
      ContainerResponseFilterProviders.FactoryList.EMPTY_LIST,
      null,
      application
    );
  }

  ServerRuntimeContext(
    final ReaderInterceptorProviders.FactoryList readerInterceptorEntityProviderFactories,
    final WriterInterceptorProviders.FactoryList writerInterceptorEntityProviderFactories,
    final MessageBodyReaderProviders.FactoryList messageBodyReaderEntityProviderFactories,
    final MessageBodyWriterProviders.FactoryList messageBodyWriterEntityProviderFactories,
    final ExceptionMapperProviders.FactoryList exceptionMapperEntityProviderFactories,
    final ParamConverterProviders.FactoryList paramConverterEntityProviderFactories,
    final ContainerRequestFilterProviders.FactoryList preMatchContainerRequestFilterEntityProviderFactories,
    final ContainerRequestFilterProviders.FactoryList containerRequestFilterEntityProviderFactories,
    final ContainerResponseFilterProviders.FactoryList containerResponseFilterEntityProviderFactories,
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