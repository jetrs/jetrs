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

class UnmodifiableMultivaluedArrayHashMap<K,V> extends MultivaluedArrayHashMap<K,V> {
  private boolean isUnmodifiable;

  UnmodifiableMultivaluedArrayHashMap() {
    super();
  }

  UnmodifiableMultivaluedArrayHashMap(final int initialCapacity) {
    super(initialCapacity);
  }

  UnmodifiableMultivaluedArrayHashMap(final Map<? extends K,? extends List<V>> map) {
    super(map);
  }

  void setUnmodifiable() {
    isUnmodifiable = true;
  }

  @Override
  public List<V> getValues(final K key) {
    return isUnmodifiable ? Collections.unmodifiableList(super.getValues(key)) : super.getValues(key);
  }

  @Override
  public void putSingle(final K key, final V value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.putSingle(key, value);
  }

  @Override
  public void add(final K key, final V value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.add(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addAll(final K key, final V ... newValues) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.addAll(key, newValues);
  }

  @Override
  public void addAll(final K key, final List<V> newValues) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.addAll(key, newValues);
  }

  @Override
  public void addFirst(final K key, final V value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.addFirst(key, value);
  }

  @Override
  public boolean removeValue(final K key, final V value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.removeValue(key, value);
  }

  @Override
  public boolean removeIf(final K key, final Predicate<? super V> test) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.removeIf(key, test);
  }

  @Override
  public boolean removeAllIf(final K key, final Predicate<? super V> test) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.removeAllIf(key, test);
  }

  @Override
  public List<V> get(final Object key) {
    final List<V> value = super.get(key);
    return value == null ? null : isUnmodifiable ? Collections.unmodifiableList(value) : value;
  }

  @Override
  public List<V> put(final K key, final List<V> value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.put(key, value);
  }

  @Override
  public void putAll(final Map<? extends K,? extends List<V>> m) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.putAll(m);
  }

  @Override
  public List<V> remove(final Object key) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.remove(key);
  }

  @Override
  public void clear() {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    return isUnmodifiable ? Collections.unmodifiableSet(super.keySet()) : super.keySet();
  }

  @Override
  public Collection<List<V>> values() {
    return isUnmodifiable ? Collections.unmodifiableCollection(super.values()) : super.values();
  }

  @Override
  public Set<Entry<K,List<V>>> entrySet() {
    return isUnmodifiable ? Collections.unmodifiableSet(super.entrySet()) : super.entrySet();
  }

  @Override
  public List<V> getOrDefault(final Object key, final List<V> defaultValue) {
    final List<V> value = super.getOrDefault(key, defaultValue);
    return value == null ? null : isUnmodifiable ? Collections.unmodifiableList(value) : value;
  }

  @Override
  public List<V> putIfAbsent(final K key, final List<V> value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.remove(key, value);
  }

  @Override
  public boolean replace(final K key, final List<V> oldValue, final List<V> newValue) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.replace(key, oldValue, newValue);
  }

  @Override
  public List<V> replace(final K key, final List<V> value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.replace(key, value);
  }

  @Override
  public List<V> computeIfAbsent(final K key, final Function<? super K,? extends List<V>> mappingFunction) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.computeIfAbsent(key, mappingFunction);
  }

  @Override
  public List<V> computeIfPresent(final K key, final BiFunction<? super K,? super List<V>,? extends List<V>> remappingFunction) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.computeIfPresent(key, remappingFunction);
  }

  @Override
  public List<V> compute(final K key, final BiFunction<? super K,? super List<V>,? extends List<V>> remappingFunction) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.compute(key, remappingFunction);
  }

  @Override
  public List<V> merge(final K key, final List<V> value, final BiFunction<? super List<V>,? super List<V>,? extends List<V>> remappingFunction) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.merge(key, value, remappingFunction);
  }

  @Override
  public void replaceAll(final BiFunction<? super K,? super List<V>,? extends List<V>> function) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.replaceAll(function);
  }
}