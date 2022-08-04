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

import javax.ws.rs.core.Request;

public class ClientRuntimeContext extends RuntimeContext {
  ClientRuntimeContext() {
    this(
      ReaderInterceptorProviders.FactoryList.EMPTY_LIST,
      WriterInterceptorProviders.FactoryList.EMPTY_LIST,
      MessageBodyReaderProviders.FactoryList.EMPTY_LIST,
      MessageBodyWriterProviders.FactoryList.EMPTY_LIST,
      ExceptionMapperProviders.FactoryList.EMPTY_LIST
    );
  }

  ClientRuntimeContext(
    final ReaderInterceptorProviders.FactoryList readerInterceptorEntityProviderFactories,
    final WriterInterceptorProviders.FactoryList writerInterceptorEntityProviderFactories,
    final MessageBodyReaderProviders.FactoryList messageBodyReaderEntityProviderFactories,
    final MessageBodyWriterProviders.FactoryList messageBodyWriterEntityProviderFactories,
    final ExceptionMapperProviders.FactoryList exceptionMapperEntityProviderFactories
  ) {
    super(readerInterceptorEntityProviderFactories, writerInterceptorEntityProviderFactories, messageBodyReaderEntityProviderFactories, messageBodyWriterEntityProviderFactories, exceptionMapperEntityProviderFactories);
  }

  @Override
  ClientRequestContext newRequestContext(final Request request) {
    return new ClientRequestContext(
      request,
      readerInterceptorEntityProviderFactories,
      writerInterceptorEntityProviderFactories,
      messageBodyReaderEntityProviderFactories,
      messageBodyWriterEntityProviderFactories,
      exceptionMapperEntityProviderFactories
    );
  }
}