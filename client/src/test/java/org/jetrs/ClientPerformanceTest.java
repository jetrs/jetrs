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
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jetrs.provider.ext.StringProvider;
import org.junit.Ignore;
import org.libj.io.Streams;

@Ignore
public class ClientPerformanceTest {
  public static void main(final String[] args) throws Exception {
    System.setProperty("http.keepAlive", "true");
    System.setProperty("http.maxConnections", "100");
    long a = 0;
    long b = 0;
    long c = 0;
    long d = 0;
    try {
      // Thread.sleep(10000L);
      final URL url = new URL("https://catfact.ninja/fact");
      for (; count < 10; ++count, System.out.print(".")) {
        Thread.sleep(1000L);
        a += a(url);
        Thread.sleep(1000L);
        b += b(url);
        Thread.sleep(1000L);
        c += c(url);
        Thread.sleep(1000L);
        d += d(url);
      }
    }
    catch (final Throwable t) {
      t.printStackTrace();
    }
    finally {
      if (count == 0)
        return;

      System.out.println();
      Thread.sleep(10);
      System.err.println(count + " timeA: " + a / count);
      System.err.println(count + " timeB: " + b / count);
      System.err.println(count + " timeC: " + c / count);
      System.err.println(count + " timeD: " + d / count);
    }
  }

  private static int count = 0;

  private static long a(final URL url) throws IOException {
    final long ts = System.currentTimeMillis();
    final HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
    try (final InputStream in = urlConnection.getInputStream()) {
      Streams.readBytes(in);
    }

    urlConnection.disconnect();

    return System.currentTimeMillis() - ts;
  }

  private static final Client jxClient;

  static {
    final ClientBuilder builder = ClientBuilder.newBuilder();
    builder.connectTimeout(5000, TimeUnit.MILLISECONDS).readTimeout(5000, TimeUnit.MILLISECONDS);
    builder.register(new StringProvider());
    jxClient = builder.build();
  }

  private static long b(final URL url) throws IOException, URISyntaxException {
    final long ts = System.currentTimeMillis();

    try (
      final Response r = jxClient.target(url.toURI()).request().get();
      final InputStream in = (InputStream)r.getEntity();
    ) {
      Streams.readBytes(in);
    }

    return System.currentTimeMillis() - ts;
  }

  private static final PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
  private static final CloseableHttpClient client = HttpClients.custom().setConnectionManager(poolingConnManager).setConnectionManagerShared(true).build();

  public static long c(final URL url) throws Exception {
    final HttpGet httpGet = new HttpGet(url.toURI());
    final long ts = System.currentTimeMillis();
    try (final CloseableHttpResponse response = client.execute(httpGet)) {
      if (response.getCode() != 200)
        throw new IllegalStateException();

      try (final HttpEntity entity = response.getEntity(); final InputStream in = entity.getContent()) {
        Streams.readBytes(in);
        EntityUtils.consume(entity);
      }
    }
    return System.currentTimeMillis() - ts;
  }

  private static final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
  private static final HttpClient httpClient = new HttpClient(sslContextFactory);

  static {
    try {
      httpClient.start();
    }
    catch (final Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static long d(final URL url) throws Exception {
    final long ts = System.currentTimeMillis();
    final ContentResponse response = httpClient.GET(url.toURI());
    response.getContent();
    return System.currentTimeMillis() - ts;
  }
}