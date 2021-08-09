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
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.libj.lang.Classes;
import org.libj.lang.IllegalAnnotationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see <a href="http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-spec/jsr339-jaxrs-2.0-final-spec.pdf">JSR339 JAX-RS 2.0 [9.2]</a>
 */
class AnnotationInjector {
  private static final Logger logger = LoggerFactory.getLogger(AnnotationInjector.class);

  private static final Comparator<Constructor<?>> parameterCountComparator = Comparator.comparingInt(Constructor::getParameterCount);

  static final AnnotationInjector CONTEXT_ONLY = new AnnotationInjector(null, null, null, null, null, null, null, null, null, null);

  @SuppressWarnings("unchecked")
  private static final Class<Annotation>[] paramAnnotationTypes = new Class[] {QueryParam.class, PathParam.class, MatrixParam.class, CookieParam.class, HeaderParam.class};
  // FIXME: Support `AsyncResponse` (JAX-RS 2.1 8.2)

  private static String toString(final Executable source) {
    final StringBuilder builder = new StringBuilder();
    builder.append(source.getDeclaringClass().getName()).append('.').append(source.getName()).append('(');
    for (int i = 0; i < source.getParameterCount(); ++i)
      builder.append(source.getParameters()[i]).append(',');

    builder.setCharAt(builder.length() - 1, ')');
    return builder.toString();
  }

  static Annotation getInjectableAnnotation(final Parameter parameter, final Annotation[] annotations) {
    Annotation injectableAnnotation = null;
    for (final Annotation annotation : annotations) {
      for (final Class<Annotation> paramAnnotationType : paramAnnotationTypes) {
        if (annotation.annotationType() == paramAnnotationType) {
          if (injectableAnnotation != null)
            throw new IllegalAnnotationException(injectableAnnotation, "Conflicting annotations on parameter " + parameter.getName() + " in " + toString(parameter.getDeclaringExecutable()));

          injectableAnnotation = annotation;
        }
      }

      if (annotation.annotationType() == Context.class) {
        if (injectableAnnotation != null)
          throw new IllegalAnnotationException(injectableAnnotation, "Conflicting annotations on parameter " + parameter.getName() + " in " + toString(parameter.getDeclaringExecutable()));

        injectableAnnotation = annotation;
      }
    }

    return injectableAnnotation;
  }

  private final ContainerRequestContext containerRequestContext;
  private final ContainerResponseContext containerResponseContext;
  private final Request request;
  private final HttpHeaders httpHeaders;
  private final ServletConfig servletConfig;
  private final ServletContext servletContext;
  private final HttpServletRequest httpServletRequest;
  private final HttpServletResponse httpServletResponse;
  private final Configuration configuration;
  private final Application application;
  // NOTE: Have to leave this non-final because there is a circular reference in the createAnnotationInjector() factory method
  private Providers providers;
  private ResourceInfo resourceInfo;

  AnnotationInjector(final ContainerRequestContext containerRequestContext, final ContainerResponseContext containerResponseContext, final Request request, final ServletConfig servletConfig, final ServletContext servletContext, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final HttpHeaders httpHeaders, final Configuration configuration, final Application application) {
    this.containerRequestContext = containerRequestContext;
    this.containerResponseContext = containerResponseContext;
    this.request = request;
    this.httpHeaders = httpHeaders;
    this.servletConfig = servletConfig;
    this.servletContext = servletContext;
    this.httpServletRequest = httpServletRequest;
    this.httpServletResponse = httpServletResponse;
    this.configuration = configuration;
    this.application = application;
  }

  void setProviders(final Providers providers) {
    this.providers = providers;
  }

  void setResourceInfo(final ResourceInfo resourceInfo) {
    this.resourceInfo = resourceInfo;
  }

