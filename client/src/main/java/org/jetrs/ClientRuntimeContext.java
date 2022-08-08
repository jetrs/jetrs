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

package org.jetrs;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

public class ClientRuntimeContext extends RuntimeContext {
  ClientRuntimeContext() {
    this(
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST,
      Collections.EMPTY_LIST
    );
  }

  ClientRuntimeContext(
    final List<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories,
    final List<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final List<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories
  ) {
    super(readerInterceptorProviderFactories, writerInterceptorProviderFactories, messageBodyReaderProviderFactories, messageBodyWriterProviderFactories, exceptionMapperProviderFactories);
  }

  @Override
  ClientRequestContext newRequestContext(final Request request) {
    return new ClientRequestContext(this, request);
  }
}