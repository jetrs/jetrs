/* Copyright (c) 2016 OpenJAX
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

package org.openjax.xrs.server.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.ws.rs.core.MultivaluedMap;

import org.openjax.standard.util.MirrorList;
import org.openjax.standard.util.ObservableMap;

public class MirrorMultivaluedMap<K,V,M> extends ObservableMap<K,List<V>> implements MultivaluedMap<K,V>, Cloneable, Serializable {
  private static final long serialVersionUID = -7406535904458617108L;

  private class KeyMirrorList<A,B> extends MirrorList<A,B> {
    private final K key;

    public KeyMirrorList(final List<A> a, final List<B> b, final K key, final BiFunction<K,A,B> forward, final BiFunction<K,B,A> reverse) {
      super(a, b, (v) -> forward.apply(key, v), (v) -> reverse.apply(key, v));
      this.key = key;
    }

    public KeyMirrorList(final KeyMirrorList<A,B> copy, final K key, final BiFunction<K,A,B> forward, final BiFunction<K,B,A> reverse) {
      super(cloneList((List<A>)copy), cloneList((List<B>)copy.getMirror()), (v) -> forward.apply(key, v), (v) -> reverse.apply(key, v));
      this.key = key;
    }
  }

  private <E>List<E> cloneList(final List<E> list) {
    final List<E> clone = listSupplier.get();
    clone.addAll(list);
    return clone;
  }

  private final Supplier<? extends Map> mapSupplier;
  private final Supplier<? extends List> listSupplier;
  private final BiFunction<K,V,M> mirror;

  protected MirrorMultivaluedMap<K,M,V> mirroredMap;

  public MirrorMultivaluedMap(final Supplier<? extends Map<K,List<V>>> mapSupplier, final Supplier<List<V>> listSupplier, final BiFunction<K,V,M> mirror1, final BiFunction<K,M,V> mirror2) {
    super(mapSupplier.get());
    this.mapSupplier = Objects.requireNonNull(mapSupplier);
    this.listSupplier = Objects.requireNonNull(listSupplier);
    this.mirror = Objects.requireNonNull(mirror1);
    this.mirroredMap = new MirrorMultivaluedMap<>(this, Objects.requireNonNull(mirror2));
  }

  private MirrorMultivaluedMap(final MirrorMultivaluedMap<K,M,V> mirroredMap, final BiFunction<K,V,M> mirror) {
    super(mirroredMap.mapSupplier.get());
    this.mapSupplier = mirroredMap.mapSupplier;
    this.listSupplier = mirroredMap.listSupplier;
    this.mirror = mirror;
    this.mirroredMap = mirroredMap;
  }

  public MultivaluedMap<K,M> getMirror() {
    return mirroredMap;
  }

  protected final List<V> getValues(final K key) {
    List<V> values = get(key);
    if (values == null)
      put(key, values = new KeyMirrorList<V,M>(listSupplier.get(), listSupplier.get(), key, mirror, mirroredMap.mirror));

    return values;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void afterPut(final K key, final List<V> oldValue, final List<V> newValue, final RuntimeException re) {
    final KeyMirrorList<V,M> list = (KeyMirrorList<V,M>)get(key);
    mirroredMap.target.put(key, list == null ? null : list.getMirror());
  }

  @Override
  protected boolean beforeRemove(final Object key, final List<V> value) {
    mirroredMap.target.remove(key);
    return true;
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
    final KeyMirrorList<V,M> mirrorList;
    if (!(value instanceof KeyMirrorList)) {
      mirrorList = new KeyMirrorList<V,M>(listSupplier.get(), listSupplier.get(), key, mirror, mirroredMap.mirror);
      mirrorList.addAll(value);
    }
    else {
      final KeyMirrorList<V,M> list = (KeyMirrorList<V,M>)value;
      if (key == null ? list.key == null : key.equals(list.key))
        mirrorList = list;
      else
        mirrorList = new KeyMirrorList<>(list, key, mirror, mirroredMap.mirror);
    }

    return super.put(key, mirrorList);
  }

  @SuppressWarnings("unchecked")
  private MirrorMultivaluedMap<K,V,M> superClone() {
    try {
      return (MirrorMultivaluedMap<K,V,M>)super.clone();
    }
    catch (final CloneNotSupportedException e) {
      throw new UnsupportedOperationException(e);
    }
  }

  @Override
  public MirrorMultivaluedMap<K,V,M> clone() {
    final MirrorMultivaluedMap<K,V,M> clone = superClone();
    clone.mirroredMap = mirroredMap.superClone();
    clone.mirroredMap.mirroredMap = clone;
    return clone;
  }
}