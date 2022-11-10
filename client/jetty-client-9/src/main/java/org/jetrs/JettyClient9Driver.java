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

import static org.eclipse.jetty.http.HttpHeader.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.MessageBodyWriter;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.WWWAuthenticationProtocolHandler;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.libj.util.CollectionUtil;

public class JettyClient9Driver extends CachedClientDriver<HttpClient> {
  private static final ThreadLocal<Long> connectTimeoutLocal = new ThreadLocal<>();

  @Override
  HttpClient newClient(final SSLContext sslContext) {
//    HTTP2Client h2Client = new HTTP2Client();
//    h2Client.setSelectors(1);
//    HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(h2Client);

    final SslContextFactory sslContextFactory = new SslContextFactory.Client();
    sslContextFactory.setSslContext(sslContext);
    final HttpClient httpClient = new HttpClient(/*transport,*/ sslContextFactory) {
      @Override
      public long getConnectTimeout() {
        final Long connectTimeout = connectTimeoutLocal.get();
        return connectTimeout != null ? connectTimeout : super.getConnectTimeout();
      }
    };

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          httpClient.stop();
        }
        catch (final Exception e) {
          e.printStackTrace();
        }
      }
    });

    httpClient.setStopTimeout(0); // FIXME: Put in config
    httpClient.setMaxConnectionsPerDestination(256);  // FIXME: Put into config
    httpClient.setCookieStore(Jdk8ClientDriver.cookieStore);
    try {
      httpClient.start();
    }
    catch (final Exception e) {
      throw new ExceptionInInitializerError(e);
    }

    httpClient.getProtocolHandlers().remove(WWWAuthenticationProtocolHandler.NAME);
    return httpClient;
  }

  @Override
  Invocation build(final HttpClient httpClient, final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final String method, final Entity<?> entity, final HttpHeadersMap<String,Object> requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) throws Exception {
    if (connectTimeout > 0)
      connectTimeoutLocal.set(connectTimeout);

    return new InvocationImpl(client, runtimeContext, url, method, entity, requestHeaders, cookies, cacheControl, executorService, scheduledExecutorService, connectTimeout, readTimeout) {
      private void flushHeaders(final Request request) {
        // Remove headers that are set by default (unsolicited).
        final HttpFields headers = request.getHeaders();
        headers.remove(ACCEPT_ENCODING);
        headers.remove(USER_AGENT);

        if (requestHeaders.size() > 0) {
          int size;
          String name;
          List<String> values;
          for (final Map.Entry<String,List<String>> entry : requestHeaders.entrySet()) // [S]
            if ((values = entry.getValue()) != null && (size = values.size()) > 0)
              request.header(name = entry.getKey(), size == 1 ? values.get(0).toString() : CollectionUtil.toString(values, HttpHeadersImpl.getHeaderValueDelimiters(name)[0]));
        }
      }

      @Override
      @SuppressWarnings("rawtypes")
      public Response invoke() {
        try {
          final long ts = System.currentTimeMillis();
          $span(Span.TOTAL, Span.INIT);

          final Request request = httpClient.newRequest(url.toURI())
            .method(method)
            .timeout(connectTimeout + readTimeout, TimeUnit.MILLISECONDS);

          if (cookies != null)
            request.header(HttpHeaders.COOKIE, CollectionUtil.toString(cookies, ';'));

          if (cacheControl != null)
            request.header(HttpHeaders.CACHE_CONTROL, cacheControl.toString());

          final InputStreamResponseListener listener = new InputStreamResponseListener();

          $span(Span.INIT);

          if (entity == null) {
            flushHeaders(request);
          }
          else {
            $span(Span.ENTITY_INIT);

            final Class<?> entityClass = entity.getEntity().getClass();
            final MessageBodyWriter messageBodyWriter = requestContext.getProviders().getMessageBodyWriter(entityClass, null, entity.getAnnotations(), entity.getMediaType());
            if (messageBodyWriter == null)
              throw new ProcessingException("Provider not found for " + entityClass.getName());

            writeContentAsync(messageBodyWriter, entityClass, () -> {
              flushHeaders(request);
              // final PipedInputStream in = new PipedInputStream();
              // out = new PipedOutputStream(in);
              // final InputStreamContentProvider provider = new InputStreamContentProvider(in, 8192);
              final OutputStreamContentProvider provider = new OutputStreamContentProvider();
              request.content(provider);
              final OutputStream out = provider.getOutputStream();
              $span(Span.ENTITY_INIT, Span.ENTITY_WRITE);
              return out;
            }, $isSpanEnabled() ? () -> $span(Span.ENTITY_WRITE) : null);

            $span(Span.RESPONSE_WAIT);
          }

          request.send(listener);
          final org.eclipse.jetty.client.api.Response response = listener.get((readTimeout > 0 ? readTimeout : Long.MAX_VALUE) + ts - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

          // System.err.println(request.getHeaders().toString());

          $span(Span.RESPONSE_WAIT, Span.RESPONSE_READ);

          final int statusCode = response.getStatus();
          final StatusType statusInfo = Responses.from(statusCode, response.getReason());
          final HttpHeadersImpl responseHeaders = new HttpHeadersImpl();
          for (final HttpField header : response.getHeaders())
            responseHeaders.add(header.getName(), header.getValue());

          final List<HttpCookie> httpCookies = Jdk8ClientDriver.cookieStore.getCookies();
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
                Jdk8ClientDriver.addCookie(cookies, httpCookies.get(i), date);
            }
            else {
              for (final HttpCookie httpCookie : httpCookies) // [L]
                Jdk8ClientDriver.addCookie(cookies, httpCookie, date);
            }
          }

          $span(Span.RESPONSE_READ);
          final InputStream entityStream = EntityUtil.makeConsumableNonEmptyOrNull(listener.getInputStream(), true);
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