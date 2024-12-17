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

import static org.libj.lang.Assertions.*;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

class WebTargetImpl implements ConfigurableImpl<WebTarget>, WebTarget {
  private final ClientImpl client;
  private final ClientRuntimeContext runtimeContext;
  private final Configuration configuration;
  private final UriBuilder uriBuilder;
  private final ExecutorService executorService;
  private final ScheduledExecutorService scheduledExecutorService;
  private final long connectTimeoutMs;
  private final long readTimeoutMs;

  WebTargetImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final Configuration configuration, final UriBuilder uriBuilder, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeoutMs, final long readTimeoutMs) {
    this.client = client;
    this.runtimeContext = runtimeContext;
    this.configuration = configuration;
    this.uriBuilder = uriBuilder;
    this.executorService = executorService;
    this.scheduledExecutorService = scheduledExecutorService;
    this.connectTimeoutMs = connectTimeoutMs;
    this.readTimeoutMs = readTimeoutMs;
  }

  // FIXME: Need to return new instances for the builder methods, because: "Create a new WebTarget instance..."

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public URI getUri() {
    return uriBuilder.build();
  }

  @Override
  public UriBuilder getUriBuilder() {
    return uriBuilder;
  }

  @Override
  public WebTarget path(final String path) {
    uriBuilder.path(path);
    return this;
  }

  @Override
  public WebTarget resolveTemplate(final String name, final Object value) {
    uriBuilder.resolveTemplate(assertNotNull(name), value);
    return this;
  }

  @Override
  public WebTarget resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) {
    uriBuilder.resolveTemplate(assertNotNull(name), value, encodeSlashInPath);
    return this;
  }

  @Override
  public WebTarget resolveTemplateFromEncoded(final String name, final Object value) {
    uriBuilder.resolveTemplate(assertNotNull(name), value);
    return this;
  }

  @Override
  public WebTarget resolveTemplates(final Map<String,Object> templateValues) {
    uriBuilder.resolveTemplates(templateValues);
    return this;
  }

  @Override
  public WebTarget resolveTemplates(final Map<String,Object> templateValues, final boolean encodeSlashInPath) {
    uriBuilder.resolveTemplates(templateValues, encodeSlashInPath);
    return this;
  }

  @Override
  public WebTarget resolveTemplatesFromEncoded(final Map<String,Object> templateValues) {
    uriBuilder.resolveTemplatesFromEncoded(templateValues);
    return this;
  }

  @Override
  public WebTarget matrixParam(final String name, final Object ... values) {
    uriBuilder.matrixParam(name, values);
    return this;
  }

  @Override
  public WebTarget queryParam(final String name, final Object ... values) {
    uriBuilder.queryParam(name, values);
    return this;
  }

  @Override
  public Invocation.Builder request() {
    client.assertNotClosed();
    return new ClientRequestContextImpl.BuilderImpl(client, runtimeContext, getUri(), executorService, scheduledExecutorService, connectTimeoutMs, readTimeoutMs);
  }

  @Override
  public Invocation.Builder request(final String ... acceptedResponseTypes) {
    client.assertNotClosed();
    return new ClientRequestContextImpl.BuilderImpl(client, runtimeContext, getUri(), executorService, scheduledExecutorService, connectTimeoutMs, readTimeoutMs, acceptedResponseTypes);
  }

  @Override
  public Invocation.Builder request(final MediaType ... acceptedResponseTypes) {
    client.assertNotClosed();
    return new ClientRequestContextImpl.BuilderImpl(client, runtimeContext, getUri(), executorService, scheduledExecutorService, connectTimeoutMs, readTimeoutMs, acceptedResponseTypes);
  }
}