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

import static org.libj.lang.Assertions.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.libj.lang.Systems;
import org.libj.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContainerResponseContextImpl extends InterceptorContextImpl<HttpServletRequest> implements Closeable, ContainerResponseContext, WriterInterceptorContext {
  private static final Logger logger = LoggerFactory.getLogger(ContainerResponseContextImpl.class);
  static final int chunkSize = assertPositive(Systems.getProperty(ServerProperties.CHUNKED_ENCODING_SIZE_SERVER, CommonProperties.CHUNKED_ENCODING_SIZE, CommonProperties.CHUNKED_ENCODING_SIZE_DEFAULT));
  static final int bufferSize = Systems.getProperty(ServerProperties.CONTENT_LENGTH_BUFFER_SERVER, CommonProperties.CONTENT_LENGTH_BUFFER, CommonProperties.CONTENT_LENGTH_BUFFER_DEFAULT);

  private static class NoopOutputStream extends OutputStream {
    int count = 0;

    @Override
    public void write(final int b) {
      ++count;
    }

    @Override
    public void write(final byte[] b) {
      count += b.length;
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
      count += len;
    }
  }

  private final ArrayList<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories;
  private final HttpServletRequest httpServletRequest;
  private final ContainerRequestContextImpl requestContext;
  private final HttpHeadersImpl headers;
  private Response.StatusType status;

  ContainerResponseContextImpl(final PropertiesAdapter<HttpServletRequest> propertiesAdapter, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final ContainerRequestContextImpl requestContext) {
    super(propertiesAdapter);
    this.httpServletRequest = httpServletRequest;
    this.headers = new HttpHeadersImpl(httpServletResponse);
    this.status = Responses.from(httpServletResponse.getStatus());
    this.requestContext = requestContext;
    this.writerInterceptorProviderFactories = requestContext.getWriterInterceptorFactoryList();
  }

  @Override
  HttpServletRequest getProperties() {
    return httpServletRequest;
  }

  @Override
  HttpHeadersImpl getHttpHeaders() {
    return headers;
  }

  @Override
  public HttpHeadersImpl getStringHeaders() {
    return headers;
  }

  @Override
  public int getStatus() {
    return status.getStatusCode();
  }

  @Override
  public void setStatus(final int code) {
    this.status = Responses.from(code);
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
  public MultivaluedArrayMap<String,Object> getHeaders() {
    return getStringHeaders().getMirrorMap();
  }

  @Override
  public Set<String> getAllowedMethods() {
    return getStringHeaders().getAllowedMethods();
  }

  @Override
  public Map<String,NewCookie> getCookies() {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public EntityTag getEntityTag() {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getLastModified() {
    return getStringHeaders().getLastModified();
  }

  @Override
  public URI getLocation() {
    return getStringHeaders().getLocation();
  }

  @Override
  public Set<Link> getLinks() {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasLink(final String relation) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public Link getLink(final String relation) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public Builder getLinkBuilder(final String relation) {
    // TODO: Implement this.
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

  private Class<?> entityClass;

  @Override
  public Class<?> getEntityClass() {
    return entityClass;
  }

  private Type entityType;

  @Override
  public Type getEntityType() {
    return entityType;
  }

  @Override
  public void setEntity(final Object entity) {
    if (entity instanceof GenericEntity) {
      final GenericEntity<?> genericEntity = (GenericEntity<?>)entity;
      this.entity = genericEntity.getEntity();
      this.entityType = genericEntity.getType();
      this.entityClass = genericEntity.getRawType();
    }
    else {
      this.entity = entity;
      this.entityClass = entity == null ? null : entity.getClass();
    }
  }

  @Override
  public void setEntity(final Object entity, final Annotation[] annotations, final MediaType mediaType) {
    setEntity(entity);
    setAnnotations(annotations);
    getStringHeaders().setMediaType(mediaType);
  }

  @Override
  public Annotation[] getEntityAnnotations() {
    return getAnnotations();
  }

  private OutputStream firstOutputStream;
  private OutputStream outputStream;
  private NoopOutputStream noopOutputStream;

  @Override
  public OutputStream getEntityStream() {
    return outputStream;
  }

  @Override
  public void setEntityStream(final OutputStream outputStream) {
    this.outputStream = outputStream;
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
  public int getLength() {
    return getStringHeaders().getLength();
  }

  private int interceptorIndex = -1;
  @SuppressWarnings("rawtypes")
  private MessageBodyWriter messageBodyWriter;

  @Override
  @SuppressWarnings("unchecked")
  public void proceed() throws IOException {
    if (++interceptorIndex < writerInterceptorProviderFactories.size()) {
      writerInterceptorProviderFactories.get(interceptorIndex).getSingletonOrFromRequestContext(requestContext).aroundWriteTo(this);
    }
    else if (interceptorIndex == writerInterceptorProviderFactories.size()) {
      try (final OutputStream entityStream = getOutputStream()) {
        messageBodyWriter.writeTo(getEntity(), getEntityClass(), getEntityType(), getEntityAnnotations(), getMediaType(), getHeaders(), entityStream);
      }
    }
    else {
      throw new IllegalStateException();
    }
  }

  private class BufferedSocketOutputStream extends SafeDirectByteArrayOutputStream {
    private final HttpServletResponse httpServletResponse;
    private final RelegateOutputStream relegateOutputStream;
    private final MessageBodyWriter<?> messageBodyWriter;
    private final boolean isException;
    private boolean isClosed;

    BufferedSocketOutputStream(final HttpServletResponse httpServletResponse, final RelegateOutputStream relegateOutputStream, final int size, final MessageBodyWriter<?> messageBodyWriter, final boolean isException) {
      super(size);
      this.httpServletResponse = httpServletResponse;
      this.relegateOutputStream = relegateOutputStream;
      this.messageBodyWriter = messageBodyWriter;
      this.isException = isException;
    }

    @Override
    boolean beforeOverflow(final int b, final byte[] bs, final int off, final int len) throws IOException {
      isClosed = true;

      if (totalCount != bufferSize)
        throw new IllegalStateException();

      getStringHeaders().add(HttpHeaders.TRANSFER_ENCODING, "chunked");
      httpServletResponse.setBufferSize(chunkSize);

      flushHeaders(httpServletResponse, messageBodyWriter, isException);
      final OutputStream socketOutputStream = httpServletResponse.getOutputStream();
      socketOutputStream.write(buf, 0, count);
      socketOutputStream.write(bs, off, len);
      relegateOutputStream.setTarget(socketOutputStream);

      return false;
    }

    @Override
    public void close() throws IOException {
      if (isClosed)
        return;

      isClosed = true;

      getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, Integer.valueOf(count));
      // httpServletResponse.setBufferSize(Streams.DEFAULT_SOCKET_BUFFER_SIZE); // FIXME: Setting this to a low value significantly reduces performance, so leaving this to the servlet container's default
      flushHeaders(httpServletResponse, messageBodyWriter, isException);
      try (final OutputStream socketOutputStream = httpServletResponse.getOutputStream()) {
        socketOutputStream.write(buf, 0, count);
      }
    }
  }

  private static MediaType getMediaType(final MessageBodyWriter<?> messageBodyWriter) {
    if (messageBodyWriter == null)
      return null;

    final Produces produces = messageBodyWriter.getClass().getAnnotation(Produces.class);
    if (produces == null)
      return null;

    final ServerMediaType[] mediaTypes = ServerMediaType.valueOf(produces.value());
    Arrays.sort(mediaTypes, ServerMediaType.SERVER_QUALITY_COMPARATOR);
    return mediaTypes[0];
  }

  private static MediaType getMediaType(final ResourceMatch resourceMatch) {
    if (resourceMatch == null)
      return null;

    final MediaType contentType = resourceMatch.getProducedMediaTypes()[0];
    if (contentType.isWildcardSubtype()) {
      if (!contentType.isWildcardType() && !"application".equals(contentType.getType()))
        throw new NotAcceptableException();

      if (logger.isWarnEnabled()) logger.warn("Content-Type not specified -- setting to " + MediaType.APPLICATION_OCTET_STREAM);
    }

    return contentType.getParameters().size() > 0 ? MediaTypes.cloneWithoutParameters(contentType) : contentType;
  }

  private void flushHeaders(final HttpServletResponse httpServletResponse, final MessageBodyWriter<?> messageBodyWriter, final boolean isException) throws IOException {
    // [JAX-RS 3.5 and 3.8 9]
    if (hasEntity() && getMediaType() == null) {
      MediaType contentType;
      if (isException) {
        contentType = getMediaType(messageBodyWriter);
      }
      else {
        contentType = getMediaType(requestContext.getResourceMatch());
        if (contentType == null)
          contentType = getMediaType(messageBodyWriter);
      }

      if (contentType == null) {
        if (logger.isWarnEnabled()) logger.warn("Content-Type not specified -- setting to " + MediaType.APPLICATION_OCTET_STREAM);
        contentType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
      }

      setMediaType(contentType);
    }

    final MultivaluedArrayMap<String,String> containerResponseHeaders = getStringHeaders();
    if (containerResponseHeaders.size() > 0) {
      for (final Map.Entry<String,List<String>> entry : containerResponseHeaders.entrySet()) { // [S]
        final List<String> values = entry.getValue();
        final int size = values.size();
        if (size == 0)
          continue;

        final String name = entry.getKey();
        if (size > 1) {
          final AbstractMap.SimpleEntry<HttpHeader<?>,HeaderDelegateImpl<?>> headerDelegate = HeaderDelegateImpl.lookup(name);
          final char[] delimiters = headerDelegate.getKey().getDelimiters();
          if (delimiters.length > 0) {
            httpServletResponse.setHeader(name, CollectionUtil.toString(values, delimiters[0]));
            continue;
          }
        }

        int i = -1;
        if (httpServletResponse.containsHeader(name))
          httpServletResponse.setHeader(name, values.get(++i));

        while (++i < size)
          httpServletResponse.addHeader(name, values.get(i));
      }
    }

    httpServletResponse.setStatus(getStatus());
    httpServletResponse.flushBuffer();
  }

  @SuppressWarnings("rawtypes")
  void writeResponse(final HttpServletResponse httpServletResponse, final ResourceInfoImpl resourceInfo, final boolean isException) throws IOException {
    final Object entity = getEntity();
    if (entity == null) {
      flushHeaders(httpServletResponse, null, isException);
      return;
    }

    final Type methodReturnType;
    final Annotation[] methodAnnotations;
    if (resourceInfo != null) {
      methodReturnType = resourceInfo.getMethodReturnType();
      methodAnnotations = resourceInfo.getMethodAnnotations();
    }
    else {
      methodReturnType = null;
      methodAnnotations = null;
    }

    final MediaType mediaType = getMediaType();
    final MessageBodyWriter messageBodyWriter = requestContext.getProviders().getMessageBodyWriter(getEntityClass(), methodReturnType, methodAnnotations, mediaType != null ? mediaType : MediaType.WILDCARD_TYPE);
    if (messageBodyWriter == null)
      throw new InternalServerErrorException("MessageBodyWriter not found for " + getEntityClass().getName()); // [JAX-RS 4.2.2 7]

    // Start WriterInterceptor process chain
    final boolean isHead = HttpMethod.HEAD.equals(requestContext.getMethod());

    if (firstOutputStream != null) {
      outputStream = firstOutputStream;
      if (outputStream instanceof RelegateOutputStream) {
        final RelegateOutputStream relegateOutputStream = (RelegateOutputStream)outputStream;
        final OutputStream target = relegateOutputStream.getTarget();
        if (target instanceof EntityOutputStream) {
          final EntityOutputStream entityOutputStream = (EntityOutputStream)target;
          if (entityOutputStream.getTarget() != null) {
            // Means this is being called a 2nd time
            httpServletResponse.reset();
            if (entityOutputStream.getTarget() instanceof BufferedSocketOutputStream)
              ((BufferedSocketOutputStream)entityOutputStream.getTarget()).reset();
            else
              entityOutputStream.setTarget(httpServletResponse.getOutputStream());
          }
        }
        else {
          if (logger.isInfoEnabled()) logger.info("Unable to overwrite committed response [" + httpServletResponse.getStatus() + "] -> [" + getStatus() + "]: " + entity);
        }
      }
    }
    else if (isHead) {
      firstOutputStream = outputStream = noopOutputStream = new NoopOutputStream();
    }
    else {
      final RelegateOutputStream relegateOutputStream = new RelegateOutputStream();
      relegateOutputStream.setTarget(new EntityOutputStream() {
        @Override
        @SuppressWarnings("null")
        protected boolean beforeWrite(final int b, final byte[] bs, final int off, final int len) throws IOException {
          if (target == null) {
            final Object contentLength = getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
            final List<String> transferEncoding = getStringHeaders().get(HttpHeaders.TRANSFER_ENCODING);
            final int chunkedIndex = transferEncoding == null ? -1 : transferEncoding.indexOf("chunked");
            if (contentLength != null) {
              if (chunkedIndex >= 0) // NOTE: This means that if "Content-Length" is present, it overrides "Content-Encoding": "chunked" (if present too)
                transferEncoding.remove(chunkedIndex);

              // httpServletResponse.setBufferSize(Streams.DEFAULT_SOCKET_BUFFER_SIZE); // FIXME: Setting this to a low value significantly reduces performance, so leaving this to the servlet container's default
              flushHeaders(httpServletResponse, messageBodyWriter, isException);
              target = httpServletResponse.getOutputStream();
            }
            else if (chunkedIndex >= 0) {
              httpServletResponse.setBufferSize(chunkSize);
              flushHeaders(httpServletResponse, messageBodyWriter, isException);
              target = httpServletResponse.getOutputStream();
            }
            else if (bufferSize < chunkSize) { // Let the servlet container try to detect the Content-Length on its own
              httpServletResponse.setBufferSize(chunkSize);
              target = httpServletResponse.getOutputStream();
            }
            else {
              target = new BufferedSocketOutputStream(httpServletResponse, relegateOutputStream, bufferSize, messageBodyWriter, isException);
            }
          }

          return true;
        }

        @Override
        protected void afterWrite(final int b, final byte[] bs, final int off, final int len) {
          if (!(target instanceof BufferedSocketOutputStream)) {
            relegateOutputStream.setTarget(target);
          }
        }

        @Override
        public void close() throws IOException {
          if (target != null)
            target.close();
        }
      });

      firstOutputStream = outputStream = relegateOutputStream;
    }

    this.messageBodyWriter = messageBodyWriter;
    this.interceptorIndex = -1;
    proceed();

    if (isHead) {
      if (noopOutputStream != null) {
        final int contentLength = noopOutputStream.count;
        if (bufferSize < chunkSize || contentLength < bufferSize)
          getHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, Integer.valueOf(contentLength));
      }

      flushHeaders(httpServletResponse, messageBodyWriter, isException);
    }
  }

  @Override
  public void close() {
    firstOutputStream = null;
    noopOutputStream = null;
    if (outputStream != null) {
      try {
        outputStream.close();
      }
      catch (final Exception e) {
        if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
      }
      finally {
        outputStream = null;
      }
    }
  }
}