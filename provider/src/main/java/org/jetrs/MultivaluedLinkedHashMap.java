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

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.jetty.util.MultiMap;

class MultivaluedLinkedHashMap<V> extends MultiMap<V> implements MultivaluedArrayMap<String,V> {
  @Override
  public void putSingle(final String key, final V value) {
    super.put(key, value);
  }

  @Override
  public V getFirst(final String key) {
    final List<V> values = super.get(key);
    return values == null ? null : values.get(0);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addAll(final String key, final V ... newValues) {
    super.addValues(key, newValues);
  }

  @Override
  public void addAll(final String key, final List<V> valueList) {
    super.addValues(key, valueList);
  }

  @Override
  public void addFirst(final String key, final V value) {
    super.add(key, value);
  }

  @Override
  public boolean equalsIgnoreValueOrder(final MultivaluedMap<String,V> otherMap) {
    return otherMap != null && otherMap.equalsIgnoreValueOrder(this);
  }
}