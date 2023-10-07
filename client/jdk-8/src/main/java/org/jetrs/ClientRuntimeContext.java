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

import java.util.ArrayList;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

class ClientRuntimeContext extends RuntimeContext {
  ClientRuntimeContext(
    final Configuration configuration,
    final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorProviderFactories,
    final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorProviderFactories,
    final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperProviderFactories
  ) {
    super(configuration, readerInterceptorProviderFactories, writerInterceptorProviderFactories, messageBodyReaderProviderFactories, messageBodyWriterProviderFactories, exceptionMapperProviderFactories);
  }

  @Override
  ClientRequestContextImpl localRequestContext() {
    throw new UnsupportedOperationException();
  }
}