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

import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import org.junit.Test;

public class HeaderDelegateCookieTest {
  @Test
  public void testCookieToString() {
    Cookie cookie = new Cookie("film", "star wars");
    String expected = "film=\"star wars\";Version=1";
    assertEquals(expected, cookie.toString());

    cookie = new Cookie("foo", "bar", "/path with whitespace", null);
    expected = "foo=bar;Path=\"/path with whitespace\";Version=1";
    assertEquals(expected, cookie.toString());

    cookie = new Cookie("root", "alpine", "/path/without/whitespace", "java.sun.com");
    expected = "root=alpine;Path=/path/without/whitespace;Domain=java.sun.com;Version=1";
    assertEquals(expected, cookie.toString());

    cookie = new Cookie("marge", "simpson", "/path", ".sun.com", 2);
    expected = "marge=simpson;Path=/path;Domain=.sun.com;Version=2";
    assertEquals(expected, cookie.toString());
  }

  @Test
  public void testCookieValueOf() {
    Cookie cookie = Cookie.valueOf("$Version=2;fred=flintstone");
    assertEquals("fred", cookie.getName());
    assertEquals("flintstone", cookie.getValue());
    assertEquals(2, cookie.getVersion());

    cookie = Cookie.valueOf("$Version=1;fred=flintstone;$Path=/path");
    assertEquals("fred", cookie.getName());
    assertEquals("flintstone", cookie.getValue());
    assertEquals(1, cookie.getVersion());
    assertEquals("/path", cookie.getPath());

    cookie = Cookie.valueOf("$Version=1;fred=flintstone;$Domain=.sun.com;$Path=/path");
    assertEquals("fred", cookie.getName());
    assertEquals("flintstone", cookie.getValue());
    assertEquals(1, cookie.getVersion());
    assertEquals(".sun.com", cookie.getDomain());
    assertEquals("/path", cookie.getPath());
  }

  private static Map<String,Cookie> toMap(final String header) {
    final HttpHeadersImpl headers = new HttpHeadersImpl();
    final String[] parts = header.split(",");
    for (final String part : parts) // [A]
      headers.add(HttpHeaders.COOKIE, part);

    return headers.getCookies();
  }

  @Test
  public void testCreateCookies() {
    Map<String,Cookie> cookies = toMap("fred=flintstone");
    assertEquals(1, cookies.size());
    Cookie c = cookies.get("fred");
    assertEquals(0, c.getVersion());
    assertEquals("fred", c.getName());
    assertEquals("flintstone", c.getValue());

    cookies = toMap("fred=flintstone,barney=rubble");
    assertEquals(2, cookies.size());
    c = cookies.get("fred");
    assertEquals(0, c.getVersion());
    assertEquals("fred", c.getName());
    assertEquals("flintstone", c.getValue());
    c = cookies.get("barney");
    assertEquals(0, c.getVersion());
    assertEquals("barney", c.getName());
    assertEquals("rubble", c.getValue());

    cookies = toMap("fred=flintstone,barney=rubble");
    assertEquals(2, cookies.size());
    c = cookies.get("fred");
    assertEquals(0, c.getVersion());
    assertEquals("fred", c.getName());
    assertEquals("flintstone", c.getValue());
    c = cookies.get("barney");
    assertEquals(0, c.getVersion());
    assertEquals("barney", c.getName());
    assertEquals("rubble", c.getValue());

    cookies = toMap("$Version=1;fred=flintstone;$Path=/path,barney=rubble;$Version=2");
    assertEquals(2, cookies.size());
    c = cookies.get("fred");
    assertEquals(1, c.getVersion());
    assertEquals("fred", c.getName());
    assertEquals("flintstone", c.getValue());
    assertEquals("/path", c.getPath());
    c = cookies.get("barney");
    assertEquals(2, c.getVersion());
    assertEquals("barney", c.getName());
    assertEquals("rubble", c.getValue());

    cookies = toMap("$Version=1;fred=flintstone;$Path=/path,barney=rubble;$Domain=.sun.com;$Version=2");
    assertEquals(2, cookies.size());
    c = cookies.get("fred");
    assertEquals(1, c.getVersion());
    assertEquals("fred", c.getName());
    assertEquals("flintstone", c.getValue());
    assertEquals("/path", c.getPath());
    c = cookies.get("barney");
    assertEquals(2, c.getVersion());
    assertEquals("barney", c.getName());
    assertEquals("rubble", c.getValue());
    assertEquals(".sun.com", c.getDomain());

    cookies = toMap("$Version=1; fred = flintstone ; $Path=/path, barney=rubble ;$Domain=.sun.com");
    assertEquals(2, cookies.size());
    c = cookies.get("fred");
    assertEquals(1, c.getVersion());
    assertEquals("fred", c.getName());
    assertEquals("flintstone", c.getValue());
    assertEquals("/path", c.getPath());
    c = cookies.get("barney");
    assertEquals(0, c.getVersion());
    assertEquals("barney", c.getName());
    assertEquals("rubble", c.getValue());
    assertEquals(".sun.com", c.getDomain());
  }