  @SuppressWarnings("unchecked")
  <T>T getContextObject(final Class<T> clazz) {
    // FIXME: Support ResourceContext
    // if (ResourceContext.class.isAssignableFrom(clazz))

    // FIXME: Support ResourceContext (JAX-RS 2.1 6.5.1)
    // if (ResourceInfo.class.isAssignableFrom(clazz))

    if (ContainerRequestContext.class.isAssignableFrom(clazz))
      return (T)containerRequestContext;

    if (UriInfo.class.isAssignableFrom(clazz))
      return (T)containerRequestContext.getUriInfo();

    if (SecurityContext.class.isAssignableFrom(clazz))
      return (T)containerRequestContext.getSecurityContext();

    if (Request.class.isAssignableFrom(clazz))
      return (T)request;

    if (HttpHeaders.class.isAssignableFrom(clazz))
      return (T)httpHeaders;

    if (ResourceInfo.class.isAssignableFrom(clazz))
      return (T)resourceInfo;

    if (ServletConfig.class.isAssignableFrom(clazz))
      return (T)servletConfig;

    if (ServletContext.class.isAssignableFrom(clazz))
      return (T)servletContext;

    if (HttpServletRequest.class.isAssignableFrom(clazz))
      return (T)httpServletRequest;

    if (HttpServletResponse.class.isAssignableFrom(clazz))
      return (T)httpServletResponse;

    if (Configuration.class.isAssignableFrom(clazz))
      return (T)configuration;

    if (Application.class.isAssignableFrom(clazz))
      return (T)application;

    if (Providers.class.isAssignableFrom(clazz))
      return (T)providers;

    if (ContainerResponseContext.class.isAssignableFrom(clazz))
      return (T)containerResponseContext;

    throw new IllegalArgumentException(getClass().getSimpleName() + " configuration does not allow injection of object of class " + clazz.getName());
  }

  <T>T newResourceInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return newInstance(clazz, true);
  }

  <T>T newProviderInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return newInstance(clazz, false);
  }

  @SuppressWarnings("unchecked")
  private <T>T newInstance(final Class<T> clazz, final boolean isResource) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    final Constructor<?>[] constructors = clazz.getConstructors();
    Arrays.sort(constructors, parameterCountComparator);
    outer:
    for (final Constructor<?> constructor : constructors) {
      final Parameter[] parameters = constructor.getParameters();
      if (parameters.length == 0)
        return (T)constructor.newInstance();

      final Object[] parameterInstances = new Object[parameters.length];
      for (int i = 0; i < parameters.length; ++i) {
        final Parameter parameter = parameters[i];
        final Annotation injectableAnnotation = isResource ? getInjectableAnnotation(parameter, parameter.getAnnotations()) : parameter.getAnnotation(Context.class);
        if (injectableAnnotation == null) {
          logger.warn("Unsupported parameter type: " + parameter.getName() + " on: " + clazz.getName() + "(" + Arrays.stream(parameters).map(p -> p.getType().getSimpleName()).collect(Collectors.joining(",")) + ")");
          continue outer;
        }

        final Object injectableObject = getContextObject(parameter.getType());
        if (injectableObject == null) {
          logger.warn("Unsupported @Context parameter: " + parameter.getName() + " on: " + clazz.getName() + "(" + Arrays.stream(parameters).map(p -> p.getType().getSimpleName()).collect(Collectors.joining(",")) + ")");
          continue outer;
        }

        parameterInstances[i] = injectableObject;
      }

      return (T)constructor.newInstance(parameterInstances);
    }

    throw new InstantiationException("No suitable constructor found on " + (isResource ? "resource" : "provider") + " " + clazz.getName());
  }

  private static Field[] EMPTY_FIELDS = new Field[0];

  static Field[] getContextFields(final Object instance) {
    final Field[] fields = Classes.getDeclaredFieldsDeep(instance.getClass());
    return getContextFields(fields, 0, 0);
  }

  private static Field[] getContextFields(final Field[] fields, final int index, final int depth) {
    if (index == fields.length)
      return depth == 0 ? EMPTY_FIELDS : new Field[depth];

    final Field field = fields[index];
    final boolean hasContext = field.isAnnotationPresent(Context.class);
    final Field[] result = getContextFields(fields, index + 1, hasContext ? depth + 1 : depth);
    if (hasContext)
      result[depth] = field;

    return result;
  }

  // the "inject" boolean is intended to allow a way to test DURING LOAD TIME whether a class WILL BE ABLE TO BE injectable during runtime
  boolean injectFields(final Object instance, final boolean inject) throws IllegalAccessException {
    boolean hasContext = false;
    final Field[] fields = Classes.getDeclaredFieldsDeep(instance.getClass());
    for (final Field field : fields) {
      if (field.isAnnotationPresent(Context.class)) {
        hasContext = true;
        final Object injectableObject = getContextObject(field.getType());
        if (injectableObject == null)
          throw new UnsupportedOperationException("Unsupported @Context type: " + field.getType().getName() + " on: " + instance.getClass().getName() + "." + field.getName());

        if (inject) {
          field.setAccessible(true);
          field.set(instance, injectableObject);
        }
      }
    }

    return hasContext;
  }
}