/* Copyright (c) 2019 OpenJAX
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.openjax.standard.io.Streams;
import org.openjax.standard.util.Numbers;
import org.openjax.xrs.server.util.MediaTypes;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Provider
public class NumberProvider implements MessageBodyReader<Number>, MessageBodyWriter<Number> {
  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return type == Number.class && MediaTypes.TEXT_PLAIN.isCompatible(mediaType);
  }

  @Override
  public Number readFrom(final Class<Number> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException, WebApplicationException {
    final byte[] bytes = Streams.readBytes(entityStream);
    // FIXME: Is this right?
    return Numbers.parseNumber(new String(bytes));
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return type == Number.class && MediaTypes.TEXT_PLAIN.isCompatible(mediaType);
  }

  @Override
  public long getSize(final Number t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(final Number t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
    final byte[] bytes = t.toString().getBytes();
    entityStream.write(bytes);
    httpHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, bytes.length);
  }
}