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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
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

class ContainerResponseContextImpl extends InterceptorContextImpl<HttpServletRequest> implements ContainerResponseContext, WriterInterceptorContext {
  private static final Logger logger = LoggerFactory.getLogger(ContainerResponseContextImpl.class);
  static final int chunkSize = assertPositive(Systems.getProperty(ServerProperties.CHUNKED_ENCODING_SIZE_SERVER, CommonProperties.CHUNKED_ENCODING_SIZE, CommonProperties.CHUNKED_ENCODING_SIZE_DEFAULT));
  static final int bufferSize = Systems.getProperty(ServerProperties.CONTENT_LENGTH_BUFFER_SERVER, CommonProperties.CONTENT_LENGTH_BUFFER, CommonProperties.CONTENT_LENGTH_BUFFER_DEFAULT);

  private static class CountingNoopOutputStream extends OutputStream {
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

  private HttpServletRequest httpServletRequest;
  private HttpHeadersImpl headers;
  private Response.StatusType status;
  private ContainerRequestContextImpl requestContext;
  private ComponentSet<MessageBodyComponent<WriterInterceptor>> writerInterceptorComponents;

  ContainerResponseContextImpl(final PropertiesAdapter<HttpServletRequest> propertiesAdapter, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final ContainerRequestContextImpl requestContext) {
    super(propertiesAdapter);
    this.httpServletRequest = httpServletRequest;
    this.headers = new HttpHeadersImpl(httpServletResponse);
    this.status = Responses.from(httpServletResponse.getStatus());
    this.requestContext = requestContext;
    this.writerInterceptorComponents = requestContext.getWriterInterceptorComponents();
  }

  @Override
  HttpServletRequest getProperties() {
    return httpServletRequest;
  }

  @Override
  public final void setProperty(final String name, final Object object) {
    // NOTE: This is done this way because I've found that properties are randomly missing from the implementation of the underlying
    // NOTE: HttpServletRequest's setAttribute method.
    // NOTE: The reason to set the property in the HttpServletRequest is because the JAX-RS contract requires it.
    super.setProperty(name, object);
    httpServletRequest.setAttribute(name, object);
  }

  @Override
  public final void removeProperty(final String name) {
    // NOTE: This is done this way because I've found that properties are randomly missing from the implementation of the underlying
    // NOTE: HttpServletRequest's setAttribute method.
    // NOTE: The reason to set the property in the HttpServletRequest is because the JAX-RS contract requires it.
    super.removeProperty(name);
    httpServletRequest.removeAttribute(name);
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

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code statusInfo} is null.
   */
  @Override
  public void setStatusInfo(final Response.StatusType statusInfo) {
    this.status = Objects.requireNonNull(statusInfo);
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
    return Links.getLinks(getStringHeaders());
  }

  @Override
  public boolean hasLink(final String relation) {
    return Links.hasLink(getStringHeaders(), relation);
  }

  @Override
  public Link getLink(final String relation) {
    return Links.getLink(getStringHeaders(), relation);
  }

