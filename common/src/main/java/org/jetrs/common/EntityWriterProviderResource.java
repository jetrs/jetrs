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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;

public class EntityWriterProviderResource extends EntityProviderResource<MessageBodyWriter<?>> {
  public EntityWriterProviderResource(final Class<MessageBodyWriter<?>> clazz, final MessageBodyWriter<?> singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    super(clazz, singleton, MessageBodyWriter.class);
  }

  @Override
  public MediaType getCompatibleMediaType(final MessageBodyWriter<?> instance, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    final MediaType matchedType = super.getCompatibleMediaType(instance, type, genericType, annotations, mediaType);
    return matchedType != null && instance.isWriteable(type, genericType, annotations, mediaType) ? matchedType : null;
  }
}