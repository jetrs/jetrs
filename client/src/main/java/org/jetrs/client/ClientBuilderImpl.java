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

package org.jetrs.client;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
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

import org.jetrs.common.core.ConfigurableImpl;

public class ClientBuilderImpl extends ClientBuilder implements ConfigurableImpl<ClientBuilder> {
  private ClientConfiguration config;
  private SSLContext sslContext;
  private KeyStore keyStore;
  private char[] password;
  private KeyStore trustStore;
  private HostnameVerifier verifier;
  private ExecutorService executorService;
  private long connectTimeout;
  private long readTimeout;

  private SSLContext newSSLContext()  {
    try {
      // Get a KeyManager and initialize it
      final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, password);

      // Get a TrustManagerFactory with the DEFAULT KEYSTORE, so we have all the
      // certificates in cacerts trusted
      final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);

      // Get the SSLContext to help create SSLSocketFactory
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
      return sslContext;
    }
    catch (final KeyManagementException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
      throw new ProcessingException(e);
    }
  }

  @Override
  public Configuration getConfiguration() {
    return config == null ? config = new ClientConfiguration() : config;
  }

  @Override
  public ClientBuilder withConfig(final Configuration config) {
    if (!(config instanceof ClientConfiguration))
      throw new UnsupportedOperationException("Unsupported type: " + config.getClass().getName());

    this.config = (ClientConfiguration)config;
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
    this.executorService = scheduledExecutorService;
    return this;
  }

  @Override
  public ClientBuilder connectTimeout(final long timeout, final TimeUnit unit) {
    if (timeout < 0)
      throw new IllegalArgumentException("timeout (" + timeout + ") cannot be negative");

    this.connectTimeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
    return this;
  }

  @Override
  public ClientBuilder readTimeout(final long timeout, final TimeUnit unit) {
    if (timeout < 0)
      throw new IllegalArgumentException("timeout (" + timeout + ") cannot be negative");

    this.readTimeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
    return this;
  }

  @Override
  public Client build() {
    return new ClientImpl(config == null ? new ClientConfiguration() : config.clone(), sslContext == null ? sslContext = newSSLContext() : sslContext, verifier, executorService, connectTimeout, readTimeout);
  }
}