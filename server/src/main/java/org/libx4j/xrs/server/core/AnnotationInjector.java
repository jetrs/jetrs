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

package org.libx4j.xrs.server.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.lib4j.lang.Classes;
import org.lib4j.lang.IllegalAnnotationException;
import org.libx4j.xrs.server.ProviderResource;
import org.libx4j.xrs.server.ResourceContext;
import org.libx4j.xrs.server.util.ParameterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see <a href="http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-spec/jsr339-jaxrs-2.0-final-spec.pdf">JSR339 JAX-RS 2.0 [9.2]</a>
 */
public class AnnotationInjector {
  private static final Logger logger = LoggerFactory.getLogger(AnnotationInjector.class);

  private static final Class<?>[] contextTypes = new Class<?>[] {
    // ResourceContext.class,
    Application.class,
    Providers.class,
    SecurityContext.class,
    UriInfo.class,
    Request.class,
    HttpHeaders.class,
    HttpServletRequest.class,
    HttpServletResponse.class
  };

  private static Class<?> getAssignableContextClass(final Class<?> clazz) {
    for (final Class<?> contextType : contextTypes)
      if (contextType.isAssignableFrom(clazz))
        return contextType;

    return null;
  }

  private static final Comparator<Constructor<?>> parameterCountComparator = new Comparator<Constructor<?>>() {
    @Override
    public int compare(final Constructor<?> o1, Constructor<?> o2) {
      return o1.getParameterCount() < o2.getParameterCount() ? -1 : 1;
    }
  };

  public static AnnotationInjector CONTEXT_ONLY = new AnnotationInjector(null, null, null, null, null, null);

  @SuppressWarnings("unchecked")
  private static final Class<Annotation>[] paramAnnotationTypes = new Class[] {QueryParam.class, PathParam.class, MatrixParam.class, CookieParam.class, HeaderParam.class};

  public static Annotation getInjectableAnnotation(final Class<?> clazz, final Annotation[] annotations) {
    Annotation injectableAnnotation = null;
    for (final Annotation annotation : annotations) {
      for (final Class<Annotation> paramAnnotationType : paramAnnotationTypes) {
        if (injectableAnnotation != null)
          throw new IllegalAnnotationException(injectableAnnotation, "Conflicting annotations on parameter of type " + clazz.getName());

        if (annotation.annotationType() == paramAnnotationType)
          injectableAnnotation = annotation;
      }

      if (annotation.annotationType() == Context.class) {
        if (injectableAnnotation != null)
          throw new IllegalAnnotationException(injectableAnnotation, "Conflicting annotations on parameter of type " + clazz.getName());

        injectableAnnotation = annotation;
      }
    }

    return injectableAnnotation;
  }

  public static AnnotationInjector createAnnotationInjector(final ContainerRequestContext containerRequestContext, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final HttpHeaders headers, final ResourceContext resourceContext) {
    final AnnotationInjector annotationInjector = new AnnotationInjector(containerRequestContext, new RequestImpl(httpServletRequest.getMethod()), httpServletRequest, httpServletResponse, headers, resourceContext.getApplication());
    annotationInjector.providers = resourceContext.getProviders(annotationInjector);
    return annotationInjector;
  }

  private final ContainerRequestContext containerRequestContext;
  private final Request request;
  private final HttpHeaders httpHeaders;
  private final HttpServletRequest httpServletRequest;
  private final HttpServletResponse httpServletResponse;
  private final Application application;
  // NOTE: Have to leave this non-final because there is a circular reference in the createAnnotationInjector() factory method.
  private Providers providers;

  private AnnotationInjector(final ContainerRequestContext containerRequestContext, final Request request, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final HttpHeaders httpHeaders, final Application application) {
    this.containerRequestContext = containerRequestContext;
    this.request = request;
    this.httpHeaders = httpHeaders;
    this.httpServletRequest = httpServletRequest;
    this.httpServletResponse = httpServletResponse;
    this.application = application;
  }

  @SuppressWarnings("unchecked")
  public <T>T getContextObject(final Class<T> clazz) {
    final Class<?> contextClass = getAssignableContextClass(clazz);
    if (contextClass == null)
      throw new IllegalArgumentException(getClass().getSimpleName() + " configuration does not allow injection of object of class " + clazz.getName());

    if (contextClass == Request.class)
      return (T)request;

    if (contextClass == HttpHeaders.class)
      return (T)httpHeaders;

    if (contextClass == HttpServletRequest.class)
      return (T)httpServletRequest;

    if (contextClass == HttpServletResponse.class)
      return (T)httpServletResponse;

    if (contextClass == UriInfo.class)
      return (T)containerRequestContext.getUriInfo();

    if (contextClass == Application.class)
      return (T)application;

    if (contextClass == SecurityContext.class)
      return (T)containerRequestContext.getSecurityContext();

    if (contextClass == Providers.class)
      return (T)providers;

    throw new IllegalStateException("Should have returned a @Context object");
  }

