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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.libj.util.CollectionUtil;

public class ApacheClient5Driver extends CachedClientDriver<CloseableHttpClient> {
  private static final PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
  private static final BasicCookieStore cookieStore = new BasicCookieStore();

  static {
    poolingConnManager.setDefaultMaxPerRoute(64); // FIXME: Put into config
    poolingConnManager.setMaxTotal(320);          // FIXME: Put into config
  }

  static void addCookie(final Map<String,NewCookie> cookies, final Cookie cookie) {
    final int version = 1; // FIXME: How to get the version?
    final int maxAge = 0; // FIXME: How to get the maxAge?
    final boolean secure = false; // FIXME: How to get the secure?
    final boolean httpOnly = false; // FIXME: How to get the httpOnly?
    final NewCookie newCookie = new NewCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), version, null, maxAge, cookie.getExpiryDate(), secure, httpOnly);
    cookies.put(newCookie.getName(), newCookie);
  }

  @Override
  CloseableHttpClient newClient(final SSLContext sslContext) {
    final CloseableHttpClient httpClient = HttpClients.custom()
      .setConnectionManager(poolingConnManager)
      .setConnectionManagerShared(true)
      .setDefaultCookieStore(cookieStore)
      .build();

    return httpClient;
  }

  private static final String[] excludeHeaders = {HttpHeader.CONTENT_LENGTH.getName(), HttpHeader.TRANSFER_ENCODING.getName()};

  @Override
  Invocation build(final CloseableHttpClient httpClient, final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final String method, final Entity<?> entity, final HttpHeadersMap<String,Object> requestHeaders, final ArrayList<javax.ws.rs.core.Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) throws Exception {
    return new InvocationImpl(client, runtimeContext, url, method, entity, requestHeaders, cookies, cacheControl, executorService, scheduledExecutorService, connectTimeout, readTimeout) {
      private final Timeout connectTimeoutObj = Timeout.of(connectTimeout, TimeUnit.MILLISECONDS);
      private final Timeout readTimeoutObj = Timeout.of(readTimeout, TimeUnit.MILLISECONDS);

      @Override
      ExecutorService getDefaultExecutorService() {
        return Executors.newCachedThreadPool(); // FIXME: Make configurable
      }

      @Override
      ScheduledExecutorService getDefaultScheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor(); // FIXME: Make configurable
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
              /** @see org.apache.hc.core5.http.protocol.RequestContent#process(org.apache.hc.core5.http.HttpRequest,org.apache.hc.core5.http.EntityDetails,org.apache.hc.core5.http.protocol.HttpContext) */
              for (final String excludeHeader : excludeHeaders) {
                if (name.equalsIgnoreCase(excludeHeader))
                  continue OUT;

                request.setHeader(name, size == 1 ? values.get(0).toString() : CollectionUtil.toString(values, HttpHeadersImpl.getHeaderValueDelimiters(name)[0]));
              }
            }
          }
        }
      }

      @Override
      @SuppressWarnings({"rawtypes", "resource"})
      public Response invoke() {
        try {
          $telemetry(Span.TOTAL, Span.INIT);
          final HttpUriRequestBase request = new HttpUriRequestBase(method, url.toURI());
          request.setConfig(RequestConfig.custom()
            .setConnectionKeepAlive(TimeValue.MAX_VALUE) // FIXME: Put into config
            .setConnectTimeout(connectTimeoutObj)
            .setResponseTimeout(readTimeoutObj)
            .build());

         if (cookies != null)
           request.setHeader(HttpHeaders.COOKIE, CollectionUtil.toString(cookies, ';'));

         if (cacheControl != null)
           request.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl.toString());

          $telemetry(Span.INIT);

          final CloseableHttpResponse response;
          if (entity == null) {
            flushHeaders(request);
            response = httpClient.execute(request);
          }
          else {
            $telemetry(Span.ENTITY_INIT);

            final Class<?> entityClass = entity.getEntity().getClass();
            final MessageBodyWriter messageBodyWriter = requestContext.getProviders().getMessageBodyWriter(entityClass, null, entity.getAnnotations(), entity.getMediaType());
            if (messageBodyWriter == null)
              throw new ProcessingException("Provider not found for " + entityClass.getName());

            writeContentAsync(messageBodyWriter, entityClass, () -> {
              flushHeaders(request);
              final PipedInputStream in = new PipedInputStream();
              request.setEntity(new InputStreamEntity(in, ContentType.parse(requestHeaders.getFirst(HttpHeaders.CONTENT_TYPE))));
              final PipedOutputStream out = new PipedOutputStream(in);
              $telemetry(Span.ENTITY_INIT, Span.ENTITY_WRITE);
              return out;
            }, () -> $telemetry(Span.ENTITY_WRITE));

            $telemetry(Span.RESPONSE_WAIT);

            response = httpClient.execute(request);
          }

          $telemetry(Span.RESPONSE_WAIT, Span.RESPONSE_READ);

          final int statusCode = response.getCode();
          final String reasonPhrase = response.getReasonPhrase();
          final StatusType statusInfo = reasonPhrase != null ? Responses.from(statusCode, reasonPhrase) : Responses.from(statusCode);
          final HttpHeadersImpl responseHeaders = new HttpHeadersImpl();
          for (final Header header : response.getHeaders())
            responseHeaders.add(header.getName(), header.getValue());

          final List<Cookie> httpCookies = cookieStore.getCookies();
          final Map<String,NewCookie> cookies;
          final int noCookies = httpCookies.size();
          if (noCookies == 0) {
            cookies = null;
          }
          else {
            cookies = new HashMap<>(noCookies);
            if (httpCookies instanceof RandomAccess) {
              for (int i = 0; i < noCookies; ++i) // [RA]
                addCookie(cookies, httpCookies.get(i));
            }
            else {
              for (final Cookie httpCookie : httpCookies) // [L]
                addCookie(cookies, httpCookie);
            }
          }

          $telemetry(Span.RESPONSE_READ);
          final HttpEntity entity = response.getEntity();
          final InputStream entityStream = entity == null ? null : EntityUtil.makeConsumableNonEmptyOrNull(entity.getContent(), true);
          if (entityStream != null)
            $telemetry(Span.ENTITY_READ);

          return new ResponseImpl(requestContext, statusCode, statusInfo, responseHeaders, cookies, entityStream, null) {
            @Override
            public void close() throws ProcessingException {
              ProcessingException pe = null;
              try {
                super.close();
              }
              catch (final ProcessingException e) {
                pe = e;
              }

              if (entityStream != null) {
                $telemetry(Span.ENTITY_READ, Span.TOTAL);
                try {
                  entityStream.close();
                }
                catch (final IOException e) {
                  if (pe != null)
                    pe.addSuppressed(e);
                }
              }

              try {
                response.close();
              }
              catch (final IOException e) {
                if (pe != null)
                  pe.addSuppressed(e);
              }

              if (pe != null)
                throw pe;
            }
          };
        }
        catch (final Exception e) {
          throw new ProcessingException(e);
        }
      }
    };
  }
}