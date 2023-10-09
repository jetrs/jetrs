/* Copyright (c) 2023 JetRS
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.jetrs.CommonProperties;
import org.jetrs.provider.ext.StringProvider;
import org.jetrs.provider.ext.interceptor.GZipCodecInterceptor;
import org.jetrs.server.app.TestAppServer;
import org.jetrs.server.app.provider.MyJsonProvider;
import org.jetrs.server.app.service.MediaTypeService;
import org.junit.AfterClass;
import org.junit.Test;

public class MediaTypeServerITest {
  static {
    System.setProperty(CommonProperties.DISABLE_STANDARD_PROVIDER, "*");
  }

  private static final TestAppServer server = new TestAppServer(new Object[] {
    new MediaTypeService(),
    new MyJsonProvider()
  }, null);
  private static final String serviceUrl = server.getServiceUrl();
  private static final Client client = ClientBuilder.newClient().register(GZipCodecInterceptor.class).register(new StringProvider());

  private static void assertResponse(final int statusCode, final String expectedContentType, final Response response) {
    assertEquals(statusCode, response.getStatus());
    final String actualContentType = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
    assertEquals(expectedContentType, actualContentType);
    if (actualContentType == null)
      assertNull(response.getEntity());
  }

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }

  private static Invocation.Builder request(final String path, final String contentType) {
    return request(path, contentType, (String[])null);
  }

  private static Invocation.Builder request(final String path, final String contentType, final String ... accepts) {
    final Invocation.Builder builder = client.target(serviceUrl + "/mime/" + path).request();
    if (contentType != null)
      builder.header(HttpHeaders.CONTENT_TYPE, contentType);

    if (accepts != null && accepts.length > 0)
      builder.accept(accepts);

    return builder;
  }

  @Test
  public void testCA() {
    assertEquals(204, request("ca", "text/plain").post(Entity.entity("[\"application/json\"]", "application/json")).getStatus());
    assertEquals(204, request("ca", "text/plain").post(Entity.entity("[\"application/json;charset=utf-8\"]", "application/json;charset=utf-8;x=3;q=.8")).getStatus());
    assertEquals(204, request("ca", "text/plain").post(Entity.entity("[\"text/json\"]", "text/json")).getStatus());
    assertEquals(204, request("ca", "text/plain").post(Entity.entity("[\"text/plain\"]", new Variant((MediaType)null, (String)null, "utf-8"))).getStatus());
    assertEquals(500, request("ca", "text/html").post(Entity.entity("[\"text/html\"]", "text/html")).getStatus());
  }

  @Test
  public void testCB() {
    assertEquals(204, request("cb", "text/plain").post(Entity.entity("[\"application/json\"]", "application/json")).getStatus());
    assertEquals(415, request("cb", "text/plain").post(Entity.entity("[\"text/json\"]", "text/json")).getStatus());
    assertEquals(415, request("cb", "text/plain").post(Entity.entity("[\"text/plain\"]", new Variant((MediaType)null, (String)null, "utf-8"))).getStatus());
    assertEquals(500, request("cb", "text/html").post(Entity.entity("[\"text/html\"]", "text/html")).getStatus());
  }

  @Test
  public void testCC() {
    assertEquals(204, request("cc", "text/plain").post(Entity.entity("[\"application/json;charset=utf-8\"]", "application/json;charset=utf-8;x=3;q=.8")).getStatus());
    assertEquals(204, request("cc", "text/plain").post(Entity.entity("[\"application/vnd.jetrs.v1+json;charset=utf-8\"]", "application/vnd.jetrs.v1+json;charset=utf-8;x=3;q=.8")).getStatus());
    assertEquals(204, request("cc", "text/plain").post(Entity.entity("[\"application/json;charset=ascii\"]", "application/json;charset=ascii;x=3;q=.8")).getStatus());
    assertEquals(204, request("cc", "text/plain").post(Entity.entity("[\"application/json\"]", "application/json;x=3;q=.8")).getStatus());
    assertEquals(415, request("cc", "text/plain").post(Entity.entity("[\"application/json;charset=iso-8859-1\"]", "application/json;charset=iso-8859-1;x=3;q=.8")).getStatus());
    assertEquals(204, request("cc", "text/plain").post(Entity.entity("[\"text/json\"]", "text/json")).getStatus());
    assertEquals(204, request("cc", "text/plain").post(Entity.entity("[\"text/json;charset=ascii\"]", "text/json;charset=ascii")).getStatus());
    assertEquals(415, request("cc", "text/plain").post(Entity.entity("[\"text/plain\"]", new Variant((MediaType)null, (String)null, "utf-8"))).getStatus());
    assertEquals(500, request("cc", "text/html").post(Entity.entity("[\"text/html\"]", "text/html")).getStatus());
  }

  @Test
  public void testPA() {
    assertResponse(200, "application/json", request("pa", null, "application/json").post(Entity.entity("[\"application/json\"]", "application/json")));
    assertResponse(200, "application/json;charset=utf-8", request("pa", null, "application/xml", "application/json;charset=utf-8", "text/xml").post(Entity.entity("[\"application/json;charset=utf-8\"]", "application/json;charset=utf-8;x=3;q=.8")));
    assertResponse(200, "text/json", request("pa", null, "text/json", "text/xml", "application/xml").post(Entity.entity("[\"text/json\"]", "text/json")));
    assertResponse(200, "text/plain", request("pa", "text/plain", "application/xml", "text/plain").post(Entity.entity("[\"text/plain\"]", new Variant((MediaType)null, (String)null, "utf-8"))));
    assertResponse(500, null, request("pa", null, "text/html", "application/xml").post(Entity.entity("[\"text/html\"]", "text/html")));
  }

  @Test
  public void testPB() {
    assertResponse(200, "application/json", request("pb", null, "text/xml", "application/json", "application/xml").post(Entity.entity("[\"application/json\"]", "application/json")));
    assertResponse(200, "application/json;charset=utf-8", request("pb", null, "application/json;charset=utf-8", "text/xml", "application/xml").post(Entity.entity("[\"application/json;charset=utf-8\"]", "application/json;charset=utf-8;x=3;q=.8")));
    assertResponse(406, null, request("pb", null, "text/xml", "text/json", "text/xml").post(Entity.entity("[\"text/json\"]", "text/json")));
    assertResponse(406, null, request("pb", null, "text/xml", "text/plain").post(Entity.entity("[\"text/plain\"]", new Variant((MediaType)null, (String)null, "utf-8"))));
    assertResponse(500, null, request("pb", null, "text/html", "text/plain").post(Entity.entity("[\"text/html\"]", "text/html")));
  }

  @Test
  public void testPC() {
    assertResponse(200, "application/json;charset=utf-8", request("pc", null, "application/xml", "application/json;charset=utf-8").post(Entity.entity("[\"application/json;charset=utf-8\"]", "application/json;charset=utf-8;x=3;q=.8")));
    assertResponse(200, "application/vnd.jetrs.v1+json;charset=utf-8", request("pc", null, "application/vnd.jetrs.v1+json;charset=utf-8", "application/vnd.jetrs.v1+xml").post(Entity.entity("[\"application/vnd.jetrs.v1+json;charset=utf-8\"]", "application/vnd.jetrs.v1+json;charset=utf-8;x=3;q=.8")));
    assertResponse(200, "application/json;charset=ascii", request("pc", null, "application/json;charset=ascii;q=.5", "application/json;charset=ascii;q=.3").post(Entity.entity("[\"application/json;charset=ascii;q=.5\"]", "application/json;charset=ascii;x=3;q=.8")));
    assertResponse(200, "application/json;charset=utf-8", request("pc", null, "application/json;charset=iso-8859-1", "application/json;charset=utf-8").post(Entity.entity("[\"application/json;charset=utf-8\"]", "application/json;charset=utf-8;x=3;q=.8")));
    assertResponse(406, null, request("pc", null, "text/plain", "application/json;charset=iso-8859-1").post(Entity.entity("[\"application/json;charset=iso-8859-1\"]", "application/json;charset=iso-8859-1;x=3;q=.8")));
    assertResponse(200, "application/json;charset=ascii", request("pc", null, "application/json").post(Entity.entity("[\"application/json\"]", "application/json;x=3;q=.8")));
    assertResponse(200, "text/json", request("pc", null, "text/json").post(Entity.entity("[\"text/json\"]", "text/json")));
    assertResponse(200, "text/json;charset=ascii", request("pc", null, "text/json;charset=ascii").post(Entity.entity("[\"text/json;charset=ascii\"]", "text/json;charset=ascii")));
    assertResponse(406, null, request("pc", null, "text/plain").post(Entity.entity("[\"text/plain\"]", new Variant((MediaType)null, (String)null, "utf-8"))));
    assertResponse(500, null, request("pc", null, "text/html").post(Entity.entity("[\"text/html\"]", "text/html")));
  }
}