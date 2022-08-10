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

package org.jetrs;

import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;

abstract class Invoker<R> {
  final ClientImpl client;
  final ClientRuntimeContext runtimeContext;
  final URL url;
  final ExecutorService executorService;
  final long connectTimeout;
  final long readTimeout;

  Invoker(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final ExecutorService executorService, final long connectTimeout, final long readTimeout) {
    this.client = client;
    this.runtimeContext = runtimeContext;
    this.url = url;
    this.executorService = executorService;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  public final R get() {
    return method(HttpMethod.GET, null);
  }

  public final R put(final Entity<?> entity) {
    return method(HttpMethod.PUT, entity);
  }

  public final R post(final Entity<?> entity) {
    return method(HttpMethod.POST, entity);
  }

  public final R delete() {
    return method(HttpMethod.DELETE, null);
  }

  public final R head() {
    return method(HttpMethod.HEAD, null);
  }

  public final R options() {
    return method(HttpMethod.OPTIONS, null);
  }

  public final R trace() {
    return method("TRACE", null);
  }

  public abstract R method(String name, Entity<?> entity);

  Invocation build(final String method, final Entity<?> entity, final HttpHeadersImpl requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl) {
    client.assertNotClosed();
    final HttpHeadersImpl headers = requestHeaders != null ? requestHeaders.clone() : new HttpHeadersImpl();
    if (entity != null && entity.getMediaType() != null && headers.getMediaType() == null)
      headers.setMediaType(entity.getMediaType());

    return new InvocationImpl(client, runtimeContext, url, method, entity, headers, cookies, cacheControl, executorService, connectTimeout, readTimeout);
  }
}