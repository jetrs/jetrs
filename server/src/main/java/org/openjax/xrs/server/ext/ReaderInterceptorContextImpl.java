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

package org.openjax.xrs.server.ext;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.openjax.xrs.server.core.HeaderMap;
import org.openjax.xrs.server.util.MediaTypes;

public abstract class ReaderInterceptorContextImpl implements ReaderInterceptorContext {
  private final HeaderMap headers;
  private Class<?> type;
  private Type genericType;
  private Annotation[] annotations;

  private Map<String,Object> properties;

  public ReaderInterceptorContextImpl(final Class<?> type, final Type genericType, final Annotation[] annotations, final HeaderMap headers, final InputStream inputStream) {
    this.headers = headers;
    setType(type);
    setGenericType(genericType);
    setAnnotations(annotations);
    setInputStream(inputStream);
  }

  @Override
  public Object getProperty(final String name) {
    return properties == null ? null : properties.get(name);
  }

  @Override
  public Collection<String> getPropertyNames() {
    return properties == null ? null : properties.keySet();
  }

  @Override
  public void setProperty(final String name, final Object object) {
    if (properties == null)
      properties = new HashMap<>();

    properties.put(name, object);
  }

  @Override
  public void removeProperty(final String name) {
    if (properties != null)
      properties.remove(name);
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotations;
  }

  @Override
  public void setAnnotations(final Annotation[] annotations) {
    this.annotations = annotations;
  }

  @Override
  public Class<?> getType() {
    return type;
  }

  @Override
  public void setType(final Class<?> type) {
    this.type = type;
  }

  @Override
  public Type getGenericType() {
    return genericType;
  }

  @Override
  public void setGenericType(final Type genericType) {
    this.genericType = genericType;
  }

  @Override
  public MediaType getMediaType() {

    try {
      return MediaTypes.parse(headers.getFirst(HttpHeaders.CONTENT_TYPE));
    }
    catch (final ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void setMediaType(final MediaType mediaType) {
    headers.putSingle(HttpHeaders.CONTENT_TYPE, mediaType == null ? null : mediaType.toString());
  }

  private InputStream is;

  @Override
  public InputStream getInputStream() {
    return is;
  }

  @Override
  public void setInputStream(final InputStream is) {
    this.is = is;
  }

  @Override
  public MultivaluedMap<String,String> getHeaders() {
    return headers;
  }
}