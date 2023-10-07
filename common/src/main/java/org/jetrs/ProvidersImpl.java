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
import java.util.ArrayList;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

class ProvidersImpl implements Providers {
  private final RequestContext<?> requestContext;

  ProvidersImpl(final RequestContext<?> requestContext) {
    this.requestContext = requestContext;
  }

  private <T> Object getProvider(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final ArrayList<? extends MessageBodyComponent<?>> factories, final boolean asHolder) {
    for (int i = 0, i$ = factories.size(); i < i$; ++i) { // [RA]
      final MessageBodyComponent<?> factory = factories.get(i);
      final MediaType[] compatibleMediaType = factory.getCompatibleMediaType(requestContext, type, genericType, annotations, mediaType);
      if (compatibleMediaType.length > 0) {
        final Object provider = factory.getSingletonOrFromRequestContext(requestContext);
        return asHolder ? new MessageBodyProviderHolder<>(provider, compatibleMediaType) : provider;
      }
    }

    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> MessageBodyReader<T> getMessageBodyReader(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return (MessageBodyReader<T>)getProvider(type, genericType, annotations, mediaType, requestContext.getMessageBodyReaderFactoryList(), false);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> MessageBodyWriter<T> getMessageBodyWriter(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return (MessageBodyWriter<T>)getProvider(type, genericType, annotations, mediaType, requestContext.getMessageBodyWriterFactoryList(), false);
  }

  @SuppressWarnings("unchecked")
  <T> MessageBodyProviderHolder<T> getMessageBodyReader(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType[] mediaTypes) {
    for (final MediaType mediaType : mediaTypes) { // [A]
      final Object provider = getProvider(type, genericType, annotations, mediaType, requestContext.getMessageBodyReaderFactoryList(), true);
      if (provider != null)
        return (MessageBodyProviderHolder<T>)provider;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  <T> MessageBodyProviderHolder<T> getMessageBodyWriter(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType[] mediaTypes) {
    for (final MediaType mediaType : mediaTypes) { // [A]
      final Object provider = getProvider(type, genericType, annotations, mediaType, requestContext.getMessageBodyWriterFactoryList(), true);
      if (provider != null)
        return (MessageBodyProviderHolder<T>)provider;
    }

    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(final Class<T> type) {
    final ArrayList<TypeComponent<ExceptionMapper<?>>> factories = requestContext.getExceptionMapperComponentList();
    for (int i = 0, i$ = factories.size(); i < i$; ++i) { // [RA]
      final TypeComponent<ExceptionMapper<?>> factory = factories.get(i);
      if (factory.getType().isAssignableFrom(type))
        return (ExceptionMapper<T>)factory.getSingletonOrFromRequestContext(requestContext);
    }

    return null;
  }

  @Override
  public <T> ContextResolver<T> getContextResolver(final Class<T> contextType, final MediaType mediaType) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }
}