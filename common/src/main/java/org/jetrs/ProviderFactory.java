/* Copyright (c) 2018 JetRS
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;

import org.libj.lang.Classes;

class ProviderFactory<T> {
  static Class<?> getGenericInterfaceFirstTypeArgument(final Class<?> cls, final Class<?> interfaceType, final Class<?> defaultValue) {
    final Type[] typeArguments = Classes.getGenericInterfaceTypeArguments(cls, interfaceType);
    return typeArguments.length > 0 && typeArguments[0] instanceof Class ? (Class<?>)typeArguments[0] : defaultValue;
  }

  private final Class<T> clazz;
  private final T singleton;
  private final int priority;

  ProviderFactory(final Class<T> clazz, final T singleton) {
    this.clazz = clazz;
    this.singleton = singleton;
    final Priority priority = clazz.getAnnotation(Priority.class);
    this.priority = priority == null ? Priorities.USER : priority.value();
  }

  final Class<T> getProviderClass() {
    return clazz;
  }

  final int getPriority() {
    return priority;
  }

  final T getSingletonOrFromRequestContext(final RequestContext requestContext) {
    if (singleton != null)
      return singleton;

    try {
      return requestContext.getProviderInstance(clazz);
    }
    catch (final IllegalAccessException | InstantiationException | IOException e) {
      throw new ProviderInstantiationException(e);
    }
    catch (final InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException)e.getCause();

      throw new ProviderInstantiationException(e.getCause());
    }
  }

  @Override
  public String toString() {
    return "Class: " + getProviderClass().getName() + ", Priority: " + getPriority();
  }
}