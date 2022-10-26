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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import org.libj.util.function.ThrowingRunnable;
import org.libj.util.function.ThrowingSupplier;

abstract class InvocationImpl implements Invocation {
  final ClientImpl client;
  final ClientRequestContextImpl requestContext;
  final URL url;
  final String method;
  final Entity<?> entity;
  final HttpHeadersMap<String,Object> requestHeaders;
  final ArrayList<Cookie> cookies;
  final CacheControl cacheControl;
  final ExecutorService executorService;
  final ScheduledExecutorService scheduledExecutorService;
  final long connectTimeout;
  final long readTimeout;

  InvocationImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final String method, final Entity<?> entity, final HttpHeadersMap<String,Object> requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) {
    this.client = client;
    this.requestContext = runtimeContext.newRequestContext(new RequestImpl(method));
    this.url = url;
    this.method = method;
    this.entity = entity;
    this.requestHeaders = requestHeaders;
    this.cookies = cookies;
    this.cacheControl = cacheControl;
    this.executorService = executorService != null ? executorService : getDefaultExecutorService();
    this.scheduledExecutorService = scheduledExecutorService != null ? scheduledExecutorService : getDefaultScheduledExecutorService();
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  abstract ExecutorService getDefaultExecutorService();
  abstract ScheduledExecutorService getDefaultScheduledExecutorService();

  @SuppressWarnings({"rawtypes", "unchecked"})
  void writeContentSync(final MessageBodyWriter messageBodyWriter, final Class<?> entityClass, final ThrowingSupplier<OutputStream,IOException> onFirstWrite, final Runnable onClose) throws IOException {
    try (final EntityOutputStream entityStream = new EntityOutputStream() {
      @Override
      void onWrite() throws IOException {
        if (out == null)
          out = onFirstWrite.get();
      }

      @Override
      public void close() throws IOException {
        onClose.run();
        super.close();
      }
    }) {
      messageBodyWriter.writeTo(entity.getEntity(), entityClass, null, entity.getAnnotations(), entity.getMediaType(), requestHeaders.getMirrorMap(), entityStream);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  void writeContentAsync(final MessageBodyWriter messageBodyWriter, final Class<?> entityClass, final ThrowingSupplier<OutputStream,IOException> onFirstWrite, final Runnable onClose) throws InterruptedException, TimeoutException {
    final AtomicBoolean ready = new AtomicBoolean();
    final ReentrantLock lock = new ReentrantLock();
    final Condition condition = lock.newCondition();

    executorService.execute(new ThrowingRunnable() { // FIXME: Need to configure the executor!
      @Override
      public void runThrows() throws Exception {
        try (final EntityOutputStream entityStream = new EntityOutputStream() {
          @Override
          void onWrite() throws IOException {
            if (out == null) {
              out = onFirstWrite.get();
              ready.set(true);
              lock.lock();
              condition.signal();
              lock.unlock();
            }
          }

          @Override
          public void close() throws IOException {
            super.close();
            onClose.run();
          }
        }) {
          messageBodyWriter.writeTo(entity.getEntity(), entityClass, null, entity.getAnnotations(), entity.getMediaType(), requestHeaders.getMirrorMap(), entityStream);
        }
      }
    });

    if (!ready.get()) {
      lock.lock();
      if (!ready.get() && !condition.await(connectTimeout + readTimeout, TimeUnit.MILLISECONDS))
        throw new TimeoutException();

      lock.unlock();
    }
  }

  @Override
  public Invocation property(final String name, final Object value) {
    // TODO
    return this;
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

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) {
      super(client, runtimeContext, url, executorService, scheduledExecutorService, connectTimeout, readTimeout);
    }

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout, final String[] acceptedResponseTypes) {
      this(client, runtimeContext, url, executorService, scheduledExecutorService, connectTimeout, readTimeout);
      accept(acceptedResponseTypes);
    }

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout, final MediaType[] acceptedResponseTypes) {
      this(client, runtimeContext, url, executorService, scheduledExecutorService, connectTimeout, readTimeout);
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
              for (int i = 0; i < size; ++i) // [RA]
                header(name, values.get(i));
            }
            else {
              for (final Object value : values) // [L]
                header(name, value);
            }
          }
        }
      }

      return this;
    }

    @Override
    public Invocation.Builder property(final String name, final Object value) {
      // TODO
      return this;
    }

    @Override
    public CompletionStageRxInvoker rx() {
      // TODO
      return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T extends RxInvoker>T rx(final Class<T> clazz) {
      // TODO
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
      return build(method, entity, requestHeaders, cookies, cacheControl);
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
      return new AsyncInvokerImpl(client, runtimeContext, url, requestHeaders, cookies, cacheControl, executorService, scheduledExecutorService, connectTimeout, readTimeout);
    }

    @Override
    public String toString() {
      final StringBuilder str = new StringBuilder();
      appendHeaders(str, getHeaders());
      str.append(url);
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

    str.append(url);
    return str.toString();
  }
}