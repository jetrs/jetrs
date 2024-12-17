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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

class AsyncInvokerImpl extends Invoker<Future<Response>> implements AsyncInvoker {
  private final HttpHeadersImpl requestHeaders;
  private final ArrayList<Cookie> cookies;
  private final CacheControl cacheControl;
  private final HashMap<String,Object> properties;

  AsyncInvokerImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final HttpHeadersImpl requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final HashMap<String,Object> properties, final long connectTimeoutMs, final long readTimeoutMs) {
    super(client, runtimeContext, uri, executorService, scheduledExecutorService, connectTimeoutMs, readTimeoutMs);
    this.requestHeaders = requestHeaders;
    this.cookies = cookies;
    this.cacheControl = cacheControl;
    this.properties = properties;
  }

  @Override
  HashMap<String,Object> getProperties() {
    return properties;
  }

  @Override
  public <T> Future<T> get(final Class<T> responseType) {
    return method(HttpMethod.GET, null, responseType);
  }

  @Override
  public <T> Future<T> get(final GenericType<T> responseType) {
    return method(HttpMethod.GET, null, responseType);
  }

  @Override
  public <T> Future<T> get(final InvocationCallback<T> callback) {
    return method(HttpMethod.GET, null, callback);
  }

  @Override
  public <T> Future<T> put(final Entity<?> entity, final Class<T> responseType) {
    return method(HttpMethod.PUT, null, responseType);
  }

  @Override
  public <T> Future<T> put(final Entity<?> entity, final GenericType<T> responseType) {
    return method(HttpMethod.PUT, null, responseType);
  }

  @Override
  public <T> Future<T> put(final Entity<?> entity, final InvocationCallback<T> callback) {
    return method(HttpMethod.PUT, null, callback);
  }

  @Override
  public <T> Future<T> post(final Entity<?> entity, final Class<T> responseType) {
    return method(HttpMethod.POST, null, responseType);
  }

  @Override
  public <T> Future<T> post(final Entity<?> entity, final GenericType<T> responseType) {
    return method(HttpMethod.POST, null, responseType);
  }

  @Override
  public <T> Future<T> post(final Entity<?> entity, final InvocationCallback<T> callback) {
    return method(HttpMethod.POST, null, callback);
  }

  @Override
  public <T> Future<T> delete(final Class<T> responseType) {
    return method(HttpMethod.DELETE, null, responseType);
  }

  @Override
  public <T> Future<T> delete(final GenericType<T> responseType) {
    return method(HttpMethod.DELETE, null, responseType);
  }

  @Override
  public <T> Future<T> delete(final InvocationCallback<T> callback) {
    return method(HttpMethod.DELETE, null, callback);
  }

  @Override
  public Future<Response> head(final InvocationCallback<Response> callback) {
    return method(HttpMethod.HEAD, null, callback);
  }

  @Override
  public <T> Future<T> options(final Class<T> responseType) {
    return method(HttpMethod.OPTIONS, null, responseType);
  }

  @Override
  public <T> Future<T> options(final GenericType<T> responseType) {
    return method(HttpMethod.OPTIONS, null, responseType);
  }

  @Override
  public <T> Future<T> options(final InvocationCallback<T> callback) {
    return method(HttpMethod.OPTIONS, null, callback);
  }

  @Override
  public <T> Future<T> trace(final Class<T> responseType) {
    return method("TRACE", null, responseType);
  }

  @Override
  public <T> Future<T> trace(final GenericType<T> responseType) {
    return method("TRACE", null, responseType);
  }

  @Override
  public <T> Future<T> trace(final InvocationCallback<T> callback) {
    return method("TRACE", null, callback);
  }

  @Override
  public Future<Response> method(final String name) {
    return method(name, (Entity<?>)null);
  }

  @Override
  public <T> Future<T> method(final String name, final Class<T> responseType) {
    return method(name, null, responseType);
  }

  @Override
  public <T> Future<T> method(final String name, final GenericType<T> responseType) {
    return method(name, null, responseType);
  }

  @Override
  public <T> Future<T> method(final String name, final InvocationCallback<T> callback) {
    return method(name, null, callback);
  }

  @Override
  public Future<Response> method(final String name, final Entity<?> entity) {
    return method(name, entity, Response.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Future<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
    client.assertNotClosed();
    final Invocation invocation = build(name, requestHeaders, cookies, cacheControl, entity);
    return executorService.submit(() -> {
      final Response response = invocation.invoke();
      if (Response.class.isAssignableFrom(responseType))
        return (T)response;

      try {
        return response.readEntity(responseType);
      }
      finally {
        response.close();
      }
    });
  }

  @Override
  public <T> Future<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
    client.assertNotClosed();
    final Invocation invocation = build(name, requestHeaders, cookies, cacheControl, entity);
    return executorService.submit(() -> {
      try (final Response response = invocation.invoke()) {
        return response.readEntity(responseType);
      }
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Future<T> method(final String name, final Entity<?> entity, final InvocationCallback<T> callback) {
    client.assertNotClosed();
    final Invocation invocation = build(name, requestHeaders, cookies, cacheControl, entity);
    return executorService.submit(() -> {
      try (final Response response = invocation.invoke()) {
        final T message = (T)response.getEntity();
        callback.completed(message);
        return message;
      }
      catch (final Throwable t) {
        callback.failed(t);
        throw t;
      }
    });
  }
}