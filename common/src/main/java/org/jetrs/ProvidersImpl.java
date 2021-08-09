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
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

class ProvidersImpl implements Providers {
  static final Comparator<TypeProviderResource<?>> providerResourceComparator = Comparator.nullsFirst((o1, o2) -> o1.getType() == o2.getType() ? Integer.compare(o1.getPriority(), o2.getPriority()) : o1.getType().isAssignableFrom(o2.getType()) ? -1 : 1);

  private final List<? extends ExceptionMappingProviderResource> exceptionMappers;
  private final List<? extends EntityReaderProviderResource> entityReaders;
  private final List<? extends EntityWriterProviderResource> entityWriters;
  private final AnnotationInjector annotationInjector;

  ProvidersImpl(final ProvidersImpl copy, final AnnotationInjector annotationInjector) {
    this.exceptionMappers = copy.exceptionMappers;
    this.entityReaders = copy.entityReaders;
    this.entityWriters = copy.entityWriters;
    this.annotationInjector = annotationInjector;
  }

  ProvidersImpl(final List<? extends ExceptionMappingProviderResource> exceptionMappers, final List<? extends EntityReaderProviderResource> entityReaders, final List<? extends EntityWriterProviderResource> entityWriters) {
    this.exceptionMappers = exceptionMappers;
    this.entityReaders = entityReaders;
    this.entityWriters = entityWriters;
    this.annotationInjector = null;

    this.exceptionMappers.sort(providerResourceComparator);
    this.entityReaders.sort(providerResourceComparator);
    this.entityWriters.sort(providerResourceComparator);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T,M>M getProvider(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final List<? extends EntityProviderResource<?>> providers) {
    for (final EntityProviderResource provider : providers)
      if (provider.getCompatibleMediaType(provider.getMatchInstance(), type, genericType, annotations, mediaType) != null)
        return (M)provider.getSingletonOrNewInstance(annotationInjector);

    return null;
  }

  @Override
  public <T>MessageBodyReader<T> getMessageBodyReader(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return getProvider(type, genericType, annotations, mediaType, entityReaders);
  }

  @Override
  public <T>MessageBodyWriter<T> getMessageBodyWriter(final Class<T> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return getProvider(type, genericType, annotations, mediaType, entityWriters);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Throwable>ExceptionMapper<T> getExceptionMapper(final Class<T> type) {
    for (final ExceptionMappingProviderResource exceptionMapper : exceptionMappers)
      if (exceptionMapper.getType().isAssignableFrom(type))
        return (ExceptionMapper<T>)exceptionMapper.getSingletonOrNewInstance(annotationInjector);

    return null;
  }

  @Override
  public <T>ContextResolver<T> getContextResolver(final Class<T> contextType, final MediaType mediaType) {
    throw new UnsupportedOperationException();
  }
}