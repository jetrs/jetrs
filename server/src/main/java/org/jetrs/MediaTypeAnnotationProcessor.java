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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.libj.lang.IllegalAnnotationException;
import org.libj.util.ArrayUtil;

class MediaTypeAnnotationProcessor<T extends Annotation> {
  private static final Class<?>[] paramAnnotations = {Context.class, CookieParam.class, HeaderParam.class, MatrixParam.class, PathParam.class, QueryParam.class};

  static <T extends Annotation>T getMethodClassAnnotation(final Class<T> annotationClass, final Method method) {
    final T annotation = method.getAnnotation(annotationClass);
    return annotation != null ? annotation : method.getDeclaringClass().getAnnotation(annotationClass);
  }

  /**
   * Tests whether the specified method contains an entity parameter.
   *
   * @param method The {@link Method}.
   * @return {@code true} if the specified method contains an entity parameter; otherwise {@code false}.
   */
  private static boolean hasEntityParameter(final Method method) {
    OUT:
    for (final Annotation[] annotations : method.getParameterAnnotations()) { // [A]
      for (final Annotation annotation : annotations) // [A]
        for (final Class<?> paramAnnotation : paramAnnotations) // [A]
          if (paramAnnotation.equals(annotation.annotationType()))
            continue OUT;

      return true;
    }

    return false;
  }

  private final T annotation;
  private final ServerMediaType[] mediaTypes;

  @SuppressWarnings("unchecked")
  MediaTypeAnnotationProcessor(final Method method, final Class<T> annotationClass) {
    if (annotationClass == Consumes.class) {
      annotation = (T)getMethodClassAnnotation((Class<Consumes>)annotationClass, method);
      if (!hasEntityParameter(method)) {
        this.mediaTypes = null;
        if (annotation != null)
          throw new IllegalAnnotationException(annotation, method.getDeclaringClass().getName() + "." + method.getName() + "(" + ArrayUtil.toString(method.getParameterTypes(), ',', Class::getName) + ") does not specify entity parameters, and thus cannot declare @Consumes annotation");
      }
      else {
        this.mediaTypes = annotation != null ? ServerMediaType.valueOf(((Consumes)annotation).value()) : MediaTypes.WILDCARD_SERVER_TYPE;
      }
    }
    else if (annotationClass == Produces.class) {
      annotation = (T)getMethodClassAnnotation((Class<Produces>)annotationClass, method);
      if (Void.TYPE.equals(method.getReturnType())) {
        this.mediaTypes = null;
        if (annotation != null)
          throw new IllegalAnnotationException(annotation, method.getDeclaringClass().getName() + "." + method.getName() + "(" + ArrayUtil.toString(method.getParameterTypes(), ',', Class::getName) + ") is void return type, and thus cannot declare @Produces annotation");
      }
      else {
        this.mediaTypes = annotation != null ? ServerMediaType.valueOf(((Produces)annotation).value()) : MediaTypes.WILDCARD_SERVER_TYPE;
      }
    }
    else {
      throw new UnsupportedOperationException("Expected @Consumes or @Produces, but got: " + annotationClass.getName());
    }
  }

  CompatibleMediaType[] getCompatibleMediaType(final List<MediaType> mediaTypes, final List<String> acceptCharsets) {
    if (this.mediaTypes == null)
      return mediaTypes == null || mediaTypes.size() == 0 ? MediaTypes.WILDCARD_COMPATIBLE_TYPE : MediaTypes.getCompatible(MediaTypes.WILDCARD_SERVER_TYPE, mediaTypes, acceptCharsets);

    return mediaTypes == null || mediaTypes.size() == 0 ? MediaTypes.WILDCARD_COMPATIBLE_TYPE : MediaTypes.getCompatible(this.mediaTypes, mediaTypes, acceptCharsets);
  }

  CompatibleMediaType[] getCompatibleMediaType(final MediaType mediaType, final List<String> acceptCharsets) {
    if (this.mediaTypes == null)
      return mediaType == null ? MediaTypes.WILDCARD_COMPATIBLE_TYPE : MediaTypes.getCompatible(MediaTypes.WILDCARD_SERVER_TYPE, mediaType, acceptCharsets);

    return mediaType == null ? MediaTypes.WILDCARD_COMPATIBLE_TYPE : MediaTypes.getCompatible(this.mediaTypes, mediaType, acceptCharsets);
  }

  T getAnnotation() {
    return annotation;
  }

  ServerMediaType[] getMediaTypes() {
    return mediaTypes;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof MediaTypeAnnotationProcessor))
      return false;

    final MediaTypeAnnotationProcessor<?> that = (MediaTypeAnnotationProcessor<?>)obj;
    return Objects.equals(annotation, that.annotation) && mediaTypes != null ? Arrays.equals(mediaTypes, that.mediaTypes) : that.mediaTypes == null;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;
    if (annotation != null)
      hashCode = 31 * hashCode + annotation.hashCode();

    if (mediaTypes != null)
      hashCode = 31 * hashCode + Arrays.hashCode(mediaTypes);

    return hashCode;
  }
}