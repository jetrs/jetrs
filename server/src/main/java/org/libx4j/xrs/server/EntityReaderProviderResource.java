/* Copyright (c) 2018 lib4j
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

package org.libx4j.xrs.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

public class EntityReaderProviderResource extends EntityProviderResource<MessageBodyReader<?>> {
  public EntityReaderProviderResource(final Class<MessageBodyReader<?>> clazz, final MessageBodyReader<?> singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, SecurityException {
    super(clazz, singleton, MessageBodyReader.class);
  }

  @Override
  public boolean matches(final MessageBodyReader<?> instance, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return super.matches(instance, type, genericType, annotations, mediaType) && instance.isReadable(type, genericType, annotations, mediaType);
  }
}