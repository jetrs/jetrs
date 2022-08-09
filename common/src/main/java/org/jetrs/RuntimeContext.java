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

import static org.libj.lang.Assertions.*;

import java.util.List;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

abstract class RuntimeContext {
  private final Configuration configuration;
  final List<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories;
  final List<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories;
  final List<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories;
  final List<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories;
  final List<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories;

  RuntimeContext(
    final Configuration configuration,
    final List<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories,
    final List<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final List<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final List<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories
  ) {
    this.configuration = configuration;
    this.readerInterceptorProviderFactories = assertNotNull(readerInterceptorProviderFactories);
    this.writerInterceptorProviderFactories = assertNotNull(writerInterceptorProviderFactories);
    this.messageBodyReaderProviderFactories = assertNotNull(messageBodyReaderProviderFactories);
    this.messageBodyWriterProviderFactories = assertNotNull(messageBodyWriterProviderFactories);
    this.exceptionMapperProviderFactories = assertNotNull(exceptionMapperProviderFactories);
  }

  abstract RequestContext<?> localRequestContext();
  abstract RequestContext<?> newRequestContext(Request request);

  Configuration getConfiguration() {
    return configuration;
  }

  List<MessageBodyProviderFactory<ReaderInterceptor>> getReaderInterceptorProviderFactories() {
    return this.readerInterceptorProviderFactories;
  }

  List<MessageBodyProviderFactory<WriterInterceptor>> getWriterInterceptorProviderFactories() {
    return this.writerInterceptorProviderFactories;
  }

  List<MessageBodyProviderFactory<MessageBodyReader<?>>> getMessageBodyReaderProviderFactories() {
    return this.messageBodyReaderProviderFactories;
  }

  List<MessageBodyProviderFactory<MessageBodyWriter<?>>> getMessageBodyWriterProviderFactories() {
    return this.messageBodyWriterProviderFactories;
  }

  List<TypeProviderFactory<ExceptionMapper<?>>> getExceptionMapperProviderFactories() {
    return this.exceptionMapperProviderFactories;
  }
}