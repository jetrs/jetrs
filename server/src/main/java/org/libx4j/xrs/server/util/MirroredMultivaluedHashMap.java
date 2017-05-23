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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.lib4j.util.MirroredList;
import org.lib4j.util.PartialMap;

public class MirroredMultivaluedHashMap<K,V,M> extends PartialMap<K,List<V>> implements MultivaluedMap<K,V> {
  private static final long serialVersionUID = 2648516310407246308L;

  @SuppressWarnings("rawtypes")
  private final Class<? extends List> listType;
  private final MirroredMultivaluedHashMap<K,M,V> mirroredMap;
  private final MirroredList.Mirror<V,M> mirror;

  @SuppressWarnings("rawtypes")
  public MirroredMultivaluedHashMap(final Class<? extends Map> type, final Class<? extends List> listType, final MirroredList.Mirror<V,M> mirror1, final MirroredList.Mirror<M,V> mirror2) {
    super(type);
    this.listType = listType;
    this.mirroredMap = new MirroredMultivaluedHashMap<K,M,V>(this, mirror2);
    this.mirror = mirror1;
  }

  private MirroredMultivaluedHashMap(final MirroredMultivaluedHashMap<K,M,V> mirroredMap, final MirroredList.Mirror<V,M> mirror) {
    super(mirroredMap.map.getClass());
    this.listType = mirroredMap.listType;
    this.mirroredMap = mirroredMap;
    this.mirror = mirror;
  }

  public MultivaluedMap<K,M> getMirroredMap() {
    return mirroredMap;
  }

  public MirroredList.Mirror<V,M> getMirror() {
    return mirror;
  }

  protected final List<V> getValues(final K key) {
    List<V> values = get(key);
    if (values == null)
      put(key, values = new MirroredList<V,M>(listType, mirror, mirroredMap.mirror));

    return values;
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
      list = new MirroredList<V,M>(listType, mirror, mirroredMap.mirror);
      list.addAll(value);
    }

    mirroredMap.map.put(key, list.getMirror());
    return map.put(key, list);
  }

  @Override
  public List<V> remove(final Object key) {
    mirroredMap.map.remove(key);
    return map.remove(key);
  }

  @Override
  public MirroredMultivaluedHashMap<K,V,M> clone() {
    final MirroredMultivaluedHashMap<K,V,M> clone = new MirroredMultivaluedHashMap<K,V,M>(map.getClass(), listType, mirror, mirroredMap.mirror);
    clone.putAll(this);
    return clone;
  }
}