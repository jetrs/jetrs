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

import static org.libj.lang.Assertions.*;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.MessageBodyWriter;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.libj.util.CollectionUtil;

class JettyClient9InvocationImpl2 extends InvocationImpl {
  private static final HttpClient httpClient = new HttpClient(new SslContextFactory.Client());

  static {
    httpClient.setStopTimeout(30000); // FIXME: Put in config
    httpClient.setMaxConnectionsPerDestination(64);  // FIXME: Put into config
//    httpClient.setConnectTimeout(connectTimeout);
    httpClient.setCookieStore(DefaultClientDriver.cookieStore);
    try {
      httpClient.start();
    }
    catch (final Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  JettyClient9InvocationImpl2(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final String method, final Entity<?> entity, final HttpHeadersMap<String,Object> requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) throws Exception {
    super(client, runtimeContext, url, method, entity, requestHeaders, cookies, cacheControl, executorService, scheduledExecutorService, connectTimeout, readTimeout);
  }

  @Override
  ExecutorService getDefaultExecutorService() {
    return Executors.newCachedThreadPool(); // FIXME: Make configurable
  }

  @Override
  ScheduledExecutorService getDefaultScheduledExecutorService() {
    return Executors.newSingleThreadScheduledExecutor(); // FIXME: Make configurable
  }

  private class ConnectionOutputStream extends FilterOutputStream {
    private final Request request;

    private ConnectionOutputStream(final Request request) {
      super(null);
      this.request = request;
    }

    @Override
    public void write(final int b) throws IOException {
      if (out == null) {
        flushHeaders(request);

        final OutputStreamContentProvider provider = new OutputStreamContentProvider();
        request.content(provider);
        out = provider.getOutputStream();
      }

      super.write(b);
    }

    @Override
    public void flush() throws IOException {
      if (out != null)
        out.flush();
    }

    @Override
    public void close() throws IOException {
      if (out != null)
        super.close();
    }
  }

  private void flushHeaders(final Request request) {
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
  @SuppressWarnings({"rawtypes", "resource", "unchecked"})
  public Response invoke() {
    try {
      final Request request = httpClient.newRequest(url.toURI())
        .method(method)
        .timeout(connectTimeout + readTimeout, TimeUnit.MILLISECONDS);

      if (cookies != null)
        request.header(HttpHeaders.COOKIE, CollectionUtil.toString(cookies, ';'));

      if (cacheControl != null)
        request.header(HttpHeaders.CACHE_CONTROL, cacheControl.toString());

      if (entity == null) {
        flushHeaders(request);
      }
      else {
        final Class<?> entityClass = entity.getEntity().getClass();
        final MessageBodyWriter messageBodyWriter = requestContext.getProviders().getMessageBodyWriter(entityClass, null, entity.getAnnotations(), entity.getMediaType());
        if (messageBodyWriter == null)
          throw new ProcessingException("Provider not found for " + entityClass.getName());

        try (final ConnectionOutputStream out = new ConnectionOutputStream(request)) {
          messageBodyWriter.writeTo(entity.getEntity(), entityClass, null, entity.getAnnotations(), entity.getMediaType(), requestHeaders.getMirrorMap(), out);
          out.flush();
        }
      }

      final class ByteBufferBackedInputStream extends InputStream {
        private final AtomicReference<org.eclipse.jetty.client.api.Response> response = new AtomicReference<>();
        private final AtomicReference<ByteBuffer> buffer = new AtomicReference<>();
        private final AtomicBoolean finished = new AtomicBoolean();

        private boolean onBufferDrained() {
          try {
            if (finished.get())
              return false;

            synchronized (buffer.get()) {
              buffer.get().notify();
              System.err.println("<< buffer.notify() onBufferDrained");
              System.err.println("** buffer.set(null)");
              buffer.set(null);
            }

            if (buffer.get() == null) {
              if (finished.get())
                return false;

              synchronized (this) {
                if (buffer.get() == null) {
                  if (finished.get())
                    return false;

                  try {
                    System.err.println(">> in.wait() onBufferDrained");
                    wait();
                  }
                  catch (final InterruptedException e) {
                    response.get().abort(e);
                  }
                }
              }
            }

            return !finished.get();
          }
          finally {
            System.err.println("<< onBufferDrained(): " + finished.get());
          }
        }

        @Override
        public int available() throws IOException {
          return finished.get() ? 0 : buffer.get().remaining();
        }

        @Override
        public int read() throws IOException {
          System.err.println(">> in.read()");
          if (finished.get())
            return -1;

          do {
            final ByteBuffer buffer = this.buffer.get();
            if (buffer.hasRemaining())
              return buffer.get() & 0xFF;
          }
          while (onBufferDrained());
          return -1;
        }

        @Override
        public int read(final byte[] b, final int off, int len) throws IOException {
          System.err.println(">> in.read(byte[],int,int)");
          if (finished.get())
            return -1;

          do {
            final ByteBuffer buffer = this.buffer.get();
            final int remaining = buffer.remaining();
            if (remaining > 0) {
              len = Math.min(len, remaining);
              buffer.get(b, off, len);
              return len;
            }
          }
          while (onBufferDrained());
          return -1;
        }
      }

      final ByteBufferBackedInputStream in = new ByteBufferBackedInputStream();
      final org.eclipse.jetty.client.api.Response.Listener.Adapter adapter = new org.eclipse.jetty.client.api.Response.Listener.Adapter() {
        @Override
        public void onBegin(final org.eclipse.jetty.client.api.Response response) {
          in.response.set(response);
        }

        @Override
        public void onContent(final org.eclipse.jetty.client.api.Response response, final ByteBuffer buffer) {
          assertNotNull(buffer);
          synchronized (in) {
            System.err.println("** buffer.set(buffer)");
            in.buffer.set(buffer);
            in.notify();
            System.err.println("<< in.notify() onContent");
          }

          if (in.buffer.get() != null) {
            synchronized (buffer) {
              if (in.buffer.get() != null) {
                try {
                  System.err.println(">> buffer.wait() onContent");
                  buffer.wait();
                }
                catch (final InterruptedException e) {
                  response.abort(e);
                }
              }
            }
          }

          System.err.println("<< onContent()");
        }

        @Override
        public void onComplete(final Result result) {
          synchronized (in) {
            in.finished.set(true);
            in.notify();
            System.err.println("<< in.notify() onComplete");
          }
        }
      };

      request.send(adapter);
      synchronized (in) {
        try {
          System.err.println(">> in.wait(readTimeout)");
          in.wait(readTimeout);
        }
        catch (final Exception e) {
          throw new ProcessingException(e);
        }
      }

      if (in.buffer == null) {
        in.close();
        throw new ProcessingException("ReadTimeout elapsed: " + readTimeout);
      }

      final org.eclipse.jetty.client.api.Response response = in.response.get();

      final int statusCode = response.getStatus();
      final StatusType statusInfo = Responses.from(statusCode);
      final HttpHeadersImpl responseHeaders = new HttpHeadersImpl();
      for (final HttpField header : response.getHeaders())
        responseHeaders.add(header.getName(), header.getValue());

      final List<HttpCookie> httpCookies = DefaultClientDriver.cookieStore.getCookies();
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
            DefaultClientDriver.addCookie(cookies, httpCookies.get(i), date);
        }
        else {
          for (final HttpCookie httpCookie : httpCookies) // [L]
            DefaultClientDriver.addCookie(cookies, httpCookie, date);
        }
      }

      final InputStream entityStream = EntityUtil.makeConsumableNonEmptyOrNull(in, true);
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
}