  @Override
  public Builder getLinkBuilder(final String relation) {
    return Links.getLinkBuilder(getStringHeaders(), relation);
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
  private CountingNoopOutputStream noopOutputStream;

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

  @SuppressWarnings("rawtypes")
  private MessageBodyWriter messageBodyWriter;
  private int interceptorIndex = -1;

  @Override
  @SuppressWarnings("unchecked")
  public void proceed() throws IOException {
    final int size = writerInterceptorComponents.size();
    if (++interceptorIndex < size) {
      writerInterceptorComponents.get(interceptorIndex).getSingletonOrFromRequestContext(requestContext).aroundWriteTo(this);
    }
    else if (interceptorIndex == size) {
      // This is deliberately not using try-with-resource, because we don't want to close the OutputStream in case there is an exception
      // thrown in the in the messageBodyWriter.writeTo(...) method. This is necessary, because after the exception is thrown,
      // RestApplicationServlet executes the requestContext.writeResponse(...) process again for the response coming from ExceptionMapper.
      @SuppressWarnings("resource")
      final OutputStream entityStream = getOutputStream();
      messageBodyWriter.writeTo(getEntity(), getEntityClass(), getEntityType(), getEntityAnnotations(), getMediaType(), getHeaders(), entityStream);
      entityStream.close();
    }
    else {
      throw new IllegalStateException();
    }
  }

  private class BufferedSocketOutputStream extends SafeDirectByteArrayOutputStream {
    private final HttpServletResponse httpServletResponse;
    private final RelegateOutputStream relegateOutputStream;
    private final MediaType compatibleMediaType;
    private final MessageBodyWriter<?> messageBodyWriter;
    private final Throwable exception;
    private boolean isClosed;

    BufferedSocketOutputStream(final HttpServletResponse httpServletResponse, final RelegateOutputStream relegateOutputStream, final int size, final MediaType compatibleMediaType, final MessageBodyWriter<?> messageBodyWriter, final Throwable exception) {
      super(size);
      this.httpServletResponse = httpServletResponse;
      this.relegateOutputStream = relegateOutputStream;
      this.compatibleMediaType = compatibleMediaType;
      this.messageBodyWriter = messageBodyWriter;
      this.exception = exception;
    }

    @Override
    boolean beforeOverflow(final int b, final byte[] bs, final int off, final int len) throws IOException {
      isClosed = true;

      if (totalCount != bufferSize)
        throw new IllegalStateException();

      getStringHeaders().add(HttpHeaders.TRANSFER_ENCODING, "chunked");
      httpServletResponse.setBufferSize(chunkSize);

      flushHeaders(httpServletResponse, compatibleMediaType, messageBodyWriter, exception);
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
      // FIXME: Setting this to a low value significantly reduces performance, so leaving this to the servlet container's default
      // FIXME: httpServletResponse.setBufferSize(Streams.DEFAULT_SOCKET_BUFFER_SIZE);
      flushHeaders(httpServletResponse, compatibleMediaType, messageBodyWriter, exception);
      try (final OutputStream socketOutputStream = httpServletResponse.getOutputStream()) {
        socketOutputStream.write(buf, 0, count);
      }

      super.close();
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

  private void flushHeaders(final HttpServletResponse httpServletResponse, final MediaType compatibleMediaType, final MessageBodyWriter<?> messageBodyWriter, final Throwable exception) throws IOException {
    // [JAX-RS 2.1 3.5 and 3.8 9]
    if (hasEntity() && getMediaType() == null) {
      MediaType contentType = exception != null ? getMediaType(messageBodyWriter) : compatibleMediaType;
      if (contentType == null || contentType.isWildcardSubtype() && (contentType.isWildcardType() || "application".equals(contentType.getType()))) { // [JAX-RS 2.1 3.8 9]
        if (logger.isWarnEnabled()) { logger.warn("Content-Type not specified -- setting to " + MediaType.APPLICATION_OCTET_STREAM); }
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
  void writeResponse(final HttpServletResponse httpServletResponse, final Throwable exception) throws IOException {
    final Object entity = getEntity();
    if (entity == null) {
      final ResourceMatch resourceMatch = requestContext.getResourceMatch();
      final MediaType[] compatibleMediaTypes;
      flushHeaders(httpServletResponse, resourceMatch == null || (compatibleMediaTypes = resourceMatch.getCompatibleMediaTypes()) == null ? MediaType.WILDCARD_TYPE : compatibleMediaTypes[0], null, exception);
      return;
    }

    final ResourceMatch resourceMatch = requestContext.getResourceMatch();
    final ProvidersImpl providers = requestContext.providers;
    final MessageBodyProviderHolder<?> messageBodyProviderHolder;
    if (exception != null) {
      messageBodyProviderHolder = providers.getMessageBodyWriterHolder(getEntityClass(), getGenericType(), getAnnotations(), requestContext.getAcceptableMediaTypes());
    }
    else {
      final MediaType[] compatibleMediaTypes;
      messageBodyProviderHolder = providers.getMessageBodyWriterHolder(getEntityClass(), getGenericType(), getAnnotations(), resourceMatch == null || (compatibleMediaTypes = resourceMatch.getCompatibleMediaTypes()) == null ? MediaTypes.WILDCARD_TYPE : compatibleMediaTypes);
    }

    // [JAX-RS 2.1 3.8 6]
    if (messageBodyProviderHolder == null) {
      final NotAcceptableException e = new NotAcceptableException();
      e.addSuppressed(exception);
      throw e;
    }

    final MessageBodyWriter messageBodyWriter = (MessageBodyWriter)messageBodyProviderHolder.getProvider();
    final MediaType writerMediaType = messageBodyProviderHolder.getMediaType();

    // Start WriterInterceptor process chain
    final boolean isHead = HttpMethod.HEAD.equals(requestContext.getMethod());

    if (firstOutputStream != null) {
      outputStream = firstOutputStream;
      if (outputStream instanceof RelegateOutputStream) {
        final RelegateOutputStream relegateOutputStream = (RelegateOutputStream)outputStream;
        final OutputStream relegateOutputStreamTarget = relegateOutputStream.getTarget();
        if (relegateOutputStreamTarget instanceof EntityOutputStream) {
          final EntityOutputStream entityOutputStream = (EntityOutputStream)relegateOutputStreamTarget;
          final OutputStream entityOutputStreamTarget = entityOutputStream.getTarget();
          if (entityOutputStreamTarget != null) {
            // Means this is being called a 2nd time
            httpServletResponse.reset();
            if (entityOutputStreamTarget instanceof BufferedSocketOutputStream)
              ((BufferedSocketOutputStream)entityOutputStreamTarget).reset();
            else
              entityOutputStream.setTarget(httpServletResponse.getOutputStream());
          }
        }
        else {
          if (logger.isInfoEnabled()) { logger.info("Unable to overwrite committed response [" + httpServletResponse.getStatus() + "] -> [" + getStatus() + "]: " + entity); }
        }
      }
    }
    else if (isHead) {
      firstOutputStream = outputStream = noopOutputStream = new CountingNoopOutputStream();
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

              // FIXME: Setting this to a low value significantly reduces performance, so leaving this to the servlet container's default
              // FIXME: httpServletResponse.setBufferSize(Streams.DEFAULT_SOCKET_BUFFER_SIZE);
              flushHeaders(httpServletResponse, writerMediaType, messageBodyWriter, exception);
              target = httpServletResponse.getOutputStream();
            }
            else if (chunkedIndex >= 0) {
              httpServletResponse.setBufferSize(chunkSize);
              flushHeaders(httpServletResponse, writerMediaType, messageBodyWriter, exception);
              target = httpServletResponse.getOutputStream();
            }
            else if (bufferSize < chunkSize) { // Let the servlet container try to detect the Content-Length on its own
              httpServletResponse.setBufferSize(chunkSize);
              target = httpServletResponse.getOutputStream();
            }
            else {
              target = new BufferedSocketOutputStream(httpServletResponse, relegateOutputStream, bufferSize, writerMediaType, messageBodyWriter, exception);
            }
          }

          return true;
        }

        @Override
        protected void afterWrite(final int b, final byte[] bs, final int off, final int len) {
          if (!(target instanceof BufferedSocketOutputStream))
            relegateOutputStream.setTarget(target);
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

      flushHeaders(httpServletResponse, writerMediaType, messageBodyWriter, exception);
    }
  }

  @Override
  public void close() throws IOException {
    super.close();

    firstOutputStream = null;
    noopOutputStream = null;
    if (outputStream != null) {
      try {
        outputStream.close();
      }
      catch (final Exception e) {
        if (logger.isDebugEnabled()) { logger.debug(e.getMessage(), e); }
      }
      finally {
        outputStream = null;
      }
    }

    entity = null;
    entityClass = null;
    entityType = null;
    headers = null;
    httpServletRequest = null;
    messageBodyWriter = null;
    requestContext = null;
    status = null;
    writerInterceptorComponents = null;
  }
}