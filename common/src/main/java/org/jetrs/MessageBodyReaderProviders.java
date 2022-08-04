/* Copyright (c) 2022 JetRS
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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

final class MessageBodyReaderProviders {
  static class Factory extends MessageBodyProviderFactory<MessageBodyReader<?>> implements MessageBodyReader<Object> {
    Factory(final Class<MessageBodyReader<?>> clazz, final MessageBodyReader<?> singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
      super(clazz, singleton, MessageBodyReader.class);
      if (getType() == null)
        throw new IllegalStateException("type == null");
    }

    @Override
    public CompatibleMediaType[] getCompatibleMediaType(final MessageBodyReader<?> instance, final Class<?> type, final Type genericType, final Annotation[] annotations, MediaType mediaType) {
      // SPEC: 4.2.1 Message Body Reader
      if (mediaType == null)
        mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;

      final CompatibleMediaType[] mediaTypes = super.getCompatibleMediaType(instance, type, genericType, annotations, mediaType);
      return mediaTypes != null && instance.isReadable(type, genericType, annotations, mediaType) ? mediaTypes : null;
    }

    @Override
    public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object readFrom(final Class<Object> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException, WebApplicationException {
      throw new UnsupportedOperationException();
    }
  }

  static class FactoryList extends org.jetrs.FactoryList<Factory,MessageBodyReader<?>> {
    static final FactoryList EMPTY_LIST = new FactoryList();

    @Override
    ContextList newContextList(final RequestContext requestContext) {
      return new ContextList(requestContext, this);
    }
  }

  static class ContextList extends org.jetrs.ContextList<Factory,MessageBodyReader<?>> {
    ContextList(final RequestContext requestContext, final FactoryList factories) {
      super(requestContext, factories);
    }
  }

  private MessageBodyReaderProviders() {
  }
}