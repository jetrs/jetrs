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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetrs.provider.ext.BytesProvider;
import org.jetrs.provider.ext.CharacterProvider;
import org.jetrs.provider.ext.NumberProvider;
import org.jetrs.provider.ext.StringProvider;
import org.jetrs.server.app.ApplicationServer;
import org.jetrs.server.app.provider.MyCharacterProvider;
import org.junit.AfterClass;
import org.junit.Test;
import org.libj.io.Streams;
import org.libj.lang.Strings;

public class ApplicationServerTest {
  private static final Random random = new Random();
  private static final ApplicationServer server = new ApplicationServer();
  private static final String serviceUrl = "http://localhost:" + server.getContainerPort() + ApplicationServer.applicationPath;
  private static final Client client;

  static {
    client = ClientBuilder.newClient();
    client.register(new CharacterProvider());
    client.register(new NumberProvider());
    client.register(new BytesProvider());
    client.register(new StringProvider());
  }

  public static String encodeLexicographically(final Map<String,?> map) {
    final ArrayList<String> list = new ArrayList<>(map.size());
    for (final Map.Entry<String,?> entry : map.entrySet()) // [S]
      list.add(entry.getKey() + ":" + String.valueOf(entry.getValue()));

    list.sort(null);
    return list.toString();
  }

  private static final int numTests = 100;

  @Test
  public void testTypeBoolean() throws Exception {
    for (int i = 0; i < numTests; ++i) { // [N]
      final boolean a = random.nextBoolean();
      final boolean b = random.nextBoolean();
      final boolean c = random.nextBoolean();
      final Response response = client.target(serviceUrl + "/type/boolean/a" + a + "/" + b + "b/c" + c + "d")
        .request()
        .get();

      final String data = assertResponse(200, response, String.class);
      assertEquals("" + a + b + c, data);
    }
  }

  @Test
  public void testTypeChar() throws Exception {
    for (int i = 0; i < numTests; ++i) { // [N]
      final char a = Strings.getRandomAlpha(1).charAt(0);
      final char b = Strings.getRandomAlpha(1).charAt(0);
      final char c = Strings.getRandomAlpha(1).charAt(0);
      final Response response = client.target(serviceUrl + "/type/char/a" + a + "/" + b + "b/c" + c + "d")
        .request()
        .get();

      final String data = assertResponse(200, response, String.class);
      assertEquals("" + a + b + c, data);
    }
  }

  @Test
  public void testTypeByte() throws Exception {
    for (int i = 0; i < numTests; ++i) { // [N]
      final byte a = (byte)random.nextInt();
      final byte b = (byte)random.nextInt();
      final byte c = (byte)random.nextInt();
      final Response response = client.target(serviceUrl + "/type/byte/a" + a + "/" + b + "b/c" + c + "d")
        .request()
        .get();

      final String data = assertResponse(200, response, String.class);
      assertEquals(a + b + c, Integer.parseInt(data));
    }
  }

  @Test
  public void testTypeShort() throws Exception {
    for (int i = 0; i < numTests; ++i) { // [N]
      final short a = (short)random.nextInt();
      final short b = (short)random.nextInt();
      final short c = (short)random.nextInt();
      final Response response = client.target(serviceUrl + "/type/short/a" + a + "/" + b + "b/c" + c + "d")
        .request()
        .get();

      final String data = assertResponse(200, response, String.class);
      assertEquals(a + b + c, Integer.parseInt(data));
    }
  }

  @Test
  public void testTypeInt() throws Exception {
    for (int i = 0; i < numTests; ++i) { // [N]
      final int a = random.nextInt();
      final int b = random.nextInt();
      final int c = random.nextInt();
      final Response response = client.target(serviceUrl + "/type/int/a" + a + "/" + b + "b/c" + c + "d")
        .request()
        .get();

      final String data = assertResponse(200, response, String.class);
      assertEquals(a + b + c, Integer.parseInt(data));
    }
  }

  @Test
  public void testTypeLong() throws Exception {
    for (int i = 0; i < numTests; ++i) { // [N]
      final long a = random.nextLong();
      final long b = random.nextLong();
      final long c = random.nextLong();
      final Response response = client.target(serviceUrl + "/type/long/a" + a + "/" + b + "b/c" + c + "d")
        .request()
        .get();

      final String data = assertResponse(200, response, String.class);
      assertEquals(a + b + c, Long.parseLong(data));
    }
  }

