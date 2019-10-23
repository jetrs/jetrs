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

package org.jetrs.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.text.ParseException;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.jetrs.common.util.MediaTypes;

public abstract class EntityProviderResource<T> extends TypeProviderResource<T> {
  private final MediaType[] allowedTypes;

  public EntityProviderResource(final Class<T> clazz, final T singleton, final Class<?> interfaceType) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    super(clazz, singleton, getGenericInterfaceType(interfaceType, clazz));
    final Consumes consumes = clazz.getAnnotation(Consumes.class);
    try {
      this.allowedTypes = consumes == null ? null : MediaTypes.parse(consumes.value());
    }
    catch (final ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public MediaType getCompatibleMediaType(final T instance, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return allowedTypes == null ? MediaTypes.getCompatible(mediaType, MediaType.WILDCARD_TYPE) : MediaTypes.getCompatible(mediaType, allowedTypes);
  }
}