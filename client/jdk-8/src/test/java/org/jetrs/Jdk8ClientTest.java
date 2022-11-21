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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;
import static org.libj.util.function.Throwing.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetrs.provider.ext.BytesProvider;
import org.jetrs.provider.ext.FormMultivaluedMapProvider;
import org.jetrs.provider.ext.FormProvider;
import org.jetrs.provider.ext.InputStreamProvider;
import org.jetrs.provider.ext.StringProvider;
import org.junit.AfterClass;
import org.junit.Test;
import org.libj.io.Streams;
import org.libj.lang.Classes;
import org.libj.util.UnsynchronizedByteArrayOutputStream;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

@SuppressWarnings("unused")
public class Jdk8ClientTest {
  private static byte[] createRandomBytes(final int length) {
    final byte[] bytes = new byte[length];
    final Random random = new Random();
    for (int i = 0, i$ = bytes.length; i < i$; ++i) // [A]
      bytes[i] = (byte)random.nextInt();

    return bytes;
  }

  private static final byte[] testBytes = createRandomBytes(Short.MAX_VALUE * 16);
  private static Client client = ClientBuilder
    .newBuilder()
    .connectTimeout(25000, TimeUnit.MILLISECONDS)
    .readTimeout(25000, TimeUnit.MILLISECONDS)
    .register(new StringProvider())
    .register(new BytesProvider())
    .register(new FormProvider())
    .register(new InputStreamProvider())
    .register(new FormMultivaluedMapProvider())
    .build();

  private static int tests = 50;

  private abstract static class Trial {
    private Trial(final String method) throws InterruptedException, IOException {
      this(method, null);
    }

    private Trial(final String method, final Supplier<Entity<?>> entity) throws InterruptedException, IOException {
      final WireMockRule server = new WireMockRule(0);
      server.start();
      configServer(server);

      final Invocation.Builder builder = buildRequest("http://localhost:" + server.port());
      for (int i = 0; i < tests; ++i)
        assertResponse(entity != null ? builder.method(method, entity.get()) : builder.method(method));

      final ExecutorService executor = Executors.newFixedThreadPool(tests);
      final CountDownLatch latch = new CountDownLatch(tests);
      for (int i = 0; i < tests; ++i) {
        executor.submit(() -> {
          try {
            final AsyncInvoker invoker = builder.async();
            try (final Response response = (entity != null ? invoker.method(method, entity.get()) : invoker.method(method)).get()) {
              assertResponse(response);
            }

            return true;
          }
          catch (final Exception e) {
            synchronized (Jdk8ClientTest.class) {
              e.printStackTrace();
              System.exit(1);
              return false;
            }
          }
          finally {
            latch.countDown();
          }
        });
      }

      latch.await();
    }

    abstract void configServer(WireMockRule server);
    abstract Invocation.Builder buildRequest(String host);
    abstract void assertResponse(Response response) throws IOException;
  }

  @Test
  public void testGet() throws InterruptedException, IOException {
    final String message = "{\"message\": \"SUCCESS\"}";
    new Trial(HttpMethod.GET) {
      @Override
      void configServer(final WireMockRule server) {
        server.stubFor(get("/get")
          .willReturn(ok()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .withBody(message)));
      }

      @Override
      Invocation.Builder buildRequest(final String host) {
        return client.target(host + "/get")
          .request().header(HttpHeaders.ACCEPT, "text/xml;q=.5,text/html");
      }

      @Override
      void assertResponse(final Response response) {
        assertEquals(Response.Status.OK, response.getStatusInfo());
        assertEquals(message, response.readEntity(String.class));
      }
    };
  }

