/* Copyright (c) 2016 OpenJAX
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

package org.jetrs.server;

import static org.junit.Assert.*;

import java.lang.annotation.Annotation;

import javax.ws.rs.Path;

import org.junit.Test;

public class PathPatternTest {
  protected static class TestPath implements Path {
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

  private static String pathToPattern(final String classPath, final String methodPath) {
    return new PathPattern(classPath == null ? null : new TestPath(classPath), methodPath == null ? null : new TestPath(methodPath)).getPattern().toString();
  }

  @Test
  public void test() {
    assertEquals("/foo/bar", pathToPattern("/foo", "/bar"));
    assertEquals("/foo/bar", pathToPattern("foo", "bar"));
    assertEquals("/foo/bar", pathToPattern("/foo", "bar"));
    assertEquals("/foo/bar", pathToPattern("foo", "/bar"));

    assertEquals("/foo", pathToPattern("/foo", null));
    assertEquals("/bar", pathToPattern(null, "/bar"));
    assertEquals("/foo/(?<id>[^\\/]+)", pathToPattern("/foo", "{id}"));
    assertEquals("/foo/bar/(?<id>[^\\/]+)", pathToPattern("/foo", "bar/{id}"));

    assertEquals("/foo/bar/(?<id>[^\\/]+)", pathToPattern("/foo", "bar/{id:([^\\/]+)}"));
    assertEquals("/foo/bar/(?<id>[^\\/]+)/(?<name>[^\\/]+)", pathToPattern("/foo", "bar/{id:([^\\/]+)}/{name}"));
    assertEquals("/foo/bar/(?<id>[^\\/]+)/blank/(?<name>[^\\/]+)", pathToPattern("/foo", "bar/{id:([^\\/]+)}/blank/{name}"));
  }
}