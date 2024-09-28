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

import static org.junit.Assert.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import org.jetrs.server.app.TestAppServer;
import org.junit.After;

abstract class SingleServiceTest {
  private static final Client client = ClientBuilder.newClient();

  private TestAppServer server;

  Invocation.Builder target(final String path) {
    return client.target(server.getServiceUrl() + path).request();
  }

  void startServer(final Class<?> ... classes) {
    server = new TestAppServer(new Object[] {}, classes);
    System.err.println(Thread.currentThread().getStackTrace()[2].toString());
  }

  @After
  public void after() throws Exception {
    if (server != null)
      server.close();
  }

  void assertCookies(final String path, final String method, final String entity, final String acceptMediaType, final Cookie ... cookies) {
    final Invocation.Builder builder = target(path);
    if (acceptMediaType != null)
      builder.accept(acceptMediaType);

    for (final Cookie cookie : cookies) // [A]
      builder.cookie(cookie);

    System.err.println(builder);
    Response method2 = builder.method(method, Entity.text(entity));
    final String content = method2.readEntity(String.class);
    if (!entity.equals(content))
      System.err.println(content);

    assertEquals(entity, content);
  }

  void assertCookies(final String path, final String method, final String entity, final Cookie ... cookies) {
    assertCookies(path, method, entity, null, cookies);
  }
}