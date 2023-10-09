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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;

import org.jetrs.EntityUtil.ConsumableByteArrayInputStream;
import org.libj.io.Streams;

class ResponseImpl extends Response {
  private final RequestContext<?> requestContext;
  private final Providers providers;
  private final int statusCode;
  private final Response.StatusType statusInfo;
  private final HttpHeadersImpl headers;
  private final Map<String,NewCookie> cookies;
  private final boolean hasEntity;
  private InputStream entityStream;
  private Object entityObject;
  final Annotation[] annotations; // FIXME: annotations are not being used, but they need to be used by the MessageBodyWriter.. there's no API to get them out of this
                                  // class
  private boolean closed;

  ResponseImpl(final RequestContext<?> requestContext, final int statusCode, final Response.StatusType statusInfo, final HttpHeadersImpl headers, final Map<String,NewCookie> cookies, final Object entity, final Annotation[] annotations) {
    this.requestContext = requestContext;
    this.providers = requestContext.getProviders();
    this.statusCode = statusCode;
    this.statusInfo = statusInfo;
    this.headers = headers;
    this.cookies = cookies != null ? cookies : Collections.EMPTY_MAP;

    if (this.hasEntity = entity != null) {
      if (entity instanceof InputStream)
        this.entityStream = (InputStream)entity;
      else
        this.entityObject = entity;
    }

    this.annotations = annotations;
  }

  @Override
  public int getStatus() {
    return statusCode;
  }

  @Override
  public StatusType getStatusInfo() {
    return statusInfo;
  }

  private boolean wasEntityConsumed() {
    return hasEntity && ((Consumable)entityStream).isConsumed();
  }

