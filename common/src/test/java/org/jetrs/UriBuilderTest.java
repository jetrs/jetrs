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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.junit.Assert;
import org.junit.Test;

public class UriBuilderTest extends RuntimeDelegateTest {
  @Test
  public void testExceptions() {
    try {
      UriBuilder.fromUri(":cts:8080//tck:90090//jaxrs ");
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }

    UriBuilder.fromUri("urn:isbn:096139210x");
  }

  @Test
  public void testNullReplaceQuery() {
    final UriBuilder builder = UriBuilder.fromUri("/foo?a=b&bar=foo");
    builder.replaceQueryParam("bar", (Object[])null);
    final URI uri = builder.build();
    assertEquals("/foo?a=b", uri.toString());
  }

  @Test
  public void testNullHost() {
    final UriBuilder builder = UriBuilder.fromUri("http://example.com/foo/bar");
    builder.scheme(null);
    builder.host(null);
    final URI uri = builder.build();
    Assert.assertEquals("/foo/bar", uri.toString());
  }

  @Test
  public void testTemplate() throws UriBuilderException, URISyntaxException {
    UriBuilder builder = UriBuilder.fromUri("http://{host}/x/y/{path}?{q}={qval}");
    String template = builder.toTemplate();
    Assert.assertEquals("http://{host}/x/y/{path}?{q}={qval}", template);

    builder = builder.resolveTemplate("host", "localhost");
    template = builder.toTemplate();
    Assert.assertEquals("http://localhost/x/y/{path}?{q}={qval}", template);

    builder = builder.resolveTemplate("q", "name");
    template = builder.toTemplate();
    Assert.assertEquals("http://localhost/x/y/{path}?name={qval}", template);

    final Map<String,Object> values = new HashMap<>();
    values.put("path", "z");
    values.put("qval", 42);
    builder = builder.resolveTemplates(values);
    template = builder.toTemplate();
    Assert.assertEquals("http://localhost/x/y/z?name=42", template);

    builder = UriBuilder.fromUri("{id: [0-9]+}");
    Assert.assertEquals(new URI("123"), builder.build("123"));

    builder = UriBuilder.fromUri("{id: [0-9]+}");
    Assert.assertEquals(new URI("abcd"), builder.build("abcd"));

    builder = UriBuilder.fromUri("/resources/{id: [0-9]+}");
    Assert.assertEquals(new URI("/resources/123"), builder.build("123"));
  }

  @Test
  public void testFromPath() {
    assertEquals("/$a", UriBuilder.fromPath("/{p}").build("$a").toString());
  }

  @Test
  public void testReplaceQueryParam() {
    assertEquals("?param=&otherParam=otherValue", UriBuilderImpl.fromUri("?param=").replaceQueryParam("otherParam", "otherValue").build().toString());
  }

  @Test
  public void testEmoji() {
    final UriBuilder builder = UriBuilder.fromPath("/my/url");
    builder.queryParam("msg", "emoji stuff %EE%81%96%EE%90%8F");
    Assert.assertEquals("/my/url?msg=emoji+stuff+%EE%81%96%EE%90%8F", builder.build().toString());
  }

  @Test
  public void testQuery() {
    final UriBuilder builder = UriBuilder.fromPath("/foo");
    builder.queryParam("mama", "   ");
    Assert.assertEquals("/foo?mama=+++", builder.build().toString());
  }

  @Test
  public void testQuery2() {
    final UriBuilder builder = UriBuilder.fromUri("http://localhost/test");
    builder.replaceQuery("a={b}");
    Assert.assertEquals("http://localhost/test?a=%3D", builder.build("=").toString());
  }

