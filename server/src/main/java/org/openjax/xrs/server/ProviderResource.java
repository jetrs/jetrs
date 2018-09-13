/* Copyright (c) 2018 OpenJAX
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

package org.openjax.xrs.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.openjax.xrs.server.core.AnnotationInjector;

public class ProviderResource<T> {
  protected static Class<?> getGenericInterfaceType(final Class<?> interfaceType, final Class<?> cls) {
    final Type[] genericInterfaces = cls.getGenericInterfaces();
    if (genericInterfaces == null || genericInterfaces.length == 0)
      return null;

    for (int i = 0; i < genericInterfaces.length; i++)
      if (genericInterfaces[i].getTypeName().startsWith(interfaceType.getTypeName() + "<"))
        return (Class<?>)((ParameterizedType)genericInterfaces[i]).getActualTypeArguments()[0];

    return null;
  }

  private final Class<T> clazz;
  private final T singleton;
  private final T matchInstance;

  public ProviderResource(final Class<T> clazz, final T singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    this.clazz = clazz;
    this.singleton = singleton;
    this.matchInstance = singleton != null ? singleton : AnnotationInjector.CONTEXT_ONLY.newProviderInstance(clazz);
  }

  public Class<T> getProviderClass() {
    return this.clazz;
  }

  public T getMatchInstance() {
    return this.matchInstance;
  }

  public T getSingletonOrNewInstance(final AnnotationInjector annotationInjector) {
    try {
      return annotationInjector.injectFields(singleton != null ? singleton : annotationInjector.newProviderInstance(clazz));
    }
    catch (final IllegalAccessException | InstantiationException | InvocationTargetException e) {
      // This should not happen, because an instance of this class would have already been instantiated once in the constructor for the matchInstance instance.
      throw new ProviderInstantiationException(e);
    }
  }
}