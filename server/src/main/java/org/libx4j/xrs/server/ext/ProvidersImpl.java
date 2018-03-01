/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server.ext;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
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

import org.lib4j.util.Collections;
import org.libx4j.xrs.server.util.MediaTypes;

public class ProvidersImpl implements Providers {
  private static Class<?> getGenericInterfaceType(final Class<?> interfaceType, final Class<?> cls) {
    final Type[] genericInterfaces = cls.getGenericInterfaces();
    if (genericInterfaces == null || genericInterfaces.length == 0)
      return null;

    for (int i = 0; i < genericInterfaces.length; i++)
      if (genericInterfaces[i].getTypeName().startsWith(interfaceType.getTypeName() + "<"))
        return (Class<?>)((ParameterizedType)genericInterfaces[i]).getActualTypeArguments()[0];

    return null;
  }

  private static class ProviderComparator implements Comparator<Object> {
    private final Class<?> providerType;

    public ProviderComparator(final Class<?> providerType) {
      this.providerType = providerType;
    }

    @Override
    public int compare(final Object o1, final Object o2) {
      final Class<?> c1 = getGenericInterfaceType(providerType, o1.getClass());
      final Class<?> c2 = getGenericInterfaceType(providerType, o2.getClass());
      return c1 == c2 ? 0 : c1.isAssignableFrom(c2) ? -1 : 1;
    }
  }

  private static class BodyProviderComparator implements Comparator<BodyProvider<?>> {
    private final Comparator<Object> providerComparator;

    public BodyProviderComparator(final Comparator<Object> providerComparator) {
      this.providerComparator = providerComparator;
    }

    @Override
    public int compare(final BodyProvider<?> o1, final BodyProvider<?> o2) {
      return providerComparator.compare(o1.getProvider(), o2.getProvider());
    }
  }

  private static final Comparator<Object> exceptionMapperComparator = new ProviderComparator(ExceptionMapper.class);
  private static final Comparator<BodyProvider<?>> messageBodyReaderComparator = new BodyProviderComparator(new ProviderComparator(MessageBodyReader.class));
  private static final Comparator<BodyProvider<?>> messageBodyWriterComparator = new BodyProviderComparator(new ProviderComparator(MessageBodyWriter.class));

  private class ReaderProvider extends BodyProvider<MessageBodyReader<?>> {
    public ReaderProvider(final MessageBodyReader<?> provider) {
      super(provider);
    }

    @Override
    public boolean matches(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
      return super.matches(type, genericType, annotations, mediaType) && getProvider().isReadable(type, genericType, annotations, mediaType);
    }
  }

  private class WriterProvider extends BodyProvider<MessageBodyWriter<?>> {
    public WriterProvider(final MessageBodyWriter<?> provider) {
      super(provider);
    }

    @Override
    public boolean matches(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
      return super.matches(type, genericType, annotations, mediaType) && getProvider().isWriteable(type, genericType, type.getAnnotations(), mediaType);
    }
  }

  private class ExceptionMappingProvider {
    private final ExceptionMapper<?> provider;
    private final Class<?> exceptionType;

    public ExceptionMappingProvider(final ExceptionMapper<?> provider) {
      this.provider = provider;
      this.exceptionType = getGenericInterfaceType(ExceptionMapper.class, provider.getClass());
    }
  }

  private abstract class BodyProvider<T> {
    private final MediaType[] allowedTypes;
    private final T provider;

    public BodyProvider(final T provider) {
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

  private final List<ExceptionMappingProvider> exceptionMappers;
  private final List<ReaderProvider> readerProviders;
  private final List<WriterProvider> writerProviders;

  public ProvidersImpl(final List<ExceptionMapper<?>> exceptionMappers, final List<MessageBodyReader<?>> readerProviders, final List<MessageBodyWriter<?>> writerProviders) {
    this.exceptionMappers = new ArrayList<ExceptionMappingProvider>(exceptionMappers.size());
    for (final ExceptionMapper<?> exceptionMapper : exceptionMappers)
      this.exceptionMappers.add(new ExceptionMappingProvider(exceptionMapper));

    this.readerProviders = new ArrayList<ReaderProvider>(readerProviders.size());
    for (final MessageBodyReader<?> readerProvider : readerProviders)
      this.readerProviders.add(new ReaderProvider(readerProvider));

    this.writerProviders = new ArrayList<WriterProvider>(writerProviders.size());
    for (final MessageBodyWriter<?> writerProvider : writerProviders)
      this.writerProviders.add(new WriterProvider(writerProvider));

    Collections.sort(this.exceptionMappers, exceptionMapperComparator);
    Collections.sort(this.readerProviders, messageBodyReaderComparator);
    Collections.sort(this.writerProviders, messageBodyWriterComparator);
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
  @SuppressWarnings("unchecked")
  public <T extends Throwable>ExceptionMapper<T> getExceptionMapper(final Class<T> type) {
    for (final ExceptionMappingProvider exceptionMapper : exceptionMappers)
      if (exceptionMapper.exceptionType.isAssignableFrom(type))
        return (ExceptionMapper<T>)exceptionMapper.provider;

    return null;
  }

  @Override
  public <T>ContextResolver<T> getContextResolver(final Class<T> contextType, final MediaType mediaType) {
    return null;
  }
}