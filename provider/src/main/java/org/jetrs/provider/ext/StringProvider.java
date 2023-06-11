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

package org.jetrs.provider.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.charset.Charset;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jetrs.CommonProperties;
import org.jetrs.MessageBodyProvider;
import org.libj.io.Readers;
import org.libj.lang.Systems;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Provider
@Singleton
@Consumes
@Produces
public class StringProvider extends MessageBodyProvider<String> {
  private static final int bufferSize = Systems.getProperty(CommonProperties.CONTENT_LENGTH_BUFFER, CommonProperties.CONTENT_LENGTH_BUFFER_DEFAULT);

  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return type == String.class;
  }

  @Override
  public String readFrom(final Class<String> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException, WebApplicationException {
    final Charset charset = MessageBodyProvider.getCharset(mediaType);
    return Readers.readFully(new InputStreamReader(entityStream, charset));
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return type == String.class;
  }

  @Override
  public long getSize(final String t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(final String t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
    if (bufferSize < t.length()) {
      final Writer writer = Channels.newWriter(Channels.newChannel(entityStream), MessageBodyProvider.getCharset(mediaType).newEncoder(), bufferSize);
      writer.write(t);
      writer.flush();
    }
    else {
      final byte[] bytes = t.getBytes(MessageBodyProvider.getCharset(mediaType));
      httpHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, bytes.length);
      entityStream.write(bytes);
    }
  }
}