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
    for (final String pathParamName : pathParamNames)
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
    assertEquals("^/foo/(?<IDZZZ9>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "{ ID}", 1));
    assertEquals("^/foo/bar/(?<iZWdZZZ14>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{ i_d}///", 1));
    assertEquals("^/foo/bar/(?<iZXdZZZ15>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{  i.d  }///", 1));
    assertEquals("^/foo/bar/(?<iZYdZZZ16>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{   i-d    }///", 1));
    assertEquals("^/foo/bar/(?<iZZdZZZ15>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{  iZd  }///", 1));
    assertEquals("^/foo/bar/(?<ZZidZZZ14>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{ Zid }///", 1));
    assertEquals("^/foo/(?<idZZZ8>[^/]+?)/bar(/.*)?$", pathToUriTemplate("/foo", "{id  }/bar", 1));
    assertEquals("^/foo/a/b(?<idZZZ12>[^/]+?)c/bar(?<idZZZ23>[^/]+?)d(/.*)?$", pathToUriTemplate("/foo/a/", "/b{ id }c/bar{ id}d/", 2));

    assertEquals("^/foo/bar/(?<idZZZ12>([^/]+?))(/.*)?$", pathToUriTemplate("/foo", "bar/{id:([^/]+?)}", 1));
    assertEquals("^/foo/bar/(?<idZZZ12>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id:[^/]+?}", 1));
    assertEquals("^/foo/bar/(?<idZZZ13>([^/]+?))/(?<nameZZZ41>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{ id:   ([^/]+?)         }/{name}", 2));
    assertEquals("^/foo/bar/(?<idZZZ13>( [^/]+?))/(?<nameZZZ45>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{ id:   ( [^/]+?)         }/{   name   }/", 2));
    assertEquals("^/foo/bar/(?<idZZZ13>(    [^/]+?))/(?<nameZZZ45>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{ id:   (    [^/]+?)         }/{name }/", 2));
    assertEquals("^/foo/bar/(?<idZZZ14>([^/]+? ))/(?<nameZZZ51>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{  id    :   ([^/]+? )         }/{    name}", 2));
    assertEquals("^/foo/bar/(?<idZZZ13>([^/]+?    ))/(?<nameZZZ46>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{ id:   ([^/]+?    )         }/{ name   }///", 2));
    assertEquals("^/foo/bar/(?<idZZZ12>[^/]+?)/(?<nameZZZ28>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id : [^/]+?}/{name}", 2));
    assertEquals("^/foo/(?<idZZZ8>([^/]+?))/bar/(?<nameZZZ35>[^/]+?)/hi(/.*)?$", pathToUriTemplate("/foo", "{id   :    ([^/]+?)}/bar/{name}//hi///", 2));
    assertEquals("^/foo/(?<idZZZ8>[^/]+?)/bar/(?<nameZZZ28>[^/]+?)/hi(/.*)?$", pathToUriTemplate("/foo", "{id: [^/]+?}/bar/{ name}/hi", 2));
    assertEquals("^/foo/bar/(?<idZZZ13>([^/]+?))/blank/(?<nameZZZ39>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar//{id  :([^/]+?)}/blank/{  name  }//", 2));
    assertEquals("^/foo/bar/(?<idZZZ12>[^/]+?)/blank/(?<nameZZZ38>[^/]+?)(/.*)?$", pathToUriTemplate("/foo", "bar/{id   :   [^/]+?}/blank/{name  }", 2));

    assertEquals("^/(?<countryZZZ12>[a-z A-Z]{2})(?<pZZZ29>/?)(?<stateZZZ43>([ a-zA-Z ]{2})?)(/.*)?$", pathToUriTemplate(null, "{   country :[a-z A-Z]{2}}{p:/?}{    state:([ a-zA-Z ]{2})?}", 3));

    assertEquals("^/foo/(?<idZZZ8>(?<Zgrp>[ a-z]{1})\\k<Zgrp>)(/.*)?$", pathToUriTemplate("/foo", "{id  :(?<grp>[ a-z]{1})\\k<grp>}", 1));
  }
}