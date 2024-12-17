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

import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

public class ClientBuilderImpl extends ClientBuilder implements ConfigurableImpl<ClientBuilder> {
  private ConfigurationImpl configuration;
  private SSLContext sslContext;
  private KeyStore keyStore;
  private char[] password;
  private KeyStore trustStore;
  private HostnameVerifier verifier;
  private ExecutorService executorService;
  private ScheduledExecutorService scheduledExecutorService;
  private long connectTimeoutMs;
  private long readTimeoutMs;

  private SSLContext newSSLContext() {
    try {
      final String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();

      // Get a KeyManager and initialize it
      final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(defaultAlgorithm);
      keyManagerFactory.init(keyStore, password);

      // Get a TrustManagerFactory with the DEFAULT KEYSTORE, so we have all the certificates in cacerts trusted
      final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
      trustManagerFactory.init(trustStore);

      // Get the SSLContext to help create SSLSocketFactory
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
      return sslContext;
    }
    catch (final Exception e) {
      throw new ProcessingException(e);
    }
  }

  @Override
  public Configuration getConfiguration() {
    return configuration == null ? configuration = new ConfigurationImpl() : configuration;
  }

  @Override
  public ClientBuilder withConfig(final Configuration config) {
    this.configuration = config instanceof ConfigurationImpl ? (ConfigurationImpl)config : new ConfigurationImpl(config);
    return this;
  }

  @Override
  public ClientBuilder sslContext(final SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  @Override
  public ClientBuilder keyStore(final KeyStore keyStore, final char[] password) {
    this.keyStore = keyStore;
    this.password = password;
    return this;
  }

  @Override
  public ClientBuilder trustStore(final KeyStore trustStore) {
    this.trustStore = trustStore;
    return this;
  }

  @Override
  public ClientBuilder hostnameVerifier(final HostnameVerifier verifier) {
    this.verifier = verifier;
    return this;
  }

  @Override
  public ClientBuilder executorService(final ExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  @Override
  public ClientBuilder scheduledExecutorService(final ScheduledExecutorService scheduledExecutorService) {
    this.scheduledExecutorService = scheduledExecutorService;
    return this;
  }

  @Override
  public ClientBuilder connectTimeout(final long timeout, final TimeUnit unit) {
    if (timeout < 0)
      throw new IllegalArgumentException("timeout (" + timeout + ") cannot be negative");

    this.connectTimeoutMs = TimeUnit.MILLISECONDS.convert(timeout, unit);
    return this;
  }

  @Override
  public ClientBuilder readTimeout(final long timeout, final TimeUnit unit) {
    if (timeout < 0)
      throw new IllegalArgumentException("timeout (" + timeout + ") cannot be negative");

    this.readTimeoutMs = TimeUnit.MILLISECONDS.convert(timeout, unit);
    return this;
  }

  @Override
  public Client build() {
    return new ClientImpl(configuration == null ? new ConfigurationImpl() : configuration.clone(), sslContext == null ? sslContext = newSSLContext() : sslContext, verifier, executorService, scheduledExecutorService, connectTimeoutMs, readTimeoutMs);
  }
}