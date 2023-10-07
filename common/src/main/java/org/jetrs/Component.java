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
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;

import org.libj.lang.Classes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Component<T> {
  private static final Logger logger = LoggerFactory.getLogger(Component.class);

  private static int getPriority(final Class<?> clazz) {
    final Priority priority = clazz.getAnnotation(Priority.class);
    return priority != null ? priority.value() : Priorities.USER;
  }

  private static boolean checkAssignable(final Class<?> componentClass, final Class<?> contract) {
    if (contract.isAssignableFrom(componentClass))
      return true;

    if (logger.isWarnEnabled()) { logger.warn("Contract class " + contract.getName() + " is not extended or implemented by component of class " + componentClass.getName()); }
    return false;
  }

  private static <T> Map<Class<?>,Integer> filterAssignable(final Class<T> componentClass, final Class<?>[] contracts) {
    final HashMap<Class<?>,Integer> assignableContracts = new HashMap<>(contracts.length);
    for (final Class<?> contract : contracts) // [A]
      if (checkAssignable(componentClass, contract))
        assignableContracts.put(contract, getPriority(contract));

    return assignableContracts;
  }

  private static <T> Map<Class<?>,Integer> filterAssignable(final Class<T> componentClass, final Map<Class<?>,Integer> contracts) {
    if (contracts == null)
      return null;

    if (contracts.size() == 0)
      return Collections.EMPTY_MAP;

    final HashMap<Class<?>,Integer> assignableContracts = new HashMap<>(contracts.size());
    for (final Map.Entry<Class<?>,Integer> entry : contracts.entrySet()) // [S]
      if (checkAssignable(componentClass, entry.getKey()))
        assignableContracts.put(entry.getKey(), entry.getValue());

    return assignableContracts;
  }

  static Class<?> getGenericInterfaceFirstTypeArgument(final Class<?> clazz, final Class<?> interfaceType, final Class<?> defaultValue) {
    final Type[] typeArguments = Classes.getGenericInterfaceTypeArguments(clazz, interfaceType);
    return typeArguments.length > 0 && typeArguments[0] instanceof Class ? (Class<?>)typeArguments[0] : defaultValue;
  }

  private final AtomicBoolean checkedInstanceAnnotation = new AtomicBoolean(false);
  final Class<T> clazz;
  final T instance;
  private T singleton;
  final boolean isSingleton;
  final int priority;
  final Map<Class<?>,Integer> contracts;

  Component(final Class<T> clazz, final T instance) {
    this(clazz, instance, null, getPriority(clazz));
  }

  Component(final Class<T> clazz, final T instance, final int priority) {
    this(clazz, instance, null, priority);
  }

  Component(final Class<T> clazz, final T instance, final Map<Class<?>,Integer> contracts) {
    this(clazz, instance, filterAssignable(clazz, contracts), getPriority(clazz));
  }

  Component(final Class<T> clazz, final T instance, final Class<?>[] contracts) {
    this(clazz, instance, contracts == null ? null : filterAssignable(clazz, contracts), getPriority(clazz));
  }

  private Component(final Class<T> clazz, final T instance, final Map<Class<?>,Integer> contracts, final int priority) {
    this.clazz = clazz;
    this.instance = instance;
    this.singleton = instance;
    this.isSingleton = instance != null;
    this.contracts = contracts;
    this.priority = priority;
  }

  final Class<T> getProviderClass() {
    return clazz;
  }

  final int getPriority() {
    return priority;
  }

  final T getSingletonOrFromRequestContext(final RequestContext<?> requestContext) {
    if (singleton != null || requestContext == null)
      return singleton;

    try {
      if (!checkedInstanceAnnotation.get()) {
        synchronized (checkedInstanceAnnotation) {
          if (singleton != null)
            return singleton;

          if (!checkedInstanceAnnotation.get()) {
            checkedInstanceAnnotation.set(true);
            if (clazz.isAnnotationPresent(Singleton.class))
              return this.singleton = requestContext.getProviderInstance(clazz);
          }
        }
      }

      return requestContext.getProviderInstance(clazz);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    catch (final IllegalAccessException | InstantiationException e) {
      throw new ProviderInstantiationException(e);
    }
    catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException)
        throw (RuntimeException)cause;

      throw new ProviderInstantiationException(cause);
    }
  }

  @Override
  public String toString() {
    return "Class: " + getProviderClass().getName() + ", Priority: " + getPriority();
  }
}