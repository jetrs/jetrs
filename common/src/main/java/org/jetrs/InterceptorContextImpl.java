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

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.InterceptorContext;

import org.libj.lang.EnumerationIterator;
import org.libj.lang.Enumerations;
import org.libj.util.ArrayUtil;
import org.libj.util.CollectionUtil;

abstract class InterceptorContextImpl<P> implements Closeable, InterceptorContext {
  final PropertiesAdapter<P> propertiesAdapter;
  private Collection<String> propertyNames;
  private Annotation[] annotations;
  private Class<?> type;
  private Type genericType;

  InterceptorContextImpl(final PropertiesAdapter<P> propertiesAdapter) {
    this.propertiesAdapter = propertiesAdapter;
  }

  abstract P getProperties();
  abstract HttpHeadersImpl getHttpHeaders();

  public final String getHeaderString(final String name) {
    return getHttpHeaders().getFirst(name);
  }

  public final Date getDate() {
    return (Date)getHttpHeaders().getMirrorMap().getFirst(HttpHeaders.DATE);
  }

  public final Locale getLanguage() {
    return getHttpHeaders().getLanguage();
  }

  @Override
  public final Object getProperty(final String name) {
    return propertiesAdapter.getProperty(getProperties(), name);
  }

  @Override
  public final Collection<String> getPropertyNames() {
    return propertyNames == null ? propertyNames = new Collection<String>() {
      @Override
      public boolean add(final String e) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean addAll(final Collection<? extends String> c) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean contains(final Object o) {
        return o instanceof String && getProperty((String)o) != null;
      }

      @Override
      public boolean containsAll(final Collection<?> c) {
        return CollectionUtil.containsAll(this, c);
      }

      @Override
      public Iterator<String> iterator() {
        final Enumeration<String> enumeration = propertiesAdapter.getPropertyNames(getProperties());
        return enumeration == null ? Collections.emptyIterator() : new EnumerationIterator<>(enumeration);
      }

      @Override
      public boolean isEmpty() {
        return size() == 0;
      }

      @Override
      public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int size() {
        return propertiesAdapter.size(getProperties());
      }

      @Override
      public Object[] toArray() {
        final Enumeration<String> enumeration = propertiesAdapter.getPropertyNames(getProperties());
        return enumeration == null ? ArrayUtil.EMPTY_ARRAY : Enumerations.toArray(enumeration, String.class);
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T>T[] toArray(final T[] a) {
        final Enumeration<T> enumeration = (Enumeration<T>)propertiesAdapter.getPropertyNames(getProperties());
        return enumeration == null ? a : Enumerations.<T>toArray(enumeration, a);
      }
    } : propertyNames;
  }

  @Override
  public void setProperty(final String name, final Object object) {
    propertiesAdapter.setProperty(getProperties(), name, object);
  }

  @Override
  public void removeProperty(final String name) {
    propertiesAdapter.removeProperty(getProperties(), name);
  }

  @Override
  public final Annotation[] getAnnotations() {
    return annotations;
  }

  @Override
  public final void setAnnotations(final Annotation[] annotations) {
    this.annotations = annotations != null ? annotations : AnnotationUtil.EMPTY_ANNOTATIONS;
  }

  @Override
  public final Class<?> getType() {
    return type;
  }

  @Override
  public final void setType(final Class<?> type) {
    this.type = type;
  }

  @Override
  public final Type getGenericType() {
    return genericType;
  }

  @Override
  public final void setGenericType(final Type genericType) {
    this.genericType = genericType;
  }

  @Override
  public final MediaType getMediaType() {
    return (MediaType)getHttpHeaders().getMirrorMap().getFirst(HttpHeaders.CONTENT_TYPE);
  }

  @Override
  public final void setMediaType(final MediaType mediaType) {
    getHttpHeaders().setMediaType(mediaType);
  }

  @Override
  public void close() throws IOException {
    annotations = null;
    type = null;
    genericType = null;
  }
}