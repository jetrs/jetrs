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
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.InterceptorContext;

import org.libj.lang.EnumerationIterator;
import org.libj.lang.Enumerations;

abstract class InterceptorContextImpl implements InterceptorContext {
  final HttpHeadersImpl headers;
  private final HttpServletRequest request;
  private Collection<String> propertyNames;
  private Annotation[] annotations;
  private Class<?> type;
  private Type genericType;

  InterceptorContextImpl(final HttpServletRequest request, final HttpHeadersImpl headers) {
    this.request = request;
    this.headers = headers;
  }

  public final HttpHeadersImpl getStringHeaders() {
    return headers;
  }

  public final String getHeaderString(final String name) {
    return getStringHeaders().getFirst(name);
  }

  public final Date getDate() {
    return (Date)getStringHeaders().getMirrorMap().getFirst(HttpHeaders.DATE);
  }

  final HttpServletRequest getHttpServletRequest() {
    return this.request;
  }

  public final Locale getLanguage() {
    return headers.getLanguage();
  }

  @Override
  public final Object getProperty(final String name) {
    return request.getAttribute(name);
  }

  @Override
  public final Collection<String> getPropertyNames() {
    return propertyNames == null ? propertyNames = new Collection<String>() {
      private EnumerationIterator<String> iterator;

      @Override
      public int size() {
        return Enumerations.getSize(request.getAttributeNames());
      }

      @Override
      public boolean isEmpty() {
        return size() == 0;
      }

      @Override
      public boolean contains(final Object o) {
        return o instanceof String && request.getAttribute((String)o) != null;
      }

      @Override
      public Iterator<String> iterator() {
        return iterator == null ? iterator = new EnumerationIterator<String>(request.getAttributeNames()) {
          private String last;

          @Override
          public String next() {
            return last = super.next();
          }

          @Override
          public void remove() {
            request.removeAttribute(last);
          }
        } : iterator;
      }

      @Override
      public Object[] toArray() {
        return Enumerations.toArray(String.class, request.getAttributeNames());
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T>T[] toArray(final T[] a) {
        final Class<?> componentType = a.getClass().getComponentType();
        if (componentType != String.class)
          throw new ClassCastException();

        return Enumerations.<T>toArray((Class<T>)a.getClass().getComponentType(), (Enumeration<T>)request.getAttributeNames(), a);
      }

      @Override
      public boolean add(final String e) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(final Object o) {
        if (!contains(o))
          return false;

        request.removeAttribute((String)o);
        return true;
      }

      @Override
      public boolean containsAll(final Collection<?> c) {
        if (c.size() == 0)
          return true;

        for (final Object o : c)
          if (!contains(o))
            return false;

        return true;
      }

      @Override
      public boolean addAll(final Collection<? extends String> c) {
        boolean changed = false;
        for (final String e : c)
          changed |= add(e);

        return changed;
      }

      @Override
      public boolean removeAll(final Collection<?> c) {
        boolean changed = false;
        for (final Object e : c)
          changed |= remove(e);

        return changed;
      }

      @Override
      public boolean retainAll(final Collection<?> c) {
        boolean changed = false;
        for (final Iterator<String> i = iterator(); i.hasNext();) {
          if (!c.contains(i.next())) {
            i.remove();
            changed = true;
          }
        }

        return changed;
      }

      @Override
      public void clear() {
        final Enumeration<String> names = request.getAttributeNames();
        for (String name; names.hasMoreElements(); name = names.nextElement(), request.removeAttribute(name));
      }
    } : propertyNames;
  }

  @Override
  public final void setProperty(final String name, final Object object) {
    request.setAttribute(name, object);
  }

  @Override
  public final void removeProperty(final String name) {
    request.removeAttribute(name);
  }

  @Override
  public final Annotation[] getAnnotations() {
    return annotations;
  }

  @Override
  public final void setAnnotations(final Annotation[] annotations) {
    this.annotations = annotations;
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
    final String mediaType = getStringHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
    return mediaType == null ? MediaType.WILDCARD_TYPE : MediaTypes.parse(mediaType);
  }

  @Override
  public final void setMediaType(final MediaType mediaType) {
    getStringHeaders().putSingle(HttpHeaders.CONTENT_TYPE, mediaType.toString());
  }
}