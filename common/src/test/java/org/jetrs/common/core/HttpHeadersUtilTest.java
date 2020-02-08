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

package org.jetrs.common.core;

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;

import org.jetrs.common.ext.RuntimeDelegateTest;
import org.jetrs.common.ext.delegate.DateHeaderDelegate;
import org.junit.Test;
import org.libj.util.Locales;
import org.libj.util.Numbers;
import org.libj.util.primitive.ArrayFloatList;

public class HttpHeadersUtilTest extends RuntimeDelegateTest {
  private static Locale l(final String language) {
    return l(language, "");
  }

  private static Locale l(final String language, final String country) {
    return new Locale(language, country);
  }

  private static void assertCompound(final float quality, final int index, final long actual) {
    assertEquals(quality, Numbers.Compound.decodeFloat(actual, 0), 0.0000001f);
    assertEquals(index, Numbers.Compound.decodeInt(actual, 1));
  }

  @Test
  public void testQualityFunction() {
    assertCompound(0.1f, 6, HttpHeadersUtil.getQualityFromString("q=0.1;", 0));
    assertCompound(0.8f, 12, HttpHeadersUtil.getQualityFromString("en-GB;q= .8 ,en;q=0.8", 3));
    assertCompound(0f, 14, HttpHeadersUtil.getQualityFromString("en-GB;q =0.0; ,fr;q=0.4", 5));
    assertCompound(1f, 11, HttpHeadersUtil.getQualityFromString("en-GB; q=1 ; q=0  ;;, fr;q=0.4", 1));
    assertCompound(0f, 15, HttpHeadersUtil.getQualityFromString("en-GB; q =0    ; foo=bar,,,", 7));
    assertCompound(0.4f, 25, HttpHeadersUtil.getQualityFromString("en-GB;q= 0.8 0 ; fr;q=0.4,", 6));
    assertCompound(1f, 15, HttpHeadersUtil.getQualityFromString("en-GB;q= 0.8 0 , fr;q=0.4,", 6));
    assertCompound(1f, 17, HttpHeadersUtil.getQualityFromString("en-GB;rq=0.8;;;;;", 5));
  }

  private static void assertHeaderWithQ(final String[] headers, final String ... expecteds) {
    final List<String> actuals = new ArrayList<>();
    ArrayFloatList qualities = null;
    for (final String header : headers) {
      qualities = HttpHeadersUtil.parseMultiHeader(actuals, qualities, header, s -> s, true);
    }

    assertEquals(expecteds.length, actuals.size());
    for (int i = 0; i < expecteds.length; ++i)
      assertEquals(expecteds[i], actuals.get(i));
  }

  @Test
  public void testParseMultiHeaderWithQ() {
    assertHeaderWithQ(new String[] {
      "fr-CH , fr ;q=0.9, en; q=0.8, de;q= 0.7, * ; q = 0.5"
    }, "fr-CH", "fr", "en", "de", "*");
    assertHeaderWithQ(new String[] {
      "*;q=0.5, de;q=0.7, en;q=0.8, fr;q=0.9, fr-CH"
    }, "fr-CH", "fr", "en", "de", "*");
    assertHeaderWithQ(new String[] {
      "*;q=0.5, de;q=0.7",
      "en;q=0.8, fr;q=0.9, fr-CH"
    }, "fr-CH", "fr", "en", "de", "*");
    assertHeaderWithQ(new String[] {
      "*;q=0.5, de;q=0.7",
      "en;q=0.8, fr;q=0.9",
      "fr-CH"
    }, "fr-CH", "fr", "en", "de", "*");
    assertHeaderWithQ(new String[] {
      "*;q=0.5, de;q=0.7",
      "fr-CH;q=x",
      "fr;q=0.9, en;q=0.8"
    }, "fr-CH", "fr", "en", "de", "*");
  }

  private static void assertParseRequestHeader(final Object expected, final String name, final String value) {
    assertEquals(expected, HttpHeadersUtil.parseRequestHeader(name, value, false));
  }

  private static void assertParseResponseHeader(final Object expected, final String name, final String value) {
    assertEquals(expected, HttpHeadersUtil.parseResponseHeader(name, value));
  }