  @Test
  public void testReplaceScheme() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .scheme("https")
      .build();
    Assert.assertEquals(URI.create("https://localhost:8080/a/b/c"), uri);
  }

  @Test
  public void testReplaceUserInfo() {
    final URI uri = UriBuilder
      .fromUri("http://bob@localhost:8080/a/b/c")
      .userInfo("sue")
      .build();
    Assert.assertEquals(URI.create("http://sue@localhost:8080/a/b/c"), uri);
  }

  @Test
  public void testReplaceHost() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .host("a.com")
      .build();
    Assert.assertEquals(URI.create("http://a.com:8080/a/b/c"), uri);
  }

  @Test
  public void testSetPort() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .port(9090)
      .build();
    Assert.assertEquals(URI.create("http://localhost:9090/a/b/c"), uri);
  }

  @Test
  public void testUnsetPort() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .port(-1)
      .build();
    Assert.assertEquals(URI.create("http://localhost/a/b/c"), uri);
  }

  @Test
  public void testReplacePath() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .replacePath("/x/y/z")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/x/y/z"), uri);
  }

  @Test
  public void testReplaceMatrixParam() {
    URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c;a=x;b=y")
      .replaceMatrix("x=a;y=b")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;x=a;y=b"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a")
      .path("/{b:A{0:10}}/c;a=x;b=y")
      .replaceMatrixParam("a", "1", "2")
      .build("b");
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;b=y;a=1;a=2"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c;a=x;b=y")
      .replaceMatrixParam("a")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;b=y"), uri);
  }

  @Test
  public void testReplaceQueryParams() {
    URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c?a=x&b=y")
      .replaceQuery("x=a&y=b")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?x=a&y=b"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c?a=x&b=y")
      .replaceQueryParam("a", "1", "2")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?b=y&a=1&a=2"), uri);
  }

  @Test
  public void testReplaceFragment() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c?a=x&b=y#frag")
      .fragment("ment")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y#ment"), uri);
  }

  @Test
  public void testReplaceUri() {
    final URI uri = URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#frag");

    UriBuilder builder = UriBuilder
      .fromUri(uri)
      .uri(URI.create("https://bob@localhost:8080"));
    Assert.assertEquals(URI.create("https://bob@localhost:8080/a/b/c?a=x&b=y#frag"), builder.build());

    builder = UriBuilder
      .fromUri(uri)
      .uri(URI.create("https://sue@localhost:8080"));
    Assert.assertEquals(URI.create("https://sue@localhost:8080/a/b/c?a=x&b=y#frag"), builder.build());

    builder = UriBuilder
      .fromUri(uri)
      .uri(URI.create("https://sue@localhost:9090"));
    Assert.assertEquals(URI.create("https://sue@localhost:9090/a/b/c?a=x&b=y#frag"), builder.build());

    builder = UriBuilder
      .fromUri(uri)
      .uri(URI.create("/x/y/z"));
    Assert.assertEquals(URI.create("http://bob@localhost:8080/x/y/z?a=x&b=y#frag"), builder.build());

    builder = UriBuilder
      .fromUri(uri)
      .uri(URI.create("?x=a&b=y"));
    Assert.assertEquals(URI.create("http://bob@localhost:8080/a/b/c?x=a&b=y#frag"), builder.build());

    builder = UriBuilder
      .fromUri(uri)
      .uri(URI.create("#ment"));
    Assert.assertEquals(URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#ment"), builder.build());
  }

  @Test
  public void testSchemeSpecificPart() {
    final URI uri = UriBuilder
      .fromUri(URI.create("http://bob@localhost:8080/a/b/c?a=x&b=y#frag"))
      .schemeSpecificPart("//sue@remotehost:9090/x/y/z?x=a&y=b")
      .build();
    Assert.assertEquals(URI.create("http://sue@remotehost:9090/x/y/z?x=a&y=b#frag"), uri);
  }

  @Test
  public void testAppendPath() {
    URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c/")
      .path("/")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c/")
      .path("/x/y/z")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .path("/x/y/z")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .path("x/y/z")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .path("/")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .path("")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/a%20/b%20/c%20")
      .path("/x /y /z ")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a%20/b%20/c%20/x%20/y%20/z%20"), uri);
  }

  @Test
  public void testAppendQueryParams() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c?a=x&b=y")
      .queryParam("c", "z")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y&c=z"), uri);
  }

  @Test
  public void testQueryParamsEncoding() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c?a=x&b=y")
      .queryParam("c", "z=z/z")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c?a=x&b=y&c=z%3Dz%2Fz"), uri);
  }

  @Test
  public void testAppendMatrixParams() {
    final URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c;a=x;b=y")
      .matrixParam("c", "z")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c;a=x;b=y;c=z"), uri);
  }

  @Test
  public void testResourceAppendPath() throws UriBuilderException, NoSuchMethodException {
    URI uri = UriBuilder
      .fromUri("http://localhost:8080/base")
      .path(UriBuilderResource.class)
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/base/resource"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/base")
      .path(UriBuilderResource.class, "get")
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/base/method"), uri);

    uri = UriBuilder
      .fromUri("http://localhost:8080/base")
      .path(UriBuilderResource.class.getMethod("get"))
      .path(UriBuilderResource.class.getMethod("locator"))
      .build();
    Assert.assertEquals(URI.create("http://localhost:8080/base/method/locator/"), uri);
  }

  @Test
  public void testTemplates() {
    URI uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .path("/{foo}/{bar}/{baz}/{foo}")
      .build("x", "y", "z");
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x"), uri);

    final Map<String,Object> map = new HashMap<>();
    map.put("foo", "x");
    map.put("bar", "y");
    map.put("baz", "z");
    uri = UriBuilder
      .fromUri("http://localhost:8080/a/b/c")
      .path("/{foo}/{bar}/{baz}/{foo}")
      .buildFromMap(map);
    Assert.assertEquals(URI.create("http://localhost:8080/a/b/c/x/y/z/x"), uri);
  }

  @Test
  public void testClone() {
    final UriBuilder ub = UriBuilder
      .fromUri("http://user@localhost:8080/?query#fragment")
      .path("a");

    final URI full = ub.clone().path("b").build();
    final URI base = ub.build();

    Assert.assertEquals(URI.create("http://user@localhost:8080/a?query#fragment"), base);
    Assert.assertEquals(URI.create("http://user@localhost:8080/a/b?query#fragment"), full);
  }

  @Test
  public void testFromUri() {
    final String[] uris = {"mailto:java-net@java.sun.com", "ftp://ftp.is.co.za/rfc/rfc1808.txt", "news:comp.lang.java", "urn:isbn:096139210x", "http://www.ietf.org/rfc/rfc2396.txt", "ldap://[2001:db8::7]/c=GB?objectClass?one", "tel:+1-816-555-1212", "telnet://192.0.2.16:80/", "foo://example.com:8042/over/there?name=ferret#nose"};
    for (int j = 0; j < uris.length; ++j) { // [A]
      final URI uri = UriBuilder.fromUri(uris[j]).build();
      assertEquals("Test failed for expected uri: " + uris[j] + " Got " + uri.toString() + " instead", 0, uri.toString().trim().compareToIgnoreCase(uris[j]));
    }
  }

  @Test
  public void testEncoding1() {
    final UriBuilder builder = UriBuilder.fromPath("/foo/{id}");
    final URI uri = builder.buildFromMap(Collections.singletonMap("id", "something %%20something"));
    Assert.assertEquals("/foo/something%20%25%2520something", uri.toString());
  }

  @Test
  public void testEncoding2() {
    final UriBuilder builder = UriBuilder.fromPath("/foo/{id}");
    final URI uri = builder.buildFromMap(Collections.singletonMap("id", "something something"));
    Assert.assertEquals("/foo/something%20something", uri.toString());
  }

  @Test
  public void testEncoding3() {
    final UriBuilder builder = UriBuilder.fromPath("/foo/{id}");
    final URI uri = builder.buildFromEncodedMap(Collections.singletonMap("id", "something%20something"));
    Assert.assertEquals("/foo/something%20something", uri.toString());
  }

  @Test
  public void testQueryParamSubstitution() {
    assertEquals("http://localhost/test?a=c", UriBuilder.fromUri("http://localhost/test").queryParam("a", "{b}").build("c").toString());
  }

  @Test
  public void testEncodedMap1() {
    final Map<String,String> map = new HashMap<>();
    map.put("x", "x%20yz");
    map.put("y", "/path-absolute/%test1");
    map.put("z", "fred@example.com");
    map.put("w", "path-rootless/test2");

    final String expectedPath = "path-rootless/test2/x%20yz//path-absolute/%25test1/fred@example.com/x%20yz";
    final URI uri = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromEncodedMap(map);
    assertEquals("Test failed for expected path: " + expectedPath + " Got " + uri.getRawPath() + " instead\n", 0, uri.getRawPath().compareToIgnoreCase(expectedPath));
  }

  @Test
  public void testEncodedMapTest3() {
    final Map<String,String> map = new HashMap<>();
    map.put("x", null);
    map.put("y", "/path-absolute/test1");
    map.put("z", "fred@example.com");
    map.put("w", "path-rootless/test2");
    map.put("u", "extra");

    try {
      UriBuilder
        .fromPath("")
        .path("{w}/{x}/{y}/{z}/{x}")
        .buildFromEncodedMap(map);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testEncodedMapTest4() {
    final Map<String,String> map = new HashMap<>();
    map.put("x", "x%yz");
    map.put("y", "/path-absolute/test1");
    map.put("z", "fred@example.com");
    map.put("w", "path-rootless/test2");
    map.put("u", "extra");

    try {
      UriBuilder
        .fromPath("")
        .path("{w}/{v}/{x}/{y}/{z}/{x}")
        .buildFromEncodedMap(map);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testBuildFromMapTest1() {
    final Map<String,String> map = new HashMap<>();
    map.put("x", "x%yz");
    map.put("y", "/path-absolute/test1");
    map.put("z", "fred@example.com");
    map.put("w", "path-rootless/test2");

    final String expected_path = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%25yz";
    final URI uri = UriBuilder
      .fromPath("")
      .path("{w}/{x}/{y}/{z}/{x}")
      .buildFromMap(map);
    assertEquals("Test failed for expected path: " + expected_path + " Got " + uri.getRawPath() + " instead\n", 0, uri.getRawPath().compareToIgnoreCase(expected_path));
  }

  @Test
  public void testBuildFromMapTest2() {
    final Map<String,String> map = new HashMap<>();
    map.put("x", "x%yz");
    map.put("y", "/path-absolute/test1");
    map.put("z", "fred@example.com");
    map.put("w", "path-rootless/test2");
    map.put("u", "extra");

    final String expected_path = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%25yz";
    final URI uri = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromMap(map);
    assertEquals("Test failed for expected path: " + expected_path + " Got " + uri.getRawPath() + " instead" + "\n", 0, uri.getRawPath().compareToIgnoreCase(expected_path));
  }

  @Test
  public void testBuildFromMapTest3() {
    final Map<String,String> map = new HashMap<>();
    map.put("x", null);
    map.put("y", "/path-absolute/test1");
    map.put("z", "fred@example.com");
    map.put("w", "path-rootless/test2");
    map.put("u", "extra");

    try {
      UriBuilder
        .fromPath("")
        .path("{w}/{x}/{y}/{z}/{x}")
        .buildFromMap(map);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testBuildFromMapTest4() {
    final Map<String,String> map = new HashMap<>();
    map.put("x", "x%yz");
    map.put("y", "/path-absolute/test1");
    map.put("z", "fred@example.com");
    map.put("w", "path-rootless/test2");
    map.put("u", "extra");
    try {
      UriBuilder
        .fromPath("")
        .path("{w}/{v}/{x}/{y}/{z}/{x}")
        .buildFromMap(map);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testBuildFromMapTest51() {
    final Map<String,String> map = new HashMap<>();
    map.put("x", "x%yz");
    map.put("y", "/path-absolute/test1");
    map.put("z", "fred@example.com");
    map.put("w", "path-rootless/test2");

    final String expectedPath = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%25yz";
    final UriBuilder builder = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}");
    URI uri = builder.buildFromMap(map);

    assertEquals("Test failed for expected path: " + expectedPath + " Got " + uri.getRawPath() + " instead" + "\n", 0, uri.getRawPath().compareToIgnoreCase(expectedPath));

    Map<String,String> map2 = new HashMap<>();
    map2.put("x", "x%20yz");
    map2.put("y", "/path-absolute/test1");
    map2.put("z", "fred@example.com");
    map2.put("w", "path-rootless/test2");
    uri = builder.buildFromMap(map2);

    final String expectedPath2 = "path-rootless%2Ftest2/x%2520yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%2520yz";
    assertEquals("Test failed for expected path: " + expectedPath2 + " got " + uri.getRawPath() + " instead", 0, uri.getRawPath().compareToIgnoreCase(expectedPath2));

    final Map<String,String> map3 = new HashMap<>();
    map3.put("x", "x%yz");
    map3.put("y", "/path-absolute/test1");
    map3.put("z", "fred@example.com");
    map3.put("w", "path-rootless/test2");
    map3.put("v", "xyz");
    uri = builder.buildFromMap(map3);

    final String expectedPath3 = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2Ftest1/fred@example.com/x%25yz";
    assertEquals("Test failed for expected path: " + expectedPath3 + " Got " + uri.getRawPath() + " instead" + "\n", 0, uri.getRawPath().compareToIgnoreCase(expectedPath3));
  }

  @Test
  public void testFromEncodedTest1() {
    String expectedValue1 = "http://localhost:8080/a/%25/=/%25G0/%25/=";
    String expectedValue2 = "http://localhost:8080/xy/%20/%25/xy";
    URI uri = UriBuilder.fromPath("http://localhost:8080").path("/{v}/{w}/{x}/{y}/{z}/{x}").buildFromEncoded("a", "%25", "=", "%G0", "%", "23");
    assertTrue("Incorrec URI returned: " + uri.toString() + ", expecting " + expectedValue1, uri.toString().equalsIgnoreCase(expectedValue1));

    uri = UriBuilder.fromPath("http://localhost:8080").path("/{x}/{y}/{z}/{x}").buildFromEncoded("xy", " ", "%");
    assertTrue("Incorrec URI returned: " + uri.toString() + ", expecting " + expectedValue2, uri.toString().equalsIgnoreCase(expectedValue2));
  }

  @Test
  public void testQueryParamTest1() {
    try {
      UriBuilder.fromPath("http://localhost:8080").queryParam(null, "x", "y");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException ilex) {
    }
  }

  @Test
  public void testQueryParam() {
    final String expectedValue = "http://localhost:8080?name=x%3D&name=y?&name=x+y&name=%26";
    final URI uri = UriBuilder.fromPath("http://localhost:8080").queryParam("name", "x=", "y?", "x y", "&").build();
    assertTrue("Incorrec URI returned: " + uri.toString() + ", expecting " + expectedValue + "\n", uri.toString().equalsIgnoreCase(expectedValue));
  }

  @Test
  public void testReplaceQueryTest3() {
    final String expectedValue = "http://localhost:8080?name1=x&name2=%20&name3=x+y&name4=23&name5=x%20y";
    final URI uri = UriBuilder.fromPath("http://localhost:8080").queryParam("name", "x=", "y?", "x y", "&").replaceQuery("name1=x&name2=%20&name3=x+y&name4=23&name5=x y").build();
    assertTrue("Incorrec URI returned: " + uri.toString() + ", expecting " + expectedValue, uri.toString().equalsIgnoreCase(expectedValue));
  }

  @Test
  public void testReplaceQueryParamTest2() {
    final String expectedValue = "http://localhost:8080";
    final String param = "name";
    final URI uri = UriBuilder
      .fromPath("http://localhost:8080")
      .queryParam(param, "x=", "y?", "x y", "&")
      .replaceQueryParam(param).build();
    assertTrue("Incorrec URI returned: " + uri.toString() + ", expecting " + expectedValue + "\n", uri.toString().equalsIgnoreCase(expectedValue));
  }

  @Test
  public void testPathEncoding() {
    final UriBuilder builder = UriBuilder.fromUri("http://{host}").path("{d}");

    URI uri = builder.build("A/B", "C/D");
    Assert.assertEquals("http://A%2FB/C%2FD", uri.toString());

    uri = builder.buildFromEncoded("A/B", "C/D");
    Assert.assertEquals("http://A/B/C/D", uri.toString());

    final Object[] params = {"A/B", "C/D"};
    uri = builder.build(params, false);
    Assert.assertEquals("http://A/B/C/D", uri.toString());

    final HashMap<String,Object> map = new HashMap<>();
    map.put("host", "A/B");
    map.put("d", "C/D");

    uri = builder.buildFromMap(map);
    Assert.assertEquals("http://A%2FB/C%2FD", uri.toString());

    uri = builder.buildFromEncodedMap(map);
    Assert.assertEquals("http://A/B/C/D", uri.toString());

    uri = builder.buildFromMap(map, false);
    Assert.assertEquals("http://A/B/C/D", uri.toString());
  }

  @Test
  public void testPercentage() {
    final UriBuilder path = UriBuilder.fromUri("http://foo.bar:678/resource").path("{path}");
    final String template = path.resolveTemplate("path", "%42%5A%61%7a%2F%%21", false).toTemplate();
    assertEquals("http://foo.bar:678/resource/%2542%255A%2561%257a%252F%25%2521", template);
  }

  @Test
  public void testWithSlashTrue() {
    final Object[] values = {"path-rootless/test2", new StringBuilder("x%yz"), "/path-absolute/%25test1", "fred@example.com"};
    final URI uri = UriBuilder.fromPath("").path("{v}/{w}/{x}/{y}/{w}").build(new Object[] {values[0], values[1], values[2], values[3], values[1]}, true);
    Assert.assertEquals("path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2F%2525test1/fred@example.com/x%25yz", uri.getRawPath());
  }

  @Test
  public void testNull() {
    try {
      UriBuilder.fromUri((String)null);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException ilex) {
    }
  }

  @Test
  public void testNullMatrixParam() {
    try {
      UriBuilder.fromPath("http://localhost:8080").matrixParam(null, "x", "y");
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }

    try {
      UriBuilder.fromPath("http://localhost:8080").matrixParam("name", (Object)null);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testReplaceMatrixParam2() {
    final String expected = "http://localhost:8080;name=x=;name=y%3F;name=x%20y;name=&;name1=x;name1=y;name1=y%20x;name1=x%25y;name1=%20";
    final URI uri = UriBuilder.fromPath("http://localhost:8080;name=x=;name=y?;name=x y;name=&").replaceMatrixParam("name1", "x", "y", "y x", "x%y", "%20").build();
    Assert.assertEquals(expected, uri.toString());
  }

  @Test
  public void testReplaceMatrixParam3() {
    final String expected = "http://localhost:8080;name=x;name=y;name=y%20x;name=x%25y;name=%20";
    final URI uri = UriBuilder.fromPath("http://localhost:8080;name=x=;name=y?;name=x y;name=&").replaceMatrixParam("name", "x", "y", "y x", "x%y", "%20").build();
    Assert.assertEquals(expected, uri.toString());
  }

  @Test
  public void testReplaceMatrixParam4() {
    final String expected = "http://localhost:8080;";
    final URI uri = UriBuilder.fromPath("http://localhost:8080").matrixParam("name", "x=", "y?", "x y", "&").replaceMatrix(null).build();
    Assert.assertEquals(expected, uri.toString());
  }

  @Test
  public void testReplaceMatrixParam5() {
    final String expected = "http://localhost:8080;name=x;name=y;name=y%20x;name=x%25y;name=%20";
    final String value = "name=x;name=y;name=y x;name=x%y;name= ";

    final URI uri = UriBuilder.fromPath("http://localhost:8080;name=x=;name=y?;name=x y;name=&").replaceMatrix(value).build();
    Assert.assertEquals(expected, uri.toString());
  }

  @Test
  public void testReplaceMatrixParam6() {
    final String expected = "http://localhost:8080;name1=x;name1=y;name1=y%20x;name1=x%25y;name1=%20";
    final String value = "name1=x;name1=y;name1=y x;name1=x%y;name1= ";

    final URI uri = UriBuilder.fromPath("http://localhost:8080;name=x=;name=y?;name=x y;name=&").replaceMatrix(value).build();
    Assert.assertEquals(expected, uri.toString());
  }

  @Test
  public void testUriReplace() throws URISyntaxException {
    final String original = "foo://example.com:8042/over/there?name=ferret#nose";
    final String expected = "http://example.com:8042/over/there?name=ferret#nose";
    final URI replacement = new URI("http", "//example.com:8042/over/there?name=ferret", null);

    final URI uri = UriBuilder.fromUri(new URI(original)).uri(replacement.toASCIIString()).build();
    Assert.assertEquals(expected, uri.toString());
  }

  @Test
  public void testUriReplace2() throws URISyntaxException {
    final String original = "tel:+1-816-555-1212";
    final String expected = "tel:+1-816-555-1212";
    final URI replacement = new URI("tel", "+1-816-555-1212", null);

    final UriBuilder uriBuilder = UriBuilder.fromUri(new URI(original));
    final URI uri = uriBuilder.uri(replacement.toASCIIString()).build();
    Assert.assertEquals(expected, uri.toString());
  }

  @Test
  public void testUriReplace3() throws URISyntaxException {
    final String original = "news:comp.lang.java";
    final String expected = "http://comp.lang.java";
    final URI replacement = new URI("http", "//comp.lang.java", null);

    final UriBuilder uriBuilder = UriBuilder.fromUri(new URI(original));
    final URI uri = uriBuilder.uri(replacement.toASCIIString()).build();
    Assert.assertEquals(uri.toString(), expected);
  }

  @Test
  public void testParse2() {
    final Matcher matcher = UriBuilderImpl.opaqueUri.matcher("mailto:bill@jboss.org");
    assertTrue(matcher.matches());
    assertEquals("mailto", matcher.group(1));
    assertEquals("bill@jboss.org", matcher.group(2));

    final String hierarchical = "http://foo.com";
    assertFalse(UriBuilderImpl.opaqueUri.matcher(hierarchical).matches());
  }

  @Test
  public void testColon() {
    assertEquals("http://foo.com/runtime/org.jbpm:HR:1.0/process/hiring/start", UriBuilder.fromUri("http://foo.com/runtime/org.jbpm:HR:1.0/process/hiring/start").build().toString());
  }

  @Test
  public void testIPv6() {
    Assert.assertEquals("http://foo", UriBuilder.fromUri("http://foo").build().toString());
    Assert.assertEquals("http://foo:8080", UriBuilder.fromUri("http://foo:8080").build().toString());
    Assert.assertEquals("http://[::1]:8080", UriBuilder.fromUri("http://[::1]:8080").build().toString());

    Assert.assertEquals("http://[0:0:0:0:0:0:0:1]:8080", UriBuilder.fromUri("http://[0:0:0:0:0:0:0:1]:8080").build().toString());
    Assert.assertEquals("http://foo", UriBuilder.fromUri("http://{host}").build("foo").toString());
    Assert.assertEquals("http://foo:8080", UriBuilder.fromUri("http://{host}:8080").build("foo").toString());

    Assert.assertEquals("http://[0:0:0:0:0:0:0:1]", UriBuilder.fromUri("http://[0:0:0:0:0:0:0:1]").build().toString());
    Assert.assertEquals("http://[::1]", UriBuilder.fromUri("http://[::1]").build().toString());

    // URI substitutes square brackets with their escaped representation
    Assert.assertEquals("http://%5B0:0:0:0:0:0:0:1%5D", UriBuilder.fromUri("http://{host}").build("[0:0:0:0:0:0:0:1]").toString());
    Assert.assertEquals("http://%5B0:0:0:0:0:0:0:1%5D:8080", UriBuilder.fromUri("http://{host}:8080").build("[0:0:0:0:0:0:0:1]").toString());

    // inspiration from https://stackoverflow.com/a/17871737
    Assert.assertEquals("http://[1:2:3:4:5:6:7:8]", UriBuilder.fromUri("http://[1:2:3:4:5:6:7:8]").build().toString());
    Assert.assertEquals("http://[1::]", UriBuilder.fromUri("http://[1::]").build().toString());
    Assert.assertEquals("http://[1:2:3:4:5:6:7::]", UriBuilder.fromUri("http://[1:2:3:4:5:6:7::]").build().toString());
    Assert.assertEquals("http://[1::8]", UriBuilder.fromUri("http://[1::8]").build().toString());
    Assert.assertEquals("http://[1:2:3:4:5:6::8]", UriBuilder.fromUri("http://[1:2:3:4:5:6::8]").build().toString());
    Assert.assertEquals("http://[1::7:8]", UriBuilder.fromUri("http://[1::7:8]").build().toString());
    Assert.assertEquals("http://[1:2:3:4:5::7:8]", UriBuilder.fromUri("http://[1:2:3:4:5::7:8]").build().toString());
    Assert.assertEquals("http://[1:2:3:4:5::8]", UriBuilder.fromUri("http://[1:2:3:4:5::8]").build().toString());
    Assert.assertEquals("http://[1::6:7:8]", UriBuilder.fromUri("http://[1::6:7:8]").build().toString());
    Assert.assertEquals("http://[1:2:3:4::6:7:8]", UriBuilder.fromUri("http://[1:2:3:4::6:7:8]").build().toString());
    Assert.assertEquals("http://[1:2:3:4::8]", UriBuilder.fromUri("http://[1:2:3:4::8]").build().toString());
    Assert.assertEquals("http://[1::5:6:7:8]", UriBuilder.fromUri("http://[1::5:6:7:8]").build().toString());
    Assert.assertEquals("http://[1:2:3::5:6:7:8]", UriBuilder.fromUri("http://[1:2:3::5:6:7:8]").build().toString());
    Assert.assertEquals("http://[1:2:3::8]", UriBuilder.fromUri("http://[1:2:3::8]").build().toString());
    Assert.assertEquals("http://[1::4:5:6:7:8]", UriBuilder.fromUri("http://[1::4:5:6:7:8]").build().toString());
    Assert.assertEquals("http://[1:2::4:5:6:7:8]", UriBuilder.fromUri("http://[1:2::4:5:6:7:8]").build().toString());
    Assert.assertEquals("http://[1:2::8]", UriBuilder.fromUri("http://[1:2::8]").build().toString());
    Assert.assertEquals("http://[1::3:4:5:6:7:8]", UriBuilder.fromUri("http://[1::3:4:5:6:7:8]").build().toString());
    Assert.assertEquals("http://[1::8]", UriBuilder.fromUri("http://[1::8]").build().toString());
    Assert.assertEquals("http://[::2:3:4:5:6:7:8]", UriBuilder.fromUri("http://[::2:3:4:5:6:7:8]").build().toString());
    Assert.assertEquals("http://[::3:4:5:6:7:8]", UriBuilder.fromUri("http://[::3:4:5:6:7:8]").build().toString());
    Assert.assertEquals("http://[::8]", UriBuilder.fromUri("http://[::8]").build().toString());
    Assert.assertEquals("http://[::]", UriBuilder.fromUri("http://[::]").build().toString());

    // link-local format
    Assert.assertEquals("http://[fe80::7:8%eth0]", UriBuilder.fromUri("http://[fe80::7:8%eth0]").build().toString());
    Assert.assertEquals("http://[fe80::7:8%1]", UriBuilder.fromUri("http://[fe80::7:8%1]").build().toString());
    Assert.assertEquals("http://[fe80::7:8%eth0]:8080", UriBuilder.fromUri("http://[fe80::7:8%eth0]:8080").build().toString());
    Assert.assertEquals("http://[fe80::7:8%1]:80", UriBuilder.fromUri("http://[fe80::7:8%1]:80").build().toString());

    Assert.assertEquals("http://[::255.255.255.255]", UriBuilder.fromUri("http://[::255.255.255.255]").build().toString());
    Assert.assertEquals("http://[::ffff:255.255.255.255]", UriBuilder.fromUri("http://[::ffff:255.255.255.255]").build().toString());
    Assert.assertEquals("http://[::ffff:0:255.255.255.255]", UriBuilder.fromUri("http://[::ffff:0:255.255.255.255]").build().toString());

    Assert.assertEquals("http://[2001:db8:3:4::192.0.2.33]", UriBuilder.fromUri("http://[2001:db8:3:4::192.0.2.33]").build().toString());
    Assert.assertEquals("http://[64:ff9b::192.0.2.33]", UriBuilder.fromUri("http://[64:ff9b::192.0.2.33]").build().toString());
  }

  @Test
  public void testReplaceNonAsciiQueryParam() throws MalformedURLException, URISyntaxException {
    final URL url = new URL("http://www.example.com/getMyName?néme=t");
    final UriBuilder builder = UriBuilder
      .fromPath(url.getPath())
      .scheme(url.getProtocol())
      .host(url.getHost())
      .port(url.getPort())
      .replaceQuery(url.getQuery())
      .fragment(url.getRef());

    builder.replaceQueryParam("néme", "value");

    final URI actual = builder.build();
    final URI expected = new URI("http://www.example.com/getMyName?néme=value");
    assertEquals(expected.toASCIIString(), actual.toASCIIString());
  }

  @Test
  public void testReplaceMatrixParamWithNull() {
    final UriBuilder builder = UriBuilder
      .fromPath("")
      .matrixParam("matrix", "param1", "param2");
    builder.replaceMatrixParam("matrix", (Object[])null);
    assertEquals("", builder.build().toString());
  }

  @Test
  public void testReplaceNullMatrixParam() {
    try {
      UriBuilder
        .fromPath("")
        .replaceMatrixParam(null, "param");
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testBuildNoSlashUri() {
    final UriBuilder builder = UriBuilder
      .fromUri(URI.create("http://localhost:8080"))
      .path("test");
    assertEquals("http://localhost:8080/test", builder.build().toString());
  }

  @Test
  public void testBuildFromMapNoSlashInUri() {
    final UriBuilder builder = UriBuilder
      .fromUri(URI.create("http://localhost:8080"))
      .path("test");
    assertEquals("http://localhost:8080/test", builder.buildFromMap(Collections.emptyMap()).toString());
  }

  @Test
  public void testBuildFromArrayNoSlashInUri() {
    final UriBuilder builder = UriBuilder
      .fromUri(URI.create("http://localhost:8080"))
      .path("test");
    assertEquals("http://localhost:8080/test", builder.build("testing").toString());
  }

  @Test
  public void testReplaceQueryParam2() {
    final URI uri = UriBuilder
      .fromPath("http://localhost/")
      .replaceQueryParam("foo", "test")
      .build();
    assertEquals("http://localhost/?foo=test", uri.toString());
  }

  @Test
  public void testReplaceQueryParamAndClone() {
    final URI uri = UriBuilder
      .fromPath("http://localhost/")
      .replaceQueryParam("foo", "test")
      .clone()
      .build();
    assertEquals("http://localhost/?foo=test", uri.toString());
  }

  @Test
  public void testEmptyQueryParamValue() {
    final URI uri = UriBuilder
      .fromPath("http://localhost/")
      .queryParam("test", "")
      .build();
    assertEquals("http://localhost/?test=", uri.toString());
  }
}