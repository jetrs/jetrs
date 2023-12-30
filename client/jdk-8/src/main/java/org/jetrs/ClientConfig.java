/* Copyright (c) 2023 JetRS
 *
 * Permission is hereby granted, final free of charge, to any person obtaining a copy
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

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

class ClientConfig {
  private static final HashMap<ClientConfig,Integer> refCounts = new HashMap<>();
  private static final ArrayList<Consumer<ClientConfig>> notifyOnRelease = new ArrayList<>(2);

  static void notifyOnRelease(final Consumer<ClientConfig> onRelease) {
    synchronized (refCounts) {
      notifyOnRelease.add(onRelease);
    }
  }

  final SSLContext sslContext;
  final ProxyConfig proxyConfig;
  final int maxConnectionsPerDestination;
  private boolean released;

  ClientConfig(final SSLContext sslContext, final int maxConnectionsPerDestination, final String proxyUri) throws NoSuchAlgorithmException {
    this.sslContext = sslContext != null ? sslContext : SSLContext.getDefault();
    this.maxConnectionsPerDestination = maxConnectionsPerDestination;
    this.proxyConfig = proxyUri != null ? new ProxyConfig(proxyUri) : null;
    synchronized (refCounts) {
      refCounts.put(this, refCounts.getOrDefault(this, 0) + 1);
    }
  }

  void release() {
    if (released)
      return;

    synchronized (refCounts) {
      if (released)
        return;

      released = true;
      final int count = refCounts.get(this) - 1;
      if (count == 1)
        refCounts.remove(this);
      else
        refCounts.put(this, count);

      if (count == 0)
        for (int i = 0, i$ = notifyOnRelease.size(); i < i$; ++i) // [RA]
          notifyOnRelease.get(i).accept(this);
    }
  }

  @Override
  public int hashCode() {
    int hashCode = 1 + sslContext.hashCode();
    hashCode = 31 * hashCode + Integer.hashCode(maxConnectionsPerDestination);
    if (proxyConfig != null)
      hashCode = 31 * hashCode + proxyConfig.hashCode();

    return hashCode;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof ClientConfig))
      return false;

    final ClientConfig that = (ClientConfig)obj;
    return sslContext.equals(that.sslContext) && maxConnectionsPerDestination == that.maxConnectionsPerDestination && Objects.equals(proxyConfig, that.proxyConfig);
  }
}