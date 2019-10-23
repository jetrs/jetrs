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

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Providers;

public class WebTargetImpl implements WebTarget, ClientConfigurable<WebTarget> {
  private final Providers providers;
  private final Configuration config;
  private final UriBuilder builder;

  WebTargetImpl(final Providers providers, final Configuration config, final UriBuilder builder) {
    this.providers = providers;
    this.config = config;
    this.builder = builder;
  }

  @Override
  public Configuration getConfiguration() {
    return config;
  }

  @Override
  public URI getUri() {
    return builder.build();
  }

  @Override
  public UriBuilder getUriBuilder() {
    return builder;
  }

  @Override
  public WebTarget path(final String path) {
    builder.path(path);
    return this;
  }

  @Override
  public WebTarget resolveTemplate(final String name, final Object value) {
    builder.resolveTemplate(Objects.requireNonNull(name), value);
    return this;
  }

  @Override
  public WebTarget resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) {
    builder.resolveTemplate(Objects.requireNonNull(name), value, encodeSlashInPath);
    return this;
  }

  @Override
  public WebTarget resolveTemplateFromEncoded(final String name, final Object value) {
    builder.resolveTemplate(Objects.requireNonNull(name), value);
    return this;
  }

  @Override
  public WebTarget resolveTemplates(final Map<String,Object> templateValues) {
    builder.resolveTemplates(templateValues);
    return this;
  }

  @Override
  public WebTarget resolveTemplates(final Map<String,Object> templateValues, final boolean encodeSlashInPath) {
    builder.resolveTemplates(templateValues, encodeSlashInPath);
    return this;
  }

  @Override
  public WebTarget resolveTemplatesFromEncoded(final Map<String,Object> templateValues) {
    builder.resolveTemplatesFromEncoded(templateValues);
    return this;
  }

  @Override
  public WebTarget matrixParam(final String name, final Object ... values) {
    builder.matrixParam(name, values);
    return this;
  }

  @Override
  public WebTarget queryParam(final String name, final Object ... values) {
    builder.queryParam(name, values);
    return this;
  }

  @Override
  public Invocation.Builder request() {
    try {
      return new InvocationImpl.BuilderImpl(providers, getUri().toURL());
    }
    catch (final MalformedURLException e) {
      throw new ProcessingException(e);
    }
  }

  @Override
  public Invocation.Builder request(final String ... acceptedResponseTypes) {
    try {
      return new InvocationImpl.BuilderImpl(providers, getUri().toURL(), acceptedResponseTypes);
    }
    catch (final MalformedURLException e) {
      throw new ProcessingException(e);
    }
  }

  @Override
  public Invocation.Builder request(final MediaType ... acceptedResponseTypes) {
    try {
      return new InvocationImpl.BuilderImpl(providers, getUri().toURL(), acceptedResponseTypes);
    }
    catch (final MalformedURLException e) {
      throw new ProcessingException(e);
    }
  }
}