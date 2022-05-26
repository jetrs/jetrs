/* Copyright (c) 2016 JetRS
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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
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

class ContainerResponseContextImpl extends InterceptorContextImpl implements ContainerResponseContext, WriterInterceptorContext {
  private final List<WriterInterceptorEntityProviderResource> writerInterceptors;
  private Response.StatusType status;

  ContainerResponseContextImpl(final HttpServletRequest request, final HttpServletResponse response, final List<WriterInterceptorEntityProviderResource> writerInterceptors) {
    super(request, new HttpHeadersImpl(response));
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
    return headers.getMirrorMap();
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

  private Object entity;

  @Override
  public boolean hasEntity() {
    return entity != null;
  }

  @Override
  public Object getEntity() {
    return entity;
  }

  private Class<?> type;

  @Override
  public Class<?> getEntityClass() {
    return type;
  }

  private Type genericType;

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
    if (entity != null) {
      if (entity instanceof GenericEntity) {
        this.entity = ((GenericEntity<?>)entity).getEntity();
        this.genericType = ((GenericEntity<?>)entity).getType();
        this.type = ((GenericEntity<?>)entity).getRawType();
      }
      else {
        this.entity = entity;
        this.type = entity.getClass();
      }
    }

    setAnnotations(annotations);
    if (mediaType != null)
      getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, mediaType);
  }

  @Override
  public Annotation[] getEntityAnnotations() {
    return getAnnotations();
  }

  private OutputStream outputStream;

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
  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public void setOutputStream(final OutputStream os) {
    this.outputStream = os;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void proceed() throws IOException {
    if (writerInterceptors == null || ++interceptorIndex == writerInterceptors.size()) {
      MessageBodyProvider.writeTo(messageBodyWriter, getEntity(), getEntityClass(), getEntityType(), getEntityAnnotations(), getMediaType(), getHeaders(), getEntityStream());
      getEntityStream().close();
    }
    else if (interceptorIndex < writerInterceptors.size()) {
      final WriterInterceptor writerInterceptor = writerInterceptors.get(interceptorIndex).getSingletonOrNewInstance(annotationInjector);
      writerInterceptor.aroundWriteTo(this);
    }
  }

  private int interceptorIndex = -1;

  private AnnotationInjector annotationInjector;
  @SuppressWarnings("rawtypes")
  private MessageBodyWriter messageBodyWriter;

  void writeBody(final AnnotationInjector annotationInjector, final MessageBodyWriter<?> messageBodyWriter) throws IOException {
    this.annotationInjector = annotationInjector;
    this.messageBodyWriter = messageBodyWriter;
    proceed();
  }
}