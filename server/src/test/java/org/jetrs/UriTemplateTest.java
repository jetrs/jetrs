/* Copyright (c) 2016 JetRS
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

import java.lang.annotation.Annotation;
import java.util.regex.Matcher;

import javax.ws.rs.Path;

import org.junit.Test;

@SuppressWarnings("unused")
public class UriTemplateTest {
  static class TestPath implements Path {
    private final String value;

    public TestPath(final String value) {
      this.value = value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return Path.class;
    }

    @Override
    public String value() {
      return value;
    }
  }

  private static UriTemplate pathToUriTemplate(final String classPath, final String methodPath) {
    return new UriTemplate("", classPath == null ? null : new TestPath(classPath), methodPath == null ? null : new TestPath(methodPath));
  }

  private static Path newPath(final String value) {
    return new Path() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Path.class;
      }

      @Override
      public String value() {
        return value;
      }
    };
  }

  private static void assertRemoveSlashes(final String expected, final String path) {
    final StringBuilder builder = new StringBuilder();
    UriTemplate.addLeadingRemoveTrailing(builder, newPath(path));
    assertEquals(expected, builder.toString());
  }

  @Test
  public void testBaseUriNull() {
    try {
      new UriTemplate(null, null, null);
      fail("Expected IllegalArgumentException");
    }
    catch (final Exception e) {
    }
  }

  @Test
  public void testBaseUriSlash() {
    try {
      new UriTemplate("/", null, null);
      fail("Expected IllegalArgumentException");
    }
    catch (final Exception e) {
    }
  }

  @Test
  public void testBaseUriNoLeadingSlash() {
    try {
      new UriTemplate("foo", null, null);
      fail("Expected IllegalArgumentException");
    }
    catch (final Exception e) {
    }
  }

  @Test
  public void testBaseUriTrailingSlash() {
    try {
      new UriTemplate("/bar/", null, null);
      fail("Expected IllegalArgumentException");
    }
    catch (final Exception e) {
    }
  }

  @Test
  public void testUriTemplatePaths() {
    try {
      new UriTemplate("/foo", null, null);
      fail("Expected IllegalArgumentException");
    }
    catch (final Exception e) {
    }
  }

  @Test
  public void testRemoveSlashes() {
    assertRemoveSlashes("", "");
    assertRemoveSlashes("", "/");
    assertRemoveSlashes("", "//");
    assertRemoveSlashes("/a", "a");
    assertRemoveSlashes("/a", "/a");
    assertRemoveSlashes("/a", "a/");
    assertRemoveSlashes("/a", "/a/");
  }

  @Test
  public void testMultiArgMatcher() {
    final String arg1 = "20bbaa58fd0ab49c6f6dc184f289abc7";
    final String arg2 = "e685667db65e4a928a4e5a8c21cc7c0f";
    final UriTemplate uriTemplate = pathToUriTemplate("test", "{arg1:[\\da-z]{32}}/literal/{arg2}");
    final String requestPath = "/test/" + arg1 + "/literal/" + arg2;
    final Matcher matcher = uriTemplate.matcher(requestPath);
    assertTrue(matcher.find());
    assertEquals(requestPath, matcher.group(0));
    assertEquals(arg1, matcher.group(1));
    assertEquals(arg2, matcher.group(2));
  }

  @Test
  public void test() {
    assertEquals("^/foo/(?<id>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "{id}").getRegex());
    assertEquals("^/foo/bar/(?<id>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id}").getRegex());
    assertEquals("^/foo/(?<id>[^/]+?)/bar(/.*)?$", pathToUriTemplate("/foo", "{id}/bar").getRegex());

    assertEquals("^/foo/bar/(?<id>([^/]+?))(/.*)?$", pathToUriTemplate("/foo", "bar/{id:([^/]+?)}").getRegex());
    assertEquals("^/foo/bar/(?<id>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id:[^/]+?}").getRegex());
    assertEquals("^/foo/bar/(?<id>([^/]+?))/(?<name>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id:([^/]+?)}/{name}").getRegex());
    assertEquals("^/foo/bar/(?<id>[^/]+?)/(?<name>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id:[^/]+?}/{name}").getRegex());
    assertEquals("^/foo/(?<id>([^/]+?))/bar/(?<name>[^/]+?)/hi(/.*)?$", pathToUriTemplate("/foo", "{id:([^/]+?)}/bar/{name}/hi").getRegex());
    assertEquals("^/foo/(?<id>[^/]+?)/bar/(?<name>[^/]+?)/hi(/.*)?$", pathToUriTemplate("/foo", "{id:[^/]+?}/bar/{name}/hi").getRegex());
    assertEquals("^/foo/bar/(?<id>([^/]+?))/blank/(?<name>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id:([^/]+?)}/blank/{name}").getRegex());
    assertEquals("^/foo/bar/(?<id>[^/]+?)/blank/(?<name>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id:[^/]+?}/blank/{name}").getRegex());

    assertEquals("^/(?<country>[a-zA-Z]{2})(?<p>/?)(?<state>([a-zA-Z]{2})?)(/.*)?$", pathToUriTemplate(null, "{country:[a-zA-Z]{2}}{p:/?}{state:([a-zA-Z]{2})?}").getRegex());
  }
}