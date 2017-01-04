/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.safris.xrs.server.util.MediaTypes;

public class MediaTypeMatcher<T extends Annotation> {
  public static <T extends Annotation>T getMethodClassAnnotation(final Class<T> annotationClass, final Method method) {
    T annotation = method.getAnnotation(annotationClass);
    if (annotation != null)
      return annotation;

    return method.getDeclaringClass().getAnnotation(annotationClass);
  }

  private final T annotation;
  private MediaType[] mediaTypes;

  @SuppressWarnings("unchecked")
  public MediaTypeMatcher(final Method method, final Class<T> annotationClass) {
    if (annotationClass == Consumes.class)
      annotation = (T)getMethodClassAnnotation((Class<Consumes>)annotationClass, method);
    else if (annotationClass == Produces.class)
      annotation = (T)getMethodClassAnnotation((Class<Produces>)annotationClass, method);
    else
      throw new IllegalArgumentException("Expected @Consumes or @Produces, but got: " + annotationClass.getName());

    this.mediaTypes = annotation == null ? null : MediaTypes.parse(annotation instanceof Consumes ? ((Consumes)annotation).value() : annotation instanceof Produces ? ((Produces)annotation).value() : null);
  }

  // FIXME: http://stackoverflow.com/questions/29857643/how-are-jersey-consumes-endpoints-matched
  public boolean matches(final Set<MediaType> mediaTypes) {
    return this.mediaTypes == null ? mediaTypes == null || MediaTypes.matches(MediaType.WILDCARD_TYPE, mediaTypes) : mediaTypes == null || MediaTypes.matches(this.mediaTypes, mediaTypes);
  }

  public T getAnnotation() {
    return annotation;
  }
}