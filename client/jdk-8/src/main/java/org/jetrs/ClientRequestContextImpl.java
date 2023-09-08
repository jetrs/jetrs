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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.libj.util.ObservableOutputStream;
import org.libj.util.function.ThrowingSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ClientRequestContextImpl extends RequestContext<ClientRuntimeContext> implements ClientRequestContext, Invocation, WriterInterceptorContext {
  private static final Logger logger = LoggerFactory.getLogger(ClientRequestContextImpl.class);

  final ClientImpl client;
  private final ArrayList<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories;
  final URI uri;
  final String method;
  final HttpHeadersImpl requestHeaders;
  final ArrayList<Cookie> cookies;
  final CacheControl cacheControl;
  final ExecutorService executorService;
  final ScheduledExecutorService scheduledExecutorService;
  private final HashMap<String,Object> properties;
  final long connectTimeout;
  final long readTimeout;

  ClientRequestContextImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final String method, final HttpHeadersImpl requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final Entity<?> entity, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final HashMap<String,Object> properties, final long connectTimeout, final long readTimeout) {
    super(runtimeContext, new RequestImpl(method));
    this.client = client;
    this.writerInterceptorProviderFactories = getWriterInterceptorFactoryList();
    this.uri = uri;
    this.method = method;
    this.requestHeaders = requestHeaders;
    this.cookies = cookies;
    this.cacheControl = cacheControl;

    if (entity != null) {
      this.entity = entity.getEntity();
      this.entityClass = this.entity == null ? null : this.entity.getClass();
      setAnnotations(entity.getAnnotations());
      final MediaType mediaType = entity.getMediaType();
      if (mediaType != null)
        requestHeaders.setMediaType(mediaType);
      else if (requestHeaders.getMediaType() == null)
        if (logger.isWarnEnabled()) logger.warn("Empty Content-Type for request with entity may be set to default value by HTTP client");
    }

    this.executorService = executorService;
    this.scheduledExecutorService = scheduledExecutorService;
    this.properties = properties;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  protected final MessageBodyWriter<?> getMessageBodyWriter() throws ProcessingException {
    MediaType mediaType = getMediaType();
    if (mediaType == null)
      mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;

    final MessageBodyWriter<?> messageBodyWriter = getProviders().getMessageBodyWriter(getEntityClass(), getGenericType(), getAnnotations(), mediaType);
    if (messageBodyWriter == null)
      throw new ProcessingException("Could not find MessageBodyWriter for {type=" + getEntityClass().getName() + ", genericType=" + (getGenericType() == null ? "null" : getGenericType().getTypeName()) + ", annotations=" + Arrays.toString(getAnnotations()) + ", mediaType=" + mediaType + "}");

    return messageBodyWriter;
  }

  @Override
  public Invocation property(final String name, final Object value) {
    properties.put(name, value);
    return this;
  }

  @Override
  HttpHeadersImpl getHttpHeaders() {
    return requestHeaders;
  }

  @Override
  @SuppressWarnings("unchecked")
  <T>T findInjectableContextValue(final Class<T> clazz) {
    if (HttpHeaders.class.isAssignableFrom(clazz))
      return (T)getHttpHeaders();

    return super.findInjectableContextValue(clazz);
  }

  @Override
  public Client getClient() {
    return client;
  }

  @Override
  public URI getUri() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setUri(final URI uri) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedArrayMap<String,Object> getHeaders() {
    return getHttpHeaders().getMirrorMap();
  }

  @Override
  public HttpHeadersImpl getStringHeaders() {
    return getHttpHeaders();
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
      this.entityClass = genericEntity.getRawType();
      this.entityType = genericEntity.getType();
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
  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public void setOutputStream(final OutputStream os) {
    this.outputStream = os;
  }

  private int interceptorIndex = -1;
  @SuppressWarnings("rawtypes")
  private MessageBodyWriter messageBodyWriter;

  @Override
  @SuppressWarnings("unchecked")
  public void proceed() throws IOException, WebApplicationException {
    final int size = writerInterceptorProviderFactories.size();
    if (++interceptorIndex < size) {
      writerInterceptorProviderFactories.get(interceptorIndex).getSingletonOrFromRequestContext(this).aroundWriteTo(this);
    }
    else if (interceptorIndex == size) {
      try (final OutputStream entityStream = getOutputStream()) {
        messageBodyWriter.writeTo(getEntity(), getEntityClass(), getEntityType(), getEntityAnnotations(), getMediaType(), getHeaders(), entityStream);
      }
    }
    else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Configuration getConfiguration() {
    return runtimeContext.getConfiguration();
  }

  @SuppressWarnings("rawtypes")
  void writeContentSync(final MessageBodyWriter messageBodyWriter, final ThrowingSupplier<OutputStream,IOException> onFirstWrite, final Runnable onClose) throws IOException {
    try (final RelegateOutputStream relegateEntityStream = new RelegateOutputStream() {
      @Override
      public void close() throws IOException {
        if (onClose != null)
          onClose.run();

        super.close();
      }
    }) {
      relegateEntityStream.setTarget(new ObservableOutputStream() {
        @Override
        protected boolean beforeWrite(final int b, final byte[] bs, final int off, final int len) {
          target = onFirstWrite.get();
          return true;
        }

        @Override
        protected void afterWrite(final int b, final byte[] bs, final int off, final int len) {
          relegateEntityStream.setTarget(target);
          target = null;
        }

        @Override
        public void close() throws IOException {
          if (target != null)
            target.close();
        }
      });

      this.messageBodyWriter = messageBodyWriter;
      setOutputStream(relegateEntityStream);
      this.interceptorIndex = -1;
      proceed();
    }
  }

  private static void unlock(final ReentrantLock lock, final Condition condition) {
    lock.lock();
    condition.signal();
    lock.unlock();
  }

  @SuppressWarnings("rawtypes")
  void writeContentAsync(final MessageBodyWriter messageBodyWriter, final ThrowingSupplier<OutputStream,IOException> onFirstWrite, final Runnable onClose) throws ExecutionException, InterruptedException, TimeoutException {
    final AtomicReference<Object> resultRef = new AtomicReference<>();
    final ReentrantLock lock = new ReentrantLock();
    final Condition condition = lock.newCondition();

    executorService.execute(() -> {
      try (final RelegateOutputStream relegateEntityStream = new RelegateOutputStream() {
        @Override
        public void close() throws IOException {
          try {
            super.close();
            if (onClose != null)
              onClose.run();
          }
          finally {
            if (resultRef.get() == null)
              unlock(lock, condition);
          }
        }
      }) {
        relegateEntityStream.setTarget(new ObservableOutputStream() {
          @Override
          protected boolean beforeWrite(final int b, final byte[] bs, final int off, final int len) {
            try {
              target = onFirstWrite.get();
              resultRef.set(Boolean.TRUE);
            }
            finally {
              unlock(lock, condition);
            }

            return true;
          }

          @Override
          protected void afterWrite(final int b, final byte[] bs, final int off, final int len) {
            relegateEntityStream.setTarget(target);
            target = null;
          }

          @Override
          public void close() throws IOException {
            if (target != null)
              target.close();
          }
        });

        this.messageBodyWriter = messageBodyWriter;
        setOutputStream(relegateEntityStream);
        this.interceptorIndex = -1;
        proceed();
      }
      catch (final Throwable t) {
        resultRef.set(t);
      }
    });

    Object result = resultRef.get();
    if (result == null) {
      lock.lock();
      result = resultRef.get();

      long timeout = connectTimeout + readTimeout;
      if (timeout == 0)
        timeout = Long.MAX_VALUE;

      if (result == null && !condition.await(timeout, TimeUnit.MILLISECONDS))
        throw new TimeoutException(timeout + "ms");

      result = resultRef.get();
      if (result == null)
        throw new IllegalStateException();

      lock.unlock();
    }

    if (result instanceof Throwable)
      throw new ExecutionException((Throwable)result);
  }

  @Override
  public <T>T invoke(final Class<T> responseType) {
    return invoke().readEntity(responseType);
  }

  @Override
  public <T>T invoke(final GenericType<T> responseType) {
    return invoke().readEntity(responseType);
  }

  private ExecutorService getExecutorService() {
    if (executorService == null)
      throw new ProcessingException("ExecutorService was not provided");

    return executorService;
  }

  @Override
  public Future<Response> submit() {
    return getExecutorService().submit(() -> invoke());
  }

  @Override
  public <T>Future<T> submit(final Class<T> responseType) {
    return getExecutorService().submit(() -> invoke().readEntity(responseType));
  }

  @Override
  public <T>Future<T> submit(final GenericType<T> responseType) {
    return getExecutorService().submit(() -> invoke().readEntity(responseType));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T>Future<T> submit(final InvocationCallback<T> callback) {
    client.assertNotClosed();
    return getExecutorService().submit(() -> {
      try {
        final T entity = (T)invoke().getEntity();
        callback.completed(entity);
        return entity;
      }
      catch (final Throwable t) {
        callback.failed(t);
        throw t;
      }
    });
  }

  static class BuilderImpl extends Invoker<Response> implements Invocation.Builder {
    private HttpHeadersImpl requestHeaders;
    private ArrayList<Cookie> cookies;
    private CacheControl cacheControl;
    private HashMap<String,Object> properties;

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) {
      super(client, runtimeContext, uri, executorService, scheduledExecutorService, connectTimeout, readTimeout);
    }

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout, final String[] acceptedResponseTypes) {
      this(client, runtimeContext, uri, executorService, scheduledExecutorService, connectTimeout, readTimeout);
      accept(acceptedResponseTypes);
    }

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout, final MediaType[] acceptedResponseTypes) {
      this(client, runtimeContext, uri, executorService, scheduledExecutorService, connectTimeout, readTimeout);
      accept(acceptedResponseTypes);
    }

    @Override
    public Invocation.Builder accept(final String ... mediaTypes) {
      getHeaders().put(HttpHeaders.ACCEPT, Arrays.asList(mediaTypes));
      return this;
    }

    @Override
    public Invocation.Builder accept(final MediaType ... mediaTypes) {
      getHeaders().getMirrorMap().put(HttpHeaders.ACCEPT, Arrays.asList((Object[])mediaTypes));
      return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(final Locale ... locales) {
      getHeaders().getMirrorMap().put(HttpHeaders.ACCEPT_LANGUAGE, Arrays.asList((Object[])locales));
      return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(final String ... locales) {
      getHeaders().put(HttpHeaders.ACCEPT_LANGUAGE, Arrays.asList(locales));
      return this;
    }

    @Override
    public Invocation.Builder acceptEncoding(final String ... encodings) {
      getHeaders().put(HttpHeaders.ACCEPT_LANGUAGE, Arrays.asList(encodings));
      return this;
    }

    @Override
    public Invocation.Builder cookie(final Cookie cookie) {
      cookies.add(cookie);
      return this;
    }

    @Override
    public Invocation.Builder cookie(final String name, final String value) {
      cookies.add(new Cookie(name, value));
      return this;
    }

    @Override
    public Invocation.Builder cacheControl(final CacheControl cacheControl) {
      this.cacheControl = cacheControl;
      return this;
    }

    private HttpHeadersImpl getHeaders() {
      return requestHeaders == null ? requestHeaders = new HttpHeadersImpl() : requestHeaders;
    }

    @Override
    public Invocation.Builder header(final String name, final Object value) {
      if (value == null)
        getHeaders().remove(name);
      else if (value instanceof String)
        getHeaders().add(name, (String)value);
      else
        getHeaders().getMirrorMap().add(name, value);

      return this;
    }

    @Override
    public Invocation.Builder headers(final MultivaluedMap<String,Object> headers) {
      getHeaders().clear();
      if (headers.size() > 0) {
        int size;
        String name;
        List<Object> values;
        for (final Map.Entry<String,List<Object>> entry : headers.entrySet()) { // [S]
          values = entry.getValue();
          if (values != null && (size = values.size()) > 0) {
            name = entry.getKey();
            if (values instanceof RandomAccess) {
              int i = 0; do // [RA]
                header(name, values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<Object> i = values.iterator(); do // [I]
                header(name, i.next());
              while (i.hasNext());
            }
          }
        }
      }

      return this;
    }

    @Override
    HashMap<String,Object> getProperties() {
      return properties;
    }

    @Override
    public Invocation.Builder property(final String name, final Object value) {
      if (properties == null)
        properties = new HashMap<>();

      properties.put(name, value);
      return this;
    }

    @Override
    public CompletionStageRxInvoker rx() {
      // TODO: Implement this.
      return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T extends RxInvoker>T rx(final Class<T> clazz) {
      // TODO: Implement this.
      return null;
    }

    @Override
    public <T>T get(final Class<T> responseType) {
      return get().readEntity(responseType);
    }

    @Override
    public <T>T get(final GenericType<T> responseType) {
      return get().readEntity(responseType);
    }

    @Override
    public <T>T put(final Entity<?> entity, final Class<T> responseType) {
      return put(entity).readEntity(responseType);
    }

    @Override
    public <T>T put(final Entity<?> entity, final GenericType<T> responseType) {
      return put(entity).readEntity(responseType);
    }

    @Override
    public <T>T post(final Entity<?> entity, final Class<T> responseType) {
      return post(entity).readEntity(responseType);
    }

    @Override
    public <T>T post(final Entity<?> entity, final GenericType<T> responseType) {
      return post(entity).readEntity(responseType);
    }

    @Override
    public <T>T delete(final Class<T> responseType) {
      return delete().readEntity(responseType);
    }

    @Override
    public <T>T delete(final GenericType<T> responseType) {
      return delete().readEntity(responseType);
    }

    @Override
    public <T>T options(final Class<T> responseType) {
      return options().readEntity(responseType);
    }

    @Override
    public <T>T options(final GenericType<T> responseType) {
      return options().readEntity(responseType);
    }

    @Override
    public <T>T trace(final Class<T> responseType) {
      return trace().readEntity(responseType);
    }

    @Override
    public <T>T trace(final GenericType<T> responseType) {
      return trace().readEntity(responseType);
    }

    @Override
    public Response method(final String name) {
      return method(name, (Entity<?>)null);
    }

    @Override
    public <T>T method(final String name, final Class<T> responseType) {
      return method(name, (Entity<?>)null).readEntity(responseType);
    }

    @Override
    public <T>T method(final String name, final GenericType<T> responseType) {
      return method(name, (Entity<?>)null).readEntity(responseType);
    }

    @Override
    public Response method(final String name, final Entity<?> entity) {
      return build(name, entity).invoke();
    }

    @Override
    public <T>T method(final String name, final Entity<?> entity, final Class<T> responseType) {
      return method(name, entity).readEntity(responseType);
    }

    @Override
    public <T>T method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
      return method(name, entity).readEntity(responseType);
    }

    @Override
    public Invocation build(final String method) {
      return build(method, null);
    }

    @Override
    public Invocation build(final String method, final Entity<?> entity) {
      return build(method, requestHeaders, cookies, cacheControl, entity);
    }

    @Override
    public Invocation buildGet() {
      return build(HttpMethod.GET, null);
    }

    @Override
    public Invocation buildDelete() {
      return build(HttpMethod.DELETE, null);
    }

    @Override
    public Invocation buildPost(final Entity<?> entity) {
      return build(HttpMethod.POST, entity);
    }

    @Override
    public Invocation buildPut(final Entity<?> entity) {
      return build(HttpMethod.PUT, entity);
    }

    @Override
    public AsyncInvoker async() {
      client.assertNotClosed();
      return new AsyncInvokerImpl(client, runtimeContext, uri, requestHeaders, cookies, cacheControl, executorService, scheduledExecutorService, properties, connectTimeout, readTimeout);
    }

    @Override
    public String toString() {
      final StringBuilder str = new StringBuilder();
      appendHeaders(str, getHeaders());
      str.append(uri);
      return str.toString();
    }
  }

  private static void appendHeader(final StringBuilder str, final String key, final String value) {
    str.append("-H '").append(key).append(": ").append(value.replace("'", "\\'")).append("' ");
  }

  private static void appendHeaders(final StringBuilder str, final HttpHeadersMap<String,Object> headers) {
    if (headers.size() > 0) {
      int size;
      String name;
      List<String> values;
      for (final Map.Entry<String,List<String>> entry : headers.entrySet()) { // [S]
        values = entry.getValue();
        if (values != null && (size = values.size()) > 0) {
          name = entry.getKey();
          for (int i = 0; i < size; ++i) // [RA]
            appendHeader(str, name, values.get(i));
        }
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder str = new StringBuilder();
    str.append("-X").append(method).append(' ');
    appendHeaders(str, requestHeaders);
    if (entity != null)
      str.append(" -d '").append(entity.toString().replace("'", "\\'")).append("' ");

    str.append(uri);
    return str.toString();
  }
}