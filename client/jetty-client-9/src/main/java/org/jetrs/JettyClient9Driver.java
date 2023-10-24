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
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import org.libj.lang.Systems;
import org.libj.util.CollectionUtil;

public class JettyClient9Driver extends CachedClientDriver<HttpClient> {
  private static final ThreadLocal<Long> connectTimeoutLocal = new ThreadLocal<>();
  private static final int maxConnectionsPerDestination = Systems.getProperty(ClientProperties.MAX_CONNECTIONS_PER_DESTINATION, ClientProperties.MAX_CONNECTIONS_PER_DESTINATION_DEFAULT);

  @Override
  HttpClient newClient(final SSLContext sslContext) {
    // HTTP2Client h2Client = new HTTP2Client();
    // h2Client.setSelectors(1);
    // HttpClientTransportOverHTTP2 transport = new HttpClientTransportOverHTTP2(h2Client);

    final SslContextFactory sslContextFactory = new SslContextFactory.Client();
    sslContextFactory.setSslContext(sslContext);
    final HttpClient httpClient = new HttpClient(/* transport, */ sslContextFactory) {
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

    httpClient.setStopTimeout(0); // NOTE: Deprecated in v10, so set to 0 to disable in v9.
    httpClient.setMaxConnectionsPerDestination(maxConnectionsPerDestination);
    httpClient.setCookieStore(Jdk8ClientDriver.cookieStore);
    try {
      httpClient.start();
    }
    catch (final Exception e) {
      throw new ExceptionInInitializerError(e);
    }

    // Remove Jetty's response handlers, to allow the JAX-RS runtime to handle the raw content.
    httpClient.getContentDecoderFactories().clear();
    httpClient.getProtocolHandlers().remove(WWWAuthenticationProtocolHandler.NAME);
    return httpClient;
  }

  @Override
  Invocation build(final HttpClient httpClient, final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final String method, final HttpHeadersImpl requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final Entity<?> entity, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final HashMap<String,Object> properties, final long connectTimeout, final long readTimeout) throws Exception {
    if (connectTimeout > 0)
      connectTimeoutLocal.set(connectTimeout);

    return new ClientRequestContextImpl(client, runtimeContext, uri, method, requestHeaders, cookies, cacheControl, entity, executorService, scheduledExecutorService, properties, connectTimeout, readTimeout) {
      private void setHeaders(final Request request) {
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

          final Request request = httpClient.newRequest(uri)
            .method(method)
            .timeout(connectTimeout + readTimeout, TimeUnit.MILLISECONDS)
            .followRedirects(Systems.getProperty(ClientProperties.FOLLOW_REDIRECTS, ClientProperties.FOLLOW_REDIRECTS_DEFAULT));

          if (cookies != null)
            request.header(HttpHeaders.COOKIE, StrictCookie.toHeader(cookies));

          if (cacheControl != null)
            request.header(HttpHeaders.CACHE_CONTROL, cacheControl.toString());

          final InputStreamResponseListener listener = new InputStreamResponseListener();

          $span(Span.INIT);

          if (entity == null) {
            setHeaders(request);
          }
          else {
            $span(Span.ENTITY_INIT);

            final MessageBodyWriter messageBodyWriter = getMessageBodyWriter();

            writeContentAsync(messageBodyWriter, () -> {
              setHeaders(request);
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
          final HttpFields headers = response.getHeaders();
          if (headers.size() > 0) {
            for (final HttpField header : headers) { // [I]
              final String headerName = header.getName();
              final List<String> headerValues = responseHeaders.getValues(headerName);
              final char[] delimiters = HttpHeadersImpl.getHeaderValueDelimiters(headerName);
              final String value = header.getValue();
              HttpHeadersImpl.parseHeaderValuesFromString(headerValues, value, delimiters);
            }
          }

          final HashMap<String,NewCookie> cookies;
          if (Systems.hasProperty(ClientProperties.DISABLE_COOKIES)) {
            cookies = null;
          }
          else {
            final List<HttpCookie> httpCookies = Jdk8ClientDriver.cookieStore.getCookies();
            final int noCookies = httpCookies.size();
            if (noCookies == 0) {
              cookies = null;
            }
            else {
              final Date date = responseHeaders.getDate();
              cookies = new HashMap<>(noCookies);
              if (httpCookies instanceof RandomAccess) {
                int i = 0;
                do // [RA]
                  Jdk8ClientDriver.addCookie(cookies, httpCookies.get(i), date);
                while (++i < noCookies);
              }
              else {
                final Iterator<HttpCookie> i = httpCookies.iterator();
                do // [I]
                  Jdk8ClientDriver.addCookie(cookies, i.next(), date);
                while (i.hasNext());
              }
            }
          }

          $span(Span.RESPONSE_READ);
          final InputStream entityStream = EntityUtil.makeConsumableNonEmptyOrNull(listener.getInputStream(), true);
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
          if (e instanceof ProcessingException)
            throw (ProcessingException)e;

          throw new ProcessingException(uri.toString(), e);
        }
      }
    };
  }
}