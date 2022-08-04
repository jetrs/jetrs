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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.*;
import static org.junit.Assert.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jetrs.provider.ext.StringProvider;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

public class ClientTest {
  private static final Client client = ClientBuilder
    .newClient()
    .register(new StringProvider());

  @Rule
  public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

  @Test
  public void testPut() {
    stubFor(put("/put")
      .withHeader("Content-Type", containing(MediaType.TEXT_PLAIN))
      .willReturn(ok()
        .withHeader("Content-Type", "text/xml")
        .withBody("<response>SUCCESS</response>")));

    try (final Response response = client
      .target("http://localhost:" + wireMockRule.port() + "/put")
      .request()
      .header(HttpHeaders.ACCEPT, "text/xml;q=.5,text/html")
      .header(HttpHeaders.ACCEPT, "text/x-dvi; q=0.8, text/x-c")
      .buildPut(Entity.text("hello world")).invoke()) {
        assertEquals(Response.Status.OK, response.getStatusInfo());
        assertEquals("<response>SUCCESS</response>", response.readEntity(String.class));
    }

    WireMock.findAll(RequestPatternBuilder.allRequests()).forEach(request -> System.err.println(request.getHeaders()));
  }

  @Test
  public void testGet() {
    stubFor(get("/get")
      .willReturn(ok()
        .withHeader("Content-Type", "text/xml")
        .withBody("<response>SUCCESS</response>")));

    try (final Response response = client.target("http://localhost:" + wireMockRule.port() + "/get")
      .request()
      .buildGet()
      .invoke()) {
      assertEquals(Response.Status.OK, response.getStatusInfo());
      assertEquals("<response>SUCCESS</response>", response.readEntity(String.class));
    }
  }

  @Test
  public void testPost() {
    stubFor(post("/post")
      .withHeader("Content-Type", containing(MediaType.TEXT_PLAIN))
      .willReturn(ok()
        .withHeader("Content-Type", "text/xml")
        .withBody("<response>SUCCESS</response>")));

    try (final Response response = client
      .target("http://localhost:" + wireMockRule.port() + "/post")
      .request()
      .buildPost(Entity.text("hello world")).invoke()) {
        assertEquals(Response.Status.OK, response.getStatusInfo());
        assertEquals("<response>SUCCESS</response>", response.readEntity(String.class));
    }
  }
}