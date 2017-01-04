/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server.ext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

import org.safris.commons.util.Collections;
import org.safris.xrs.server.util.MediaTypes;

public class ProvidersImpl implements Providers {
  private static final Comparator<EntityProvider<?>> comparator = new Comparator<EntityProvider<?>>() {
    @Override
    public int compare(final EntityProvider<?> o1, final EntityProvider<?> o2) {
      final Class<?> c1 = o1.getProvider().getClass();
      final Class<?> c2 = o2.getProvider().getClass();
      return c1 == c2 ? 0 : c1.isAssignableFrom(c2) ? 1 : -1;
    }
  };

  private class ReaderProvider extends EntityProvider<MessageBodyReader<?>> {
    public ReaderProvider(final MessageBodyReader<?> provider) {
      super(provider);
    }

    @Override
    public boolean matches(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
      return super.matches(type, genericType, annotations, mediaType) && getProvider().isReadable(type, genericType, annotations, mediaType);
    }
  }

  private class WriterProvider extends EntityProvider<MessageBodyWriter<?>> {
    public WriterProvider(final MessageBodyWriter<?> provider) {
      super(provider);
    }

    @Override
    public boolean matches(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
      return super.matches(type, genericType, annotations, mediaType) && getProvider().isWriteable(type, genericType, type.getAnnotations(), mediaType);
    }
  }

  private abstract class EntityProvider<T> {
    private final MediaType[] allowedTypes;
    private final T provider;

    public EntityProvider(final T provider) {
      this.provider = provider;
      final Consumes consumes = provider.getClass().getAnnotation(Consumes.class);
      this.allowedTypes = consumes == null ? new MediaType[] {MediaType.WILDCARD_TYPE} : MediaTypes.parse(consumes.value());
    }

    public boolean matches(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
      return MediaTypes.matches(mediaType, allowedTypes);
    }

    public T getProvider() {
      return provider;
    }
  }

  private final List<ReaderProvider> readerProviders = new ArrayList<ReaderProvider>();
  private final List<WriterProvider> writerProviders = new ArrayList<WriterProvider>();

  public ProvidersImpl(final List<MessageBodyReader<?>> readerProviders, final List<MessageBodyWriter<?>> writerProviders) {
    for (final MessageBodyReader<?> readerProvider : readerProviders)
      this.readerProviders.add(new ReaderProvider(readerProvider));

    for (final MessageBodyWriter<?> writerProvider : writerProviders)
      this.writerProviders.add(new WriterProvider(writerProvider));

    Collections.sort(this.readerProviders, comparator);
    Collections.sort(this.writerProviders, comparator);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T>MessageBodyReader<T> getMessageBodyReader(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    for (final ReaderProvider provider : readerProviders)
      if (provider.matches(type, genericType, annotations, mediaType))
        return (MessageBodyReader<T>)provider.getProvider();

    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T>MessageBodyWriter<T> getMessageBodyWriter(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    for (final WriterProvider provider : writerProviders)
      if (provider.matches(type, genericType, annotations, mediaType))
        return (MessageBodyWriter<T>)provider.getProvider();

    return null;
  }

  @Override
  public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(final Class<T> type) {
    return null;
  }

  @Override
  public <T>ContextResolver<T> getContextResolver(final Class<T> contextType, final MediaType mediaType) {
    return null;
  }
}