  @Test
  public void testParseRequestHeader() {
    assertParseRequestHeader(MediaType.valueOf("application/json;q=.5"), HttpHeaders.ACCEPT, "application/json;q=.5");
    assertParseRequestHeader(Arrays.asList(Charset.forName("iso-8859-5"), StandardCharsets.UTF_8, null),  HttpHeaders.ACCEPT_CHARSET, "*;q=0.1, iso-8859-5, utf-8;q=0.8");
    assertParseRequestHeader(Arrays.asList(Locales.fromRFC1766("fr-CH"), Locales.fromRFC1766("fr"), Locales.fromRFC1766("en"), Locales.fromRFC1766("de"), Locales.fromRFC1766("*")), HttpHeaders.ACCEPT_LANGUAGE, "fr-CH , fr ;q=0.9, en; q=0.8, de;q= 0.7, * ; q = 0.5");
    assertParseRequestHeader(DateHeaderDelegate.parse("Thu, 31 May 2007 20:35:00 GMT"), "Accept-Datetime", "Thu, 31 May 2007 20:35:00 GMT");
    assertParseRequestHeader(CacheControl.valueOf("no-cache, must-revalidate"), HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate");
    assertParseRequestHeader(4372943279L, HttpHeaders.CONTENT_LENGTH, "4372943279");
    assertParseRequestHeader(MediaType.valueOf("application/json;q=.5"), HttpHeaders.CONTENT_TYPE, "application/json;q=.5");
    final Map<String,Cookie> cookies = new HashMap<>();
    cookies.put("yummy_cookie", new Cookie("yummy_cookie", "choco"));
    cookies.put("tasty_cookie", new Cookie("tasty_cookie", "strawberry"));
    assertParseRequestHeader(cookies, HttpHeaders.COOKIE, "yummy_cookie=choco; tasty_cookie=strawberry");
    assertParseRequestHeader(DateHeaderDelegate.parse("Wed, 21 Oct 2015 07:28:00 GMT"), HttpHeaders.DATE, "Wed, 21 Oct 2015 07:28:00 GMT");
    assertParseRequestHeader(DateHeaderDelegate.parse("Tue, 20 Oct 2015 07:28:20 GMT"), HttpHeaders.IF_MODIFIED_SINCE, "Tue, 20 Oct 2015 07:28:20 GMT");
    assertParseRequestHeader(DateHeaderDelegate.parse("Sat, 29 Oct 1994 19:43:31 GMT"), HttpHeaders.IF_UNMODIFIED_SINCE, "Sat, 29 Oct 1994 19:43:31 GMT");
    assertParseRequestHeader(3L, "Max-Forwards", "3");
    assertParseRequestHeader(Arrays.asList("1.0 fred", "1.1 p.example.net"), "Via", "1.0 fred, 1.1 p.example.net");
    assertParseRequestHeader(112L, "Upgrade-Insecure-Requests", "112");
  }

  @Test
  public void testParseResponseHeader() {
    assertParseResponseHeader(MediaType.valueOf("application/json;q=.5"), "Accept-Patch", "application/json;q=.5");
    assertParseResponseHeader(3L, "Age", "3");
    assertParseResponseHeader(Locale.forLanguageTag("en"), HttpHeaders.CONTENT_LANGUAGE, "en");
    assertParseResponseHeader(8432957497543L, HttpHeaders.CONTENT_LENGTH, "8432957497543");
    assertParseResponseHeader(new Date((3432 + System.currentTimeMillis() / 1000) * 1000), HttpHeaders.EXPIRES, "3432");
    assertParseResponseHeader(DateHeaderDelegate.parse("Thu, 01 Dec 1994 16:00:00 GMT"), HttpHeaders.EXPIRES, "Thu, 01 Dec 1994 16:00:00 GMT");
    assertParseResponseHeader(new NewCookie("Part_Number", "Rocket_Launcher_0001", "/acme", null, 1, null, -1, false), HttpHeaders.SET_COOKIE, "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\"; Path=\"/acme\"");
  }
}