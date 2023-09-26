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

package org.jetrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.libj.io.Readers;

public abstract class MessageBodyProvider<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {
  private static final Charset defaultCharset = StandardCharsets.UTF_8;

  protected static Charset getCharset(final MediaType mediaType) {
    if (mediaType == null)
      return defaultCharset;

    final String charsetParameter = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
    return charsetParameter != null && Charset.isSupported(charsetParameter) ? Charset.forName(charsetParameter) : defaultCharset;
  }

  protected static byte[] toBytes(final Object value, final MediaType mediaType) {
    return value.toString().getBytes(getCharset(mediaType));
  }

  protected static String toString(final InputStream in, final String charset) throws IOException {
    return Readers.readFully(charset != null && Charset.isSupported(charset) ? new InputStreamReader(in, charset) : new InputStreamReader(in));
  }

  protected static <K,V> V getFirstOrDefault(final MultivaluedMap<K,? extends V> headers, final K key, final V defaultValue) {
    final V value = headers.getFirst(key);
    return value != null ? value : defaultValue;
  }

  protected static <K,V,W> W getFirstOrDefault(final MultivaluedMap<K,? extends V> headers, final K key, final W defaultValue, final Function<? super V,? extends W> function) {
    final V value = headers.getFirst(key);
    return value != null ? function.apply(value) : defaultValue;
  }

  protected MessageBodyProvider() {
  }
}