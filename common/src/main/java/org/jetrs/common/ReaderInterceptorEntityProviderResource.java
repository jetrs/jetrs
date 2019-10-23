package org.jetrs.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ReaderInterceptor;

public class ReaderInterceptorEntityProviderResource extends EntityProviderResource<ReaderInterceptor> {
  ReaderInterceptorEntityProviderResource(final Class<ReaderInterceptor> clazz, final ReaderInterceptor singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    super(clazz, singleton, ReaderInterceptor.class);
  }

  @Override
  public MediaType getCompatibleMediaType(final ReaderInterceptor instance, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return super.getCompatibleMediaType(instance, type, genericType, annotations, mediaType);
  }

  @Override
  public Class<?> getType() {
    return super.getType();
  }
}