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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Context;

import org.libj.lang.Classes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class Component<T> {
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

  static <T> Map<Class<?>,Integer> filterAssignable(final Class<T> componentClass, final Class<?>[] contracts) {
    final HashMap<Class<?>,Integer> assignableContracts = new HashMap<>(contracts.length);
    for (final Class<?> contract : contracts) // [A]
      if (checkAssignable(componentClass, contract))
        assignableContracts.put(contract, getPriority(contract));

    return assignableContracts;
  }

  static <T> Map<Class<?>,Integer> filterAssignable(final Class<T> componentClass, final Map<Class<?>,Integer> contracts) {
    if (contracts == null)
      return null;

    final int size = contracts.size();
    if (size == 0)
      return Collections.EMPTY_MAP;

    final HashMap<Class<?>,Integer> assignableContracts = new HashMap<>(size);
    for (final Map.Entry<Class<?>,Integer> entry : contracts.entrySet()) // [S]
      if (checkAssignable(componentClass, entry.getKey()))
        assignableContracts.put(entry.getKey(), entry.getValue());

    return assignableContracts;
  }

  static Class<?> getGenericInterfaceFirstTypeArgument(final Class<?> clazz, final Class<?> interfaceType, final Class<?> defaultValue) {
    final Type[] typeArguments = Classes.getGenericInterfaceTypeArguments(clazz, interfaceType);
    return typeArguments.length > 0 && typeArguments[0] instanceof Class ? (Class<?>)typeArguments[0] : defaultValue;
  }

  static final Predicate<Field> injectableFieldPredicate = (final Field field) -> {
    final int modifiers = field.getModifiers();
    return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers);
  };

  final Class<T> clazz;
  final T instance;
  private T singleton;
  final boolean isSingleton;
  final int priority;
  final Map<Class<?>,Integer> contracts;

  Component(final Class<T> clazz, final T instance, final Map<Class<?>,Integer> contracts, final int priority) {
    this.clazz = clazz;
    this.contracts = contracts;
    this.priority = priority < 0 ? getPriority(clazz) : priority;

    // Check whether this class has @Singleton annotation. If it also has @Context fields, then it cannot be a singleton.
    final Field[] fields = Classes.getDeclaredFieldsDeep(clazz, injectableFieldPredicate);
    if (logger.isWarnEnabled()) {
      StringBuilder log = null;
      for (final Field field : fields) { // [A]
        if (field.isAnnotationPresent(Context.class)) {
          if (log == null)
            log = new StringBuilder();

          log.append(", ").append(field.getName());
        }
      }

      final boolean hasSingletonAnnotation = clazz.isAnnotationPresent(Singleton.class);
      if (log != null) {
        log.setCharAt(0, ':');
        log.setCharAt(1, ' ');
        if (hasSingletonAnnotation)
          logger.warn("Ignoring @Singleton annotation for component class " + clazz.getName() + " due to presence of @Context fields" + log);
        else if (instance != null)
          logger.warn("Considering instance of component class " + clazz.getName() + " as non-singleton due to presence of @Context fields" + log);

        this.instance = null;
        this.singleton = null;
        this.isSingleton = false;
      }
      else {
        this.instance = instance;
        this.singleton = instance;
        if ((this.isSingleton = instance != null) && !hasSingletonAnnotation && !clazz.isAnonymousClass())
          logger.warn("Instance of component class " + clazz.getName() + " is missing @Singleton annotation");
      }
    }
    else {
      boolean hasContextFields = false;
      for (final Field field : fields) // [A]
        if (hasContextFields = field.isAnnotationPresent(Context.class))
          break;

      if (hasContextFields) {
        this.instance = null;
        this.singleton = null;
        this.isSingleton = false;
      }
      else {
        this.instance = instance;
        this.singleton = instance;
        this.isSingleton = instance != null;
      }
    }
  }

  final Class<T> getProviderClass() {
    return clazz;
  }

  final int getPriority() {
    return priority;
  }

  final T getSingletonOrFromRequestContext(final RequestContext<?> requestContext) throws IOException {
    if (singleton != null || requestContext == null)
      return singleton;

    try {
      return requestContext.getProviderInstance(clazz);
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