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

import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;

public class AsyncInvokerImpl extends Invoker<Future<Response>> implements AsyncInvoker {
  private final MultivaluedMap<String,Object> requestHeaders;
  private final List<Cookie> cookies;
  private final CacheControl cacheControl;

  AsyncInvokerImpl(final Providers providers, final URL url, final MultivaluedMap<String,Object> requestHeaders, final List<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final long connectTimeout, final long readTimeout) {
    super(providers, url, executorService, connectTimeout, readTimeout);
    this.requestHeaders = requestHeaders;
    this.cookies = cookies;
    this.cacheControl = cacheControl;
  }

  @Override
  public <T>Future<T> get(final Class<T> responseType) {
    return method(HttpMethod.GET, null, responseType);
  }

  @Override
  public <T>Future<T> get(final GenericType<T> responseType) {
    return method(HttpMethod.GET, null, responseType);
  }

  @Override
  public <T>Future<T> get(final InvocationCallback<T> callback) {
    return method(HttpMethod.GET, null, callback);
  }

  @Override
  public <T>Future<T> put(final Entity<?> entity, final Class<T> responseType) {
    return method(HttpMethod.PUT, null, responseType);
  }

  @Override
  public <T>Future<T> put(final Entity<?> entity, final GenericType<T> responseType) {
    return method(HttpMethod.PUT, null, responseType);
  }

  @Override
  public <T>Future<T> put(final Entity<?> entity, final InvocationCallback<T> callback) {
    return method(HttpMethod.PUT, null, callback);
  }

  @Override
  public <T>Future<T> post(final Entity<?> entity, final Class<T> responseType) {
    return method(HttpMethod.POST, null, responseType);
  }

  @Override
  public <T>Future<T> post(final Entity<?> entity, final GenericType<T> responseType) {
    return method(HttpMethod.POST, null, responseType);
  }

  @Override
  public <T>Future<T> post(final Entity<?> entity, final InvocationCallback<T> callback) {
    return method(HttpMethod.POST, null, callback);
  }

  @Override
  public <T>Future<T> delete(final Class<T> responseType) {
    return method(HttpMethod.DELETE, null, responseType);
  }

  @Override
  public <T>Future<T> delete(final GenericType<T> responseType) {
    return method(HttpMethod.DELETE, null, responseType);
  }

  @Override
  public <T>Future<T> delete(final InvocationCallback<T> callback) {
    return method(HttpMethod.DELETE, null, callback);
  }

  @Override
  public Future<Response> head(final InvocationCallback<Response> callback) {
    return method(HttpMethod.HEAD, null, callback);
  }

  @Override
  public <T>Future<T> options(final Class<T> responseType) {
    return method(HttpMethod.OPTIONS, null, responseType);
  }

  @Override
  public <T>Future<T> options(final GenericType<T> responseType) {
    return method(HttpMethod.OPTIONS, null, responseType);
  }

  @Override
  public <T>Future<T> options(final InvocationCallback<T> callback) {
    return method(HttpMethod.OPTIONS, null, callback);
  }

  @Override
  public <T>Future<T> trace(final Class<T> responseType) {
    return method("TRACE", null, responseType);
  }

  @Override
  public <T>Future<T> trace(final GenericType<T> responseType) {
    return method("TRACE", null, responseType);
  }

  @Override
  public <T>Future<T> trace(final InvocationCallback<T> callback) {
    return method("TRACE", null, callback);
  }

  @Override
  public Future<Response> method(final String name) {
    return method(name, (Entity<?>)null);
  }

  @Override
  public <T>Future<T> method(final String name, final Class<T> responseType) {
    return method(name, null, responseType);
  }

  @Override
  public <T>Future<T> method(final String name, final GenericType<T> responseType) {
    return method(name, null, responseType);
  }

  @Override
  public <T>Future<T> method(final String name, final InvocationCallback<T> callback) {
    return method(name, null, callback);
  }

  @Override
  public Future<Response> method(final String name, final Entity<?> entity) {
    return method(name, null, Response.class);
  }

  private ExecutorService getExecutorService() {
    if (executorService == null)
      throw new ProcessingException("ExecutorService was not provided");

    return executorService;
  }

  @Override
  public <T>Future<T> method(final String name, final Entity<?> entity, final Class<T> responseType) {
    return getExecutorService().submit(new Callable<T>() {
      @Override
      public T call() throws Exception {
        return build(name, entity, requestHeaders, cookies, cacheControl).invoke().readEntity(responseType);
      }
    });
  }

  @Override
  public <T>Future<T> method(final String name, final Entity<?> entity, final GenericType<T> responseType) {
    return getExecutorService().submit(new Callable<T>() {
      @Override
      public T call() throws Exception {
        return build(name, entity, requestHeaders, cookies, cacheControl).invoke().readEntity(responseType);
      }
    });
  }

  @Override
  public <T>Future<T> method(final String name, final Entity<?> entity, final InvocationCallback<T> callback) {
    return getExecutorService().submit(new Callable<T>() {
      @Override
      public T call() throws Exception {
        try {
          // FIXME: How do I know the type of entity from the InvocationCallback<T>?
          final T response = (T)build(name, entity, requestHeaders, cookies, cacheControl).invoke().getEntity();
          callback.completed(response);
          return response;
        }
        catch (final Throwable t) {
          callback.failed(t);
          throw t;
        }
      }
    });
  }
}