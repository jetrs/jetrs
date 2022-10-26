/* Copyright (c) 2019 JetRS
 *
 * Permission is hereby granted, final free of charge, final to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), final to deal
 * in the Software without restriction, final including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, final and/or sell
 * copies of the Software, final and to permit persons to whom the Software is
 * furnished to do so, final subject to the following conditions:
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
import java.io.ByteArrayOutputStream;
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetrs.provider.ext.BytesProvider;
import org.jetrs.provider.ext.FormMultivaluedMapProvider;
import org.jetrs.provider.ext.FormProvider;
import org.jetrs.provider.ext.InputStreamProvider;
import org.jetrs.provider.ext.StringProvider;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.libj.io.Streams;
import org.libj.lang.Classes;
import org.libj.net.Sockets;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class DefaultClientTest {
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

  private static int tests = 10;

  @ClassRule
  public static final WireMockRule serverGet = new WireMockRule(Sockets.findRandomOpenPort());

  @Test
  public void testGet() {
    final String message = "{\"message\": \"SUCCESS\"}";
    serverGet.stubFor(get("/get")
      .willReturn(ok()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .withBody(message)));

    for (int i = 0; i < tests; ++i) {
      try (final Response response = client.target("http://localhost:" + serverGet.port() + "/get")
        .request()
        .header(HttpHeaders.ACCEPT, "text/xml;q=.5,text/html")
        .buildGet()
        .invoke()) {
        assertEquals(Response.Status.OK, response.getStatusInfo());
        assertEquals(message, response.readEntity(String.class));
      }
    }
  }

  @ClassRule
  public static final WireMockRule serverPut = new WireMockRule(Sockets.findRandomOpenPort());

  @Test
  public void testPut() {
    final String message = "<response>SUCCESS</response>";
    serverPut.stubFor(put("/put")
      .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.TEXT_XML))
      .willReturn(ok()
        .withBody(message)
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)));

    for (int i = 0; i < tests; ++i) {
      try (final Response response = client
        .target("http://localhost:" + serverPut.port() + "/put")
        .request("text/xml;q=.5,text/html", "text/x-dvi; q=0.8, text/x-c")
        .buildPut(Entity.entity(message, MediaType.TEXT_XML)).invoke()) {
          assertEquals(Response.Status.OK, response.getStatusInfo());
          assertEquals(message, response.readEntity(String.class));
      }
    }
  }

  @ClassRule
  public static final WireMockRule serverPostSmall = new WireMockRule(Sockets.findRandomOpenPort());

  @Test
  public void testPostSmall() {
    final String message = "<response>SUCCESS</response>";
    serverPostSmall.stubFor(post("/post")
      .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_OCTET_STREAM))
      .willReturn(ok()
        .withBody(message)
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)));

    for (int i = 0; i < tests; ++i) {
      try (final Response response = client
        .target("http://localhost:" + serverPostSmall.port() + "/post")
        .request()
        .buildPost(Entity.entity(message, MediaType.APPLICATION_OCTET_STREAM)).invoke()) {
          assertEquals(Response.Status.OK, response.getStatusInfo());
          assertEquals(message, response.readEntity(String.class));
      }
    }
  }

  @ClassRule
  public static final WireMockRule serverPostForm = new WireMockRule(Sockets.findRandomOpenPort());

  @Test
  public void testPostForm() throws IOException {
    final MultivaluedLinkedHashMap<String> form = new MultivaluedLinkedHashMap<>();
    form.add("foo", "bar");
    form.add("one", "two");
    form.add("emptyValue", "");
    form.add("", "emptyKey");
    form.add("nullValue", null);
    form.add(null, "nullKey");
    form.add(null, null);

    final ByteArrayOutputStream entity = new ByteArrayOutputStream();
    EntityUtil.writeFormParams(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE, entity);
    serverPostForm.stubFor(post("/post")
      .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_FORM_URLENCODED))
      .willReturn(ok()
        .withBody(entity.toByteArray())
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)));

    for (int i = 0; i < tests; ++i) {
      try (final Response response = client
        .target("http://localhost:" + serverPostForm.port() + "/post")
        .request()
        .buildPost(Entity.form(form)).invoke()) {
        assertEquals(Response.Status.OK, response.getStatusInfo());

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        EntityUtil.writeFormParams(form, null, out);
        final String message = new String(out.toByteArray());

        assertEquals(message, response.readEntity(String.class));
      }
    }
  }

  @ClassRule
  public static final WireMockRule serverPostRandom = new WireMockRule(Sockets.findRandomOpenPort());

  @Test
  public void testPostRandom() throws IOException {
    serverPostRandom.stubFor(post("/post")
      .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_OCTET_STREAM))
      .willReturn(ok()
        .withBody(testBytes)
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)));

    for (int i = 0; i < tests; ++i) {
      try (
        final InputStream in = new ByteArrayInputStream(testBytes);
        final Response response = client
          .target("http://localhost:" + serverPostRandom.port() + "/post")
          .request()
          .buildPost(Entity.entity(in, MediaType.APPLICATION_OCTET_STREAM)).invoke();
      ) {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
        final byte[] actual = Streams.readBytes((InputStream)response.getEntity());
        assertArrayEquals(testBytes, actual);
      }
    }
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