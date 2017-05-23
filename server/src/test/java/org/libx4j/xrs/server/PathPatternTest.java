/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server;

import java.lang.annotation.Annotation;

import javax.ws.rs.Path;

import org.junit.Assert;
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
    Assert.assertEquals("/foo/bar", pathToPattern("/foo", "/bar"));
    Assert.assertEquals("/foo/bar", pathToPattern("foo", "bar"));
    Assert.assertEquals("/foo/bar", pathToPattern("/foo", "bar"));
    Assert.assertEquals("/foo/bar", pathToPattern("foo", "/bar"));

    Assert.assertEquals("/foo", pathToPattern("/foo", null));
    Assert.assertEquals("/bar", pathToPattern(null, "/bar"));
    Assert.assertEquals("/foo/(?<id>[^\\/]+)", pathToPattern("/foo", "{id}"));
    Assert.assertEquals("/foo/bar/(?<id>[^\\/]+)", pathToPattern("/foo", "bar/{id}"));

    Assert.assertEquals("/foo/bar/(?<id>[^\\/]+)", pathToPattern("/foo", "bar/{id:([^\\/]+)}"));
    Assert.assertEquals("/foo/bar/(?<id>[^\\/]+)/(?<name>[^\\/]+)", pathToPattern("/foo", "bar/{id:([^\\/]+)}/{name}"));
    Assert.assertEquals("/foo/bar/(?<id>[^\\/]+)/blank/(?<name>[^\\/]+)", pathToPattern("/foo", "bar/{id:([^\\/]+)}/blank/{name}"));
  }
}