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

package org.jetrs.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jetrs.common.util.MediaTypes;
import org.libj.lang.IllegalAnnotationException;

class ResourceAnnotationProcessor<T extends Annotation> {
  private static final Class<?>[] paramAnnotations = {Context.class, CookieParam.class, HeaderParam.class, MatrixParam.class, PathParam.class, QueryParam.class};
  private static final MediaType[] wildcard = {MediaType.WILDCARD_TYPE};

  static <T extends Annotation>T getMethodClassAnnotation(final Class<T> annotationClass, final Method method) {
    T annotation = method.getAnnotation(annotationClass);
    return annotation != null ? annotation : method.getDeclaringClass().getAnnotation(annotationClass);
  }

  /**
   * Tests whether the specified method contains an entity parameter.
   *
   * @param method The {@link Method}.
   * @return {@code true} if the specified method contains an entity parameter;
   *         otherwise {@code false}.
   */
  private static boolean hasEntityParameter(final Method method) {
    OUT:
    for (final Annotation[] annotations : method.getParameterAnnotations()) {
      for (final Annotation annotation : annotations)
        for (final Class<?> paramAnnotation : paramAnnotations)
          if (paramAnnotation.equals(annotation.annotationType()))
            continue OUT;

      return true;
    }

    return false;
  }

  private final T annotation;
  private final MediaType[] mediaTypes;

  @SuppressWarnings("unchecked")
  ResourceAnnotationProcessor(final Method method, final Class<T> annotationClass) {
    try {
      if (annotationClass == Consumes.class) {
        annotation = (T)getMethodClassAnnotation((Class<Consumes>)annotationClass, method);
        if (!hasEntityParameter(method)) {
          this.mediaTypes = null;
          if (annotation != null)
            throw new IllegalAnnotationException(annotation, method.getDeclaringClass().getName() + "#" + method.getName() + " does not specify entity parameters, and thus cannot declare @Consumes annotation");
        }
        else {
          this.mediaTypes = annotation != null ? MediaTypes.parse(((Consumes)annotation).value()) : wildcard;
        }
      }
      else if (annotationClass == Produces.class) {
        annotation = (T)getMethodClassAnnotation((Class<Produces>)annotationClass, method);
        if (Void.TYPE.equals(method.getReturnType())) {
          this.mediaTypes = null;
          if (annotation != null)
            throw new IllegalAnnotationException(annotation, method.getDeclaringClass().getName() + "#" + method.getName() + " is void return type, and thus cannot declare @Produces annotation");
        }
        else {
          this.mediaTypes = annotation != null ? MediaTypes.parse(((Produces)annotation).value()) : wildcard;
        }
      }
      else {
        throw new UnsupportedOperationException("Expected @Consumes or @Produces, but got: " + annotationClass.getName());
      }
    }
    catch (final ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  MediaType getCompatibleMediaType(final MediaType[] mediaTypes) {
    if (this.mediaTypes == null)
      return mediaTypes == null ? MediaType.WILDCARD_TYPE : MediaTypes.getCompatible(MediaType.WILDCARD_TYPE, mediaTypes);

    return mediaTypes == null ? this.mediaTypes[0] : MediaTypes.getCompatible(this.mediaTypes, mediaTypes);
  }

  T getAnnotation() {
    return annotation;
  }

  MediaType[] getMediaTypes() {
    return this.mediaTypes;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof ResourceAnnotationProcessor))
      return false;

    final ResourceAnnotationProcessor<?> that = (ResourceAnnotationProcessor<?>)obj;
    return (annotation != null ? annotation.equals(that.annotation) : that.annotation == null) && mediaTypes != null ? Arrays.equals(mediaTypes, that.mediaTypes) : that.mediaTypes == null;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    if (annotation != null)
      hashCode *= 31 ^ hashCode + annotation.hashCode();

    if (mediaTypes != null)
      hashCode *= 31 ^ hashCode + mediaTypes.hashCode();

    return hashCode;
  }
}