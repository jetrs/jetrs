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
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.MessageBodyWriter;

import org.libj.util.CollectionUtil;
import org.libj.util.Dates;

public class Jdk8ClientDriver extends ClientDriver {
  static final CookieStore cookieStore;

  static {
    final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    CookieHandler.setDefault(cookieManager);
    cookieStore = cookieManager.getCookieStore();
  }

  static void addCookie(final Map<String,NewCookie> cookies, final HttpCookie cookie, final Date date) {
    final int maxAge = (int)cookie.getMaxAge();
    final Date expiry = Dates.addTime(date, 0, 0, maxAge);
    final NewCookie newCookie = new NewCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), cookie.getVersion(), cookie.getComment(), maxAge, expiry, cookie.getSecure(), cookie.isHttpOnly());
    cookies.put(newCookie.getName(), newCookie);
  }

  @Override
  Invocation build(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final String method, final Entity<?> entity, final HttpHeadersMap<String,Object> requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) throws Exception {
    return new InvocationImpl(client, runtimeContext, url, method, entity, requestHeaders, cookies, cacheControl, executorService, scheduledExecutorService, connectTimeout, readTimeout) {
      private final SSLContext sslContext;

      {
        SSLContext sslContext = client.getSslContext();
        if (sslContext == null)
          sslContext = SSLContext.getDefault();

        this.sslContext = sslContext;
      }

      private void flushHeaders(final HttpURLConnection connection) {
        if (requestHeaders.size() > 0) {
          int size;
          String name;
          List<String> values;
          for (final Map.Entry<String,List<String>> entry : requestHeaders.entrySet()) // [S]
            if ((values = entry.getValue()) != null && (size = values.size()) > 0)
              connection.setRequestProperty(name = entry.getKey(), size == 1 ? values.get(0).toString() : CollectionUtil.toString(values, HttpHeadersImpl.getHeaderValueDelimiters(name)[0]));
        }
      }

      @Override
      @SuppressWarnings("rawtypes")
      public Response invoke() {
        try {
          $span(Span.TOTAL, Span.INIT);

          final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
          if (connection instanceof HttpsURLConnection)
            ((HttpsURLConnection)connection).setSSLSocketFactory(sslContext.getSocketFactory());

          connection.setRequestMethod(method);
          if (connectTimeout > 0) {
            if ((int)connectTimeout <= 0)
              throw new IllegalArgumentException("connectTimeout (" + connectTimeout + ") overflows (int)connectTimeout (" + (int)connectTimeout + ")");

            connection.setConnectTimeout((int)connectTimeout);
          }

          if (readTimeout > 0) {
            if ((int)readTimeout <= 0)
              throw new IllegalArgumentException("readTimeout (" + readTimeout + ") overflows (int)readTimeout (" + (int)readTimeout + ")");

            connection.setReadTimeout((int)readTimeout);
          }

          if (cookies != null)
            connection.setRequestProperty(HttpHeaders.COOKIE, CollectionUtil.toString(cookies, ';'));

          if (cacheControl != null)
            connection.setRequestProperty(HttpHeaders.CACHE_CONTROL, cacheControl.toString());
          else
            connection.setUseCaches(false);

          $span(Span.INIT);

          if (entity == null) {
            flushHeaders(connection);
          }
          else {
            $span(Span.ENTITY_INIT);

            connection.setDoOutput(true);
            final Class<?> entityClass = entity.getEntity().getClass();
            final MessageBodyWriter messageBodyWriter = requestContext.getProviders().getMessageBodyWriter(entityClass, null, entity.getAnnotations(), entity.getMediaType());
            if (messageBodyWriter == null)
              throw new ProcessingException("Provider not found for " + entityClass.getName());

            writeContentSync(messageBodyWriter, entityClass, () -> {
              flushHeaders(connection);
              final HttpHeadersMap<Object,String> mirrorMap = requestHeaders.getMirrorMap();
              final Number contentLength = (Number)mirrorMap.getFirst(HttpHeaders.CONTENT_LENGTH);
              if (contentLength != null)
                connection.setFixedLengthStreamingMode(contentLength.longValue());

              final OutputStream out = connection.getOutputStream();
              $span(Span.ENTITY_INIT, Span.ENTITY_WRITE);
              return out;
            }, $isSpanEnabled() ? () -> $span(Span.ENTITY_WRITE) : null);
          }

          $span(Span.RESPONSE_WAIT);

          final int statusCode = connection.getResponseCode();

          $span(Span.RESPONSE_WAIT, Span.RESPONSE_READ);

          final String reasonPhrase = connection.getResponseMessage();
          final StatusType statusInfo = reasonPhrase != null ? Responses.from(statusCode, reasonPhrase) : Responses.from(statusCode);
          final HttpHeadersImpl responseHeaders = new HttpHeadersImpl(connection.getHeaderFields());

          final List<HttpCookie> httpCookies = cookieStore.getCookies();
          final Map<String,NewCookie> cookies;
          final int noCookies = httpCookies.size();
          if (noCookies == 0) {
            cookies = null;
          }
          else {
            final Date date = responseHeaders.getDate();
            cookies = new HashMap<>(noCookies);
            if (httpCookies instanceof RandomAccess) {
              for (int i = 0; i < noCookies; ++i) // [RA]
                addCookie(cookies, httpCookies.get(i), date);
            }
            else {
              for (final HttpCookie httpCookie : httpCookies) // [L]
                addCookie(cookies, httpCookie, date);
            }
          }

          $span(Span.RESPONSE_READ);
          final InputStream entityStream = EntityUtil.makeConsumableNonEmptyOrNull(statusCode < 400 ? connection.getInputStream() : connection.getErrorStream(), true);
          if (entityStream != null)
            $span(Span.ENTITY_READ);

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
                $span(Span.ENTITY_READ, Span.TOTAL);
                try {
                  entityStream.close();
                }
                catch (final IOException e) {
                  if (pe != null)
                    pe.addSuppressed(e);
                }
              }

              connection.disconnect();
              if (pe != null)
                throw pe;
            }
          };
        }
        catch (final Exception e) {
          if (e instanceof ProcessingException)
            throw (ProcessingException)e;

          throw new ProcessingException(e);
        }
      }
    };
  }
}