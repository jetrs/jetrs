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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

abstract class MessageBodyProviderFactory<T> extends TypeProviderFactory<T> {
  private final ServerMediaType[] allowedTypes;

  MessageBodyProviderFactory(final Class<T> clazz, final T singleton, final Class<?> interfaceType) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    super(clazz, singleton, getGenericInterfaceFirstTypeArgument(clazz, interfaceType, Object.class));
    final Consumes consumes = clazz.getAnnotation(Consumes.class);
    this.allowedTypes = consumes != null ? ServerMediaType.valueOf(consumes.value()) : MediaTypes.WILDCARD_SERVER_TYPE;
  }

  /**
   * Returns a compatible {@link MediaType} for the specified {@code provider} and the entity of the given parameters, if one
   * exists.
   *
   * @param requestContext The {@link RequestContext}.
   * @param type The {@link Class} of the entity.
   * @param genericType The generic {@link Type} of the entity.
   * @param annotations The annotations attached to the entity.
   * @param mediaType The {@link MediaType} of the entity.
   * @return A compatible {@link MediaType} for the specified {@code provider} and the entity of the given parameters, if one
   *         exists.
   * @throws IllegalArgumentException If {@code mediaType} is null.
   */
  CompatibleMediaType[] getCompatibleMediaType(final RequestContext<?> requestContext, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return MediaTypes.getCompatible(allowedTypes, mediaType, null);
  }
}