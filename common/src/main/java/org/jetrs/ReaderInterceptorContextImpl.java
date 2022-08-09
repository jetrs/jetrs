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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptorContext;

abstract class ReaderInterceptorContextImpl extends InterceptorContextImpl<HashMap<String,Object>> implements ReaderInterceptorContext {
  private final HttpHeadersImpl headers;

  ReaderInterceptorContextImpl(final Class<?> type, final Type genericType, final Annotation[] annotations, final HttpHeadersImpl headers, final InputStream inputStream) {
    super(PropertiesAdapter.MAP_ADAPTER);
    this.headers = headers;
    setType(type);
    setGenericType(genericType);
    setAnnotations(annotations);
    setInputStream(inputStream);
  }

  @Override
  HashMap<String,Object> getProperties() {
    return null;
  }

  @Override
  HttpHeadersImpl getHttpHeaders() {
    return headers;
  }

  @Override
  public MultivaluedMap<String,String> getHeaders() {
    return headers;
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
}