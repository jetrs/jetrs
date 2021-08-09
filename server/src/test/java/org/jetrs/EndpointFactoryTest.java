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

import static org.junit.Assert.*;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EndpointFactoryTest {
  private static final String packageName = EndpointFactoryTest.class.getPackage().getName();
  private static final EndpointFactory endpointFactory = new EndpointFactory();

  @ApplicationPath("/")
  public static class App extends Application {
  }

  @Test
  public void testAnnotation() {
    final RestApplicationServlet servlet = endpointFactory.apply(new App(), HttpServlet.class);
    final Class<?> cls = servlet.getClass();
    final WebServlet webServlet = cls.getAnnotation(WebServlet.class);
    assertEquals(1, webServlet.urlPatterns().length);
    assertEquals("/*", webServlet.urlPatterns()[0]);
    assertTrue(webServlet.name(), webServlet.name().matches(packageName.replace('.', '/') + "/Endpoint\\dServlet"));
    assertTrue(webServlet.displayName(), webServlet.displayName().matches("JetRS Endpoint \\d: " + App.class.getName().replace("$", "\\$")));
  }

  @Test
  public void testClassLoader() {
    final RestApplicationServlet servlet = endpointFactory.apply(new App(), HttpServlet.class);
    final Class<?> cls = servlet.getClass();
    assertSame(getClass().getClassLoader(), cls.getClassLoader());
  }

  @Test
  public void testMultiple() {
    final RestApplicationServlet servlet1 = endpointFactory.apply(new App(), HttpServlet.class);
    final RestApplicationServlet servlet2 = endpointFactory.apply(new App(), HttpServlet.class);
    assertNotEquals(servlet1.getClass(), servlet2.getClass());
  }
}