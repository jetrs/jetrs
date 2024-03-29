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

package org.jetrs.provider.ext.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.HttpHeaders;

import org.libj.util.zip.UnsynchronizedGZIPOutputStream;

/**
 * GZIP codec support. Interceptor that encodes the output or decodes the input if {@value HttpHeaders#CONTENT_ENCODING} value
 * equals to {@code gzip} or {@code x-gzip}.
 */
@Priority(Priorities.ENTITY_CODER)
public class GZipCodecInterceptor extends ContentCodecInterceptor {
  public GZipCodecInterceptor() {
    super("gzip", "x-gzip");
  }

  @Override
  public InputStream decode(final String contentEncoding, final InputStream encodedStream) throws IOException {
    return new GZIPInputStream(encodedStream);
  }

  @Override
  public OutputStream encode(final String contentEncoding, final OutputStream entityStream) throws IOException {
    return new UnsynchronizedGZIPOutputStream(entityStream);
  }
}