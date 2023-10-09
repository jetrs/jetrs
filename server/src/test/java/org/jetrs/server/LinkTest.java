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

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;

import org.jetrs.provider.ext.interceptor.GZipCodecInterceptor;
import org.jetrs.server.app.TestAppServer;
import org.jetrs.server.app.service.LinkHeaderService;
import org.junit.AfterClass;
import org.junit.Test;

public class LinkTest {
  private static final TestAppServer server = new TestAppServer(new Object[] {new LinkHeaderService()}, null);
  private static final String serviceUrl = server.getServiceUrl();
  private static final Client client = ClientBuilder.newClient().register(GZipCodecInterceptor.class);

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }

  private static Invocation.Builder request(final String path) {
    return request(path, (String[])null);
  }

  private static Invocation.Builder request(final String path, final String ... accepts) {
    final Invocation.Builder builder = client.target(serviceUrl + "/link/" + path).request();
    if (accepts != null && accepts.length > 0)
      builder.accept(accepts);

    return builder;
  }

  @Test
  public void test() {
    final Response response = request("links")
      .header(HttpHeaders.LINK, "<http://example.com/TheBook/chapter2>; rel=\"previous\"; title=\"previous chapter\"; rel=copyright, </TheBook/chapter3>; rel=\"next\"; title=\"next chapter\"; rel=copyright")
      .post(Entity.text(""));

    Link link = response.getLink("previous");
    assertNotNull(link);
    assertEquals("http://example.com/TheBook/chapter2", link.getUri().toString());
    assertEquals("previous chapter", link.getTitle());
    assertTrue(link.getRel().contains("\n"));
    List<String> rels = link.getRels();
    assertEquals(2, rels.size());
    assertTrue(rels.contains("previous"));
    assertTrue(rels.contains("copyright"));

    link = response.getLink("next");
    assertNotNull(link);
    assertEquals("/TheBook/chapter3", link.getUri().toString());
    assertEquals("next chapter", link.getTitle());
    assertTrue(link.getRel().contains("\n"));
    rels = link.getRels();
    assertEquals(2, rels.size());
    assertTrue(rels.contains("next"));
    assertTrue(rels.contains("copyright"));
  }
}