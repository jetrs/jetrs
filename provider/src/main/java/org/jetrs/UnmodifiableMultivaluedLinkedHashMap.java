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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.jetty.util.MultiMap;

class UnmodifiableMultivaluedLinkedHashMap<V> extends MultiMap<V> implements MultivaluedArrayMap<String,V> {
  private boolean isUnmodifiable;

  @Override
  public void putSingle(final String key, final V value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

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
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.addValues(key, newValues);
  }

  @Override
  public void addAll(final String key, final List<V> valueList) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.addValues(key, valueList);
  }

  @Override
  public void addFirst(final String key, final V value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.add(key, value);
  }

  @Override
  public boolean equalsIgnoreValueOrder(final MultivaluedMap<String,V> otherMap) {
    return otherMap != null && otherMap.equalsIgnoreValueOrder(this);
  }


  void setUnmodifiable() {
    isUnmodifiable = true;
  }

  @Override
  public List<V> getValues(final String key) {
    return isUnmodifiable ? Collections.unmodifiableList(super.getValues(key)) : super.getValues(key);
  }

  @Override
  public void add(final String key, final V value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.add(key, value);
  }

  @Override
  public boolean removeValue(final String key, final V value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.removeValue(key, value);
  }

  @Override
  public boolean removeIf(final String key, final Predicate<? super V> test) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAllIf(final String key, final Predicate<? super V> test) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<V> get(final Object key) {
    final List<V> value = super.get(key);
    return value == null ? null : isUnmodifiable ? Collections.unmodifiableList(value) : value;
  }

  @Override
  public List<V> put(final String key, final List<V> value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.put(key, value);
  }

  @Override
  public void putAll(final Map<? extends String,? extends List<V>> m) {
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
  public Set<String> keySet() {
    return isUnmodifiable ? Collections.unmodifiableSet(super.keySet()) : super.keySet();
  }

  @Override
  public Collection<List<V>> values() {
    return isUnmodifiable ? Collections.unmodifiableCollection(super.values()) : super.values();
  }

  @Override
  public Set<Entry<String,List<V>>> entrySet() {
    return isUnmodifiable ? Collections.unmodifiableSet(super.entrySet()) : super.entrySet();
  }

  @Override
  public List<V> getOrDefault(final Object key, final List<V> defaultValue) {
    final List<V> value = super.getOrDefault(key, defaultValue);
    return value == null ? null : isUnmodifiable ? Collections.unmodifiableList(value) : value;
  }

  @Override
  public List<V> putIfAbsent(final String key, final List<V> value) {
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
  public boolean replace(final String key, final List<V> oldValue, final List<V> newValue) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.replace(key, oldValue, newValue);
  }

  @Override
  public List<V> replace(final String key, final List<V> value) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.replace(key, value);
  }

  @Override
  public List<V> computeIfAbsent(final String key, final Function<? super String,? extends List<V>> mappingFunction) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.computeIfAbsent(key, mappingFunction);
  }

  @Override
  public List<V> computeIfPresent(final String key, final BiFunction<? super String,? super List<V>,? extends List<V>> remappingFunction) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.computeIfPresent(key, remappingFunction);
  }

  @Override
  public List<V> compute(final String key, final BiFunction<? super String,? super List<V>,? extends List<V>> remappingFunction) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.compute(key, remappingFunction);
  }

  @Override
  public List<V> merge(final String key, final List<V> value, final BiFunction<? super List<V>,? super List<V>,? extends List<V>> remappingFunction) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    return super.merge(key, value, remappingFunction);
  }

  @Override
  public void replaceAll(final BiFunction<? super String,? super List<V>,? extends List<V>> function) {
    if (isUnmodifiable)
      throw new UnsupportedOperationException();

    super.replaceAll(function);
  }
}