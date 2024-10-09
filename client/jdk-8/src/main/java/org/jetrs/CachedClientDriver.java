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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;

public abstract class CachedClientDriver<C> extends ClientDriver implements Consumer<ClientConfig> {
  private final HashMap<ClientConfig,C> clientConfigToClient = new HashMap<>();

  CachedClientDriver() {
    ClientConfig.notifyOnRelease(this);
  }

  private C getClient(final Object key) throws Exception {
    C client = clientConfigToClient.get(key);
    if (client != null)
      return client;

    synchronized (this) {
      client = clientConfigToClient.get(key);
      if (client != null)
        return client;

      final ClientConfig clientConfig = (ClientConfig)key;
      clientConfigToClient.put(clientConfig, client = newClient(clientConfig));
      return client;
    }
  }

  @Override
  public void accept(final ClientConfig clientConfig) {
    clientConfigToClient.remove(clientConfig);
  }

  abstract C newClient(ClientConfig clientConfig) throws Exception;

  @Override
  final Invocation build(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final String method, final HttpHeadersImpl requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final Entity<?> entity, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final HashMap<String,Object> properties, final long connectTimeout, final long readTimeout) throws Exception {
    return build(getClient(client.getClientConfig()), client, runtimeContext, uri, method, requestHeaders, cookies, cacheControl, entity, executorService, scheduledExecutorService, properties, connectTimeout, readTimeout);
  }

  abstract Invocation build(C httpClient, ClientImpl client, ClientRuntimeContext runtimeContext, URI uri, String method, HttpHeadersImpl requestHeaders, ArrayList<Cookie> cookies, CacheControl cacheControl, Entity<?> entity, ExecutorService executorService, ScheduledExecutorService scheduledExecutorService, HashMap<String,Object> properties, long connectTimeout, long readTimeout) throws Exception;
}