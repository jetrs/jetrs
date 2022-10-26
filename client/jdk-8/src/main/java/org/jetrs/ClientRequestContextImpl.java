/* Copyright (c) 2022 JetRS
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

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

class ClientRequestContextImpl extends RequestContext<HashMap<String,Object>> implements ClientRequestContext {
  private final RuntimeContext runtimeContext;
  private HashMap<String,Object> properties;
  private HttpHeadersImpl httpHeaders;

  ClientRequestContextImpl(final PropertiesAdapter<HashMap<String,Object>> propertiesAdapter, final RuntimeContext runtimeContext, final Request request) {
    super(propertiesAdapter, runtimeContext, request);
    this.runtimeContext = runtimeContext;
  }

  @Override
  HashMap<String,Object> getProperties() {
    return properties == null ? properties = new HashMap<>() : properties;
  }

  @Override
  HttpHeadersImpl getHttpHeaders() {
    return httpHeaders == null ? httpHeaders = new HttpHeadersImpl() : httpHeaders;
  }

  @Override
  public URI getUri() {
    return null;
  }

  @Override
  public void setUri(final URI uri) {
  }

  @Override
  public MultivaluedArrayMap<String,Object> getHeaders() {
    return getHttpHeaders().getMirrorMap();
  }

  @Override
  public MultivaluedArrayMap<String,String> getStringHeaders() {
    return getHttpHeaders();
  }

  @Override
  public boolean hasEntity() {
    return false;
  }

  @Override
  public Object getEntity() {
    return null;
  }

  @Override
  public Class<?> getEntityClass() {
    return null;
  }

  @Override
  public Type getEntityType() {
    return null;
  }

  @Override
  public void setEntity(final Object entity) {
  }

  @Override
  public void setEntity(final Object entity, final Annotation[] annotations, final MediaType mediaType) {
  }

  @Override
  public Annotation[] getEntityAnnotations() {
    return null;
  }

  @Override
  public OutputStream getEntityStream() {
    return null;
  }

  @Override
  public void setEntityStream(final OutputStream outputStream) {
  }

  @Override
  public Client getClient() {
    return null;
  }

  @Override
  public Configuration getConfiguration() {
    return runtimeContext.getConfiguration();
  }
}