/* Copyright (c) 2019 JetRS
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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.libj.lang.PackageNotFoundException;

class ClientImpl implements Client, ConfigurableImpl<Client> {
  private final Configuration config;
  private final SSLContext sslContext;
  private final HostnameVerifier verifier;
  private final ExecutorService executorService;
  private final long connectTimeout;
  private final long readTimeout;

  ClientImpl(final Configuration config, final SSLContext sslContext, final HostnameVerifier verifier, final ExecutorService executorService, final long connectTimeout, final long readTimeout) {
    this.config = config;
    this.sslContext = sslContext;
    this.verifier = verifier;
    this.executorService = executorService;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  private ClientRuntimeContext runtimeContext;
  private Set<?> singletons;
  private Set<Class<?>> classes;

  private ClientRuntimeContext buildProviders() {
    if (runtimeContext != null && (singletons != null ? singletons.equals(config.getInstances()) : config.getInstances() == null) && (classes != null ? classes.equals(config.getClasses()) : config.getClasses() == null))
      return runtimeContext;

    try {
      final ReaderInterceptorProviders.FactoryList readerInterceptorEntityProviderFactories = new ReaderInterceptorProviders.FactoryList();
      final WriterInterceptorProviders.FactoryList writerInterceptorEntityProviderFactories = new WriterInterceptorProviders.FactoryList();
      final MessageBodyReaderProviders.FactoryList messageBodyReaderEntityProviderFactories = new MessageBodyReaderProviders.FactoryList();
      final MessageBodyWriterProviders.FactoryList messageBodyWriterEntityProviderFactories = new MessageBodyWriterProviders.FactoryList();
      final ExceptionMapperProviders.FactoryList exceptionMapperEntityProviderFactories = new ExceptionMapperProviders.FactoryList();

      final Bootstrap<?> bootstrap = new Bootstrap<>(
        readerInterceptorEntityProviderFactories,
        writerInterceptorEntityProviderFactories,
        messageBodyReaderEntityProviderFactories,
        messageBodyWriterEntityProviderFactories,
        exceptionMapperEntityProviderFactories
      );

      bootstrap.init(config.getInstances(), config.getClasses(), null);
      this.singletons = config.getInstances();
      this.classes = config.getClasses();
      return new ClientRuntimeContext(readerInterceptorEntityProviderFactories, writerInterceptorEntityProviderFactories, messageBodyReaderEntityProviderFactories, messageBodyWriterEntityProviderFactories, exceptionMapperEntityProviderFactories);
    }
    catch (final IllegalAccessException | PackageNotFoundException e) {
      throw new RuntimeException(e);
    }
    catch (final InstantiationException | IOException e) {
      throw new ProcessingException(e);
    }
    catch (final InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException)e.getCause();

      throw new ProcessingException(e.getCause());
    }
  }

  private boolean closed;

  void assertNotClosed() {
    if (closed)
      throw new IllegalStateException("Client is closed");
  }

  @Override
  public void close() {
    // FIXME: Are there any resources to release?
    closed = true;
  }

  @Override
  public Configuration getConfiguration() {
    return config;
  }

  @Override
  public SSLContext getSslContext() {
    return sslContext;
  }

  @Override
  public HostnameVerifier getHostnameVerifier() {
    return verifier;
  }

  @Override
  public WebTarget target(final String uri) {
    assertNotClosed();
    return new WebTargetImpl(this, buildProviders(), config, UriBuilder.fromUri(uri), executorService, connectTimeout, readTimeout);
  }

  @Override
  public WebTarget target(final URI uri) {
    assertNotClosed();
    return new WebTargetImpl(this, buildProviders(), config, UriBuilder.fromUri(uri), executorService, connectTimeout, readTimeout);
  }

  @Override
  public WebTarget target(final UriBuilder uriBuilder) {
    assertNotClosed();
    return new WebTargetImpl(this, buildProviders(), config, uriBuilder, executorService, connectTimeout, readTimeout);
  }

  @Override
  public WebTarget target(final Link link) {
    assertNotClosed();
    return new WebTargetImpl(this, buildProviders(), config, UriBuilder.fromLink(link), executorService, connectTimeout, readTimeout);
  }

  @Override
  public Invocation.Builder invocation(final Link link) {
    assertNotClosed();
    try {
      return new InvocationImpl.BuilderImpl(this, buildProviders(), link.getUri().toURL(), executorService, connectTimeout, readTimeout);
    }
    catch (final MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }
}