  @Test
  public void testMultipleCookiesWithSameName() {
    Map<String,Cookie> cookies = toMap("kobe=longeststring, kobe=shortstring");
    assertEquals(1, cookies.size());

    Cookie cookie = cookies.get("kobe");
    assertEquals(0, cookie.getVersion());
    assertEquals("kobe", cookie.getName());
    assertEquals("longeststring", cookie.getValue());

    cookies = toMap("bryant=longeststring; bryant=shortstring, fred=shortstring ;fred=longeststring;$Path=/path");
    assertEquals(2, cookies.size());

    cookie = cookies.get("bryant");
    assertEquals(0, cookie.getVersion());
    assertEquals("bryant", cookie.getName());
    assertEquals("longeststring", cookie.getValue());

    cookie = cookies.get("fred");
    assertEquals(0, cookie.getVersion());
    assertEquals("fred", cookie.getName());
    assertEquals("longeststring", cookie.getValue());
    assertEquals("/path", cookie.getPath());
  }

  @Test
  public void testNewCookieToString() {
    NewCookie cookie = new NewCookie("fred", "flintstone");
    String expected = "fred=flintstone;Version=1";
    assertEquals(expected, cookie.toString());

    cookie = new NewCookie("fred", "flintstone", null, null, null, 60, false);
    expected = "fred=flintstone;Version=1;Max-Age=60";
    assertEquals(expected, cookie.toString());

    cookie = new NewCookie("fred", "flintstone", null, null, "a modern stonage family", 60, false);
    expected = "fred=flintstone;Version=1;Comment=\"a modern stonage family\";Max-Age=60";
    assertEquals(expected, cookie.toString());
  }

  @Test
  public void testNewCookieValueOf() {
    NewCookie cookie = NewCookie.valueOf("fred=flintstone;Version=2");
    assertEquals("fred", cookie.getName());
    assertEquals("flintstone", cookie.getValue());
    assertEquals(2, cookie.getVersion());

    cookie = NewCookie.valueOf("fred=flintstone;Version=1;Max-Age=60");
    assertEquals("fred", cookie.getName());
    assertEquals("flintstone", cookie.getValue());
    assertEquals(1, cookie.getVersion());
    assertEquals(60, cookie.getMaxAge());

    cookie = NewCookie.valueOf("fred=flintstone;Version=1;Comment=\"a modern stonage family\";Max-Age=60;Secure");
    assertEquals("fred", cookie.getName());
    assertEquals("flintstone", cookie.getValue());
    assertEquals("a modern stonage family", cookie.getComment());
    assertEquals(1, cookie.getVersion());
    assertEquals(60, cookie.getMaxAge());
    assertTrue(cookie.isSecure());
  }
}