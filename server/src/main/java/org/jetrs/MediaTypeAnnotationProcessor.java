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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.libj.lang.IllegalAnnotationException;
import org.libj.util.ArrayUtil;

class MediaTypeAnnotationProcessor<T extends Annotation> {
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
        for (final Class<?> paramAnnotation : ContainerRequestContextImpl.injectableAnnotationTypes) // [A]
          if (paramAnnotation.equals(annotation.annotationType()))
            continue OUT;

      return true;
    }

    return false;
  }

  private final T annotation;
  private final ServerMediaType[] mediaTypes;

  MediaTypeAnnotationProcessor(final Method method, final Class<T> annotationClass) {
    this.annotation = getMethodClassAnnotation(annotationClass, method);
    ServerMediaType[] mediaTypes = MediaTypes.EMPTY_SERVER_TYPE;
    if (annotationClass == Consumes.class) {
      if (annotation != null) {
        if (!hasEntityParameter(method))
          throw new IllegalAnnotationException(annotation, method.getDeclaringClass().getName() + "." + method.getName() + "(" + ArrayUtil.toString(method.getParameterTypes(), ',', Class::getName) + ") does not specify entity parameters, and thus cannot declare @Consumes annotation");

        mediaTypes = ServerMediaType.valueOf(((Consumes)annotation).value());
      }

      if (mediaTypes.length == 0)
        mediaTypes = MediaTypes.WILDCARD_SERVER_TYPE;
    }
    else if (annotationClass == Produces.class) {
      if (annotation != null) {
        if (Void.TYPE.equals(method.getReturnType()))
          throw new IllegalAnnotationException(annotation, method.getDeclaringClass().getName() + "." + method.getName() + "(" + ArrayUtil.toString(method.getParameterTypes(), ',', Class::getName) + ") is void return type, and thus cannot declare @Produces annotation");

        mediaTypes = ServerMediaType.valueOf(((Produces)annotation).value());
      }

      if (mediaTypes.length == 0)
        mediaTypes = MediaTypes.OCTET_SERVER_TYPE;
    }
    else {
      throw new UnsupportedOperationException("Expected @Consumes or @Produces, but got: " + annotationClass.getName());
    }

    this.mediaTypes = mediaTypes;
  }

  CompatibleMediaType[] getCompatibleMediaType(final List<MediaType> mediaTypes, final List<String> acceptCharsets) {
    return MediaTypes.getCompatible(this.mediaTypes, mediaTypes, acceptCharsets);
  }

  CompatibleMediaType[] getCompatibleMediaType(final MediaType mediaType, final List<String> acceptCharsets) {
    return MediaTypes.getCompatible(this.mediaTypes, mediaType, acceptCharsets);
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