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

package org.jetrs.provider.ext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

import javax.activation.DataSource;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jetrs.MessageBodyProvider;
import org.libj.io.Streams;
import org.libj.lang.ObjectUtil;

/**
 * JAX-RS 2.1 Section 4.2.4
 */
@Singleton
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class DataSourceProvider extends MessageBodyProvider<DataSource> {
  private static final int DEFAULT_BUFFER_SIZE = 65536;

  private final int bufferSize;

  public DataSourceProvider(final int bufferSize) {
    this.bufferSize = bufferSize;
  }

  public DataSourceProvider() {
    this(DEFAULT_BUFFER_SIZE);
  }

  private abstract static class ProviderDataSource implements DataSource {
    private final String contentType;
    private final String name;

    private ProviderDataSource(final String contentType, final String name) {
      this.contentType = contentType;
      this.name = name;
    }

    @Override
    public OutputStream getOutputStream() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getContentType() {
      return contentType;
    }

    @Override
    public String getName() {
      return name;
    }
  }

  @Override
  public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return DataSource.class.isAssignableFrom(type);
  }

  @Override
  public DataSource readFrom(final Class<DataSource> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,String> httpHeaders, final InputStream entityStream) throws IOException {
    final byte[] bytes = new byte[bufferSize];
    final int readCount = entityStream.read(bytes);
    if (readCount < bytes.length) {
      return new ProviderDataSource(mediaType.toString(), "") {
        @Override
        public InputStream getInputStream() {
          return new ByteArrayInputStream(bytes, 0, readCount);
        }
      };
    }

    final File file = Files.createTempFile("jetrs", null).toFile();
    file.deleteOnExit();
    try (
      final FileOutputStream out = new FileOutputStream(file);
      final FileChannel snk = out.getChannel();
      final ReadableByteChannel src = Channels.newChannel(entityStream);
    ) {
      snk.position(snk.size());
      snk.transferFrom(src, 0, Long.MAX_VALUE);
    }

    return new ProviderDataSource(mediaType.toString(), ObjectUtil.identityString(entityStream)) {
      @Override
      public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
      }
    };
  }

  @Override
  public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
    return DataSource.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(final DataSource dataSource, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,Object> httpHeaders, final OutputStream entityStream) throws IOException {
    Streams.pipe(dataSource.getInputStream(), entityStream);
  }
}