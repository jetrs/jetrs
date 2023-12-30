/* Copyright (c) 2023 JetRS
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.ws.rs.core.MultivaluedMap;

class UnmodifiableMultivaluedArrayHashMap<K,V> extends MultivaluedArrayHashMap<K,V> {
  UnmodifiableMultivaluedArrayHashMap() {
    super(0);
  }

  @Override
  public List<V> getValues(final K key) {
    return Collections.unmodifiableList(super.getValues(key));
  }

  @Override
  public void putSingle(final K key, final V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final K key, final V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addAll(final K key, final V ... newValues) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAll(final K key, final List<V> newValues) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addFirst(final K key, final V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeValue(final K key, final V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeIf(final K key, final Predicate<? super V> test) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAllIf(final K key, final Predicate<? super V> test) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equalsIgnoreValueOrder(final MultivaluedMap<K,V> otherMap) {
    return super.equalsIgnoreValueOrder(otherMap);
  }

  @Override
  public List<V> get(final Object key) {
    final List<V> value = super.get(key);
    return value == null ? null : Collections.unmodifiableList(value);
  }

  @Override
  public List<V> put(final K key, final List<V> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(final Map<? extends K,? extends List<V>> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<V> remove(final Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    return Collections.unmodifiableSet(super.keySet());
  }

  @Override
  public Collection<List<V>> values() {
    return Collections.unmodifiableCollection(super.values());
  }

  @Override
  public Set<Entry<K,List<V>>> entrySet() {
    return Collections.unmodifiableSet(super.entrySet());
  }

  @Override
  public List<V> getOrDefault(final Object key, final List<V> defaultValue) {
    final List<V> value = super.getOrDefault(key, defaultValue);
    return value == null ? null : Collections.unmodifiableList(value);
  }

  @Override
  public List<V> putIfAbsent(final K key, final List<V> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean replace(final K key, final List<V> oldValue, final List<V> newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<V> replace(final K key, final List<V> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<V> computeIfAbsent(final K key, final Function<? super K,? extends List<V>> mappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<V> computeIfPresent(final K key, final BiFunction<? super K,? super List<V>,? extends List<V>> remappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<V> compute(final K key, final BiFunction<? super K,? super List<V>,? extends List<V>> remappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<V> merge(final K key, final List<V> value, final BiFunction<? super List<V>,? super List<V>,? extends List<V>> remappingFunction) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replaceAll(final BiFunction<? super K,? super List<V>,? extends List<V>> function) {
    throw new UnsupportedOperationException();
  }
}