/* Copyright (c) 2016 OpenJAX
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

package org.openjax.xrs.server.container;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.openjax.xrs.server.core.HeaderMap;

public class ContainerResponseContextImpl extends ContainerContextImpl implements ContainerResponseContext, WriterInterceptorContext {
  private final Object[] writerInterceptors;
  private final Map<String,Object> properties;
  private final HeaderMap headers;
  private Response.StatusType status;

  private OutputStream outputStream;
  private Object entity;
  private Annotation[] entityAnnotations;
  private Annotation[] annotations;

  private Class<?> type;
  private Type genericType;

  public ContainerResponseContextImpl(final HttpServletResponse response, final Map<String,Object> properties, final Object[] writerInterceptors) {
    super(response.getLocale());
    this.properties = properties;
    this.headers = new HeaderMap(response);
    this.writerInterceptors = writerInterceptors;
    this.status = Response.Status.fromStatusCode(response.getStatus());
  }

  @Override
  public int getStatus() {
    return status.getStatusCode();
  }

  @Override
  public void setStatus(final int code) {
    this.status = Response.Status.fromStatusCode(code);
  }

  @Override
  public Response.StatusType getStatusInfo() {
    return status;
  }

  @Override
  public void setStatusInfo(final Response.StatusType statusInfo) {
    this.status = statusInfo;
  }

  @Override
  public MultivaluedMap<String,Object> getHeaders() {
    return headers.getMirror();
  }

  @Override
  public MultivaluedMap<String,String> getStringHeaders() {
    return headers;
  }

  @Override
  public Set<String> getAllowedMethods() {
    return headers.getAllowedMethods();
  }

  @Override
  public Map<String,NewCookie> getCookies() {
    throw new UnsupportedOperationException();
  }

  @Override
  public EntityTag getEntityTag() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getLastModified() {
    return headers.getLastModified();
  }

  @Override
  public URI getLocation() {
    return headers.getLocation();
  }

  @Override
  public Set<Link> getLinks() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasLink(final String relation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Link getLink(final String relation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Builder getLinkBuilder(final String relation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEntity() {
    return entity != null;
  }

  @Override
  public Object getEntity() {
    return entity;
  }

  @Override
  public Class<?> getEntityClass() {
    return type;
  }

  @Override
  public Type getEntityType() {
    return genericType;
  }

  @Override
  public void setEntity(final Object entity) {
    this.setEntity(entity, null, null);
  }

  @Override
  public void setEntity(final Object entity, final Annotation[] annotations, final MediaType mediaType) {
    // FIXME: What is the getEntityType() method supposed to return???
    this.entity = entity;
    this.type = entity.getClass();
    this.genericType = type.getGenericSuperclass();
    this.entityAnnotations = annotations;
    if (mediaType != null)
      getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, mediaType);
  }

  @Override
  public Annotation[] getEntityAnnotations() {
    return entityAnnotations;
  }

  @Override
  public OutputStream getEntityStream() {
    return outputStream;
  }

  @Override
  public void setEntityStream(final OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  @Override
  public int getLength() {
    return headers.getLength();
  }

  @Override
  public MediaType getMediaType() {
    return headers.getMediaType();
  }







  @Override
  public Object getProperty(final String name) {
    return properties.get(name);
  }

  @Override
  public Collection<String> getPropertyNames() {
    return properties.keySet();
  }

  @Override
  public void setProperty(final String name, final Object object) {
    properties.put(name, object);
  }

  @Override
  public void removeProperty(final String name) {
    properties.remove(name);
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotations;
  }

  @Override
  public void setAnnotations(final Annotation[] annotations) {
    this.annotations = annotations;
  }

  @Override
  public Class<?> getType() {
    return getEntityClass();
  }

  @Override
  public void setType(final Class<?> type) {
    this.type = type;
  }

  @Override
  public Type getGenericType() {
    return genericType;
  }

  @Override
  public void setGenericType(final Type genericType) {
    this.genericType = genericType;
  }

  @Override
  public void setMediaType(final MediaType mediaType) {
    headers.putSingle(HttpHeaders.CONTENT_TYPE, mediaType == null ? null : mediaType.toString());
  }

  @Override
  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public void setOutputStream(final OutputStream os) {
    this.outputStream = os;
  }

  private int interceptorIndex = -1;

  @Override
  public void proceed() throws IOException {
    if (writerInterceptors == null || ++interceptorIndex == writerInterceptors.length - 1)
      ((MessageBodyWriter<Object>)writerInterceptors[interceptorIndex]).writeTo(getEntity(), getEntityClass(), getEntityType(), getEntityAnnotations(), getMediaType(), getHeaders(), getEntityStream());
    else if (interceptorIndex < writerInterceptors.length)
      ((WriterInterceptor)writerInterceptors[interceptorIndex]).aroundWriteTo(this);
  }

  public void writeBody(final MessageBodyWriter<?> messageBodyWriter) throws IOException {
    writerInterceptors[writerInterceptors.length - 1] = messageBodyWriter;
    proceed();
  }
}