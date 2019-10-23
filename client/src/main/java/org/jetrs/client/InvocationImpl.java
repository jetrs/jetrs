package org.jetrs.client;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

import org.jetrs.common.core.HttpHeadersImpl;
import org.jetrs.common.core.ResponseImpl;
import org.jetrs.common.util.Responses;
import org.libj.util.CollectionUtil;
import org.libj.util.Dates;

public class InvocationImpl implements Invocation {
  private final Providers providers;
  private final URL url;
  private final String method;
  private final Entity<?> entity;
  private final MultivaluedMap<String,Object> headers;
  private final List<Cookie> cookies;
  private final CacheControl cacheControl;

  InvocationImpl(final Providers providers, final URL url, final String method, final Entity<?> entity, final MultivaluedMap<String,Object> headers, final List<Cookie> cookies, final CacheControl cacheControl) {
    this.providers = providers;
    this.url = url;
    this.method = method;
    this.entity = entity;
    this.headers = headers;
    this.cookies = cookies;
    this.cacheControl = cacheControl;
  }

  @Override
  public Invocation property(final String name, final Object value) {
    // TODO
    return this;
  }

  @Override
  public Response invoke() {
    try {
      final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      connection.setRequestMethod(method);
      if (headers != null)
        for (final Map.Entry<String,List<Object>> entry : headers.entrySet())
          connection.setRequestProperty(entry.getKey(), CollectionUtil.toString(entry.getValue(), ','));

      if (cookies != null)
        connection.setRequestProperty(HttpHeaders.COOKIE, CollectionUtil.toString(cookies, ';'));

      // TODO: cacheControl

      if (entity != null) {
        final MessageBodyWriter messageBodyWriter = providers.getMessageBodyWriter(entity.getEntity().getClass(), null, entity.getAnnotations(), entity.getMediaType());
        messageBodyWriter.writeTo(entity.getEntity(), entity.getEntity().getClass(), null, entity.getAnnotations(), entity.getMediaType(), null, connection.getOutputStream());
      }

      final int responseCode = connection.getResponseCode();
      final String reasonPhrase = connection.getResponseMessage();
      final StatusType status = reasonPhrase != null ? Responses.from(responseCode, reasonPhrase) : Responses.from(responseCode);
      final HttpHeadersImpl headers = new HttpHeadersImpl(connection.getHeaderFields());

      final CookieManager cookieManager = new CookieManager();
      CookieHandler.setDefault(cookieManager);

      final List<HttpCookie> httpCookies = cookieManager.getCookieStore().getCookies();
      final List<String> setCookies = headers.get(HttpHeaders.SET_COOKIE);
      final Map<String,NewCookie> cookies = new HashMap<>();
      for (final String setCookie : setCookies) {
        cookieManager.getCookieStore().add(null, HttpCookie.parse(setCookie).get(0));
        for (final HttpCookie httpCookie : httpCookies) {
          final Date expiry = Dates.addTime(headers.getDate(), 0, 0, (int)httpCookie.getMaxAge());
          final NewCookie cookie = new NewCookie(httpCookie.getName(), httpCookie.getValue(), httpCookie.getPath(), httpCookie.getDomain(), httpCookie.getVersion(), httpCookie.getComment(), (int)httpCookie.getMaxAge(), expiry, httpCookie.getSecure(), httpCookie.isHttpOnly());
          cookies.put(cookie.getName(), cookie);
        }
      }

      return new ResponseImpl(null, null, status, headers, cookies, connection.getInputStream(), null);
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

  @Override
  public Future<Response> submit() {
    return new FutureResponse<Response>() {
      @Override
      public Response get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return invoke();
      }
    };
  }

  @Override
  public <T>Future<T> submit(final Class<T> responseType) {
    return new FutureResponse<T>() {
      @Override
      public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return invoke().readEntity(responseType);
      }
    };
  }

  @Override
  public <T>Future<T> submit(final GenericType<T> responseType) {
    return new FutureResponse<T>() {
      @Override
      public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return invoke().readEntity(responseType);
      }
    };
  }

  @Override
  public <T>Future<T> submit(final InvocationCallback<T> callback) {
    return new FutureResponse<T>() {
      @Override
      public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
          final T entity = (T)invoke().getEntity();
          callback.completed(entity);
          return entity;
        }
        catch (final Throwable e) {
          callback.failed(e);
          throw e;
        }
      }
    };
  }

  public static class BuilderImpl implements Invocation.Builder {
    private final URL url;
    private final Providers providers;

    private MultivaluedMap<String,Object> requestHeaders;
    private List<Cookie> cookies;
    private CacheControl cacheControl;

    BuilderImpl(final Providers providers, final URL url) {
      this.providers = providers;
      this.url = url;
    }

    BuilderImpl(final Providers providers, final URL url, final String ... acceptedResponseTypes) {
      this.providers = providers;
      this.url = url;
      accept(acceptedResponseTypes);
    }

    BuilderImpl(final Providers providers, final URL url, final MediaType ... acceptedResponseTypes) {
      this.providers = providers;
      this.url = url;
      accept(acceptedResponseTypes);
    }

    @Override
    public Response get() {
      return method(HttpMethod.GET, (Entity<?>)null);
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
    public Response put(final Entity<?> entity) {
      return method(HttpMethod.PUT, (Entity<?>)null);
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
    public Response post(final Entity<?> entity) {
      return method(HttpMethod.POST, (Entity<?>)null);
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
    public Response delete() {
      return method(HttpMethod.DELETE, (Entity<?>)null);
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
    public Response head() {
      return method(HttpMethod.HEAD, (Entity<?>)null);
    }

    @Override
    public Response options() {
      return method(HttpMethod.OPTIONS, (Entity<?>)null);
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
    public Response trace() {
      return method("TRACE", (Entity<?>)null);
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
      return new InvocationImpl(providers, url, method, entity, requestHeaders, cookies, cacheControl);
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
      // TODO
      return null;
    }

    @Override
    public Invocation.Builder accept(final String ... mediaTypes) {
      requestHeaders.put(HttpHeaders.ACCEPT, Arrays.asList(mediaTypes));
      return this;
    }

    @Override
    public Invocation.Builder accept(final MediaType ... mediaTypes) {
      requestHeaders.put(HttpHeaders.ACCEPT, Arrays.asList(mediaTypes));
      return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(final Locale ... locales) {
      requestHeaders.put(HttpHeaders.ACCEPT_LANGUAGE, Arrays.asList(locales));
      return this;
    }

    @Override
    public Invocation.Builder acceptLanguage(final String ... locales) {
      requestHeaders.put(HttpHeaders.ACCEPT_LANGUAGE, Arrays.asList(locales));
      return this;
    }

    @Override
    public Invocation.Builder acceptEncoding(final String ... encodings) {
      requestHeaders.put(HttpHeaders.ACCEPT_LANGUAGE, Arrays.asList(encodings));
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

    @Override
    public Invocation.Builder header(final String name, final Object value) {
      requestHeaders.put(name, Collections.singletonList(value));
      return this;
    }

    @Override
    public Invocation.Builder headers(final MultivaluedMap<String,Object> headers) {
      requestHeaders = headers;
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
  }
}