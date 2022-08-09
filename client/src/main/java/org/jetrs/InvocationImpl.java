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
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.libj.util.CollectionUtil;
import org.libj.util.Dates;

class InvocationImpl implements Invocation {
  private static final CookieStore cookieStore;

  static {
    final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    CookieHandler.setDefault(cookieManager);
    cookieStore = cookieManager.getCookieStore();
  }

  private final ClientImpl client;
  private final ClientRequestContextImpl requestContext;
  private final URL url;
  private final String method;
  private final Entity<?> entity;
  private final HttpHeadersMap<String,Object> headers;
  private final List<Cookie> cookies;
  private final CacheControl cacheControl;
  private final ExecutorService executorService;
  private final long connectTimeout;
  private final long readTimeout;

  InvocationImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final String method, final Entity<?> entity, final HttpHeadersMap<String,Object> headers, final List<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final long connectTimeout, final long readTimeout) {
    this.client = client;
    this.requestContext = runtimeContext.newRequestContext(new RequestImpl(method));
    this.url = url;
    this.method = method;
    this.entity = entity;
    this.headers = headers;
    this.cookies = cookies;
    this.cacheControl = cacheControl;
    this.executorService = executorService;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  @Override
  public Invocation property(final String name, final Object value) {
    // TODO
    return this;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Response invoke() {
    try {
      final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod(method);
      connection.setConnectTimeout((int)connectTimeout);
      connection.setReadTimeout((int)readTimeout);
      if (headers != null) {
        for (final Map.Entry<String,List<String>> entry : headers.entrySet()) { // [S]
          final String headerName = entry.getKey();
          connection.setRequestProperty(headerName, CollectionUtil.toString(entry.getValue(), HttpHeadersImpl.getHeaderValueDelimiters(headerName)[0]));
        }
      }

      if (cookies != null)
        connection.setRequestProperty(HttpHeaders.COOKIE, CollectionUtil.toString(cookies, ';'));

      if (cacheControl != null)
        connection.setRequestProperty(HttpHeaders.CACHE_CONTROL, cacheControl.toString());
      else
        connection.setUseCaches(false);

      if (entity != null) {
        connection.setDoOutput(true);
        final Providers providers = requestContext.getProviders();
        final Class<?> entityClass = entity.getEntity().getClass();
        final MessageBodyWriter messageBodyWriter = providers.getMessageBodyWriter(entityClass, null, entity.getAnnotations(), entity.getMediaType());
        if (messageBodyWriter == null) {
          providers.getMessageBodyWriter(entityClass, null, entity.getAnnotations(), entity.getMediaType());
          throw new ProcessingException("Provider not found for " + entityClass.getName());
        }

        final OutputStream entityStream = connection.getOutputStream();
        MessageBodyProvider.writeTo(messageBodyWriter, entity.getEntity(), entityClass, null, entity.getAnnotations(), entity.getMediaType(), headers == null ? null : headers.getMirrorMap(), entityStream);
        entityStream.flush();
      }

      final int statusCode = connection.getResponseCode();
      final String reasonPhrase = connection.getResponseMessage();
      final StatusType statusInfo = reasonPhrase != null ? Responses.from(statusCode, reasonPhrase) : Responses.from(statusCode);
      final HttpHeadersImpl headers = new HttpHeadersImpl(connection.getHeaderFields());

      final List<HttpCookie> httpCookies = cookieStore.getCookies();
      final Map<String,NewCookie> cookies;
      if (httpCookies.size() == 0) {
        cookies = null;
      }
      else {
        cookies = new HashMap<>(httpCookies.size());
        for (int i = 0, i$ = cookies.size(); i < i$; ++i) { // [L]
          final HttpCookie httpCookie = httpCookies.get(i);
          final Date expiry = Dates.addTime(headers.getDate(), 0, 0, (int)httpCookie.getMaxAge());
          final NewCookie cookie = new NewCookie(httpCookie.getName(), httpCookie.getValue(), httpCookie.getPath(), httpCookie.getDomain(), httpCookie.getVersion(), httpCookie.getComment(), (int)httpCookie.getMaxAge(), expiry, httpCookie.getSecure(), httpCookie.isHttpOnly());
          cookies.put(cookie.getName(), cookie);
        }
      }

      return new ResponseImpl(requestContext, statusCode, statusInfo, headers, cookies, 200 <= statusCode && statusCode < 400 ? connection.getInputStream() : connection.getErrorStream(), null) {
        @Override
        public void close() {
          try {
            super.close();
          }
          finally {
            connection.disconnect();
          }
        }
      };
    }
    catch (final IOException e) {
      throw new ProcessingException(e);
    }
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
    private List<Cookie> cookies;
    private CacheControl cacheControl;

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final ExecutorService executorService, final long connectTimeout, final long readTimeout) {
      super(client, runtimeContext, url, executorService, connectTimeout, readTimeout);
    }

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final ExecutorService executorService, final long connectTimeout, final long readTimeout, final String ... acceptedResponseTypes) {
      this(client, runtimeContext, url, executorService, connectTimeout, readTimeout);
      accept(acceptedResponseTypes);
    }

    BuilderImpl(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final ExecutorService executorService, final long connectTimeout, final long readTimeout, final MediaType ... acceptedResponseTypes) {
      this(client, runtimeContext, url, executorService, connectTimeout, readTimeout);
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
      for (final Map.Entry<String,List<Object>> entry : headers.entrySet()) { // [S]
        final List<Object> values = entry.getValue();
        for (int i = 0, i$ = values.size(); i < i$; ++i) // [L]
          header(entry.getKey(), values.get(i));
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
      return new AsyncInvokerImpl(client, runtimeContext, url, requestHeaders, cookies, cacheControl, executorService, connectTimeout, readTimeout);
    }

    @Override
    public String toString() {
      final StringBuilder str = new StringBuilder();
      appendHeaders(str, getHeaders());
      str.append(url);
      return str.toString();
    }
  }

  private static void appendHeaders(final StringBuilder str, final HttpHeadersMap<String,Object> headers) {
    for (final Map.Entry<String,List<String>> entry : headers.entrySet()) { // [S]
      final List<String> values = entry.getValue();
      for (int i = 0, i$ = values.size(); i < i$; ++i) // [L]
        str.append("-H '").append(entry.getKey()).append(": ").append(values.get(i).replace("'", "\\'")).append("' ");
    }
  }

  @Override
  public String toString() {
    final StringBuilder str = new StringBuilder();
    str.append("-X").append(method).append(' ');
    appendHeaders(str, headers);
    if (entity != null)
      str.append(" -d '").append(entity.toString().replace("'", "\\'")).append("' ");

    str.append(url);
    return str.toString();
  }
}