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

public class ApplicationServerTest {
  private static final Random random = new Random();
  private static final ApplicationServer server = new ApplicationServer();
  private static final String serviceUrl = "http://localhost:" + server.getContainerPort();
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
    final URL url = new URL("http://localhost:" + server.getContainerPort() + "/upload?len=" + len);
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
  public void testMatchGet1() {
    final Response response = client.target(serviceUrl + "/root1/1")
      .request()
      .get();

    assertEquals(200, response.getStatus());
    // FIXME: Is the client supposed to automatically return a String due to "text/plain" MediaType?!
    assertEquals("GET", response.readEntity(String.class));
  }

  @Test
  public void testMatchOptions1() {
    final Response response = client.target(serviceUrl + "/root1/1")
      .request()
      .options();

    assertEquals(200, response.getStatus());
    assertEquals("GET,POST", response.getHeaderString("Access-Control-Allow-Methods"));
  }

  @Test
  public void testMatchOptions2() {
    final Response response = client.target(serviceUrl + "/root1/2")
      .request()
      .options();

    assertEquals(200, response.getStatus());
    assertEquals(new HashSet<>(Arrays.asList("HEAD", "GET")), new HashSet<>(Arrays.asList(response.getHeaderString("Access-Control-Allow-Methods").split(","))));
  }

  @Test
  public void testMatchPost1String() {
    Response response = client.target(serviceUrl + "/root1/1")
      .request()
      .post(Entity.text("data"));

    assertEquals(405, response.getStatus());
    response = client.target(serviceUrl + "/root1/1/eyj1ijoicgfub2fpiiwiysi6imnrzmu5")
      .request()
      .post(Entity.text("data"));

    assertEquals(200, response.getStatus());
    assertEquals("POST", response.readEntity(String.class));
  }

  @Test
  public void testMatchPost1Int() {
    Response response = client.target(serviceUrl + "/root1/1")
      .request()
      .post(Entity.text("data"));

    assertEquals(405, response.getStatus());
    response = client.target(serviceUrl + "/root1/1/1")
      .request()
      .post(Entity.text("data"));

    assertEquals(200, response.getStatus());
    assertEquals("POST", response.readEntity(String.class));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }
}