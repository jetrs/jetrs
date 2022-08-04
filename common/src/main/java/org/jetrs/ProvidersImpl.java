/* Copyright (c) 2016 JetRS
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
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

class ProvidersImpl implements Providers {
  private final ExceptionMapperProviders.ContextList exceptionMapperContexts;
  private final MessageBodyReaderProviders.ContextList messageBodyReaderContexts;
  private final MessageBodyWriterProviders.ContextList messageBodyWriterContexts;

  ProvidersImpl(final ExceptionMapperProviders.ContextList exceptionMapperContexts, final MessageBodyReaderProviders.ContextList messageBodyReaderContexts, final MessageBodyWriterProviders.ContextList messageBodyWriterContexts) {
    this.exceptionMapperContexts = exceptionMapperContexts;
    this.messageBodyReaderContexts = messageBodyReaderContexts;
    this.messageBodyWriterContexts = messageBodyWriterContexts;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T,M>M getProvider(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final ContextList<? extends MessageBodyProviderFactory<?>,?> providers) {
    for (int i = 0, len = providers.size(); i < len; ++i) {
      final MessageBodyProviderFactory factory = providers.getFactory(i);
      final Object provider = providers.getInstance(i);
      if (factory.getCompatibleMediaType(provider, type, genericType, annotations, mediaType) != null)
        return (M)provider;
    }

    return null;
  }

  @Override
  public <T>MessageBodyReader<T> getMessageBodyReader(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return getProvider(type, genericType, annotations, mediaType, messageBodyReaderContexts);
  }

  @Override
  public <T>MessageBodyWriter<T> getMessageBodyWriter(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return getProvider(type, genericType, annotations, mediaType, messageBodyWriterContexts);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Throwable>ExceptionMapper<T> getExceptionMapper(final Class<T> type) {
    for (int i = 0, len = exceptionMapperContexts.size(); i < len; ++i) {
      final ExceptionMapperProviders.Factory factory = exceptionMapperContexts.getFactory(i);
      if (factory.getType().isAssignableFrom(type))
        return (ExceptionMapper<T>)exceptionMapperContexts.getInstance(i);
    }

    return null;
  }

  @Override
  public <T>ContextResolver<T> getContextResolver(final Class<T> contextType, final MediaType mediaType) {
    throw new UnsupportedOperationException();
  }
}