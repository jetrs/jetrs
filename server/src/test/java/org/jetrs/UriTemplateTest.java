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

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("unused")
public class UriTemplateTest {
  @SuppressWarnings("all")
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

  public static void assertEquals(final Object expected, final Object actual) {
    Assert.assertEquals(null, expected, actual);
  }

  public static void assertEquals(final Object expected, final UriTemplate actual) {
    Assert.assertEquals(null, expected, actual.matcher("").pattern().pattern());
  }

  private static UriTemplate pathToUriTemplate(final String classPath, final String methodPath, final int noParams) {
    final UriTemplate uriTemplate = new UriTemplate("", classPath == null ? null : new TestPath(classPath), methodPath == null ? null : new TestPath(methodPath));
    final String[] pathParamNames = uriTemplate.getPathParamNames();
    assertEquals(noParams, pathParamNames.length);
    for (final String pathParamName : pathParamNames) // [A]
      assertNotNull(pathParamName);

    return uriTemplate;
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
    final StringBuilder b = new StringBuilder();
    UriTemplate.addLeadingRemoveTrailing(b, newPath(path));
    assertEquals(expected, b.toString());
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
      new UriTemplate("test", null, null);
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
      new UriTemplate("/test", null, null);
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
    final UriTemplate uriTemplate = pathToUriTemplate("test", "{arg1:[\\da-z]{32}}/literal/{arg2}", 2);
    final String requestPath = "/test/" + arg1 + "/literal/" + arg2;
    final Matcher matcher = uriTemplate.matcher(requestPath);
    assertTrue(matcher.find());
    assertEquals(requestPath, matcher.group(0));
    assertEquals(arg1, matcher.group(1));
    assertEquals(arg2, matcher.group(2));
  }

  @Test
  public void testFailEmptyName() {
    try {
      pathToUriTemplate("/", "{}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
    try {
      pathToUriTemplate("/", "{ }", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
    try {
      pathToUriTemplate("/", "{  }", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
    try {
      pathToUriTemplate("/", "{   }", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testFailInvalidNameStart() {
    try {
      pathToUriTemplate("/", "{%ab}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
    try {
      pathToUriTemplate("/", "{.ab}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
    try {
      pathToUriTemplate("/", "{-ab}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testFailInvalidNamePart1() {
    try {
      pathToUriTemplate("/", "{a b%}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testFailInvalidNamePart() {
    try {
      pathToUriTemplate("/", "{ab%}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
    try {
      pathToUriTemplate("/", "{0&a}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
    try {
      pathToUriTemplate("/", "{A^}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
    try {
      pathToUriTemplate("/", "{9#}", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void test() {
    assertEquals("^/test/(?<IDZZZ10>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "{ ID}", 1));
    assertEquals("^/test/bar/(?<iZWdZZZ15>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{ i_d}///", 1));
    assertEquals("^/test/bar/(?<iZXdZZZ16>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{  i.d  }///", 1));
    assertEquals("^/test/bar/(?<iZYdZZZ17>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{   i-d    }///", 1));
    assertEquals("^/test/bar/(?<iZZdZZZ16>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{  iZd  }///", 1));
    assertEquals("^/test/bar/(?<ZZidZZZ15>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{ Zid }///", 1));
    assertEquals("^/test/(?<idZZZ9>[^/]+?)/bar(/.*)?$", pathToUriTemplate("/test", "{id  }/bar", 1));
    assertEquals("^/test/a/b(?<idZZZ13>[^/]+?)c/bar(?<idZZZ24>[^/]+?)d(/.*)?$", pathToUriTemplate("/test/a/", "/b{ id }c/bar{ id}d/", 2));

    assertEquals("^/test/bar/(?<idZZZ13>([^/]+?))(/.*)?$", pathToUriTemplate("/test", "bar/{id:([^/]+?)}", 1));
    assertEquals("^/test/bar/(?<idZZZ13>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{id:[^/]+?}", 1));
    assertEquals("^/test/bar/(?<idZZZ14>([^/]+?))/(?<nameZZZ42>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{ id:   ([^/]+?)         }/{name}", 2));
    assertEquals("^/test/bar/(?<idZZZ14>( [^/]+?))/(?<nameZZZ46>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{ id:   ( [^/]+?)         }/{   name   }/", 2));
    assertEquals("^/test/bar/(?<idZZZ14>(    [^/]+?))/(?<nameZZZ46>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{ id:   (    [^/]+?)         }/{name }/", 2));
    assertEquals("^/test/bar/(?<idZZZ15>([^/]+? ))/(?<nameZZZ52>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{  id    :   ([^/]+? )         }/{    name}", 2));
    assertEquals("^/test/bar/(?<idZZZ14>([^/]+?    ))/(?<nameZZZ47>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{ id:   ([^/]+?    )         }/{ name   }///", 2));
    assertEquals("^/test/bar/(?<idZZZ13>[^/]+?)/(?<nameZZZ29>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{id : [^/]+?}/{name}", 2));
    assertEquals("^/test/(?<idZZZ9>([^/]+?))/bar/(?<nameZZZ36>[^/]+?)/hi(/.*)?$", pathToUriTemplate("/test", "{id   :    ([^/]+?)}/bar/{name}//hi///", 2));
    assertEquals("^/test/(?<idZZZ9>[^/]+?)/bar/(?<nameZZZ29>[^/]+?)/hi(/.*)?$", pathToUriTemplate("/test", "{id: [^/]+?}/bar/{ name}/hi", 2));
    assertEquals("^/test/bar/(?<idZZZ14>([^/]+?))/blank/(?<nameZZZ40>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar//{id  :([^/]+?)}/blank/{  name  }//", 2));
    assertEquals("^/test/bar/(?<idZZZ13>[^/]+?)/blank/(?<nameZZZ39>[^/]+?)(/.*)?$", pathToUriTemplate("/test", "bar/{id   :   [^/]+?}/blank/{name  }", 2));

    assertEquals("^/(?<countryZZZ12>[a-z A-Z]{2})(?<pZZZ29>/?)(?<stateZZZ43>([ a-zA-Z ]{2})?)(/.*)?$", pathToUriTemplate(null, "{   country :[a-z A-Z]{2}}{p:/?}{    state:([ a-zA-Z ]{2})?}", 3));

    assertEquals("^/test/(?<idZZZ9>(?<Zgrp>[ a-z]{1})\\k<Zgrp>)(/.*)?$", pathToUriTemplate("/test", "{id  :(?<grp>[ a-z]{1})\\k<grp>}", 1));
  }
}