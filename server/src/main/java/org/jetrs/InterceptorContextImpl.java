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
  private final HttpServletRequest httpServletRequest;
  private Collection<String> propertyNames;
  private Annotation[] annotations;
  private Class<?> type;
  private Type genericType;

  InterceptorContextImpl(final HttpServletRequest httpServletRequest, final HttpHeadersImpl headers) {
    this.httpServletRequest = httpServletRequest;
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
    return httpServletRequest;
  }

  public final Locale getLanguage() {
    return headers.getLanguage();
  }

  @Override
  public final Object getProperty(final String name) {
    return httpServletRequest.getAttribute(name);
  }

  @Override
  public final Collection<String> getPropertyNames() {
    return propertyNames == null ? propertyNames = new Collection<String>() {
      private EnumerationIterator<String> iterator;

      @Override
      public int size() {
        return Enumerations.getSize(httpServletRequest.getAttributeNames());
      }

      @Override
      public boolean isEmpty() {
        return size() == 0;
      }

      @Override
      public boolean contains(final Object o) {
        return o instanceof String && httpServletRequest.getAttribute((String)o) != null;
      }

      @Override
      public Iterator<String> iterator() {
        return iterator == null ? iterator = new EnumerationIterator<String>(httpServletRequest.getAttributeNames()) {
          private String last;

          @Override
          public String next() {
            return last = super.next();
          }

          @Override
          public void remove() {
            httpServletRequest.removeAttribute(last);
          }
        } : iterator;
      }

      @Override
      public Object[] toArray() {
        return Enumerations.toArray(httpServletRequest.getAttributeNames(), String.class);
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T>T[] toArray(final T[] a) {
        return Enumerations.<T>toArray((Enumeration<T>)httpServletRequest.getAttributeNames(), a);
      }

      @Override
      public boolean add(final String e) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(final Object o) {
        if (!contains(o))
          return false;

        httpServletRequest.removeAttribute((String)o);
        return true;
      }

      @Override
      public boolean containsAll(final Collection<?> c) {
        if (c.size() == 0)
          return true;

        for (final Object o : c) // [C]
          if (!contains(o))
            return false;

        return true;
      }

      @Override
      public boolean addAll(final Collection<? extends String> c) {
        boolean changed = false;
        for (final String e : c) // [C]
          changed |= add(e);

        return changed;
      }

      @Override
      public boolean removeAll(final Collection<?> c) {
        boolean changed = false;
        for (final Object e : c) // [C]
          changed |= remove(e);

        return changed;
      }

      @Override
      public boolean retainAll(final Collection<?> c) {
        boolean changed = false;
        for (final Iterator<String> i = iterator(); i.hasNext();) { // [X]
          if (!c.contains(i.next())) {
            i.remove();
            changed = true;
          }
        }

        return changed;
      }

      @Override
      public void clear() {
        final Enumeration<String> names = httpServletRequest.getAttributeNames();
        for (String name; names.hasMoreElements(); name = names.nextElement(), httpServletRequest.removeAttribute(name)); // [X]
      }
    } : propertyNames;
  }

  @Override
  public final void setProperty(final String name, final Object object) {
    httpServletRequest.setAttribute(name, object);
  }

  @Override
  public final void removeProperty(final String name) {
    httpServletRequest.removeAttribute(name);
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
    final MediaType mediaType = (MediaType)getStringHeaders().getMirrorMap().getFirst(HttpHeaders.CONTENT_TYPE);
    return mediaType != null ? mediaType : MediaType.WILDCARD_TYPE;
  }

  @Override
  public final void setMediaType(final MediaType mediaType) {
    headers.setMediaType(mediaType);
  }
}