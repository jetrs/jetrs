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

package org.jetrs.client;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Providers;

import org.jetrs.common.core.HttpHeadersImpl;
import org.jetrs.common.util.HttpHeadersMap;
import org.jetrs.common.util.MirrorMultivaluedMap;

abstract class Invoker<R> {
  final ClientImpl client;
  final Providers providers;
  final URL url;
  final ExecutorService executorService;
  final long connectTimeout;
  final long readTimeout;

  Invoker(final ClientImpl client, final Providers providers, final URL url, final ExecutorService executorService, final long connectTimeout, final long readTimeout) {
    this.client = client;
    this.providers = providers;
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

  public abstract R method(final String name, final Entity<?> entity);

  @SuppressWarnings("unchecked")
  Invocation build(final String method, final Entity<?> entity, final MultivaluedMap<String,Object> requestHeaders, final List<Cookie> cookies, final CacheControl cacheControl) {
    client.assertNotClosed();
    final MirrorMultivaluedMap<String,Object,String> headers = requestHeaders == null ? new HttpHeadersImpl().getMirrorMap() : requestHeaders instanceof HttpHeadersMap ? ((HttpHeadersMap<String,Object,String>)requestHeaders).clone() : new HttpHeadersImpl(requestHeaders).getMirrorMap();
    if (entity != null && entity.getMediaType() != null)
      headers.putSingle(HttpHeaders.CONTENT_TYPE, entity.getMediaType());

    return new InvocationImpl(client, providers, url, method, entity, headers.getMirrorMap(), cookies, cacheControl, executorService, connectTimeout, readTimeout);
  }
}