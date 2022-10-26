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

import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;

import org.libj.lang.Systems;

public abstract class ClientDriver {
  public static final String JETRS_CLIENT_DRIVER_PROPERTY = ClientDriver.class.getName();
  private static final Telemetry telemetry = Systems.hasProperty(ClientDriver.class.getName() + ".telemetry") ? new Telemetry() : null;

  static class Statistics extends LongSummaryStatistics {
    private volatile long start = -1;
  }

  static enum Span {
    INIT,
    ENTITY_INIT,
    ENTITY_WRITE,
    RESPONSE_WAIT,
    RESPONSE_READ,
    ENTITY_READ,
    TOTAL
  }

  private static class Telemetry extends HashMap<Class<?>,EnumMap<Span,Statistics>> {
    @Override
    public EnumMap<Span,Statistics> get(final Object key) {
      EnumMap<Span,Statistics> value = super.get(key);
      if (value == null) {
        super.put((Class<?>)key, value = new EnumMap<Span,Statistics>(Span.class) {
          @Override
          public Statistics get(final Object key) {
            Statistics value = super.get(key);
            if (value == null)
              super.put((Span)key, value = new Statistics());

            return value;
          }
        });
      }

      return value;
    }

    @Override
    public String toString() {
      final StringBuilder b = new StringBuilder();
      for (final Map.Entry<Class<?>,EnumMap<Span,Statistics>> entry : entrySet()) {
        b.append("-- " + entry.getKey().getName() + " ----------------------------\n");
        b.append(String.format("%14s :: [ %10s , %10s ] @ %10s -> %10s ~ %10s", "Tag", "Min", "Max", "Count", "Sum", "Avg\n"));
        b.append("--------------------------------------------------------------------------------------\n");
        for (final Map.Entry<Span,Statistics> m : entry.getValue().entrySet()) {
          final Statistics s = m.getValue();
          b.append(String.format("%14s :: [ %10d , %10d ] @ %10d -> %10d ~ %10d\n", m.getKey(), (int)s.getMin(), (int)s.getMax(), (int)s.getCount(), (int)s.getSum(), (int)s.getAverage()));
        }
      }

      return b.toString();
    }
  }

  static void dump() {
    if (telemetry != null)
      System.err.println(telemetry);
  }

  void $telemetry(final Span ... tags) {
    if (telemetry != null) {
      final long ts = System.nanoTime() / 1000; // in microseconds
      for (final Span tag : tags) {
        final Statistics s = telemetry.get(getClass()).get(tag);
        synchronized (s) {
          if (s.start == -1) {
            s.start = ts;
          }
          else {
            s.accept(ts - s.start);
            s.start = -1;
          }
        }
      }
    }
  }

  abstract Invocation build(ClientImpl client, ClientRuntimeContext runtimeContext, URL url, String method, Entity<?> entity, HttpHeadersMap<String,Object> requestHeaders, ArrayList<Cookie> cookies, CacheControl cacheControl, ExecutorService executorService, ScheduledExecutorService scheduledExecutorService, long connectTimeout, long readTimeout, boolean async) throws Exception;
}