  @Test
  public void testPut() throws InterruptedException, IOException {
    final String message = "<response>SUCCESS</response>";
    new Trial(HttpMethod.PUT, () -> Entity.entity(message, MediaType.TEXT_XML)) {
      @Override
      void configServer(final WireMockRule server) {
        server.stubFor(put("/put")
          .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.TEXT_XML))
          .willReturn(ok()
            .withBody(message)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)));
      }

      @Override
      Invocation.Builder buildRequest(final String host) {
        return client.target(host + "/put")
          .request("text/xml;q=.5,text/html", "text/x-dvi; q=0.8, text/x-c");
      }

      @Override
      void assertResponse(final Response response) {
        assertEquals(Response.Status.OK, response.getStatusInfo());
        assertEquals(message, response.readEntity(String.class));
      }
    };
  }

  @Test
  public void testPostSmall() throws InterruptedException, IOException {
    final String message = "<response>SUCCESS</response>";
    new Trial(HttpMethod.POST, () -> Entity.entity(message, MediaType.APPLICATION_OCTET_STREAM)) {
      @Override
      void configServer(final WireMockRule server) {
        server.stubFor(post("/post")
          .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_OCTET_STREAM))
          .willReturn(ok()
            .withBody(message)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)));
      }

      @Override
      Invocation.Builder buildRequest(final String host) {
        return client.target(host + "/post")
          .request();
      }

      @Override
      void assertResponse(final Response response) {
        assertEquals(Response.Status.OK, response.getStatusInfo());
        assertEquals(message, response.readEntity(String.class));
      }
    };
  }

  @Test
  public void testPostForm() throws InterruptedException, IOException {
    final MultivaluedLinkedHashMap<String> form = new MultivaluedLinkedHashMap<>();
    form.add("foo", "bar");
    form.add("one", "two");
    form.add("emptyValue", "");
    form.add("", "emptyKey");
    form.add("nullValue", null);
    form.add(null, "nullKey");
    form.add(null, null);

    final UnsynchronizedByteArrayOutputStream entity = new UnsynchronizedByteArrayOutputStream();
    EntityUtil.writeFormParams(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE, entity);

    new Trial(HttpMethod.POST, () -> Entity.form(form)) {
      @Override
      void configServer(final WireMockRule server) {
        server.stubFor(post("/post")
          .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_FORM_URLENCODED))
          .willReturn(ok()
            .withBody(entity.toByteArray())
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)));
      }

      @Override
      Invocation.Builder buildRequest(final String host) {
        return client.target(host + "/post")
          .request();
      }

      @Override
      void assertResponse(final Response response) throws IOException {
        assertEquals(Response.Status.OK, response.getStatusInfo());
        final UnsynchronizedByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream();
        EntityUtil.writeFormParams(form, null, out);
        final String message = new String(out.toByteArray());
        assertEquals(message, response.readEntity(String.class));
      }
    };
  }

  @Test
  public void testPostRandom() throws InterruptedException, IOException {
    final String message = "<response>SUCCESS</response>";
    new Trial(HttpMethod.POST, () -> Entity.entity(new ByteArrayInputStream(testBytes), MediaType.APPLICATION_OCTET_STREAM)) {
      @Override
      void configServer(final WireMockRule server) {
        server.stubFor(post("/post")
          .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_OCTET_STREAM))
          .willReturn(ok()
            .withBody(testBytes)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)));
      }

      @Override
      Invocation.Builder buildRequest(final String host) {
        return client.target(host + "/post")
          .request();
      }

      @Override
      void assertResponse(final Response response) throws IOException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        final byte[] actual = Streams.readBytes((InputStream)response.getEntity());
        assertArrayEquals(testBytes, actual);
      }
    };
  }

  @Test
  public void testParallel() throws InterruptedException {
    final int iterations = 5;
    final List<Throwable> exceptions = new ArrayList<>();
    final Method[] methods = Classes.getDeclaredMethodsWithAnnotationDeep(getClass(), Test.class);
    final int threadCount = (methods.length - 1) * iterations;
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final CountDownLatch latch = new CountDownLatch(threadCount);
    for (int i = 0; i < iterations; ++i) { // [N]
      for (final Method method : methods) {
        if (!"testParallel".equals(method.getName())) {
          executor.execute(rethrow(() -> {
            try {
              method.invoke(this);
            }
            catch (final Throwable t) {
              exceptions.add(t);
            }
            finally {
              latch.countDown();
            }
          }));
        }
      }
    }

    latch.await();
    if (exceptions.size() > 0) {
      for (final Throwable t : exceptions)
        t.printStackTrace();

      fail();
    }
  }

  @AfterClass
  public static void afterClass() {
    ClientDriver.dump();
  }
}