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

package org.jetrs.server.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Provider
public class ReaderProvider implements MessageBodyReader<Reader>, MessageBodyWriter<Reader> {
  private static final int DEFAULT_BUFFER_SIZE = 65536;

  private final int bufferSize;

  public ReaderProvider(final int bufferSize) {
    this.bufferSize = bufferSize;
  }

  public ReaderProvider() {
    this(DEFAULT_BUFFER_SIZE);
  }

  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return Reader.class.isAssignableFrom(type);
  }

  @Override
  public Reader readFrom(final Class<Reader> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException, WebApplicationException {
    return new InputStreamReader(entityStream);
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return Reader.class.isAssignableFrom(type);
  }

  @Override
  public long getSize(final Reader t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(final Reader t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
    final OutputStreamWriter out = new OutputStreamWriter(entityStream);
    final char[] buffer = new char[bufferSize];
    while (true) {
      for (int len; (len = t.read(buffer)) != 0;)
        out.write(buffer, 0, len);

      final int ch = t.read();
      if (ch == -1)
        break;

      entityStream.write(ch);
    }

    t.close();
  }
}