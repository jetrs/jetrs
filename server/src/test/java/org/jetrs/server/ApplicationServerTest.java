/* Copyright (c) 2021 JetRS
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

package org.jetrs.server;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetrs.provider.ext.BytesProvider;
import org.jetrs.provider.ext.NumberProvider;
import org.jetrs.provider.ext.StringProvider;
import org.jetrs.server.app.ApplicationServer;
import org.junit.AfterClass;
import org.junit.Test;
import org.libj.io.Streams;

public class ApplicationServerTest {
  private static final Random random = new Random();
  private static final ApplicationServer server = new ApplicationServer();
  private static final String serviceUrl = "http://localhost:" + server.getContainerPort() + ApplicationServer.applicationPath;
  private static final Client client;

  static {
    client = ClientBuilder.newClient();
    client.register(new NumberProvider());
    client.register(new BytesProvider());
    client.register(new StringProvider());
  }

  @Test
  public void testUpload() throws IOException {
    final int len = Math.abs(random.nextInt() / 100);
    final URL url = new URL(serviceUrl + "/upload?len=" + len);
    final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    connection.addRequestProperty("Content-Type", MediaType.MULTIPART_FORM_DATA);
    try (final OutputStream out = connection.getOutputStream()) {
      for (int i = 0; i < len; ++i)
        out.write((byte)random.nextInt());

      out.close();
      connection.getInputStream();
    }
  }

  @Test
  public void testMatchRoot404() throws Exception {
    final Response response = client.target(serviceUrl)
      .request()
      .get();

    assertResponse(404, response, String.class);
  }

  @Test
  public void testMatchGet1() throws Exception {
    final Response response = client.target(serviceUrl + "/root1/1")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    // FIXME: Is the client supposed to automatically return a String due to "text/plain" MediaType?!
    assertEquals("GET", data);
  }

  @Test
  public void testMatchGet2() throws Exception {
    final Response response = client.target(serviceUrl + "/root2/2")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    // FIXME: Is the client supposed to automatically return a String due to "text/plain" MediaType?!
    assertEquals("[" + ApplicationServer.applicationPath + "/root2/2]\norg.jetrs.server.app.service.RootService2\n{}", data);
  }

  @Test
  public void testMatchGet2Id1() throws Exception {
    final Response response = client.target(serviceUrl + "/root2/2/123")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    // FIXME: Is the client supposed to automatically return a String due to "text/plain" MediaType?!
    assertEquals("[" + ApplicationServer.applicationPath + "/root2/2/123, " + ApplicationServer.applicationPath + "/root2/2/123]\norg.jetrs.server.app.service.RootService2, org.jetrs.server.app.service.RootService2\n{id=[123]}", data);
  }

  @Test
  public void testMatchGet2Id2() throws Exception {
    final Response response = client.target(serviceUrl + "/root2/2/123/456")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    // FIXME: Is the client supposed to automatically return a String due to "text/plain" MediaType?!
    assertEquals("[" + ApplicationServer.applicationPath + "/root2/2/123/456, " + ApplicationServer.applicationPath + "/root2/2/123/456, " + ApplicationServer.applicationPath + "/root2/2/123/456]\norg.jetrs.server.app.service.RootService2, org.jetrs.server.app.service.RootService2, org.jetrs.server.app.service.RootService2\n{id2=[456], id1=[123]}", data);
  }

  @Test
  public void testMatchOptions1() throws Exception {
    final Response response = client.target(serviceUrl + "/root1/1")
      .request()
      .options();

    assertResponse(200, response, null);
    assertEquals("GET,POST", response.getHeaderString("Access-Control-Allow-Methods"));
  }

  @Test
  public void testMatchOptions2() throws Exception {
    final Response response = client.target(serviceUrl + "/root1/2")
      .request()
      .options();

    assertResponse(200, response, null);
    assertEquals(new HashSet<>(Arrays.asList("HEAD", "GET")), new HashSet<>(Arrays.asList(response.getHeaderString("Access-Control-Allow-Methods").split(","))));
  }

  @Test
  public void testMatchPost1String() throws Exception {
    Response response = client.target(serviceUrl + "/root1/1")
      .request()
      .post(Entity.text("data"));

    assertResponse(405, response, null);
    response = client.target(serviceUrl + "/root1/1/eyj1ijoicgfub2fpiiwiysi6imnrzmu5")
      .request()
      .post(Entity.text("data"));

    final String data = assertResponse(200, response, String.class);
    assertEquals("POST", data);
  }

  @Test
  public void testMatchPost1Int() throws Exception {
    Response response = client.target(serviceUrl + "/root1/1")
      .request()
      .post(Entity.text("data"));

    assertResponse(405, response, null);
    response = client.target(serviceUrl + "/root1/1/1")
      .request()
      .post(Entity.text("data"));

    final String data = assertResponse(200, response, String.class);
    assertEquals("POST", data);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }

  private static <T>T assertResponse(final int status, final Response response, final Class<T> entityClass) throws Exception {
    final T data;
    if (status != response.getStatus() || entityClass == null) {
      data = null;
      if (response.hasEntity()) {
        try (final InputStream in = (InputStream)response.getEntity()) {
          System.err.println(new String(Streams.readBytes(in)));
        }
      }
    }
    else {
      data = response.readEntity(entityClass);
    }

    assertEquals(status, response.getStatus());
    return data;
  }
}