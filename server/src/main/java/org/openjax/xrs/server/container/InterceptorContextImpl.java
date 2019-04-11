/* Copyright (c) 2016 OpenJAX
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

package org.openjax.xrs.server.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.InterceptorContext;

import org.openjax.xrs.server.ext.DateHeaderDelegate;
import org.openjax.xrs.server.util.MediaTypes;

abstract class InterceptorContextImpl implements InterceptorContext {
  private final Locale locale;
  protected final Map<String,Object> properties;
  private Annotation[] annotations;
  private Class<?> type;
  private Type genericType;

  protected InterceptorContextImpl(final Locale locale, final Map<String,Object> properties) {
    this.locale = locale;
    this.properties = properties;
  }

  abstract MultivaluedMap<String,String> getStringHeaders();

  public final String getHeaderString(final String name) {
    return getStringHeaders().getFirst(name);
  }

  public final Date getDate() {
    final String date = getStringHeaders().getFirst(HttpHeaders.DATE);
    return date == null ? null : DateHeaderDelegate.parse(date);
  }

  public final Locale getLanguage() {
    return locale;
  }

  @Override
  public final Object getProperty(final String name) {
    return properties.get(name);
  }

  @Override
  public final Collection<String> getPropertyNames() {
    return properties.keySet();
  }

  @Override
  public final void setProperty(final String name, final Object object) {
    properties.put(name, object);
  }

  @Override
  public final void removeProperty(final String name) {
    properties.remove(name);
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
    try {
      final String mediaType = getStringHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
      return mediaType == null ? null : MediaTypes.parse(mediaType.toString());
    }
    catch (final ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public final void setMediaType(final MediaType mediaType) {
    getStringHeaders().putSingle(HttpHeaders.CONTENT_TYPE, mediaType == null ? null : mediaType.toString());
  }
}