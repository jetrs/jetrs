/* Copyright (c) 2016 JetRS
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

package org.jetrs.common.util;

import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;

public final class MultivaluedMaps {
  public static <K,V>V getFirstOrDefault(final MultivaluedMap<K,V> headers, final K key, final V defaultValue) {
    final V value = headers.getFirst(key);
    return value != null ? value : defaultValue;
  }

  public static <K,V,W>W getFirstOrDefault(final MultivaluedMap<K,V> headers, final K key, final W defaultValue, final Function<V,W> function) {
    final V value = headers.getFirst(key);
    return value != null ? function.apply(value) : defaultValue;
  }

  private MultivaluedMaps() {
  }
}