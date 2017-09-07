/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;

import org.lib4j.util.MirroredList;
import org.lib4j.util.ObservableMap;

public class MirroredMultivaluedHashMap<K,V,M> extends ObservableMap<K,List<V>> implements MultivaluedMap<K,V>, Cloneable, Serializable {
  private static final long serialVersionUID = -7406535904458617108L;

  @SuppressWarnings("rawtypes")
  private final Class<? extends List> componentType;
  protected MirroredMultivaluedHashMap<K,M,V> mirroredMap;
  private final Function<V,M> mirror;

  @SuppressWarnings("rawtypes")
  public MirroredMultivaluedHashMap(final Class<? extends List> componentType, final Function<V,M> mirror1, final Function<M,V> mirror2) {
    super(new HashMap<K,List<V>>());
    this.componentType = componentType;
    this.mirroredMap = new MirroredMultivaluedHashMap<K,M,V>(this, mirror2);
    this.mirror = mirror1;
  }

  private MirroredMultivaluedHashMap(final MirroredMultivaluedHashMap<K,M,V> mirroredMap, final Function<V,M> mirror) {
    super(new HashMap<K,List<V>>());
    this.componentType = mirroredMap.componentType;
    this.mirroredMap = mirroredMap;
    this.mirror = mirror;
  }

  public MultivaluedMap<K,M> getMirroredMap() {
    return mirroredMap;
  }

  public Function<V,M> getMirror() {
    return mirror;
  }

  protected final List<V> getValues(final K key) {
    List<V> values = get(key);
    if (values == null)
      put(key, values = new MirroredList<V,M>(componentType, mirror, mirroredMap.mirror));

    return values;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void afterPut(final K key, final List<V> oldValue, final List<V> newValue) {
    final MirroredList<V,M> list = (MirroredList<V,M>)get(key);
    mirroredMap.source.put(key, list == null ? null : list.getMirror());
  }

  @Override
  protected void beforeRemove(final Object key, final List<V> value) {
    mirroredMap.source.remove(key);
  }

  @Override
  public void putSingle(final K key, final V value) {
    final List<V> values = getValues(key);
    values.clear();
    values.add(value);
  }

  @Override
  public void add(final K key, final V value) {
    getValues(key).add(value);
  }

  @Override
  public V getFirst(final K key) {
    final List<V> value = get(key);
    return value != null ? value.get(0) : null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addAll(final K key, final V ... newValues) {
    if (newValues.length != 0)
      addAll(key, Arrays.asList(newValues));
  }

  @Override
  public void addAll(final K key, final List<V> valueList) {
    getValues(key).addAll(valueList);
  }

  @Override
  public void addFirst(final K key, final V value) {
    getValues(key).add(0, value);
  }

  @Override
  public boolean equalsIgnoreValueOrder(final MultivaluedMap<K,V> otherMap) {
    if (otherMap == this)
      return true;

    if (!keySet().equals(otherMap.keySet()))
      return false;

    for (final Map.Entry<K,List<V>> entry : entrySet()) {
      final List<V> otherValue = otherMap.get(entry.getKey());
      if (otherValue.size() != entry.getValue().size() || !otherValue.containsAll(entry.getValue()))
        return false;
    }

    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<V> put(final K key, final List<V> value) {
    final MirroredList<V,M> list;
    if (value instanceof MirroredList) {
      list = (MirroredList<V,M>)value;
    }
    else {
      list = new MirroredList<V,M>(componentType, mirror, mirroredMap.mirror);
      list.addAll(value);
    }

    return super.put(key, list);
  }

  @SuppressWarnings("unchecked")
  private MirroredMultivaluedHashMap<K,V,M> superClone() {
    try {
      return (MirroredMultivaluedHashMap<K,V,M>)super.clone();
    }
    catch (final CloneNotSupportedException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  @Override
  public MirroredMultivaluedHashMap<K,V,M> clone() {
    final MirroredMultivaluedHashMap<K,V,M> clone = superClone();
    clone.mirroredMap = mirroredMap.superClone();
    return clone;
  }
}