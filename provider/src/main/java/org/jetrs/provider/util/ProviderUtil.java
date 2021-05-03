/* Copyright (c) 2019 JetRS
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

package org.jetrs.provider.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.libj.io.CountingBufferedOutputStream;
import org.libj.io.Readers;
import org.libj.lang.Numbers;

public final class ProviderUtil {
  private static final int AUTO_CONTENT_LENGTH_THRESHOLD;

  static {
    final String autoContentLengthThreshold = System.getProperty("org.jetrs.AUTO_CONTENT_LENGTH_THRESHOLD");
    AUTO_CONTENT_LENGTH_THRESHOLD = Numbers.isNumber(autoContentLengthThreshold) ? Integer.parseInt(autoContentLengthThreshold) : 65536;
  }

  public static <T>void writeTo(final MessageBodyWriter<T> provider, final T t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
    final boolean hasContentLength = httpHeaders.containsKey(HttpHeaders.CONTENT_LENGTH);
    final OutputStream out = hasContentLength ? entityStream : new CountingBufferedOutputStream(entityStream, AUTO_CONTENT_LENGTH_THRESHOLD);
    provider.writeTo(t, type, genericType, annotations, mediaType, httpHeaders, out);
    // FIXME: This sets the CONTENT_LENGTH only if the written size is less than DEFAULT_BUFFER_SIZE. Is this correct?!?!
    if (!hasContentLength) {
      final long length;
      if (!httpHeaders.containsKey(HttpHeaders.CONTENT_LENGTH) && (length = ((CountingBufferedOutputStream)out).getCount()) < AUTO_CONTENT_LENGTH_THRESHOLD)
        httpHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));

      out.flush();
    }
  }

  public static Charset getCharset(final MediaType mediaType) {
    final String charsetParameter = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
    return charsetParameter != null && Charset.isSupported(charsetParameter) ? Charset.forName(charsetParameter) : Charset.defaultCharset();
  }

  public static byte[] toBytes(final Object value, final MediaType mediaType) {
    return value.toString().getBytes(getCharset(mediaType));
  }

  public static String toString(final InputStream in, final String charset) throws IOException {
    return Readers.readFully(charset != null && Charset.isSupported(charset) ? new InputStreamReader(in, charset) : new InputStreamReader(in));
  }

  private ProviderUtil() {
  }
}