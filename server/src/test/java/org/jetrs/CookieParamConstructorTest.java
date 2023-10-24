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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import org.junit.Test;

public class CookieParamConstructorTest extends SingleServiceTest {
  private static final String content = "content";

  @Path("/")
  public static class ResourceString {
    @GET
    public String doGet(@CookieParam("arg1") final BigDecimal arg1, @CookieParam("arg2") final BigInteger arg2, @CookieParam("arg3") final URI arg3) {
      assertEquals("3.145", arg1.toString());
      assertEquals("3145", arg2.toString());
      assertEquals("http://test", arg3.toString());
      return content;
    }
  }

  @Path("/")
  public static class ResourceStringList {
    @GET
    public String doGetString(@CookieParam("args") final List<BigDecimal> args) {
      assertEquals("3.145", args.get(0).toString());
      return content;
    }
  }

  @Path("/")
  public static class ResourceStringListEmpty {
    @GET
    public String doGetString(@CookieParam("args") final List<BigDecimal> args) {
      assertEquals(1, args.size());
      assertEquals(null, args.get(0));
      return content;
    }
  }

  @Path("/")
  public static class ResourceStringNullDefault {
    @GET
    public String doGet(@CookieParam("arg1") final BigDecimal arg1) {
      assertEquals(null, arg1);
      return content;
    }
  }

  @Path("/")
  public static class ResourceStringDefault {
    @GET
    public String doGet(@CookieParam("arg1") @DefaultValue("3.145") final BigDecimal arg1) {
      assertEquals("3.145", arg1.toString());
      return content;
    }
  }

  @Path("/")
  public static class ResourceStringDefaultOverride {
    @GET
    public String doGet(@CookieParam("arg1") @DefaultValue("3.145") final BigDecimal arg1) {
      assertEquals("2.718", arg1.toString());
      return content;
    }
  }

  @Path("/")
  public static class ResourceStringListEmptyDefault {
    @GET
    public String doGetString(@CookieParam("args") final List<BigDecimal> args) {
      assertEquals(0, args.size());
      return content;
    }
  }

  @Path("/")
  public static class ResourceStringListDefault {
    @GET
    public String doGetString(@CookieParam("args") @DefaultValue("3.145") final List<BigDecimal> args) {
      assertEquals("3.145", args.get(0).toString());
      return content;
    }
  }

  @Path("/")
  public static class ResourceStringListDefaultOverride {
    @GET
    public String doGetString(@CookieParam("args") @DefaultValue("3.145") final List<BigDecimal> args) {
      assertEquals("2.718", args.get(0).toString());
      return content;
    }
  }

  @Test
  public void testStringConstructorGet() {
    startServer(ResourceString.class);
    assertCookies("/", HttpMethod.GET, content, new Cookie("arg1", "3.145"), new Cookie("arg2", "3145"), new Cookie("arg3", "http://test"));
  }

  @Test
  public void testStringConstructorListGet() {
    startServer(ResourceStringList.class);
    assertCookies("/", HttpMethod.GET, content, "application/stringlist", new Cookie("args", "3.145"));
  }

  @Test
  public void testStringConstructorListEmptyGet() {
    startServer(ResourceStringListEmpty.class);

    final Response responseContext = target("/")
      .accept("application/stringlist")
      .cookie(new Cookie("args", ""))
      .get();

    assertEquals(400, responseContext.getStatus());
  }

  @Test
  public void testStringConstructorNullDefault() {
    startServer(ResourceStringNullDefault.class);
    assertCookies("/", HttpMethod.GET, content, HttpMethod.GET);
  }

  @Test
  public void testStringConstructorDefault() {
    startServer(ResourceStringDefault.class);
    assertCookies("/", HttpMethod.GET, content);
  }

  @Test
  public void testStringConstructorDefaultOverride() {
    startServer(ResourceStringDefault.class);
    assertCookies("/", HttpMethod.GET, content, new Cookie("args", "2.718"));
  }

  @Test
  public void testStringConstructorListEmptyDefault() {
    startServer(ResourceStringListEmptyDefault.class);
    assertCookies("/", HttpMethod.GET, content);
  }

  @Test
  public void testStringConstructorListDefault() {
    startServer(ResourceStringListDefault.class);
    assertCookies("/", HttpMethod.GET, content);
  }

  @Test
  public void testStringConstructorListDefaultOverride() {
    startServer(ResourceStringListDefaultOverride.class);
    assertCookies("/", HttpMethod.GET, content, new Cookie("args", "2.718"));
  }

  @Test
  public void testBadStringConstructorValue() {
    startServer(ResourceString.class);

    final Response responseContext = target("/")
      .cookie(new Cookie("arg1", "ABCDEF"))
      .cookie(new Cookie("arg2", "3145"))
      .cookie(new Cookie("arg3", "http://test"))
      .get();

    assertEquals(400, responseContext.getStatus());
  }
}