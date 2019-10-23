package org.jetrs.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.WriterInterceptor;

public class WriterInterceptorEntityProviderResource extends EntityProviderResource<WriterInterceptor> {
  public WriterInterceptorEntityProviderResource(final Class<WriterInterceptor> clazz, final WriterInterceptor singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    super(clazz, singleton, WriterInterceptor.class);
  }

  @Override
  public MediaType getCompatibleMediaType(final WriterInterceptor instance, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return super.getCompatibleMediaType(instance, type, genericType, annotations, mediaType);
  }

  @Override
  public Class<?> getType() {
    return super.getType();
  }
}