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

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.jetrs.common.ext.RuntimeDelegateTest;
import org.jetrs.common.util.HttpHeadersMap;
import org.jetrs.common.util.MirrorQualityList;
import org.jetrs.provider.ext.header.Delegate;
import org.jetrs.provider.ext.header.MediaTypes;
import org.jetrs.provider.ext.header.QualifiedCharset;
import org.jetrs.provider.ext.header.Tk;
import org.junit.Test;
import org.libj.util.ArrayUtil;
import org.libj.util.MirrorList;
import org.libj.util.function.TriConsumer;

public class HttpHeadersImplTest extends RuntimeDelegateTest {
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testAddFirst(final HttpHeadersMap<?,?> headers, final String name, final Object value) {
    if (value instanceof String)
      throw new IllegalArgumentException();

    final String string = value.toString();
    if (headers instanceof HttpHeadersImpl) {
      ((HttpHeadersMap)headers).addFirst(name, string);
      final MirrorList<?,?> a = ((HttpHeadersMap)headers).get(name);
      final MirrorList<?,?> b = ((HttpHeadersMap)headers.getMirrorMap()).get(name);
      assertEquals(a.getMirrorList(), b);
      assertEquals(b.getMirrorList(), a);
      assertEquals(string, headers.getFirst(name));
      assertEquals(value, headers.getMirrorMap().getFirst(name));
    }
    else {
      ((HttpHeadersMap)headers).addFirst(name, value);
      assertEquals(value, headers.getFirst(name));
      assertEquals(string, headers.getMirrorMap().getFirst(name));
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testGetAdd(final HttpHeadersMap<?,?> headers, final String name, final Object value) {
    if (value instanceof String)
      throw new IllegalArgumentException();

    List list = headers.get(name);
    if (list == null)
      headers.put(name, list = new ArrayList<>());

    list = headers.get(name);
    final String string = value.toString();
    if (headers instanceof HttpHeadersImpl) {
      list.add(string);
      assertEquals(string, headers.getFirst(name));
      assertEquals(value, headers.getMirrorMap().getFirst(name));
    }
    else {
      list.add(value);
      assertEquals(value, headers.getFirst(name));
      assertEquals(string, headers.getMirrorMap().getFirst(name));
    }
  }

  @SuppressWarnings("rawtypes")
  private static void testAddFirstRemove(final HttpHeadersMap<?,?> headers, final String name, final Object value, final TriConsumer<? super HttpHeadersMap,? super String,Object> consumer) {
    assertSize(0, headers);
    consumer.accept(headers, name, value);
    assertSize(1, headers.getMirrorMap());

    testRemove(headers.getMirrorMap(), name, value);
    assertSize(0, headers.getMirrorMap());
    consumer.accept(headers.getMirrorMap(), name, value);
    assertSize(1, headers);
    testRemove(headers, name, value);
    assertSize(0, headers.getMirrorMap());

    assertSize(0, headers.getMirrorMap());
    consumer.accept(headers.getMirrorMap(), name, value);
    assertSize(1, headers);
    testRemove(headers.getMirrorMap(), name, value);
    assertSize(0, headers.getMirrorMap());
    consumer.accept(headers, name, value);
    assertSize(1, headers.getMirrorMap());
    consumer.accept(headers, name, value);
    assertSize(2, headers.get(name));
  }

  private static void testRemove(final HttpHeadersMap<?,?> headers, final String name, final Object value) {
    if (value instanceof String)
      throw new IllegalArgumentException();

    assertFalse(headers.remove(name, null));
    if (headers instanceof HttpHeadersImpl) {
      assertTrue(headers.remove(name, Collections.singletonList(value.toString())));
    }
    else {
      assertTrue(headers.remove(name, Collections.singletonList(value)));
    }
  }

  private static void assertSize(final int expected, final HttpHeadersMap<?,?> headers) {
    assertEquals(expected, headers.size());
    assertEquals(expected, headers.getMirrorMap().size());
  }

  private static void assertSize(final int expected, final MirrorList<?,?> list) {
    assertEquals(expected, list.size());
    assertEquals(expected, list.getMirrorList().size());
  }

  @Test
  public void testInvalidHeaderName() {
    try {
      final HttpHeadersImpl headers = new HttpHeadersImpl();
      headers.put("Invalid()", null);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
      if (!"Illegal header name: \"Invalid()\"".equals(e.getMessage()))
        throw e;
    }
  }

  @Test
  public void testAddFirst() {
    final String name = HttpHeaders.ACCEPT;
    final MediaType value = MediaTypes.parse("application/json;q=.5");
    final HttpHeadersImpl headers = new HttpHeadersImpl();

    testAddFirstRemove(headers, name, value, HttpHeadersImplTest::testAddFirst);
    headers.clear();
    testAddFirstRemove(headers.getMirrorMap(), name, value, HttpHeadersImplTest::testAddFirst);
  }

  @Test
  public void testGetAdd() {
    final String name = HttpHeaders.ACCEPT;
    final MediaType value = MediaTypes.parse("application/json;q=.5");
    final HttpHeadersImpl headers = new HttpHeadersImpl();

    testAddFirstRemove(headers, name, value, HttpHeadersImplTest::testGetAdd);
    headers.clear();
    testAddFirstRemove(headers.getMirrorMap(), name, value, HttpHeadersImplTest::testGetAdd);
  }

  private static double getQ(final String s) {
    int i = s.indexOf("q=");
    if (i < 0)
      return 1;

    for (int x = i - 1; x > 0; --x) {
      final char ch = s.charAt(x);
      if (ch == ';')
        break;

      if (ch != ' ')
        return 1;
    }

    int j = i += 2;
    for (char ch; j < s.length(); ++j) {
      ch = s.charAt(j);
      if ((ch < '0' || '9' < ch) && ch != '.')
        break;
    }

    return Double.parseDouble(s.substring(i, j));
  }

  private static String hackToString(final Object obj) {
    if (obj instanceof Charset)
      return QualifiedCharset.toString((Charset)obj);

    return String.valueOf(obj);
  }

  private static final Comparator<Object> qualityComparator = new Comparator<Object>() {
    @Override
    public int compare(final Object o1, final Object o2) {
      return -Double.compare(getQ(hackToString(o1)), getQ(hackToString(o2)));
    }
  };

  private static void testRemove(final HttpHeadersImpl strings, final List<String> expectedStrings, final List<Object> expectedObjects, final String headerName, final String headerString, final HeaderDelegate<?> headerDelegate) {
    final HttpHeadersMap<Object,String> objects = strings.getMirrorMap();
    for (int i = 0; i < 2; ++i) {
      final Object headerObject = headerDelegate.fromString(headerString);
      expectedStrings.remove(headerString);
      expectedStrings.sort(qualityComparator);
      expectedObjects.remove(headerObject);
      expectedObjects.sort(qualityComparator);

      final boolean isForward = i % 2 == 0;
      if (isForward)
        strings.get(headerName).remove(headerString);
      else
        objects.get(headerName).remove(headerObject);

      assertEquals(i + ": " + headerString, expectedStrings, strings.get(headerName));
      assertEquals(i + ": " + headerString, expectedObjects, objects.get(headerName));
    }
  };

  private static void testAdd(final HttpHeadersImpl strings, final List<String> expectedStrings, final List<Object> expectedObjects, final String headerName, final String headerString, final HeaderDelegate<?> headerDelegate) {
    final HttpHeadersMap<Object,String> objects = strings.getMirrorMap();
    for (int i = 0; i < 2; ++i) {
      final Object headerObject = headerDelegate.fromString(headerString);
      expectedStrings.add(headerString);
      expectedStrings.sort(qualityComparator);
      expectedObjects.add(headerObject);
      expectedObjects.sort(qualityComparator);

      final boolean isForward = i % 2 == 0;
      if (isForward)
        strings.add(headerName, headerString);
      else
        objects.add(headerName, headerObject);

      assertEquals(i + ": " + headerString, expectedStrings, strings.get(headerName));
      assertEquals(i + ": " + headerString, expectedObjects, objects.get(headerName));
    }
  }

  private static void test(final String headerName, final Class<?> type, final String ... args) {
    final HeaderDelegate<?> headerDelegate = Delegate.lookup(headerName, type);

    final HttpHeadersImpl strings = new HttpHeadersImpl();
    final List<String> expectedStrings = new ArrayList<>();
    final List<Object> expectedObjects = new ArrayList<>();
    for (int i = 0; i < args.length; ++i)
      testAdd(strings, expectedStrings, expectedObjects, headerName, args[i], headerDelegate);

    ArrayUtil.shuffle(args);
    for (int i = 0; i < args.length; ++i)
      testRemove(strings, expectedStrings, expectedObjects, headerName, args[i], headerDelegate);
  }

  @Test
  public void testAccept() {
    test(HttpHeaders.ACCEPT, MediaType.class, "application/json;q=.5", "application/xml;q=.7", "application/rss+xml;q=.6");
  }

  @Test
  public void testAcceptCharset() {
    test(HttpHeaders.ACCEPT_CHARSET, Charset.class, "UTF-8;q=0.9", "iso-8859-1", "*;q=0.5");
  }

  @Test
  public void testAcceptEncoding() {
    test(HttpHeaders.ACCEPT_ENCODING, String.class, "*;q=0.5", "gzip;q=.9", "deflate");
  }

  @Test
  public void testAcceptLanguage() {
    test(HttpHeaders.ACCEPT_LANGUAGE, Locale.class, "fr-CH", "fr;q=0.9", "en;q=0.8", "de;q=0.7", "*;q=0.5");
  }

  @Test
  public void testAcceptDatetime() {
    test("Accept-Datetime", Date.class, "Thu, 31 May 2007 20:35:00 +0000", "Thu, 31 May 2007 20:35:00 GMT", "Tue, 10 Aug 2010 09:56:13 GMT", "Tue, 11 Sep 2001 20:35:00 GMT");
  }

  @Test
  public void testAccessControlRequestMethod() {
    test("Access-Control-Request-Method", String.class, "POST", "GET", "PUT", "PATCH");
  }

  @Test
  public void testAuthorization() {
    test(HttpHeaders.AUTHORIZATION, String.class, "Basic YWxhZGRpbjpvcGVuc2VzYW1l", "Bearer hY_9.B5f-4.1BfE", "Hawk id=\"abcxyz123\", ts=\"1592459563\", nonce=\"gWqbkw\", mac=\"vxBCccCutXGV30gwEDKu1NDXSeqwfq7Z0sg/HP1HjOU=\"");
  }

  @Test
  public void testCacheControl() {
    test(HttpHeaders.CACHE_CONTROL, CacheControl.class, "no-store", "no-store,max-age=0", "public,max-age=604800,immutable", "max-age=0,must-revalidate", "private,no-cache,no-store,max-age=0,proxy-revalidate,pre-check=0,post-check=0");
  }

  @Test
  public void testConnection() {
    test("Connection", String.class, "keep-alive", "close");
  }

  @Test
  public void testContentLength() {
    test(HttpHeaders.CONTENT_LENGTH, Long.class, "0", "432", "328932", "48329849328492");
  }

  @Test
  public void testContentMD5() {
    test("Content-MD5", String.class, "FbKf/c5m4QUnplvG1xrZTQ==", "MTViMjlmZmRjZTY2ZTEwNTI3YTY1YmM2ZDcxYWQ5NGQ=");
  }

  @Test
  public void testContentType() {
    test(HttpHeaders.CONTENT_TYPE, MediaType.class, "application/json;q=.5", "application/xml;q=.7", "application/rss+xml;q=.6");
  }

  @Test
  public void testCookie() {
    test(HttpHeaders.COOKIE, Cookie.class, "PHPSESSID=298zf09hf012fh2; csrftoken=u32t4o3tb3gg43; _gat=1", "yummy_cookie=choco; tasty_cookie=strawberry", "theme=light", "sessionToken=abc123; Expires=Wed, 09 Jun 2021 10:18:14 GMT", "theme=light; sessionToken=abc123");
  }

  @Test
  public void testDate() {
    test(HttpHeaders.DATE, Date.class, "Thu, 31 May 2007 20:35:00 GMT", "Tue, 10 Aug 2010 09:56:13 GMT", "Tue, 11 Sep 2001 20:35:00 GMT");
  }

  @Test
  public void testDNT() {
    test("DNT", String.class, "1", "2", null);
  }

  @Test
  public void testExpect() {
    test("Expect", String.class, "100-continue");
  }

  @Test
  public void testForwarded() {
    test("Forwarded", String.class, "for=192.0.2.43", "for=198.51.100.17", "for=\"_mdn\"", "For=\"[2001:db8:cafe::17]:4711\"", "for=192.0.2.60;proto=http;by=203.0.113.43");
  }

  @Test
  public void testFrom() {
    test("From", String.class, "webmaster@example.org", "postmaster@example.org", "hostmaster@example.org");
  }

  @Test
  public void testHost() {
    test(HttpHeaders.HOST, String.class, "dev.mozilla.org", "stage.mozilla.org", "mozilla.org");
  }

  @Test
  public void testHTTP2Settings() {
    test("HTTP2-Settings", String.class, "FbKf/c5m4QUnplvG1xrZTQ==", "MTViMjlmZmRjZTY2ZTEwNTI3YTY1YmM2ZDcxYWQ5NGQ=");
  }

  @Test
  public void testIfMatch() {
    test(HttpHeaders.IF_MATCH, String.class, "\"bfc13a64729c4290ef5b2c2730249c88ca92d82d\"", "\"67ab43\"", "\"54ed21\"", "\"7892dd\"", "*");
  }

  @Test
  public void testIfModifiedSince() {
    test(HttpHeaders.IF_MODIFIED_SINCE, Date.class, "Thu, 31 May 2007 20:35:00 GMT", "Tue, 10 Aug 2010 09:56:13 GMT", "Tue, 11 Sep 2001 20:35:00 GMT");
  }

  @Test
  public void testIfNoneMatch() {
    test(HttpHeaders.IF_NONE_MATCH, String.class, "\"bfc13a64729c4290ef5b2c2730249c88ca92d82d\"", "W/\"67ab43\"", "\"54ed21\"", "\"7892dd\"", "*");
  }

  @Test
  public void testIfUnmodifiedSince() {
    test(HttpHeaders.IF_UNMODIFIED_SINCE, Date.class, "Thu, 31 May 2007 20:35:00 GMT", "Tue, 10 Aug 2010 09:56:13 GMT", "Tue, 11 Sep 2001 20:35:00 GMT");
  }

  @Test
  public void testMaxForwards() {
    test("Max-Forwards", Long.class, "0", "5", "20", "122");
  }

  @Test
  public void testOrigin() {
    test("Origin", String.class, "http://dev.mozilla.org", "https://stage.mozilla.org", "https://mozilla.org");
  }

  @Test
  public void testPragma() {
    test("Pragma", String.class, "no-cache");
  }

  @Test
  public void testProxyAuthorization() {
    test("Proxy-Authorization", String.class, "Basic YWxhZGRpbjpvcGVuc2VzYW1l", "Bearer hY_9.B5f-4.1BfE", "Hawk id=\"abcxyz123\", ts=\"1592459563\", nonce=\"gWqbkw\", mac=\"vxBCccCutXGV30gwEDKu1NDXSeqwfq7Z0sg/HP1HjOU=\"");
  }

  @Test
  public void testRange() {
    test("Range", String.class, "bytes=200-1000", "2000-6576", "19000-", "bytes=0-499", "-500");
  }

  @Test
  public void testReferer() {
    test("Referer", String.class, "https://developer.mozilla.org/en-US/docs/Web/JavaScript", "https://example.com/page?q=.2", "https://example.com/");
  }

  @Test
  public void testTE() {
    test("TE", String.class, "compress", "deflate;q=0.3", "gzip", "trailers;q=.5");
  }

  @Test
  public void testUserAgent() {
    test(HttpHeaders.USER_AGENT, String.class, "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41", "Opera/9.80 (Macintosh; Intel Mac OS X; U; en) Presto/2.2.15 Version/10.00", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1.1 Mobile/15E148 Safari/604.1", "Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0)");
  }

  @Test
  public void testUpgrade() {
    test("Upgrade", String.class, "example/1", "foo/2", "a_protocol/1", "example", "another_protocol/2.2");
  }

  @Test
  public void testVia() {
    test("Via", String.class, "1.1 vegur", "HTTP/1.1 GWA", "1.0 fred", "1.1 p.example.net");
  }

  @Test
  public void testWarning() {
    test("Warning", String.class, "110 anderson/1.3.37 \"Response is stale\"", "112 - \"cache down\" \"Wed, 21 Oct 2015 07:28:00 GMT\"");
  }

  @Test
  public void testUpgradeInsecureRequests() {
    test("Upgrade-Insecure-Requests", Long.class, "1", "2", "10", "322");
  }

  @Test
  public void testXRequestedWith() {
    test("X-Requested-With", String.class, "XMLHttpRequest");
  }

  @Test
  public void testXForwardedFor() {
    test("X-Forwarded-For", String.class, "192.0.2.43", "198.51.100.17", "\"_mdn\"", "\"[2001:db8:cafe::17]:4711\"", "192.0.2.60;proto=http;by=203.0.113.43");
  }

  @Test
  public void testXForwardedHost() {
    test("X-Forwarded-Host", String.class, "id42.example-cdn.com");
  }

  @Test
  public void testXForwardedProto() {
    test("X-Forwarded-Proto", String.class, "https", "http");
  }

  @Test
  public void testFrontEndHttps() {
    test("Front-End-Https", String.class, "on");
  }

  @Test
  public void testXHttpMethodOverride() {
    test("X-Http-Method-Override", String.class, "POST", "GET", "PUT", "PATCH");
  }

  @Test
  public void testXATTDeviceId() {
    test("X-ATT-DeviceId", String.class, "GT-P7320/P7320XXLPG");
  }

  @Test
  public void testXWapProfile() {
    test("X-Wap-Profile", String.class, "http://wap.samsungmobile.com/uaprof/SGH-I777.xml");
  }

  @Test
  public void testProxyConnection() {
    test("Proxy-Connection", String.class, "keep-alive", "close");
  }

  @Test
  public void testXUIDH() {
    test("X-UIDH", String.class, "OTgxNTk2NDk0ADJVquRu5NS5+rSbBANlrp+13QL7CXLGsFHpMi4LsUHw");
  }

  @Test
  public void testXCsrfToken() {
    test("X-Csrf-Token", String.class, "i8XNjC4b8KVok4uw5RftR38Wgp2BFwql");
  }

  @Test
  public void testXRequestID() {
    test("X-Request-ID", String.class, "f058ebd6-02f7-4d3f-942e-904344e8cde5");
  }

  @Test
  public void testXCorrelationID() {
    test("X-Correlation-ID", String.class, "f058ebd6-02f7-4d3f-942e-904344e8cde5");
  }

  @Test
  public void testSaveData() {
    test("Save-Data", String.class, "on");
  }

  @Test
  public void testAcceptPatch() {
    test("Accept-Patch", MediaType.class, "application/example", "text/example", "text/example;charset=utf-8", "application/merge-patch+json");
  }

  @Test
  public void testAcceptRanges() {
    test("Accept-Ranges", String.class, "none", "bytes");
  }

  @Test
  public void testAccessControlAllowCredentials() {
    test("Access-Control-Allow-Credentials", Boolean.class, "true", "false");
  }

  @Test
  public void testAccessControlAllowHeaders() {
    test("Access-Control-Allow-Headers", String.class, "X-Custom-Header", "*", "Upgrade-Insecure-Requests", "Accept");
  }

  @Test
  public void testAccessControlAllowMethods() {
    test("Access-Control-Allow-Methods", String.class, "POST", "GET", "OPTIONS", "*");
  }

  @Test
  public void testAccessControlAllowOrigin() {
    test("Access-Control-Allow-Origin", String.class, "null", "*", "https://developer.mozilla.org");
  }

  @Test
  public void testAccessControlExposeHeaders() {
    test("Access-Control-Expose-Headers", String.class, "*", "Content-Encoding", "X-Kuma-Revision", "Authorization");
  }

  @Test
  public void testAccessControlMaxAge() {
    test("Access-Control-Max-Age", Long.class, "600", "0", "398928932");
  }

  @Test
  public void testAge() {
    test("Age", Long.class, "600", "0", "398928932");
  }

  @Test
  public void testAltSvc() {
    test("Alt-Svc", String.class, "h2=\":443\"; ma=2592000;", "h2=\":443\"; ma=2592000; persist=1", "h2=\"alt.example.com:443\", h2=\":443\"", "h3-25=\":443\"; ma=3600, h2=\":443\"; ma=3600");
  }

  @Test
  public void testContentRange() {
    test("Content-Range", String.class, "bytes 200-1000/67589", "bytes */43892");
  }

  @Test
  public void testContentSecurityPolicy() {
    test("Content-Security-Policy", String.class, "default-src https:", "default-src 'self' http://example.com; connect-src 'none';", "onnect-src http://example.com/; script-src http://example.com/", "default-src https: 'unsafe-eval' 'unsafe-inline'; object-src 'none'");
  }

  @Test
  public void testDeltaBase() {
    test("Delta-Base", String.class, "abc");
  }

  @Test
  public void testIM() {
    test("IM", String.class, "feed");
  }

  @Test
  public void testP3P() {
    test("P3P", String.class, "CP=\"This is not a P3P policy! See https://en.wikipedia.org/wiki/Special:CentralAutoLogin/P3P for more info.\"");
  }

  @Test
  public void testProxyAuthenticate() {
    test("Proxy-Authenticate", String.class, "Basic", "Basic realm=\"Access to the internal site\"");
  }

  @Test
  public void testPublicKeyPins() {
    test("Public-Key-Pins", String.class, "max-age=2592000; pin-sha256=\"E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=\";");
  }

  @Test
  public void testRefresh() {
    test("Refresh", String.class, "5; url=http://www.w3.org/pub/WWW/People.html");
  }

  @Test
  public void testServer() {
    test("Server", String.class, "Apache/2.4.1 (Unix)");
  }

  @Test
  public void testStatus() {
    test("Status", Response.StatusType.class, "200 OK");
  }

  @Test
  public void testStrictTransportSecurity() {
    test("Strict-Transport-Security", String.class, "max-age=16070400; includeSubDomains");
  }

  @Test
  public void testTimingAllowOrigin() {
    test("Timing-Allow-Origin", String.class, "*", "https://developer.mozilla.org");
  }

  @Test
  public void testTk() {
    test("Tk", Tk.class, "N", "G", "!");
  }

  @Test
  public void testTrailer() {
    test("Trailer", String.class, "Expires", "Accept");
  }

  @Test
  public void testTransferEncoding() {
    test("Transfer-Encoding", String.class, "chunked", "compress", "deflate", "gzip", "identity");
  }

  @Test
  public void testXContentDuration() {
    test("X-Content-Duration", BigDecimal.class, "0", "0.32", "294.24", "32.439");
  }

  @Test
  public void testXContentSecurityPolicy() {
    test("X-Content-Security-Policy", String.class, "default-src 'self'");
  }

  @Test
  public void testXContentTypeOptions() {
    test("X-Content-Type-Options", String.class, "nosniff");
  }

  @Test
  public void testXFrameOptions() {
    test("X-Frame-Options", String.class, "deny");
  }

  @Test
  public void testXPoweredBy() {
    test("X-Powered-By", String.class, "PHP/5.4.0");
  }

  @Test
  public void testXUACompatible() {
    test("X-UA-Compatible", String.class, "IE=edge", "IE=EmulateIE7", "Chrome=1");
  }

  @Test
  public void testXWebKitCSP() {
    test("X-WebKit-CSP", String.class, "default-src 'self'");
  }

  @Test
  public void testXXSSProtection() {
    test("X-XSS-Protection", String.class, "1; mode=block");
  }

  @Test
  public void testAllow() {
    test(HttpHeaders.ALLOW, String.class, "GET", "POST", "HEAD");
  }

  @Test
  public void testContentDisposition() {
    test(HttpHeaders.CONTENT_DISPOSITION, String.class, "inline", "attachment", "attachment; filename=\"filename.jpg\"");
  }

  @Test
  public void testContentEncoding() {
    test(HttpHeaders.CONTENT_ENCODING, String.class, "gzip", "compress", "deflate", "br");
  }

  @Test
  public void testContentLanguage() {
    test(HttpHeaders.CONTENT_LANGUAGE, Locale.class, "de-DE", "de", "en-US", "en", "de-DE,", "en-CA");
  }

  @Test
  public void testContentLocation() {
    test(HttpHeaders.CONTENT_LOCATION, String.class, "/documents/foo.json", "/documents/foo.xml", "/documents/foo.txt", "");
  }

  @Test
  public void testEtag() {
    test(HttpHeaders.ETAG, String.class, "\"33a64df551425fcc55e4d42a148795d9f25f89d4\"", "W/\"33a64df551425fcc55e4d42a148795d9f25f89d4\"");
  }

  @Test
  public void testExpires() {
    test(HttpHeaders.EXPIRES, Date.class, "Thu, 31 May 2007 20:35:00 GMT", "Tue, 10 Aug 2010 09:56:13 GMT", "Tue, 11 Sep 2001 20:35:00 GMT");
  }

  @Test
  public void testLastModified() {
    test(HttpHeaders.LAST_MODIFIED, Date.class, "Thu, 31 May 2007 20:35:00 GMT", "Tue, 10 Aug 2010 09:56:13 GMT", "Tue, 11 Sep 2001 20:35:00 GMT");
  }

  @Test
  public void testLink() {
    test(HttpHeaders.LINK, String.class, "<https://example.com>; rel=\"preconnect\"", "<https://one.example.com>; rel=\"preconnect\"", "<https://two.example.com>; rel=\"preconnect\"", "<https://three.example.com>; rel=\"preconnect\"");
  }

  @Test
  public void testLocation() {
    test(HttpHeaders.LOCATION, URI.class, "/index.html", "http://www.w3.org/pub/WWW/People.html");
  }

  @Test
  public void testRetryAfter() {
    test(HttpHeaders.RETRY_AFTER, null, "120", "Fri, 07 Nov 2014 23:59:59 GMT");
  }

  @Test
  public void testSetCookie() {
    // FIXME: This does not properly handle multiple cookies being set in one Set-Cookie header, i.e.:
    // FIXME: hest2=spam, pony2=spam, sovs2=spam; expires=Wed, 04-May-2011 07:51:27 GMT, NO_CACHE=Y; expires=Wed, 04-May-2011 07:56:27 GMT; path=/; domain=.something.d6.revealit.dk
    test(HttpHeaders.SET_COOKIE, NewCookie.class, "sessionId=38afes7a8", "id=a3fWa; Expires=Wed, 21 Oct 2015 07:28:00 GMT", "id=a3fWa; Max-Age=2592000", "qwerty=219ffwef9w0f; Domain=somecompany.co.uk", "sessionId=e8bb43229de9; Domain=foo.example.com", "__Secure-ID=123; Secure; Domain=example.com", "__Host-ID=123; Secure; Path=/", "__Secure-id=1", "__Host-id=1; Secure", "__Host-id=1; Secure; Path=/; Domain=example.com", "LSID=DQAAAK…Eaem_vYg; Path=/accounts; Expires=Wed, 13 Jan 2021 22:23:01 GMT; Secure; HttpOnly", "HSID=AYQEVn…DKrdst; Domain=.foo.com; Path=/; Expires=Wed, 13 Jan 2021 22:23:01 GMT; HttpOnly", "SSID=Ap4P…GTEq; Domain=foo.com; Path=/; Expires=Wed, 13 Jan 2021 22:23:01 GMT; Secure; HttpOnly", "lu=Rg3vHJZnehYLjVg7qi3bZjzg; Expires=Tue, 15 Jan 2013 21:47:38 GMT; Path=/; Domain=.example.com; HttpOnly", "made_write_conn=1295214458; Path=/; Domain=.example.com", "reg_fb_gate=deleted; Expires=Thu, 01 Jan 1970 00:00:01 GMT; Path=/; Domain=.example.com; HttpOnly");
  }

  @Test
  public void testVary() {
    test(HttpHeaders.VARY, String.class, "*", "Accept-Language", "User-Agent");
  }

  @Test
  public void testWwwAuthenticate() {
    test(HttpHeaders.WWW_AUTHENTICATE, String.class, "Basic realm=\"Access to the staging site\", charset=\"UTF-8\"", "Basic");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testStory() {
    final HttpHeadersImpl headers = new HttpHeadersImpl();

    final String acceptName = HttpHeaders.ACCEPT;
    final MediaType acceptValue1 = MediaTypes.parse("application/json;q=.5");
    headers.getMirrorMap().putSingle(acceptName, acceptValue1);
    assertEquals(acceptValue1, headers.getMirrorMap().getFirst(acceptName));
    assertEquals(acceptValue1.toString(), headers.getFirst(acceptName));

    final MediaType acceptValue2 = MediaTypes.parse("application/xml;q=.6");
    headers.add(acceptName, acceptValue2.toString());
    assertEquals(acceptValue2, headers.getMirrorMap().getFirst(acceptName));
    assertEquals(acceptValue2.toString(), headers.getFirst(acceptName));

    final String contentTypeName = HttpHeaders.CONTENT_TYPE;
    final List<Object> contentTypes = new ArrayList<>();
    final MediaType contentType1 = MediaTypes.parse("text/html; charset=UTF-8; q=.1");
    contentTypes.add(contentType1);
    headers.getMirrorMap().put(contentTypeName, contentTypes);
//    contentTypes.add(Boolean.TRUE);

    assertEquals(contentType1, headers.getMirrorMap().getFirst(contentTypeName));
    assertEquals(contentType1.toString(), headers.getFirst(contentTypeName));

    final MediaType contentType2 = MediaTypes.parse("text/plain; charset=UTF-8; q=.9");
    headers.get(contentTypeName).add(contentType2.toString());
    assertEquals(contentType2, headers.getMirrorMap().getFirst(contentTypeName));
    assertEquals(contentType2.toString(), headers.getFirst(contentTypeName));

    final MediaType contentType3 = MediaTypes.parse("text/xml; charset=UTF-8; q=1");
    headers.get(contentTypeName).getMirrorList().add(contentType3);
    assertEquals(contentType3, headers.getMirrorMap().getFirst(contentTypeName));
    assertEquals(contentType3.toString(), headers.getFirst(contentTypeName));

    final MediaType contentType4 = MediaTypes.parse("text/csv; charset=UTF-8; q=.2");
    headers.get(contentTypeName).add(contentType4.toString());
    assertEquals(contentType3, headers.getMirrorMap().getFirst(contentTypeName));
    assertEquals(contentType3.toString(), headers.getFirst(contentTypeName));

    final Iterator<Map.Entry<String,List<Object>>> mapIterator = headers.getMirrorMap().entrySet().iterator();
    final Map.Entry<String,?> entry = mapIterator.next();
    mapIterator.remove();

    final String key = entry.getKey();
    final MirrorQualityList<MediaType,String> list = (MirrorQualityList<MediaType,String>)entry.getValue();
    assertFalse(headers.containsKey(key));

    final ListIterator<MediaType> listIterator = list.listIterator();
    final MediaType value = listIterator.next();

    listIterator.remove();
    assertFalse(list.contains(value));
    assertFalse(list.getMirrorList().contains(value.toString()));

    list.getMirrorList().add(value.toString());
    assertTrue(list.contains(value));

    headers.put(key, list.getMirrorList());
    assertTrue(headers.getMirrorMap().containsKey(key));

    assertSame(list.getMirrorList(), headers.get(key));
    assertSame(list, headers.getMirrorMap().get(key));
  }

  @Test
  public void testParseMultiHeaderNoSort() {
    final String value = "text/html,text/html,application/xhtml+xml,text/html,application/xhtml+xml,application/xml;q=0.9,text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9";
    final String[] expected = value.split(",");
    final ArrayList<String> values = new ArrayList<>(expected.length);
    HttpHeadersImpl.parseMultiHeaderNoSort(values, value, ',');
    final String[] actual = values.toArray(new String[values.size()]);
    assertArrayEquals(expected, actual);
  }
}