  @Test
  public void testTypeFloat() throws Exception {
    for (int i = 0; i < numTests; ++i) { // [N]
      final float a = random.nextFloat();
      final float b = random.nextFloat();
      final float c = random.nextFloat();
      final Response response = client.target(serviceUrl + "/type/float/a" + a + "/" + b + "b/c" + c + "d")
        .request()
        .get();

      final String data = assertResponse(200, response, String.class);
      assertEquals(a + b + c, Float.parseFloat(data), 0);
    }
  }

  @Test
  public void testTypeDouble() throws Exception {
    for (int i = 0; i < numTests; ++i) { // [N]
      final double a = random.nextDouble();
      final double b = random.nextDouble();
      final double c = random.nextDouble();
      final Response response = client.target(serviceUrl + "/type/double/a" + a + "/" + b + "b/c" + c + "d")
        .request()
        .get();

      final String data = assertResponse(200, response, String.class);
      assertEquals(a + b + c, Double.parseDouble(data), 0);
    }
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
      for (int i = 0; i < len; ++i) // [N]
        out.write((byte)random.nextInt());

      out.close();
      connection.getInputStream();
    }
  }

  @Test
  public void testBookService0() throws Exception {
    final Response response = client.target(serviceUrl + "/books/aaabb;a=b/xxx;x=x/some;c=d/amazingly/cool/stuff")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    assertEquals("aaabb:bb:xxx:xxx|some|amazingly|cool", data);
  }

  @Test
  public void testBookService01() throws Exception {
    final Response response = client.target(serviceUrl + "/books/aaab/xxx;x=x/a/lot;r=r/of/stuff")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    assertEquals("aaab:b:xxx:xxx|a|lot|of", data);
  }

  @Test
  public void testBookService1() throws Exception {
    final Response response = client.target(serviceUrl + "/books/2011;author=jim/bla;country=th")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    assertEquals("getBooks is called, years: [2011], segs: [2011;author=jim], author: jim, country: th", data);
  }

  @Test
  public void testBookService12() throws Exception {
    final Response response = client.target(serviceUrl + "/books/2011;author=bob/2012;author=joe;country=fr/2013")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    assertEquals("getBooks is called, years: [2011/2012/2013], segs: [2011;author=bob, 2012;country=fr;author=joe, 2013], author: bob, country: fr", data);
  }

  @Test
  public void testBookService2() throws Exception {
    final Response response = client.target(serviceUrl + "/books/2011;country=usa/2012/2013;author=mkyong")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    assertEquals("getBooks is called, years: [2011/2012/2013], segs: [2011;country=usa, 2012, 2013;author=mkyong], author: mkyong, country: usa", data);
  }

  @Test
  public void testBookService3() throws Exception {
    final Response response = client.target(serviceUrl + "/books/2011;author=mkyong;country=malaysia")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    assertEquals("getBooks is called, years: [2011], segs: [2011;country=malaysia;author=mkyong], author: mkyong, country: malaysia", data);
  }

  @Test
  public void testBookService4() throws Exception {
    final Response response = client.target(serviceUrl + "/books/query/aA/BaCb/cDc/ba///////foo/bar/hi/hello/good/bye")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    assertEquals("List of Books order by: [aA, BaCb] :: [BaCb, ba, , , , , , , foo, bar, hi, hello, good, bye] :: null", data);
  }

  @Test
  public void testBookService6() throws Exception {
    final Response response = client.target(serviceUrl + "/books/categories1;name=cat;name=bla;cat=hemi/static;stat=foo/objects1;name=green;value=ok")
      .request()
      .get();

    final String data = assertResponse(200, response, String.class);
    assertEquals("{books}: [] | {categories1}: [cat:[hemi], name:[cat, bla]] | {static}: [stat:[foo]] | {objects1}: [name:[green], value:[ok]] | {categories1}: [cat:[hemi], name:[cat, bla]] | {objects1}: [name:[green], value:[ok]] | cat | cat | bla | green", data);
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
//    Response response = client.target(serviceUrl + "/root1/1")
//      .request()
//      .post(Entity.text("data"));

//    assertResponse(405, response, null);
    Response response = client.target(serviceUrl + "/root1/1/eyj1ijoicgfub2fpiiwiysi6imnrzmu5")
      .request()
      .post(Entity.text("data"));

    final String data = assertResponse(200, response, String.class);
    assertEquals("POST", data);
  }

  @Test
  public void testMatchPost0() throws Exception {
    final Response response = client.target(serviceUrl + "/root1/0/Y")
      .request()
      .post(Entity.text('Z'));

    final Character data = assertResponse(200, response, Character.class);
    assertEquals('Z', (char)data);
    assertEquals(1, MyCharacterProvider.instanceCount);
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