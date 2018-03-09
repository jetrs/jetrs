package org.libx4j.xrs.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.libx4j.xrs.server.util.MediaTypes;

public abstract class EntityProviderResource<T> extends ProviderResource<T> {
  private final MediaType[] allowedTypes;
  private final Class<?> type;

  protected EntityProviderResource(final Class<T> clazz, final T singleton, final Class<?> interfaceType) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, SecurityException {
    super(clazz, singleton);
    this.type = getGenericInterfaceType(interfaceType, clazz);
    final Consumes consumes = clazz.getAnnotation(Consumes.class);
    this.allowedTypes = consumes == null ? new MediaType[] {MediaType.WILDCARD_TYPE} : MediaTypes.parse(consumes.value());
  }

  public boolean matches(final T instance, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return MediaTypes.matches(mediaType, allowedTypes);
  }

  public Class<?> getType() {
    return this.type;
  }
}