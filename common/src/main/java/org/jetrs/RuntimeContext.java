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
import javax.ws.rs.core.Request;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

abstract class RuntimeContext {
  private final Configuration configuration;
  final ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories;
  final ArrayList<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories;
  final ArrayList<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories;
  final ArrayList<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories;
  final ArrayList<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories;

  RuntimeContext(
    final Configuration configuration,
    final ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> readerInterceptorProviderFactories,
    final ArrayList<MessageBodyProviderFactory<WriterInterceptor>> writerInterceptorProviderFactories,
    final ArrayList<MessageBodyProviderFactory<MessageBodyReader<?>>> messageBodyReaderProviderFactories,
    final ArrayList<MessageBodyProviderFactory<MessageBodyWriter<?>>> messageBodyWriterProviderFactories,
    final ArrayList<TypeProviderFactory<ExceptionMapper<?>>> exceptionMapperProviderFactories
  ) {
    this.configuration = configuration;
    this.readerInterceptorProviderFactories = readerInterceptorProviderFactories;
    this.writerInterceptorProviderFactories = writerInterceptorProviderFactories;
    this.messageBodyReaderProviderFactories = messageBodyReaderProviderFactories;
    this.messageBodyWriterProviderFactories = messageBodyWriterProviderFactories;
    this.exceptionMapperProviderFactories = exceptionMapperProviderFactories;
  }

  abstract RequestContext<?> localRequestContext();
  abstract RequestContext<?> newRequestContext(Request request);

  Configuration getConfiguration() {
    return configuration;
  }

  ArrayList<MessageBodyProviderFactory<ReaderInterceptor>> getReaderInterceptorProviderFactories() {
    return readerInterceptorProviderFactories;
  }

  ArrayList<MessageBodyProviderFactory<WriterInterceptor>> getWriterInterceptorProviderFactories() {
    return writerInterceptorProviderFactories;
  }

  ArrayList<MessageBodyProviderFactory<MessageBodyReader<?>>> getMessageBodyReaderProviderFactories() {
    return messageBodyReaderProviderFactories;
  }

  ArrayList<MessageBodyProviderFactory<MessageBodyWriter<?>>> getMessageBodyWriterProviderFactories() {
    return messageBodyWriterProviderFactories;
  }

  ArrayList<TypeProviderFactory<ExceptionMapper<?>>> getExceptionMapperProviderFactories() {
    return exceptionMapperProviderFactories;
  }
}