  public Object getParamObject(final Annotation annotation, final Class<?> parameterType, final Annotation[] annotations, final Type genericParameterType, final List<ProviderResource<ParamConverterProvider>> paramConverterProviders) throws ReflectiveOperationException {
    if (annotation.annotationType() == QueryParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      return ParameterUtil.convertParameter(parameterType, genericParameterType, annotations, containerRequestContext.getUriInfo().getQueryParameters(decode).get(((QueryParam)annotation).value()), paramConverterProviders);
    }

    if (annotation.annotationType() == PathParam.class) {
      final boolean decode = ParameterUtil.decode(annotations);
      final String pathParam = ((PathParam)annotation).value();
      return ParameterUtil.convertParameter(parameterType, genericParameterType, annotations, containerRequestContext.getUriInfo().getPathParameters(decode).get(pathParam), paramConverterProviders);
    }

    if (annotation.annotationType() == MatrixParam.class) {
      // TODO:
      throw new UnsupportedOperationException();
    }

    if (annotation.annotationType() == CookieParam.class) {
      // TODO:
      throw new UnsupportedOperationException();
    }

    if (annotation.annotationType() == HeaderParam.class) {
      // TODO:
      throw new UnsupportedOperationException();
    }

    if (annotation.annotationType() == Context.class) {
      return getContextObject(parameterType);
    }

    throw new UnsupportedOperationException("Unsupported param annotation type: " + annotation.annotationType());
  }

  public <T>T newResourceInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return newInstance(clazz, true, true);
  }

  public <T>T newProviderInstance(final Class<T> clazz) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return newInstance(clazz, false, true);
  }

  @SuppressWarnings("unchecked")
  private <T>T newInstance(final Class<T> clazz, final boolean isResource, final boolean inject) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    final Constructor<?>[] constructors = clazz.getConstructors();
    Arrays.sort(constructors, parameterCountComparator);
    outer:
    for (final Constructor<?> constructor : constructors) {
      final Class<?>[] parameterTypes = constructor.getParameterTypes();
      if (parameterTypes.length == 0)
        return (T)constructor.newInstance();

      final Object[] parameters = new Object[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; i++) {
        final Class<?> parameterType = parameterTypes[i];
        final Annotation injectableAnnotation = isResource ? getInjectableAnnotation(parameterType, parameterType.getAnnotations()) : parameterType.getAnnotation(Context.class);
        if (injectableAnnotation == null) {
          logger.warn("Unsupported parameter type: " + parameterType.getName() + " on: " + clazz.getName() + "(" + Arrays.stream(parameterTypes).map(p -> p.getSimpleName()).collect(Collectors.joining(",")) + ")");
          continue outer;
        }

        final Object injectableObject = getContextObject(parameterType);
        if (injectableObject == null) {
          logger.warn("Unsupported @Context parameter: " + parameterType.getName() + " on: " + clazz.getName() + "(" + Arrays.stream(parameterTypes).map(p -> p.getSimpleName()).collect(Collectors.joining(",")) + ")");
          continue outer;
        }

        parameters[i] = injectableObject;
      }

      return (T)constructor.newInstance(parameters);
    }

    throw new InstantiationException("No suitable constructor found on " + (isResource ? "resource" : "provider") + " " + clazz.getName());
  }

  public <T>T injectFields(final T instance) throws IllegalAccessException {
    return injectFields(instance, true);
  }

  // the "inject" boolean is intended to allow a way to test DURING LOAD TIME whether a class WILL BE ABLE TO BE injectable during runtime
  private <T>T injectFields(final T instance, final boolean inject) throws IllegalAccessException {
    final Field[] fields = Classes.getDeclaredFieldsDeep(instance.getClass());
    for (final Field field : fields) {
      if (field.isAnnotationPresent(Context.class)) {
        final Object injectableObject = getContextObject(field.getType());
        if (injectableObject == null)
          throw new UnsupportedOperationException("Unsupported @Context type: " + field.getType().getName() + " on: " + instance.getClass().getName() + "." + field.getName());

        if (inject) {
          field.setAccessible(true);
          field.set(instance, injectableObject);
        }
      }
    }

    return instance;
  }
}