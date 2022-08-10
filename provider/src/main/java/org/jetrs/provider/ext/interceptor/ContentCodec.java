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

import static org.libj.lang.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.libj.util.CollectionUtil;

/**
 * Provides a standard way of implementing encoding {@link WriterInterceptor} and decoding {@link ReaderInterceptor}. Implementing
 * this class ensures the encoding supported by the implementation will be considered during the content negotiation phase when
 * deciding which encoding should be used based on the accepted encodings (and the associated quality parameters) in the request
 * headers.
 */
@Priority(Priorities.ENTITY_CODER)
public abstract class ContentCodec implements ReaderInterceptor, WriterInterceptor {
  private final Set<String> supportedEncodings;

  @Context
  private HttpHeaders requestHeaders;

  /**
   * Creates a new {@link ContentCodec} with the supported content encodings.
   *
   * @param supportedEncodings Values of {@code Content-Encoding} header supported by this encoding provider.
   */
  protected ContentCodec(final String ... supportedEncodings) {
    assertNotEmpty(supportedEncodings);
    this.supportedEncodings = Collections.unmodifiableSet(CollectionUtil.asCollection(new HashSet<>(supportedEncodings.length), supportedEncodings));
  }

  /**
   * Returns the set of supported {@code Content-Encoding} values.
   *
   * @return The set of supported {@code Content-Encoding} values.
   */
  public final Set<String> getSupportedEncodings() {
    return supportedEncodings;
  }

  /**
   * Implementations of this method should take the encoded stream, wrap it and return a stream that can be used to read the decoded
   * entity.
   *
   * @param contentEncoding Encoding to be used to decode the stream (guaranteed to be one of the supported encoding values).
   * @param encodedStream Encoded input stream.
   * @return Decoded entity stream.
   * @throws IOException If an I/O error has occurred.
   */
  public abstract InputStream decode(String contentEncoding, InputStream encodedStream) throws IOException;

  /**
   * Implementations of this method should take the entity stream, wrap it and return a stream that is encoded using the specified
   * encoding.
   *
   * @param contentEncoding Encoding to be used to encode the entity (guaranteed to be one of the supported encoding values).
   * @param entityStream Entity stream to be encoded.
   * @return Encoded stream.
   * @throws IOException If an I/O error has occurred.
   */
  public abstract OutputStream encode(String contentEncoding, OutputStream entityStream) throws IOException;

  @Override
  public final Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
    final String contentEncoding = context.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
    if (contentEncoding != null && getSupportedEncodings().contains(contentEncoding))
      context.setInputStream(decode(contentEncoding, context.getInputStream()));

    return context.proceed();
  }

  @Override
  public final void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
    // Must remove Content-Length header since the encoded message will have a different length
    final List<String> acceptEncodings = requestHeaders.getRequestHeader(HttpHeaders.ACCEPT_ENCODING);
    if (acceptEncodings != null && acceptEncodings.size() > 0) {
      final Set<String> supportedEncodings = getSupportedEncodings();
      if (CollectionUtil.isRandomAccess(acceptEncodings)) {
        for (int i = 0, i$ = acceptEncodings.size(); i < i$; ++i) // [RA]
          if (setEncoding(acceptEncodings.get(i), supportedEncodings, context))
            break;
      }
      else {
        for (final String acceptEncoding : acceptEncodings) // [L]
          if (setEncoding(acceptEncoding, supportedEncodings, context))
            break;
      }
    }

    context.proceed();
  }

  private boolean setEncoding(final String acceptEncoding, final Set<String> supportedEncodings, final WriterInterceptorContext context) throws IOException {
    if (acceptEncoding == null || !supportedEncodings.contains(acceptEncoding))
      return false;

    context.setOutputStream(encode(acceptEncoding, context.getOutputStream()));
    final MultivaluedMap<String,Object> headers = context.getHeaders();
    headers.addFirst(HttpHeaders.CONTENT_ENCODING, acceptEncoding);
    headers.remove(HttpHeaders.CONTENT_LENGTH);
    return true;
  }
}