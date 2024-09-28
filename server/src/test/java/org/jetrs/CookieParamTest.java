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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CookieParamTest extends SingleServiceTest {
  private static final String entity = "content";

  @Parameterized.Parameters
  public static Object[][] data() {
    return new Object[10][0];
  }

  @Path("/")
  public static class ResourceString {
    @GET
    public String get(@CookieParam("arg1") final String arg1, @CookieParam("arg2") final String arg2, @CookieParam("arg3") final String arg3) {
      assertEquals("a", arg1);
      assertEquals("b", arg2);
      assertEquals("c", arg3);
      return entity;
    }

    @POST
    public String post(@CookieParam("arg1") final String arg1, @CookieParam("arg2") final String arg2, @CookieParam("arg3") final String arg3, final String entity) {
      assertEquals("a", arg1);
      assertEquals("b", arg2);
      assertEquals("c", arg3);
      assertEquals(entity, entity);
      return entity;
    }
  }

  @Test
  public void testStringGet() {
    startServer(ResourceString.class);
    assertCookies("/", HttpMethod.GET, entity, new Cookie("arg1", "a"), new Cookie("arg2", "b"), new Cookie("arg3", "c"));
  }

  @Test
  public void testStringPost() {
    startServer(ResourceString.class);
    assertCookies("/", HttpMethod.POST, entity, new Cookie("arg1", "a"), new Cookie("arg2", "b"), new Cookie("arg3", "c"));
  }

  @Path("/")
  public static class ResourceStringEmpty {
    @GET
    @Produces("application/string")
    public String get(@CookieParam("arg1") final String arg1) {
      assertEquals("", arg1);
      return entity;
    }

    @GET
    @Produces("application/list")
    public String get(@CookieParam("args") final List<String> args) {
      assertEquals(1, args.size());
      assertEquals("", args.iterator().next());
      return entity;
    }

    @GET
    @Produces("application/set")
    public String get(@CookieParam("args") final Set<String> args) {
      assertEquals(1, args.size());
      assertEquals("", args.iterator().next());
      return entity;
    }

    @GET
    @Produces("application/sortedset")
    public String get(@CookieParam("args") final SortedSet<String> args) {
      assertEquals(1, args.size());
      assertEquals("", args.iterator().next());
      return entity;
    }

    @GET
    @Produces("application/array")
    public String get(@CookieParam("args") final String[] args) {
      assertEquals(1, args.length);
      assertEquals("", args[0]);
      return entity;
    }
  }

  @Test
  public void testStringEmpty() {
    startServer(ResourceStringEmpty.class);
    assertCookies("/", HttpMethod.GET, entity, "application/string", new Cookie("arg1", ""));
  }

  @Test
  public void testStringEmptyList() {
    startServer(ResourceStringEmpty.class);
    assertCookies("/", HttpMethod.GET, entity, "application/list", new Cookie("args", ""));
  }

  @Test
  public void testStringEmptySet() {
    startServer(ResourceStringEmpty.class);
    assertCookies("/", HttpMethod.GET, entity, "application/set", new Cookie("args", ""));
  }

  @Test
  public void testStringEmptySortedSet() {
    startServer(ResourceStringEmpty.class);
    assertCookies("/", HttpMethod.GET, entity, "application/sortedset", new Cookie("args", ""));
  }

  @Test
  public void testStringEmptyArray() {
    startServer(ResourceStringEmpty.class);
    assertCookies("/", HttpMethod.GET, entity, "application/array", new Cookie("args", ""));
  }

  @Path("/")
  public static class ResourceStringAbsent {
    @GET
    public String get(@CookieParam("arg1") final String arg1) {
      assertEquals(null, arg1);
      return entity;
    }

    @GET
    @Produces("application/list")
    public String get(@CookieParam("arg1") final List<String> arg1) {
      assertEquals(0, arg1.size());
      return entity;
    }

    @GET
    @Produces("application/set")
    public String get(@CookieParam("arg1") final Set<String> arg1) {
      assertEquals(0, arg1.size());
      return entity;
    }

    @GET
    @Produces("application/sortedset")
    public String get(@CookieParam("arg1") final SortedSet<String> arg1) {
      assertEquals(0, arg1.size());
      return entity;
    }

    @GET
    @Produces("application/array")
    public String get(@CookieParam("arg1") final String[] arg1) {
      assertEquals(0, arg1.length);
      return entity;
    }
  }

  @Test
  public void testStringAbsent() {
    startServer(ResourceStringAbsent.class);
    assertCookies("/", HttpMethod.GET, entity);
  }

  @Test
  public void testStringAbsentList() {
    startServer(ResourceStringAbsent.class);
    assertCookies("/", HttpMethod.GET, entity, "application/list");
  }

  @Test
  public void testStringAbsentSet() {
    startServer(ResourceStringAbsent.class);
    assertCookies("/", HttpMethod.GET, entity, "application/set");
  }

  @Test
  public void testStringAbsentSortedSet() {
    startServer(ResourceStringAbsent.class);
    assertCookies("/", HttpMethod.GET, entity, "application/sortedset");
  }

  @Test
  public void testStringAbsentSortedArray() {
    startServer(ResourceStringAbsent.class);
    assertCookies("/", HttpMethod.GET, entity, "application/array");
  }

  @Path("/")
  public static class ResourceStringNullDefault {
    @GET
    public String get(@CookieParam("arg1") final String arg1, @CookieParam("arg2") final String arg2, @CookieParam("arg3") final String arg3) {
      assertEquals(null, arg1);
      assertEquals(null, arg2);
      assertEquals(null, arg3);
      return entity;
    }
  }

  @Test
  public void testStringNullDefault() {
    startServer(ResourceStringNullDefault.class);
    assertCookies("/", HttpMethod.GET, entity);
  }

  @Path("/")
  public static class ResourceStringEmptyDefault {
    @GET
    public String get(@CookieParam("args") String args) {
      assertNull(args);
      return entity;
    }

    @GET
    @Produces("application/list")
    public String get(@CookieParam("args") List<String> args) {
      assertEquals(0, args.size());
      return entity;
    }

    @GET
    @Produces("application/set")
    public String get(@CookieParam("args") Set<String> args) {
      assertEquals(0, args.size());
      return entity;
    }

    @GET
    @Produces("application/sortedset")
    public String get(@CookieParam("args") SortedSet<String> args) {
      assertEquals(0, args.size());
      return entity;
    }

    @GET
    @Produces("application/array")
    public String get(@CookieParam("args") String[] args) {
      assertEquals(0, args.length);
      return entity;
    }
  }

  @Test
  public void testStringEmptyDefault() {
    startServer(ResourceStringEmptyDefault.class);
    assertCookies("/", HttpMethod.GET, entity);
  }

  @Test
  public void testStringEmptyDefaultList() {
    startServer(ResourceStringEmptyDefault.class);
    assertCookies("/", HttpMethod.GET, entity, "application/list");
  }

  @Test
  public void testStringEmptyDefaultSet() {
    startServer(ResourceStringEmptyDefault.class);
    assertCookies("/", HttpMethod.GET, entity, "application/set");
  }

  @Test
  public void testStringEmptyDefaultSortedSet() {
    startServer(ResourceStringEmptyDefault.class);
    assertCookies("/", HttpMethod.GET, entity, "application/sortedset");
  }

  @Test
  public void testStringEmptyDefaultArray() {
    startServer(ResourceStringEmptyDefault.class);
    assertCookies("/", HttpMethod.GET, entity, "application/array");
  }

  @Path("/")
  public static class ResourceStringDefault {
    @GET
    public String get(@CookieParam("arg1") @DefaultValue("a") final String arg1, @CookieParam("arg2") @DefaultValue("b") final String arg2, @CookieParam("arg3") @DefaultValue("c") final String arg3) {
      assertEquals("a", arg1);
      assertEquals("b", arg2);
      assertEquals("c", arg3);
      return entity;
    }

    @GET
    @Produces("application/list")
    public String get(@CookieParam("args") @DefaultValue("a") final List<String> args) {
      assertEquals("a", args.get(0));
      return entity;
    }

    @GET
    @Produces("application/set")
    public String get(@CookieParam("args") @DefaultValue("a") final Set<String> args) {
      assertEquals("a", args.iterator().next());
      return entity;
    }

    @GET
    @Produces("application/sortedset")
    public String get(@CookieParam("args") @DefaultValue("a") final SortedSet<String> args) {
      assertEquals("a", args.iterator().next());
      return entity;
    }

    @GET
    @Produces("application/array")
    public String get(@CookieParam("args") @DefaultValue("a") final String[] args) {
      assertEquals("a", args[0]);
      return entity;
    }
  }

  @Test
  public void testStringDefault() {
    startServer(ResourceStringDefault.class);
    assertCookies("/", HttpMethod.GET, entity);
  }

  @Test
  public void testStringDefaultList() {
    startServer(ResourceStringDefault.class);
    assertCookies("/", HttpMethod.GET, entity, "application/list");
  }

  @Test
  public void testStringDefaultSet() {
    startServer(ResourceStringDefault.class);
    assertCookies("/", HttpMethod.GET, entity, "application/set");
  }

  @Test
  public void testStringDefaultSortedSet() {
    startServer(ResourceStringDefault.class);
    assertCookies("/", HttpMethod.GET, entity, "application/sortedset");
  }

  @Test
  public void testStringDefaultArray() {
    startServer(ResourceStringDefault.class);
    assertCookies("/", HttpMethod.GET, entity, "application/array");
  }

  @Path("/")
  public static class ResourceStringDefaultOverride {
    @GET
    @Produces("application/string")
    public String get(@CookieParam("arg1") @DefaultValue("a") final String arg1, @CookieParam("arg2") @DefaultValue("b") final String arg2, @CookieParam("arg3") @DefaultValue("c") final String arg3) {
      assertEquals("d", arg1);
      assertEquals("e", arg2);
      assertEquals("f", arg3);
      return entity;
    }

    @GET
    @Produces("application/list")
    public String get(@CookieParam("args") @DefaultValue("a") final List<String> args) {
      assertEquals("b", args.get(0));
      return entity;
    }

    @GET
    @Produces("application/set")
    public String get(@CookieParam("args") @DefaultValue("a") final Set<String> args) {
      assertEquals("b", args.iterator().next());
      return entity;
    }

    @GET
    @Produces("application/sortedset")
    public String get(@CookieParam("args") @DefaultValue("a") final SortedSet<String> args) {
      assertEquals("b", args.iterator().next());
      return entity;
    }

    @GET
    @Produces("application/array")
    public String get(@CookieParam("args") @DefaultValue("a") final String[] args) {
      assertEquals("b", args[0]);
      return entity;
    }
  }

  @Test
  public void testStringDefaultOverride() {
    startServer(ResourceStringDefaultOverride.class);
    assertCookies("/", HttpMethod.GET, entity, "application/string", new Cookie("arg1", "d"), new Cookie("arg2", "e"), new Cookie("arg3", "f"));
  }

  @Test
  public void testStringDefaultOverrideList() {
    startServer(ResourceStringDefaultOverride.class);
    assertCookies("/", HttpMethod.GET, entity, "application/list", new Cookie("args", "b"));
  }

  @Test
  public void testStringDefaultOverrideSet() {
    startServer(ResourceStringDefaultOverride.class);
    assertCookies("/", HttpMethod.GET, entity, "application/set", new Cookie("args", "b"));
  }

  @Test
  public void testStringDefaultOverrideSortedSet() {
    startServer(ResourceStringDefaultOverride.class);
    assertCookies("/", HttpMethod.GET, entity, "application/sortedset", new Cookie("args", "b"));
  }

  @Test
  public void testStringDefaultOverrideArray() {
    startServer(ResourceStringDefaultOverride.class);
    assertCookies("/", HttpMethod.GET, entity, "application/array", new Cookie("args", "b"));
  }

  @Path("/")
  public static class CookieTypeResource {
    @POST
    public String post(@Context final HttpHeaders headers, @CookieParam("arg1") final Cookie arg1, @CookieParam("arg2") final Cookie arg2, @CookieParam("arg3") final Cookie arg3) {
      assertEquals("arg1", arg1.getName());
      assertEquals("a", arg1.getValue());

      assertEquals("arg2", arg2.getName());
      assertEquals("b", arg2.getValue());

      assertEquals(null, arg3);

      final Map<String,Cookie> cookies = headers.getCookies();
      assertEquals(2, cookies.size());
      assertEquals("a", cookies.get("arg1").getValue());
      assertEquals("b", cookies.get("arg2").getValue());

      return entity;
    }
  }

  @Test
  public void testCookieParam() {
    startServer(CookieTypeResource.class);
    assertCookies("/", HttpMethod.POST, entity, new Cookie("arg1", "a"), new Cookie("arg2", "b"));
  }
}