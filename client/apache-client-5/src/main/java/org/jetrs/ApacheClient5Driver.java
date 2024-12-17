/* Copyright (c) 2022 JetRS
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
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.ManagedHttpClientConnection;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.HttpConnectionFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.libj.lang.Booleans;
import org.libj.lang.Numbers;
import org.libj.util.CollectionUtil;
import org.libj.util.ObservableOutputStream;
import org.libj.util.UnsynchronizedByteArrayOutputStream;
import org.libj.util.function.Throwing;

public class ApacheClient5Driver extends CachedClientDriver<CloseableHttpClient> {
  private static final BasicCookieStore cookieStore = new BasicCookieStore();

  static void addCookie(final Map<String,NewCookie> cookies, final Cookie cookie) {
    final int version = 1; // FIXME: How to get the version?
    final int maxAge = 0; // FIXME: How to get the maxAge?
    final boolean secure = false; // FIXME: How to get the secure?
    final boolean httpOnly = false; // FIXME: How to get the httpOnly?
    final NewCookie newCookie = new NewCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), version, null, maxAge, cookie.getExpiryDate(), secure, httpOnly);
    cookies.put(newCookie.getName(), newCookie);
  }

  private static PoolingHttpClientConnectionManager buildConnectionManager(
    final PlainConnectionSocketFactory socketFactory,
    final LayeredConnectionSocketFactory sslSocketFactory,
    final PoolConcurrencyPolicy poolConcurrencyPolicy,
    final PoolReusePolicy poolReusePolicy,
    final SchemePortResolver schemePortResolver,
    final DnsResolver dnsResolver,
    final HttpConnectionFactory<ManagedHttpClientConnection> connectionFactory,
    final Resolver<HttpRoute,SocketConfig> socketConfigResolver,
    final Resolver<HttpRoute,ConnectionConfig> connectionConfigResolver,
    final Resolver<HttpHost,TlsConfig> tlsConfigResolver,
    final int maxConnTotal,
    final int maxConnPerRoute
  ) {
    final boolean systemProperties = false;
    final PoolingHttpClientConnectionManager poolingmgr = new PoolingHttpClientConnectionManager(
      RegistryBuilder.<ConnectionSocketFactory>create()
        .register(URIScheme.HTTP.id, socketFactory)
        .register(URIScheme.HTTPS.id, sslSocketFactory != null ? sslSocketFactory : (systemProperties ? SSLConnectionSocketFactory.getSystemSocketFactory() : SSLConnectionSocketFactory.getSocketFactory()))
        .build(),
      poolConcurrencyPolicy,
      poolReusePolicy,
      null,
      schemePortResolver,
      dnsResolver,
      connectionFactory);
    poolingmgr.setSocketConfigResolver(socketConfigResolver);
    poolingmgr.setConnectionConfigResolver(connectionConfigResolver);
    poolingmgr.setTlsConfigResolver(tlsConfigResolver);
    if (maxConnTotal > 0)
      poolingmgr.setMaxTotal(maxConnTotal);

    if (maxConnPerRoute > 0)
      poolingmgr.setDefaultMaxPerRoute(maxConnPerRoute);

    return poolingmgr;
  }

  @Override
  CloseableHttpClient newClient(final ClientConfig clientConfig) {
    final SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder = SSLConnectionSocketFactoryBuilder.create();

    final ProxyConfig proxyConfig = clientConfig.proxyConfig;

    final PlainConnectionSocketFactory socketFactory;
    final SSLConnectionSocketFactory sslSocketFactory;

    if (proxyConfig == null) {
      socketFactory = PlainConnectionSocketFactory.getSocketFactory();
      sslSocketFactory = sslConnectionSocketFactoryBuilder.setSslContext(clientConfig.sslContext).build();
    }
    else {
      socketFactory = new PlainConnectionSocketFactory() {
        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
          return new Socket(proxyConfig.getProxy());
        }
      };

      sslSocketFactory = new SSLConnectionSocketFactory(clientConfig.sslContext, HttpsSupport.getDefaultHostnameVerifier()) {
        @Override
        public Socket createSocket(final HttpContext context) {
          return new Socket(proxyConfig.getProxy());
        }
      };
    }

    final PoolingHttpClientConnectionManager connectionManager = buildConnectionManager(socketFactory, sslSocketFactory, null, null, null, null, null, null, null, null, 0, 0);
    // final PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslSocketFactory).build();
    connectionManager.setDefaultMaxPerRoute(clientConfig.maxConnectionsPerDestination);
    connectionManager.setMaxTotal(Integer.MAX_VALUE); // NOTE: Not supporting this option, because it is not supported by all drivers.

    return HttpClients.custom()
      .setDefaultCookieStore(cookieStore)
      .setConnectionManager(connectionManager)
      .setConnectionManagerShared(false)
      .disableAutomaticRetries()
      .evictExpiredConnections()
      .build();
  }

  private static final String[] excludeHeaders = {HttpHeader.CONTENT_LENGTH.getName(), HttpHeader.TRANSFER_ENCODING.getName()};

  @Override
  Invocation build(final CloseableHttpClient httpClient, final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final String method, final HttpHeadersImpl requestHeaders, final ArrayList<javax.ws.rs.core.Cookie> cookies, final CacheControl cacheControl, final Entity<?> entity, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final HashMap<String,Object> properties, final long connectTimeout, final long readTimeout) throws Exception {
    return new ClientRequestContextImpl(client, runtimeContext, uri, method, requestHeaders, cookies, cacheControl, entity, executorService, scheduledExecutorService, properties, connectTimeout, readTimeout) {
      private final Timeout connectTimeoutObj = connectTimeoutMs > 0 ? Timeout.of(connectTimeoutMs, TimeUnit.MILLISECONDS) : null;
      private final Timeout readTimeoutObj = readTimeoutMs > 0 ? Timeout.of(readTimeoutMs, TimeUnit.MILLISECONDS) : null;

      private CloseableHttpResponse response = null;
      private InputStream entityStream = null;

      void executeRequest(final AtomicBoolean executed, final AtomicReference<Object> resultRef, final HttpUriRequestBase request) {
        executed.set(true);
        try {
          resultRef.set(httpClient.execute(request));
        }
        catch (final Throwable t) {
          resultRef.set(t);
        }
      }

      private void flushHeaders(final HttpUriRequestBase request) {
        if (requestHeaders.size() > 0) {
          int size;
          String name;
          List<String> values;
          OUT:
          for (final Map.Entry<String,List<String>> entry : requestHeaders.entrySet()) { // [S]
            if ((values = entry.getValue()) != null && (size = values.size()) > 0) {
              name = entry.getKey();
              /**
               * @see org.apache.hc.core5.http.protocol.RequestContent#process(org.apache.hc.core5.http.HttpRequest,org.apache.hc.core5.http.EntityDetails,org.apache.hc.core5.http.protocol.HttpContext)
               */
              for (final String excludeHeader : excludeHeaders) { // [A]
                if (name.equalsIgnoreCase(excludeHeader))
                  continue OUT;

                request.setHeader(name, size == 1 ? values.get(0).toString() : CollectionUtil.toString(values, HttpHeadersImpl.getHeaderValueDelimiters(name)[0]));
              }
            }
          }
        }
      }

      Object getResult(final AtomicReference<Object> resultRef) {
        final Object result = resultRef.get();
        if (result instanceof Exception)
          throw new ProcessingException((Exception)result);

        if (result instanceof Throwable)
          Throwing.rethrow((Throwable)result);

        return result;
      }

      void await(final Condition condition, final AtomicLong timeoutMs) throws IOException {
        final long ts = System.currentTimeMillis();
        try {
          if (!condition.await(timeoutMs.get(), TimeUnit.MILLISECONDS))
            throw new IOException("Elapsed timeout of " + readTimeoutMs + "ms");
        }
        catch (final InterruptedException e) {
          throw new IOException(e);
        }

        timeoutMs.addAndGet(ts - System.currentTimeMillis());
      }

      @Override
      @SuppressWarnings({"rawtypes", "unchecked"})
      public Response invoke() {
        final URI uri = getUri();
        try {
          $span(Span.TOTAL, Span.INIT);

          final RequestConfig.Builder config = RequestConfig.custom().setConnectionKeepAlive(TimeValue.MAX_VALUE); // FIXME: Put into config

          if (connectTimeoutObj != null)
            config.setConnectTimeout(connectTimeoutObj);

          if (readTimeoutObj != null)
            config.setResponseTimeout(readTimeoutObj);

          if (Booleans.parseBoolean(client.getProperty(ClientProperties.FOLLOW_REDIRECTS), ClientProperties.FOLLOW_REDIRECTS_DEFAULT)) {
            config.setRedirectsEnabled(true);
            config.setMaxRedirects(Numbers.parseInt(client.getProperty(ClientProperties.MAX_REDIRECTS), ClientProperties.MAX_REDIRECTS_DEFAULT));
          }
          else {
            config.setRedirectsEnabled(false);
          }

          final RequestConfig requestConfig = config.build();
          final HttpUriRequestBase request = new HttpUriRequestBase(method, uri);
          request.setConfig(requestConfig);

          if (cookies != null)
            request.setHeader(HttpHeaders.COOKIE, StrictCookie.toHeader(cookies));

          if (cacheControl != null)
            request.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl.toString());

          $span(Span.INIT);

          final ProxyConfig proxyConfig = client.getClientConfig().proxyConfig;
          if (proxyConfig != null)
            proxyConfig.acquire();

          try {
            if (entity == null) {
              flushHeaders(request);
              response = httpClient.execute(request);
            }
            else {
              $span(Span.ENTITY_INIT);

              final MessageBodyWriter messageBodyWriter = getMessageBodyWriter();

              final AtomicLong timeoutMs = new AtomicLong(readTimeoutMs > 0 ? readTimeoutMs : Long.MAX_VALUE);
              final ReentrantLock lock = new ReentrantLock();
              final Condition condition = lock.newCondition();

              // This convoluted approach allows MessageBodyWriter#writeTo() to be called (which may set headers), and upon its first
              // OutputStream#write(), the actual request is executed. Before it is executed, the request sets an AbstractHttpEntity.
              // When AbstractHttpEntity#writeTo() is called, this approach sets the OutputStream from AbstractHttpEntity#writeTo() to the call
              // thread waiting to continue MessageBodyWriter#writeTo().
              final AtomicReference<Object> resultRef = new AtomicReference<>();
              final AtomicReference<UnsynchronizedByteArrayOutputStream> tempOutputStream = new AtomicReference<>();
              final AtomicBoolean executed = new AtomicBoolean();
              try (final RelegateOutputStream relegateEntityStream = new RelegateOutputStream() {
                @Override
                public void close() throws IOException {
                  try {
                    super.close();
                  }
                  finally {
                    if (!executed.get()) {
                      executeRequest(executed, resultRef, request);
                      getResult(resultRef);
                    }

                    lock.lock();
                    condition.signal();
                    lock.unlock();

                    $span(Span.ENTITY_WRITE);
                  }
                }
              }) {
                relegateEntityStream.setTarget(new ObservableOutputStream() {
                  @Override
                  protected boolean beforeWrite(final int b, final byte[] bs, final int off, final int len) throws IOException {
                    if (target == null) {
                      flushHeaders(request);
                      final UnsynchronizedByteArrayOutputStream out = bs != null ? new UnsynchronizedByteArrayOutputStream(len != -1 ? len : bs.length) : new UnsynchronizedByteArrayOutputStream(1);
                      tempOutputStream.set(out);
                      target = out;
                      $span(Span.ENTITY_INIT, Span.ENTITY_WRITE);
                    }

                    request.setEntity(new AbstractHttpEntity((String)null, null) {
                      @Override
                      public boolean isRepeatable() {
                        return false;
                      }

                      @Override
                      public long getContentLength() {
                        return -1;
                      }

                      @Override
                      public boolean isStreaming() {
                        return false;
                      }

                      @Override
                      public InputStream getContent() {
                        throw new UnsupportedOperationException();
                      }

                      @Override
                      public void writeTo(final OutputStream outStream) throws IOException {
                        lock.lock();
                        target = outStream;
                        condition.signal();
                        try {
                          await(condition, timeoutMs);
                        }
                        finally {
                          lock.unlock();
                        }
                      }

                      @Override
                      public void close() {
                      }
                    });

                    lock.lock();
                    try {
                      executorService.execute(() -> {
                        executeRequest(executed, resultRef, request);
                        lock.lock();
                        condition.signal();
                        lock.unlock();
                      });

                      await(condition, timeoutMs);
                    }
                    finally {
                      lock.unlock();
                    }

                    getResult(resultRef);
                    target.write(tempOutputStream.get().toByteArray());
                    tempOutputStream.set(null);

                    return true;
                  }

                  @Override
                  protected void afterWrite(final int b, final byte[] bs, final int off, final int len) {
                    relegateEntityStream.setTarget(target);
                  }
                });

                messageBodyWriter.writeTo(getEntity(), getEntityClass(), getGenericType(), getAnnotations(), getMediaType(), requestHeaders.getMirrorMap(), relegateEntityStream);
              }

              $span(Span.RESPONSE_WAIT);

              if (resultRef.get() == null) {
                lock.lock();
                try {
                  if (resultRef.get() == null) {
                    await(condition, timeoutMs);
                  }
                }
                finally {
                  lock.unlock();
                }
              }

              response = (CloseableHttpResponse)getResult(resultRef);
            }
          }
          finally {
            if (proxyConfig != null)
              proxyConfig.release();
          }

          $span(Span.RESPONSE_WAIT, Span.RESPONSE_READ);

          final int statusCode = response.getCode();
          final String reasonPhrase = response.getReasonPhrase();
          final StatusType statusInfo = Responses.from(statusCode, reasonPhrase);
          final HttpHeadersImpl responseHeaders = new HttpHeadersImpl();
          for (final Header header : response.getHeaders()) { // [A]
            final String headerName = header.getName();
            final List<String> headerValues = responseHeaders.getValues(headerName);
            final char[] delimiters = HttpHeadersImpl.getHeaderValueDelimiters(headerName);
            final String value = header.getValue();
            HttpHeadersImpl.parseHeaderValuesFromString(headerValues, value, delimiters);
          }

          // FIXME: This code is confusing Cookie vs NewCookie, and needs to be redone.
          final Map<String,NewCookie> cookies;
          if (client.hasProperty(ClientProperties.DISABLE_COOKIES)) {
            cookies = null;
          }
          else {
            final List<Cookie> httpCookies = cookieStore.getCookies();
            final int noCookies = httpCookies.size();
            if (noCookies == 0) {
              cookies = null;
            }
            else {
              cookies = new HashMap<>(noCookies);
              if (httpCookies instanceof RandomAccess) {
                int i = 0;
                do // [RA]
                  addCookie(cookies, httpCookies.get(i));
                while (++i < noCookies);
              }
              else {
                final Iterator<Cookie> i = httpCookies.iterator();
                do // [I]
                  addCookie(cookies, i.next());
                while (i.hasNext());
              }
            }
          }

          $span(Span.RESPONSE_READ);

          final HttpEntity entity = response.getEntity();
          entityStream = entity == null ? null : EntityUtil.makeConsumableNonEmptyOrNull(entity.getContent(), true);
          if (entityStream != null)
            $span(Span.ENTITY_READ);

          return new ResponseImpl(this, statusCode, statusInfo, responseHeaders, cookies, entityStream, null) {
            @Override
            public void close() throws ProcessingException {
              ProcessingException pe = null;
              try {
                super.close();
              }
              catch (final ProcessingException e) {
                pe = e;
              }

              closeResponse(pe);

              if (pe != null)
                throw pe;
            }
          };
        }
        catch (final Exception e) {
          closeResponse(e);

          if (e instanceof ProcessingException)
            throw (ProcessingException)e;

          throw new ProcessingException(uri.toString(), e);
        }
      }

      @Override
      void closeResponse(final Exception e) {
        if (entityStream != null) {
          $span(Span.ENTITY_READ, Span.TOTAL);
          try {
            entityStream.close();
          }
          catch (final IOException e1) {
            if (e != null)
              e.addSuppressed(e1);
          }

          entityStream = null;
        }

        if (response != null) {
          try {
            response.close();
          }
          catch (final IOException e1) {
            if (e != null)
              e.addSuppressed(e1);
          }

          response = null;
        }
      }
    };
  }
}