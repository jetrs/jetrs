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

import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bbottema.javasocksproxyserver.junit.SockServerRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.libj.test.TestExecutorService;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import socks.ProxyServer;
import socks.server.UserPasswordAuthenticator;
import socks.server.UserValidation;

public class Jdk8ClientProxyTest {
  private static final int noTests = 10;
  private static final String username = "username";
  private static final String password = "password";
  private static final int proxyPortNoAuth = 1080;
  private static final int proxyPortAuth = 1081;
  private static final String success = "success";
  private static final Client client = ClientBuilder.newBuilder().build();

//  @ClassRule
//  public static final SockServerRule sockServerRule = new SockServerRule(proxyPortNoAuth);

  @ClassRule
  public static final WireMockRule server = new WireMockRule(0);

  private static boolean authenticated;

  static {
    server.stubFor(get("/")
      .willReturn(ok()
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
        .withBody(success)));

    new Thread() {
      @Override
      public void run() {
        final ProxyServer proxyServer = new ProxyServer(new UserPasswordAuthenticator(new UserValidation() {
          @Override
          public boolean isUserValid(final String username, final String password, final Socket connection) {
            return authenticated = Jdk8ClientProxyTest.username.equals(username) && Jdk8ClientProxyTest.password.equals(password);
          }
        }));

        proxyServer.start(proxyPortAuth);
      }
    }.start();
  }

  private static void test() {
    final Response response = client.target("http://localhost:" + server.port()).request().get();
    assertEquals(success, response.readEntity(String.class));
  }

  @Test
  public void testConnectionRefused() {
    System.setProperty(ClientProperties.PROXY_URI, "socks://localhost:" + (proxyPortNoAuth - 1));
    try {
      test();
      fail("Expected ProcessingException");
    }
    catch (final ProcessingException e) {
      final Throwable cause = e.getCause();
      if (!(cause instanceof SocketException) || !"Connection refused (Connection refused)".equals(cause.getMessage()))
        throw e;
    }
  }

  @Test
  public void testSuccessNoAuth() throws InterruptedException {
    System.setProperty(ClientProperties.PROXY_URI, "socks://localhost:" + proxyPortNoAuth);
    final TestExecutorService executor = new TestExecutorService(Executors.newFixedThreadPool(noTests));
    for (int i = 0; i < noTests; ++i) // [N]
      executor.submit(Jdk8ClientProxyTest::test);

    executor.shutdown();
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testSuccessAuth() {
    System.setProperty(ClientProperties.FOLLOW_REDIRECTS, "false");
    System.setProperty(ClientProperties.PROXY_URI, "socks://" + username + ":" + password + "@localhost:" + proxyPortAuth);
    test();
    assertTrue(authenticated);
  }
}