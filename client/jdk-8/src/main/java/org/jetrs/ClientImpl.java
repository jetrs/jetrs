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

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import org.libj.lang.Numbers;
import org.libj.lang.Systems;

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
    return new ClientRuntimeContext(configuration);
  }

  private boolean closed;

  void assertNotClosed() {
    if (closed)
      throw new IllegalStateException("Client is closed");
  }

  private ClientConfig clientConfig;

  String getProperty(final String key) {
    final Object value = getConfiguration().getProperties().get(key);
    return value != null ? value.toString() : System.getProperty(key);
  }

  boolean hasProperty(final String key) {
    final Object value = getConfiguration().getProperties().get(key);
    return value != null ? !value.equals("false") : Systems.hasProperty(key);
  }

  ClientConfig getClientConfig() throws NoSuchAlgorithmException {
    final String proxyUri = getProperty(ClientProperties.PROXY_URI);
    final int maxConnectionsPerDestination = Numbers.parseInt(getProperty(ClientProperties.MAX_CONNECTIONS_PER_DESTINATION), ClientProperties.MAX_CONNECTIONS_PER_DESTINATION_DEFAULT);
    if (clientConfig == null)
      return clientConfig = new ClientConfig(sslContext, maxConnectionsPerDestination, proxyUri);

    if (proxyUri == null) {
      if (clientConfig.maxConnectionsPerDestination == maxConnectionsPerDestination && clientConfig.proxyConfig == null)
        return clientConfig;

      clientConfig.release();
      return clientConfig = new ClientConfig(sslContext, maxConnectionsPerDestination, proxyUri);
    }

    final ProxyConfig proxyConfig;
    if (clientConfig.maxConnectionsPerDestination == maxConnectionsPerDestination && (proxyConfig = this.clientConfig.proxyConfig) != null && proxyUri.equals(proxyConfig.toString()))
      return clientConfig;

    return clientConfig = new ClientConfig(sslContext, maxConnectionsPerDestination, proxyUri);
  }

  @Override
  public void close() {
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