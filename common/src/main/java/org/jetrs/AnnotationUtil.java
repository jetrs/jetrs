/* Copyright (c) 2022 JetRS
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
import java.util.HashSet;
import java.util.Objects;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;

import org.libj.lang.Classes;

final class AnnotationUtil {
  static final Annotation[] EMPTY_ANNOTATIONS = {};

  /**
   * Returns all annotations on the class itself, and all JaxRs annotations encountered on the first layer of traversing super
   * classes.
   *
   * @param cls The {@link Class}.
   * @return All annotations on the class itself, and all JaxRs annotations encountered on the first layer of traversing super
   *         classes.
   * @throws IllegalStateException If {@code cls} is null.
   * @throws NullPointerException If {@code cls} is null.
   */
  static Annotation[] getAnnotations(final Class<?> cls) {
    return getAnnotations(Objects.requireNonNull(cls), 0);
  }

  private static Annotation[] getAnnotations(final Class<?> cls, final int depth) {
    if (cls == null)
      return depth == 0 ? null : new Annotation[depth];

    boolean hasJaxRsAnnotations = false;
    final Annotation[] annotations = cls.getAnnotations();
    for (final Annotation annotation : annotations) // [A]
      if (annotation.annotationType().getPackage().getName().startsWith("javax.ws.rs."))
        hasJaxRsAnnotations = true;

    // [JAX-RS 3.6] If a subclass or implementation method has any JAX-RS annotations then all of the annotations on the superclass or
    // interface method are ignored.
    if (hasJaxRsAnnotations) {
      final Annotation[] result = new Annotation[depth + annotations.length];
      System.arraycopy(annotations, 0, result, depth, annotations.length);
      return result;
    }

    final boolean isLeaf = depth == 0;
    final Annotation[] result = getAnnotations(cls.getSuperclass(), isLeaf ? depth + annotations.length : depth);
    if (isLeaf)
      System.arraycopy(annotations, 0, result, depth, annotations.length);

    return result;
  }

  /**
   * Returns all annotations on the method itself, and all JaxRs annotations encountered on the first layer of traversing super
   * implementations.
   *
   * @param method The {@link Method}.
   * @return All annotations on the method itself, and all JaxRs annotations encountered on the first layer of traversing super
   *         implementations.
   * @throws NullPointerException If {@code method} is null.
   */
  static Annotation[] getAnnotations(final Method method) {
    return getAnnotations(Objects.requireNonNull(method), 0);
  }

  private static Annotation[] getAnnotations(final Method method, final int depth) {
    if (method == null)
      return depth == 0 ? null : new Annotation[depth];

    boolean hasJaxRsAnnotations = false;
    final Annotation[] annotations = Classes.getAnnotations(method);
    for (final Annotation annotation : annotations) // [A]
      if (annotation.annotationType().getPackage().getName().startsWith("javax.ws.rs."))
        hasJaxRsAnnotations = true;

    // [JAX-RS 3.6] If a subclass or implementation method has any JAX-RS annotations then all of the annotations on the superclass or
    // interface method are ignored.
    if (hasJaxRsAnnotations) {
      final Annotation[] result = new Annotation[depth + annotations.length];
      System.arraycopy(annotations, 0, result, depth, annotations.length);
      return result;
    }

    final boolean isLeaf = depth == 0;
    final Class<?> declaringClass = method.getDeclaringClass().getSuperclass();
    final Annotation[] result;
    if (declaringClass == null)
      result = isLeaf ? annotations : new Annotation[depth + annotations.length];
    else
      result = getAnnotations(Classes.getDeclaredMethodDeep(declaringClass, method.getName(), method.getParameterTypes()), isLeaf ? depth + annotations.length : depth);

    if (isLeaf)
      System.arraycopy(annotations, 0, result, depth, annotations.length);

    return result;
  }

  @SuppressWarnings("unchecked")
  static <A extends Annotation> A getAnnotation(final Class<?> cls, final Class<A> annotationClass) {
    Class<?> parent = cls;
    boolean hasJaxRsAnnotations = false;
    do {
      for (final Annotation annotation : cls.getAnnotations()) { // [A]
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        if (annotationClass.isAssignableFrom(annotationType))
          return (A)annotation;

        if (annotationType.getPackage().getName().startsWith("javax.ws.rs."))
          hasJaxRsAnnotations = true;
      }

      // [JAX-RS 3.6] If a subclass or implementation method has any JAX-RS annotations then all of the annotations on the superclass or
      // interface method are ignored.
      if (hasJaxRsAnnotations)
        return null;
    }
    while ((parent = parent.getSuperclass()) != null);
    return null;
  }

  @SuppressWarnings("unchecked")
  static <A extends Annotation> A getAnnotation(Method method, final Class<A> annotationClass) {
    boolean hasJaxRsAnnotations = false;
    Class<?> declaringClass;
    do {
      for (final Annotation annotation : Classes.getAnnotations(method)) { // [A]
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        if (annotationClass.isAssignableFrom(annotationType))
          return (A)annotation;

        if (annotationType.getPackage().getName().startsWith("javax.ws.rs."))
          hasJaxRsAnnotations = true;
      }

      // [JAX-RS 3.6] If a subclass or implementation method has any JAX-RS annotations then all of the annotations on the superclass or
      // interface method are ignored.
      if (hasJaxRsAnnotations)
        return null;
    }
    while ((declaringClass = method.getDeclaringClass().getSuperclass()) != null && (method = Classes.getDeclaredMethodDeep(declaringClass, method.getName(), method.getParameterTypes())) != null);
    return null;
  }

  static Path digestAnnotations(Method method, final HashSet<HttpMethod> httpMethodAnnotations) {
    boolean hasJaxRsAnnotations = false;
    Path path = null;
    Class<?> declaringClass;
    do {
      for (final Annotation annotation : Classes.getAnnotations(method)) { // [A]
        if (annotation instanceof Path) {
          path = (Path)annotation;
          hasJaxRsAnnotations = true;
        }
        else {
          final Class<? extends Annotation> annotationType = annotation.annotationType();
          final HttpMethod httpMethodAnnotation = annotationType.getAnnotation(HttpMethod.class);
          if (httpMethodAnnotation != null) {
            httpMethodAnnotations.add(httpMethodAnnotation);
            hasJaxRsAnnotations = true;
          }
          else if (annotationType.getPackage().getName().startsWith("javax.ws.rs.")) {
            hasJaxRsAnnotations = true;
          }
        }
      }

      // [JAX-RS 3.6] If a subclass or implementation method has any JAX-RS annotations then all of the annotations on the superclass or
      // interface method are ignored.
      if (hasJaxRsAnnotations)
        return path;
    }
    while ((declaringClass = method.getDeclaringClass().getSuperclass()) != null && (method = Classes.getDeclaredMethodDeep(declaringClass, method.getName(), method.getParameterTypes())) != null);
    return null;
  }

  static boolean isAnnotationPresent(final Class<?> cls, final Class<? extends Annotation> annotationClass) {
    Class<?> parent = cls;
    boolean hasJaxRsAnnotations = false;
    do {
      for (final Annotation annotation : parent.getAnnotations()) { // [A]
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        if (annotationClass.isAssignableFrom(annotationType))
          return true;

        if (annotationType.getPackage().getName().startsWith("javax.ws.rs."))
          hasJaxRsAnnotations = true;
      }

      // [JAX-RS 3.6] If a subclass or implementation method has any JAX-RS annotations then all of the annotations on the superclass or
      // interface method are ignored.
      if (hasJaxRsAnnotations)
        return false;
    }
    while ((parent = parent.getSuperclass()) != null);
    return false;
  }

  static boolean isAnnotationPresent(Method method, final Class<? extends Annotation> annotationClass) {
    boolean hasJaxRsAnnotations = false;
    do {
      for (final Annotation annotation : Classes.getAnnotations(method)) { // [A]
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        if (annotationClass.isAssignableFrom(annotationType))
          return true;

        if (annotationType.getPackage().getName().startsWith("javax.ws.rs."))
          hasJaxRsAnnotations = true;
      }

      // [JAX-RS 3.6] If a subclass or implementation method has any JAX-RS annotations,
      // then all of the annotations on the superclass or interface method are ignored.
      if (hasJaxRsAnnotations)
        return false;
    }
    while ((method = Classes.getDeclaredMethodDeep(method.getDeclaringClass().getSuperclass(), method.getName(), method.getParameterTypes())) != null);
    return false;
  }

  @SafeVarargs
  static boolean isAnnotationPresent(Method method, final Class<? extends Annotation> ... annotationClasses) {
    boolean hasJaxRsAnnotations = false;
    Class<?> declaringClass;
    do {
      for (final Annotation annotation : Classes.getAnnotations(method)) { // [A]
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        for (final Class<? extends Annotation> annotationClass : annotationClasses) // [A]
          if (annotationClass.isAssignableFrom(annotationType))
            return true;

        if (annotationType.getPackage().getName().startsWith("javax.ws.rs."))
          hasJaxRsAnnotations = true;
      }

      // [JAX-RS 3.6] If a subclass or implementation method has any JAX-RS annotations,
      // then all of the annotations on the superclass or interface method are ignored.
      if (hasJaxRsAnnotations)
        return false;
    }
    while ((declaringClass = method.getDeclaringClass().getSuperclass()) != null && (method = Classes.getDeclaredMethodDeep(declaringClass, method.getName(), method.getParameterTypes())) != null);
    return false;
  }

  private AnnotationUtil() {
  }
}