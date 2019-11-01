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

package org.jetrs.common.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.jetrs.common.ProviderResource;
import org.jetrs.common.util.ParameterUtil;
import org.libj.lang.IllegalAnnotationException;
import org.libj.util.Classes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see <a href="http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-spec/jsr339-jaxrs-2.0-final-spec.pdf">JSR339 JAX-RS 2.0 [9.2]</a>
 */
public class AnnotationInjector {
  private static final Logger logger = LoggerFactory.getLogger(AnnotationInjector.class);

  private static final Class<?>[] contextTypes = {
    // ResourceContext.class,
    Application.class,
    Configuration.class,
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

  private static final Comparator<Constructor<?>> parameterCountComparator = (o1, o2) -> o1.getParameterCount() < o2.getParameterCount() ? -1 : 1;

  public static final AnnotationInjector CONTEXT_ONLY = new AnnotationInjector(null, null, null, null, null, null, null);

  @SuppressWarnings("unchecked")
  private static final Class<Annotation>[] paramAnnotationTypes = new Class[] {QueryParam.class, PathParam.class, MatrixParam.class, CookieParam.class, HeaderParam.class};

  private static String toString(final Executable source) {
    final StringBuilder builder = new StringBuilder();
    builder.append(source.getDeclaringClass().getName()).append('.').append(source.getName()).append('(');
    for (int i = 0; i < source.getParameterCount(); ++i)
      builder.append(source.getParameters()[i]).append(',');

    builder.setCharAt(builder.length() - 1, ')');
    return builder.toString();
  }

  public static Annotation getInjectableAnnotation(final Parameter parameter, final Annotation[] annotations) {
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
  private final Request request;
  private final HttpHeaders httpHeaders;
  private final HttpServletRequest httpServletRequest;
  private final HttpServletResponse httpServletResponse;
  private final Configuration configuration;
  private final Application application;
  // NOTE: Have to leave this non-final because there is a circular reference in the createAnnotationInjector() factory method
  private Providers providers;

  public AnnotationInjector(final ContainerRequestContext containerRequestContext, final Request request, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final HttpHeaders httpHeaders, final Configuration configuration, final Application application) {
    this.containerRequestContext = containerRequestContext;
    this.request = request;
    this.httpHeaders = httpHeaders;
    this.httpServletRequest = httpServletRequest;
    this.httpServletResponse = httpServletResponse;
    this.configuration = configuration;
    this.application = application;
  }

  public void setProviders(final Providers providers) {
    this.providers = providers;
  }

  @SuppressWarnings("unchecked")
  public <T>T getContextObject(final Class<T> clazz) {
    final Class<?> contextClass = getAssignableContextClass(clazz);
    if (contextClass == null)
      throw new IllegalArgumentException(getClass().getSimpleName() + " configuration does not allow injection of object of class " + clazz.getName());

    if (Request.class.isAssignableFrom(contextClass))
      return (T)request;

    if (HttpHeaders.class.isAssignableFrom(contextClass))
      return (T)httpHeaders;

    if (HttpServletRequest.class.isAssignableFrom(contextClass))
      return (T)httpServletRequest;

    if (HttpServletResponse.class.isAssignableFrom(contextClass))
      return (T)httpServletResponse;

    if (UriInfo.class.isAssignableFrom(contextClass))
      return (T)containerRequestContext.getUriInfo();

    if (Configuration.class.isAssignableFrom(contextClass))
      return (T)configuration;

    if (Application.class.isAssignableFrom(contextClass))
      return (T)application;

    if (SecurityContext.class.isAssignableFrom(contextClass))
      return (T)containerRequestContext.getSecurityContext();

    if (Providers.class.isAssignableFrom(contextClass))
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
      final boolean decode = ParameterUtil.decode(annotations);
      final List<PathSegment> pathSegments = containerRequestContext.getUriInfo().getPathSegments(decode);
      // FIXME: Is it the last PathSegment that from which to get the matrix?
      final PathSegment pathSegment = pathSegments.get(pathSegments.size() - 1);
      final MultivaluedMap<String,String> matrixParameters = pathSegment.getMatrixParameters();
      return matrixParameters == null ? null : matrixParameters.get(((MatrixParam)annotation).value());
    }

    if (annotation.annotationType() == CookieParam.class) {
      final Map<String,Cookie> cookies = containerRequestContext.getCookies();
      if (cookies == null)
        return null;

      final String cookieParam = ((CookieParam)annotation).value();
      return cookies.get(cookieParam);
    }

    if (annotation.annotationType() == HeaderParam.class) {
      final String headerParam = ((HeaderParam)annotation).value();
      return containerRequestContext.getHeaderString(headerParam);
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