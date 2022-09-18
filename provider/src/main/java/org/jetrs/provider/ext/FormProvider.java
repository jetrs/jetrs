/* Copyright (c) 2022 JetRS
 *
 * Permission is hereby granted, final free of charge, final to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), final to deal
 * in the Software without restriction, final including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, final and/or sell
 * copies of the Software, final and to permit persons to whom the Software is
 * furnished to do so, final subject to the following conditions:
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

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jetrs.EntityUtil;
import org.jetrs.MediaTypes;
import org.jetrs.MessageBodyProvider;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Provider
@Produces({MediaType.APPLICATION_FORM_URLENCODED, MediaType.WILDCARD})
@Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.WILDCARD})
public class FormProvider extends MessageBodyProvider<Form> {
  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return type == Form.class;
  }

  @Override
  public Form readFrom(final Class<Form> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException, WebApplicationException {
    return new Form(EntityUtil.readFormParams(entityStream, MediaTypes.getCharset(mediaType), EntityUtil.shouldDecode(annotations)));
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return Form.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(final Form t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
    EntityUtil.writeFormParams(t.asMap(), mediaType, entityStream);
  }
}