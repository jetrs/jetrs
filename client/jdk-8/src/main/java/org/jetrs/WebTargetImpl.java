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

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

class WebTargetImpl implements ConfigurableImpl<WebTarget>, WebTarget {
  private final ClientImpl client;
  private final ClientRuntimeContext runtimeContext;
  private final Configuration config;
  private final UriBuilder uriBuilder;
  private final ExecutorService executorService;
  private final ScheduledExecutorService scheduledExecutorService;
  private final long connectTimeout;
  private final long readTimeout;

  WebTargetImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final Configuration config, final UriBuilder uriBuilder, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) {
    this.client = client;
    this.runtimeContext = runtimeContext;
    this.config = config;
    this.uriBuilder = uriBuilder;
    this.executorService = executorService;
    this.scheduledExecutorService = scheduledExecutorService;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  // FIXME: Need to return new instances for the builder methods, because: "Create a new WebTarget instance..."

  @Override
  public Configuration getConfiguration() {
    return config;
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
    try {
      return new InvocationImpl.BuilderImpl(client, runtimeContext, getUri().toURL(), executorService, scheduledExecutorService, connectTimeout, readTimeout);
    }
    catch (final MalformedURLException e) {
      throw new ProcessingException(e);
    }
  }

  @Override
  public Invocation.Builder request(final String ... acceptedResponseTypes) {
    client.assertNotClosed();
    try {
      return new InvocationImpl.BuilderImpl(client, runtimeContext, getUri().toURL(), executorService, scheduledExecutorService, connectTimeout, readTimeout, acceptedResponseTypes);
    }
    catch (final MalformedURLException e) {
      throw new ProcessingException(e);
    }
  }

  @Override
  public Invocation.Builder request(final MediaType ... acceptedResponseTypes) {
    client.assertNotClosed();
    try {
      return new InvocationImpl.BuilderImpl(client, runtimeContext, getUri().toURL(), executorService, scheduledExecutorService, connectTimeout, readTimeout, acceptedResponseTypes);
    }
    catch (final MalformedURLException e) {
      throw new ProcessingException(e);
    }
  }
}