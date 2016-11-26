/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.safris.commons.util.DelegatedList;
import org.safris.commons.util.MirroredDelegatedList;

public class MirroredDelegatedMultivaluedHashMap<K,V,M> extends DelegatedMultivaluedHashMap<K,V> implements Cloneable, MultivaluedMap<K,V> {
  private static final long serialVersionUID = 2648516310407246308L;

  private final MirroredDelegatedMultivaluedHashMap<K,M,V> mirroredMap;
  private final MirroredDelegatedList.Mirror<V,M> mirror;

  @SuppressWarnings("rawtypes")
  public MirroredDelegatedMultivaluedHashMap(final Class<? extends Map> type, final Class<? extends List> listType, final MirroredDelegatedList.Mirror<V,M> mirror1, final MirroredDelegatedList.Mirror<M,V> mirror2, final MultivaluedMapDelegate<K,V> delegate1, final MultivaluedMapDelegate<K,M> delegate2) {
    super(type, listType, delegate1);
    this.mirroredMap = new MirroredDelegatedMultivaluedHashMap<K,M,V>(this, mirror2, delegate2);
    this.mirror = mirror1;
  }

  public MirroredDelegatedMultivaluedHashMap(final MirroredDelegatedMultivaluedHashMap<K,V,M> copy) {
    this(copy.map.getClass(), copy.listType, copy.mirror, copy.mirroredMap.mirror, copy.delegate, copy.mirroredMap.delegate);
  }

  private MirroredDelegatedMultivaluedHashMap(final MirroredDelegatedMultivaluedHashMap<K,M,V> mirroredMap, final MirroredDelegatedList.Mirror<V,M> mirror, final MultivaluedMapDelegate<K,V> delegate) {
    super(mirroredMap.map.getClass(), mirroredMap.listType, delegate);
    this.mirroredMap = mirroredMap;
    this.mirror = mirror;
  }

  public MirroredDelegatedMultivaluedHashMap<K,M,V> getMirroredMap() {
    return mirroredMap;
  }

  public MirroredDelegatedList.Mirror<V,M> getMirror() {
    return mirror;
  }

  @Override
  protected List<V> getValues(final K key) {
    super.getValues(key);
    List<V> values = get(key);
    if (values == null)
      put(key, values = new MirroredDelegatedList<V,M>(ArrayList.class, mirror, new DelegatedList.ListDelegate<V>() {
        @Override
        public void add(final int index, final V element) {
          delegate.add(key, index, element);
        }

        @Override
        public void remove(final int index) {
          delegate.remove(key, index);
        }
      }, mirroredMap.mirror, null));

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

  private List<V> superPut(final K key, final List<V> value) {
    return super.put(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<V> put(final K key, final List<V> value) {
    final MirroredDelegatedList<V,M> list = value instanceof MirroredDelegatedList ? (MirroredDelegatedList<V,M>)value : new MirroredDelegatedList<V,M>(ArrayList.class, mirror, new DelegatedList.ListDelegate<V>() {
      @Override
      public void add(final int index, final V element) {
        delegate.add(key, index, element);
      }

      @Override
      public void remove(final int index) {
        delegate.remove(key, index);
      }
    }, mirroredMap.mirror, null);

    mirroredMap.superPut(key, list.getMirror());
    return super.put(key, list);
  }

  private List<V> superRemove(final Object key) {
    return super.remove(key);
  }

  @Override
  public List<V> remove(final Object key) {
    mirroredMap.superRemove(key);
    return super.remove(key);
  }

  @Override
  public MirroredDelegatedMultivaluedHashMap<K,V,M> clone() {
    return new MirroredDelegatedMultivaluedHashMap<K,V,M>(this);
  }
}