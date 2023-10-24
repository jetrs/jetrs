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

package org.jetrs;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Jdk8ClientCookieTest {
  private static final String path;
  private static final String cookieName = "mycookie";

  static {
    final WireMockRule server = new WireMockRule(WireMockConfiguration.options().extensions(new ResponseTemplateTransformer(true)));
    server.start();
    server.stubFor(get("/")
      .willReturn(ok()
        .withHeader(HttpHeaders.SET_COOKIE, cookieName + "=x{{request.cookies." + cookieName + "}}x")
        .withBody("x{{request.cookies." + cookieName + "}}x")));

    path = "http://localhost:" + server.port() + "/";
  }

  private static void assertResponse(final boolean expected, final boolean cookiesDisabled, final Response response) {
    final NewCookie cookie = response.getCookies().get(cookieName);
    if (cookiesDisabled) {
      assertNull(cookie);
    }
    else {
      final String expectedValue = expected ? "xxxx" : "xx";
      assertEquals(expectedValue, cookie.getValue());
      assertEquals(expectedValue, response.readEntity(String.class));
    }
  }

  @Test
  public void testCookies() {
    final Client client = ClientBuilder
      .newBuilder()
      .build();

    final Invocation.Builder request = client.target(path).request();
    assertResponse(false, false, request.get());
    assertResponse(true, false, request.get());

    client.close();
  }

  @Test
  public void testDisabledCookies() {
    System.setProperty(ClientProperties.DISABLE_COOKIES, "true");
    final Client client = ClientBuilder
      .newBuilder()
      .build();

    final Invocation.Builder request = client.target(path).request();
    assertResponse(false, true, request.get());
    assertResponse(false, true, request.get());

    client.close();
  }
}