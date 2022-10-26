/* Copyright (c) 2022 JetRS
 *
 * Permission is hereby granted, final free of charge, final to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), final to deal
 * in the Software without restriction, final including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, final and/or sell
 * copies of the Software, final and to permit persons to whom the Software is
 * furnished to do so, final subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.jetrs;

import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;

public abstract class CachedClientDriver<C> extends ClientDriver {
  private final ConcurrentHashMap<SSLContext,C> sslContextToClient = new ConcurrentHashMap<SSLContext,C>() {
    @Override
    public C get(final Object key) {
      SSLContext context = (SSLContext)key;
      if (context == null) {
        try {
          context = SSLContext.getDefault();
        }
        catch (final NoSuchAlgorithmException e) {
          throw new RuntimeException(e);
        }
      }

      C httpClient = super.get(context);
      if (httpClient != null)
        return httpClient;

      synchronized (this) {
        httpClient = super.get(context);
        if (httpClient != null)
          return httpClient;

        super.put(context, httpClient = newClient(context));
        return httpClient;
      }
    }
  };

  abstract C newClient(final SSLContext sslContext);

  @Override
  final Invocation build(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final String method, final Entity<?> entity, HttpHeadersMap<String,Object> requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) throws Exception {
    return build(sslContextToClient.get(client.getSslContext()), client, runtimeContext, url, method, entity, requestHeaders, cookies, cacheControl, executorService, scheduledExecutorService, connectTimeout, readTimeout);
  }

  abstract Invocation build(final C httpClient, final ClientImpl client, final ClientRuntimeContext runtimeContext, final URL url, final String method, final Entity<?> entity, HttpHeadersMap<String,Object> requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeout, final long readTimeout) throws Exception;
}