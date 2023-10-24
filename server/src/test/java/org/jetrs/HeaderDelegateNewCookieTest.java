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

import javax.ws.rs.core.NewCookie;

import org.junit.Test;
import org.libj.util.SimpleDateFormats;

public class HeaderDelegateNewCookieTest {
  private static final String name = "foo";
  private static final String value = "bar";
  private static final String expires = "Tue, 15 Jan 2023 21:47:38 GMT";
  private static final int maxAge = 42;
  private static final String domain = ".example.com";
  private static final String comment = "Some Comment";
  private static final String path = "/some path/foo";
  private static final int version = 1;

  private static void assertCookie(final String expires$, final String maxAge$, final String path$, final String domain$, final String comment$, final String version$, final String secure$, final String httpOnly$) throws Exception {
    final String header = name + "=" + value + ";" + expires$ + "=" + expires + ";" + maxAge$ + "=" + maxAge + ";" + path$ + "=" + path + ";" + domain$ + "=" + domain + ";" + comment$ + "=" + comment + ";" + version$ + "=" + version + ";" + secure$ + ";" + httpOnly$;
    final NewCookie cookie = HeaderDelegateImpl.NEW_COOKIE.valueOf(header);

    assertEquals(name, cookie.getName());
    assertEquals(value, cookie.getValue());
    assertEquals(SimpleDateFormats.RFC_1123.get().parse(expires), cookie.getExpiry());
    assertEquals(maxAge, cookie.getMaxAge());
    assertEquals(path, cookie.getPath());
    assertEquals(domain, cookie.getDomain());
    assertEquals(comment, cookie.getComment());
    assertEquals(version, cookie.getVersion());
    assertEquals(true, cookie.isSecure());
    assertEquals(true, cookie.isHttpOnly());
  }

  @Test
  public void testCaseInsensitiveNewCookieParams() throws Exception {
    assertCookie("expires", "max-age", "path", "domain", "comment", "version", "secure", "httponly");
    assertCookie("Expires", "Max-Age", "Path", "Domain", "Comment", "Version", "Secure", "HttpOnly");
    assertCookie("exPires", "max-aGe", "patH", "doMAin", "Comment", "vErsion", "secuRe", "httPonly");
  }
}