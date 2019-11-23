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

package org.jetrs.common.ext.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.libj.util.function.BiObjBiLongConsumer;
import org.libj.util.function.Throwing;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Provider
public class FileProvider implements MessageBodyReader<File>, MessageBodyWriter<File> {
  static void writeTo(final Object header, final File file, final OutputStream out, final BiObjBiLongConsumer<RandomAccessFile,OutputStream> consumer) throws IOException {
    final String range;
    final int start;
    if (header == null || (range = String.valueOf(header).trim()).length() == 0 || (start = range.indexOf("bytes=")) == -1) {
      Files.copy(file.toPath(), out);
      return;
    }

    try (final RandomAccessFile raf = new RandomAccessFile(file, "r")) {
      parseRange(range, start + 6, raf, out, consumer);
    }
  }

  private static void parseRange(final String range, int i, final RandomAccessFile raf, final OutputStream out, final BiObjBiLongConsumer<RandomAccessFile,OutputStream> consumer) {
    final StringBuilder builder = new StringBuilder();
    long from = Long.MIN_VALUE;
    long to = Long.MAX_VALUE;
    final int len = range.length();
    for (char ch; i <= len; ++i) {
      if (i == len || (ch = range.charAt(i)) == ',') {
        if (builder.length() > 0) {
          if (from != Long.MIN_VALUE)
            to = Long.valueOf(builder.toString());
          else
            from = -Long.valueOf(builder.toString());
        }

        if (from <= to && (from != Long.MIN_VALUE || to != Long.MAX_VALUE))
          consumer.accept(raf, out, from, to);

        from = Long.MIN_VALUE;
        to = Long.MAX_VALUE;
        builder.setLength(0);
      }
      else if (ch == '-') {
        if (from != Long.MIN_VALUE) {
          to = Long.valueOf(builder.toString());
          builder.setLength(0);
        }
        else if (builder.length() > 0) {
          from = Long.valueOf(builder.toString());
          builder.setLength(0);
        }
      }
      else if (Character.isDigit(ch)) {
        builder.append(ch);
      }
    }
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

        for (int i = 0; i < len; ++i)
          out.write(raf.read());
      }
      catch (final IOException e) {
        Throwing.rethrow(e);
      }
    }
  };

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
    writeTo(httpHeaders.getFirst("Range"), t, entityStream, consumer);
  }
}