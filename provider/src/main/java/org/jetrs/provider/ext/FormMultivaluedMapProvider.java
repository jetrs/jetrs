/* Copyright (c) 2022 JetRS
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

package org.jetrs.provider.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jetrs.EntityUtil;
import org.jetrs.MediaTypes;
import org.jetrs.MessageBodyProvider;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Singleton
@Produces(MediaType.APPLICATION_FORM_URLENCODED)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class FormMultivaluedMapProvider extends MessageBodyProvider<MultivaluedMap<String,String>> {
  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return type == MultivaluedMap.class;
  }

  @Override
  public MultivaluedMap<String,String> readFrom(final Class<MultivaluedMap<String,String>> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException {
    return EntityUtil.readFormParams(entityStream, MediaTypes.getCharset(mediaType), EntityUtil.shouldDecode(annotations));
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return MultivaluedMap.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(final MultivaluedMap<String,String> t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException {
    EntityUtil.writeFormParams(t, mediaType, entityStream);
  }
}