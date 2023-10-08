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
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

class ClientImpl implements Client, ConfigurableImpl<Client> {
  private final ConfigurationImpl configuration;
  private final SSLContext sslContext;
  private final HostnameVerifier verifier;
  private final ExecutorService executorService;
  private final ScheduledExecutorService scheduledExecutorService;
  private final long connectTimeout;
  private final long readTimeout;

  ClientImpl(final ConfigurationImpl configuration, final SSLContext sslContext, final HostnameVerifier verifier, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) {
    this.configuration = configuration;
    this.sslContext = sslContext;
    this.verifier = verifier;
    this.executorService = executorService;
    this.scheduledExecutorService = scheduledExecutorService;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  private ClientRuntimeContext newClientRuntimeContext() {
    try {
      final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorComponents = new ArrayList<>();
      final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorComponents = new ArrayList<>();
      final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderComponents = new ArrayList<>();
      final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterComponents = new ArrayList<>();
      final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperComponents = new ArrayList<>();

      final Bootstrap<?> bootstrap = new Bootstrap<>(
        readerInterceptorComponents,
        writerInterceptorComponents,
        messageBodyReaderComponents,
        messageBodyWriterComponents,
        exceptionMapperComponents);

      final Components components = configuration.getComponents();
      if (components != null)
        bootstrap.init(components.instances(), components.classes(), null);
      else
        bootstrap.init(null, null, null);

      return new ClientRuntimeContext(configuration, readerInterceptorComponents, writerInterceptorComponents, messageBodyReaderComponents, messageBodyWriterComponents, exceptionMapperComponents);
    }
    catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    catch (final InstantiationException | IOException e) {
      throw new ProcessingException(e);
    }
    catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException)
        throw (RuntimeException)cause;

      throw new ProcessingException(cause);
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
    return configuration;
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
    return new WebTargetImpl(this, newClientRuntimeContext(), configuration, UriBuilder.fromUri(uri), executorService, scheduledExecutorService, connectTimeout, readTimeout);
  }

  @Override
  public WebTarget target(final URI uri) {
    assertNotClosed();
    return new WebTargetImpl(this, newClientRuntimeContext(), configuration, UriBuilder.fromUri(uri), executorService, scheduledExecutorService, connectTimeout, readTimeout);
  }

  @Override
  public WebTarget target(final UriBuilder uriBuilder) {
    assertNotClosed();
    return new WebTargetImpl(this, newClientRuntimeContext(), configuration, uriBuilder, executorService, scheduledExecutorService, connectTimeout, readTimeout);
  }

  @Override
  public WebTarget target(final Link link) {
    assertNotClosed();
    return new WebTargetImpl(this, newClientRuntimeContext(), configuration, UriBuilder.fromLink(link), executorService, scheduledExecutorService, connectTimeout, readTimeout);
  }

  @Override
  public Invocation.Builder invocation(final Link link) {
    assertNotClosed();
    return new ClientRequestContextImpl.BuilderImpl(this, newClientRuntimeContext(), link.getUri(), executorService, scheduledExecutorService, connectTimeout, readTimeout);
  }
}