  @Override
  public Object getEntity() {
    if (entityObject != null)
      return entityObject;

    if (closed && wasEntityConsumed())
      throw new IllegalStateException("Response has been closed");

    return readEntity(InputStream.class);
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code entityType} is null.
   */
  @Override
  public <T> T readEntity(final Class<T> entityType) throws IllegalStateException, ResponseProcessingException {
    return readEntity(entityType, null, null);
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code entityType} is null.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T readEntity(final GenericType<T> entityType) throws IllegalStateException, ResponseProcessingException {
    return (T)readEntity(entityType.getRawType(), entityType.getType(), null);
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code entityType} is null.
   */
  @Override
  public <T> T readEntity(final Class<T> entityType, final Annotation[] annotations) throws IllegalStateException, ResponseProcessingException {
    return readEntity(entityType, null, annotations);
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code entityType} is null.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T readEntity(final GenericType<T> entityType, final Annotation[] annotations) throws IllegalStateException, ResponseProcessingException {
    return (T)readEntity(entityType.getRawType(), entityType.getType(), annotations);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T> T readEntity(final Class<T> rawType, final Type genericType, final Annotation[] annotations) throws IllegalStateException, ResponseProcessingException {
    if (providers == null)
      throw new ProcessingException("No providers were registered for required MessageBodyReader for type: " + rawType.getName());

    if (!hasEntity)
      return null;

    if (entityStream == null)
      throw new IllegalStateException("Entity is not backed by an InputStream");

    final MediaType mediaType = headers.getMediaType();
    final MessageBodyReader<T> messageBodyReader = providers.getMessageBodyReader(rawType, genericType, annotations, mediaType);
    if (messageBodyReader == null)
      throw new ProcessingException("Could not find MessageBodyReader for {type=" + rawType.getName() + ", genericType=" + genericType.getTypeName() + ", annotations=" + Arrays.toString(annotations) + ", mediaType=" + mediaType + "}");

    final boolean wasBuffered = wasBuffered();
    if (wasBuffered)
      entityStream.mark(0);
    else if (wasEntityConsumed())
      throw new IllegalStateException("Entity InputStream was previously consumed and not buffered");
    else if (closed)
      throw new IllegalStateException("Response has been closed");

    try {
      final ComponentSet<MessageBodyComponent<ReaderInterceptor>> readerInterceptorComponents = requestContext.getReaderInterceptorComponents();
      if (readerInterceptorComponents == null)
        return (messageBodyReader.readFrom(rawType, genericType, annotations, mediaType, headers, entityStream));

      try (final ReaderInterceptorContextImpl readerInterceptorContext = new ReaderInterceptorContextImpl(rawType, genericType, annotations, headers, entityStream) {
        private int interceptorIndex = -1;
        private Object lastProceeded;

        @Override
        public Object proceed() throws IOException {
          final int size = readerInterceptorComponents.size();
          if (++interceptorIndex < size)
            return lastProceeded = (readerInterceptorComponents.get(interceptorIndex).getSingletonOrFromRequestContext(requestContext)).aroundReadFrom(this);

          if (interceptorIndex == size && getInputStream() != null)
            lastProceeded = ((MessageBodyReader)messageBodyReader).readFrom(getType(), getGenericType(), getAnnotations(), getMediaType(), getHeaders(), getInputStream());

          return lastProceeded;
        }
      }) {
        return EntityUtil.checktNotNull((T)readerInterceptorContext.proceed(), readerInterceptorContext.getAnnotations());
      }
    }
    catch (final Exception e) {
      throw new ResponseProcessingException(this, e);
    }
    finally {
      try {
        if (wasBuffered)
          entityStream.reset();
        else if (!InputStream.class.isAssignableFrom(rawType))
          entityStream.close();
      }
      catch (final IOException e) {
        throw new ResponseProcessingException(this, e);
      }
    }
  }

  @Override
  public boolean hasEntity() {
    return hasEntity;
  }

  private Boolean buffered;

  private boolean wasBuffered() {
    return buffered != null && buffered;
  }

  @Override
  public boolean bufferEntity() {
    if (buffered != null)
      return buffered;

    if (entityStream == null || ((Consumable)entityStream).isConsumed())
      return buffered = Boolean.FALSE;

    final byte[] data;
    try {
      data = Streams.readBytes(entityStream);
      entityStream.close();
    }
    catch (final IOException e) {
      throw new ProcessingException(e);
    }

    entityStream = new ConsumableByteArrayInputStream(data);
    entityStream.mark(0);

    return buffered = Boolean.TRUE;
  }

  @Override
  public void close() throws ProcessingException {
    this.closed = true;
    if (entityStream != null) {
      try {
        entityStream.close();
      }
      catch (final IOException e) {
        throw new ProcessingException(e);
      }
    }
  }

  @Override
  public MediaType getMediaType() {
    return (MediaType)getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
  }

  @Override
  public Locale getLanguage() {
    return headers.getLanguage();
  }

  @Override
  public int getLength() {
    return headers.getLength();
  }

  @Override
  public Set<String> getAllowedMethods() {
    return headers.getAllowedMethods();
  }

  @Override
  public Map<String,NewCookie> getCookies() {
    return cookies;
  }

  @Override
  public EntityTag getEntityTag() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getDate() {
    return headers.getDate();
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
    return Links.getLinks(headers);
  }

  @Override
  public boolean hasLink(final String relation) {
    return Links.hasLink(headers, relation);
  }

  @Override
  public Link getLink(final String relation) {
    return Links.getLink(headers, relation);
  }

  @Override
  public Builder getLinkBuilder(final String relation) {
    return Links.getLinkBuilder(headers, relation);
  }

  @Override
  @Deprecated
  public final MultivaluedArrayMap<String,Object> getMetadata() {
    return getHeaders();
  }

  @Override
  public MultivaluedArrayMap<String,Object> getHeaders() {
    return headers.getMirrorMap();
  }

  @Override
  public MultivaluedArrayMap<String,String> getStringHeaders() {
    return headers;
  }

  @Override
  public String getHeaderString(final String name) {
    return headers.getString(name);
  }
}