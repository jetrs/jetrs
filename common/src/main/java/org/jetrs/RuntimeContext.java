/* Copyright (c) 2021 JetRS
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

abstract class RuntimeContext {
  private final Configuration configuration;
  final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorProviderFactories;
  final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorProviderFactories;
  final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderProviderFactories;
  final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterProviderFactories;
  final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperProviderFactories;

  RuntimeContext(
    final Configuration configuration,
    final ArrayList<MessageBodyComponent<ReaderInterceptor>> readerInterceptorProviderFactories,
    final ArrayList<MessageBodyComponent<WriterInterceptor>> writerInterceptorProviderFactories,
    final ArrayList<MessageBodyComponent<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final ArrayList<TypeComponent<ExceptionMapper<?>>> exceptionMapperProviderFactories
  ) {
    this.configuration = configuration;
    this.readerInterceptorProviderFactories = readerInterceptorProviderFactories;
    this.writerInterceptorProviderFactories = writerInterceptorProviderFactories;
    this.messageBodyReaderProviderFactories = messageBodyReaderProviderFactories;
    this.messageBodyWriterProviderFactories = messageBodyWriterProviderFactories;
    this.exceptionMapperProviderFactories = exceptionMapperProviderFactories;
  }

  abstract RequestContext<?> localRequestContext();

  Configuration getConfiguration() {
    return configuration;
  }

  ArrayList<MessageBodyComponent<ReaderInterceptor>> getReaderInterceptorProviderFactories() {
    return readerInterceptorProviderFactories;
  }

  ArrayList<MessageBodyComponent<WriterInterceptor>> getWriterInterceptorProviderFactories() {
    return writerInterceptorProviderFactories;
  }

  ArrayList<MessageBodyComponent<MessageBodyReader<?>>> getMessageBodyReaderProviderFactories() {
    return messageBodyReaderProviderFactories;
  }

  ArrayList<MessageBodyComponent<MessageBodyWriter<?>>> getMessageBodyWriterProviderFactories() {
    return messageBodyWriterProviderFactories;
  }

  ArrayList<TypeComponent<ExceptionMapper<?>>> getExceptionMapperProviderFactories() {
    return exceptionMapperProviderFactories;
  }
}