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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jetrs.MessageBodyProvider;
import org.libj.util.function.BiObjBiLongConsumer;
import org.libj.util.function.Throwing;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Provider
@Singleton
@Consumes({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
@Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.WILDCARD})
public class FileProvider extends MessageBodyProvider<File> {
  static long writeTo(final Object range, final File file, final OutputStream out, final BiObjBiLongConsumer<? super RandomAccessFile,? super OutputStream> consumer) throws IOException {
    final String rangeString;
    final int start;
    if (range == null || (rangeString = String.valueOf(range).trim()).length() == 0 || (start = rangeString.indexOf("bytes=")) == -1) {
      final long len = file.length();
      final long copied = Files.copy(file.toPath(), out);
      if (len != copied)
        throw new IOException("Only " + copied + " of " + len + " bytes were written for: " + file.getName());

      return len;
    }

    try (final RandomAccessFile raf = new RandomAccessFile(file, "r")) {
      return parseRange(rangeString, start + 6, raf, out, consumer);
    }
  }

  private static long parseRange(final String range, int i, final RandomAccessFile raf, final OutputStream out, final BiObjBiLongConsumer<? super RandomAccessFile,? super OutputStream> consumer) {
    final StringBuilder builder = new StringBuilder();
    long from = Long.MIN_VALUE;
    long to = Long.MAX_VALUE;
    final int len = range.length();
    long total = 0;
    for (char ch; i <= len; ++i) { // [N]
      if (i == len || (ch = range.charAt(i)) == ',') {
        if (builder.length() > 0) {
          if (from != Long.MIN_VALUE)
            to = Long.parseLong(builder.toString());
          else
            from = -Long.parseLong(builder.toString());
        }

        if (from <= to && (from != Long.MIN_VALUE || to != Long.MAX_VALUE)) {
          consumer.accept(raf, out, from, to);
          total += to - from;
        }

        from = Long.MIN_VALUE;
        to = Long.MAX_VALUE;
        builder.setLength(0);
      }
      else if (ch == '-') {
        if (from != Long.MIN_VALUE) {
          to = Long.parseLong(builder.toString());
          builder.setLength(0);
        }
        else if (builder.length() > 0) {
          from = Long.parseLong(builder.toString());
          builder.setLength(0);
        }
      }
      else if (Character.isDigit(ch)) {
        builder.append(ch);
      }
      else if (ch != ' ') {
        break;
      }
    }

    return total;
  }

  private static final BiObjBiLongConsumer<RandomAccessFile,OutputStream> consumer = new BiObjBiLongConsumer<RandomAccessFile,OutputStream>() {
    @Override
    public void accept(final RandomAccessFile raf, final OutputStream out, final long from, final long to) {
      try {
        final long len;
        if (from < 0) {
          raf.seek(raf.length() + from);
          len = -from;
        }
        else {
          raf.seek(from);
          len = to == Long.MAX_VALUE ? raf.length() - from : to;
        }

        for (int i = 0; i < len; ++i) // [N]
          out.write(raf.read());
      }
      catch (final IOException e) {
        Throwing.rethrow(e);
      }
    }
  };

  @Context
  private HttpHeaders requestHeaders;

  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return File.class.isAssignableFrom(type);
  }

  @Override
  public File readFrom(final Class<File> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException {
    final Path path = Files.createTempFile("jetrs", null);
    Files.copy(entityStream, path);
    return path.toFile();
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return File.class.isAssignableFrom(type);
  }

  @Override
  public long getSize(final File t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(final File t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException {
    final String range = requestHeaders.getRequestHeaders().getFirst("Range");
    final long contentLength = writeTo(range, t, entityStream, consumer);
    httpHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, contentLength);
  }
}