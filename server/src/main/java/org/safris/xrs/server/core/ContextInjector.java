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

package org.safris.xrs.server.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.safris.commons.lang.reflect.Classes;

/**
 * @see <a href="http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-spec/jsr339-jaxrs-2.0-final-spec.pdf">JSR339 JAX-RS 2.0 [9.2]</a>
 */
public class ContextInjector {
  private static ContextInjector injectionContextPrototype = new ContextInjector(new Class<?>[] {
    // ResourceContext.class,
    // Providers.class,
    // Application.class,
    SecurityContext.class,
    UriInfo.class,
    Request.class,
    HttpHeaders.class
  });

  public static ContextInjector createInjectionContext(final ContainerRequestContext containerRequestContext, final Request request, final HttpHeaders headers) {
    return new ContextInjector(injectionContextPrototype.allowedClasses, containerRequestContext, request, headers);
  }

  public static boolean allowsInjectableClass(final Class<?> type, final Class<?> injectableClass) {
    return injectionContextPrototype.allowsInjectableClass(injectableClass);
  }

  private final Set<Class<?>> allowedInjectableClasses = new HashSet<Class<?>>();
  private final Class<?>[] allowedClasses;
  private final ContainerRequestContext containerRequestContext;
  private final Request request;
  private final HttpHeaders httpHeaders;

  private ContextInjector(final Class<?>[] allowedClasses, final ContainerRequestContext containerRequestContext, final Request request, final HttpHeaders headers) {
    this.allowedClasses = allowedClasses;
    this.request = request;
    this.httpHeaders = headers;
    for (final Class<?> allowedClass : allowedClasses)
      allowedInjectableClasses.add(allowedClass);
    this.containerRequestContext = containerRequestContext;
  }

  private ContextInjector(final Class<?>[] allowedClasses) {
    this(allowedClasses, null, null, null);
  }

  private final Map<Class<?>,Object> injectableClassToObject = new HashMap<Class<?>,Object>();

  private boolean allowsInjectableClass(final Class<?> injectableClass) {
    return getAllowedInjectableClass(injectableClass) != null;
  }

  private Class<?> getAllowedInjectableClass(final Class<?> injectableClass) {
    for (final Class<?> allowedInjectableClass : allowedInjectableClasses)
      if (allowedInjectableClass.isAssignableFrom(injectableClass))
        return allowedInjectableClass;

    return null;
  }

  @SuppressWarnings("unchecked")
  public <T>T getInjectableObject(final Class<T> injectableClass) {
    final Class<?> allowedInjectibleClass = getAllowedInjectableClass(injectableClass);
    if (allowedInjectibleClass == null)
      throw new IllegalArgumentException("InjectionContext configuration does not allow injection of object of class " + injectableClass.getName());

    if (allowedInjectibleClass == Request.class)
      return (T)request;

    if (allowedInjectibleClass == HttpHeaders.class)
      return (T)httpHeaders;

    if (allowedInjectibleClass == UriInfo.class)
      return (T)containerRequestContext.getUriInfo();

    if (allowedInjectibleClass == SecurityContext.class)
      return (T)containerRequestContext.getSecurityContext();

    return (T)injectableClassToObject.get(allowedInjectibleClass);
  }

  private <T>T testOrInject(final Class<T> targetClass, final boolean inject) {
    try {
      final T object = targetClass.newInstance();
      final Field[] fields = Classes.getDeclaredFieldsDeep(targetClass);
      for (final Field field : fields) {
        if (field.isAnnotationPresent(Context.class)) {
          final Object injectableObject = getInjectableObject(field.getType());
          if (injectableObject == null)
            throw new UnsupportedOperationException("Unsupported @Context type: " + field.getType().getName() + " on: " + targetClass.getName() + "." + field.getName());

          if (inject) {
            field.setAccessible(true);
            field.set(object, injectableObject);
          }
        }
      }

      return object;
    }
    catch (final IllegalArgumentException | ReflectiveOperationException e) {
      throw new WebApplicationException(e);
    }
  }

  public void test(final Class<?> targetClass) {
    testOrInject(targetClass, false);
  }

  public <T>T inject(final Class<T> targetClass) {
    return testOrInject(targetClass, true);
  }
}
