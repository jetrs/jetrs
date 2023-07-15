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

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
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

  abstract C newClient(SSLContext sslContext);

  @Override
  final Invocation build(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final String method, final HttpHeadersImpl requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final Entity<?> entity, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final HashMap<String,Object> properties, final long connectTimeout, final long readTimeout) throws Exception {
    return build(sslContextToClient.get(client.getSslContext()), client, runtimeContext, uri, method, requestHeaders, cookies, cacheControl, entity, executorService, scheduledExecutorService, properties, connectTimeout, readTimeout);
  }

  abstract Invocation build(C httpClient, ClientImpl client, ClientRuntimeContext runtimeContext, URI uri, String method, HttpHeadersImpl requestHeaders, ArrayList<Cookie> cookies, CacheControl cacheControl, Entity<?> entity, ExecutorService executorService, ScheduledExecutorService scheduledExecutorService, HashMap<String,Object> properties, long connectTimeout, long readTimeout) throws Exception;
}