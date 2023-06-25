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

package org.jetrs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.libj.lang.ObjectUtil;
import org.libj.util.MirrorList;
import org.libj.util.MirrorMap;

/**
 * A {@link MirrorMap} that implements the {@link MultivaluedMap} interface.
 *
 * @param <K> The type of keys maintained by this map.
 * @param <V> The type of value elements in this map.
 * @param <R> The type of reflected value elements in the mirror map.
 */
class MirrorMultivaluedArrayMap<K,V,R> extends MirrorMap<K,List<V>,List<R>> implements MultivaluedArrayMap<K,V>, Cloneable {
  /**
   * Interface providing methods for the reflection of one type of object to another, and vice-versa.
   *
   * @param <K> The type of key object of this {@link Mirror}.
   * @param <V> The type of value object of this {@link Mirror}.
   * @param <R> The type of reflected value object of this {@link Mirror}.
   */
  interface Mirror<K,V,R> extends MirrorMap.Mirror<K,List<V>,List<R>> {
    @Override
    MirrorList<R,List<R>,V,List<V>> valueToReflection(K key, List<V> value);

    @Override
    MirrorList<V,List<V>,R,List<R>> reflectionToValue(K key, List<R> reflection);

    @Override
    default Mirror<K,R,V> reverse() {
      return new Mirror<K,R,V>() {
        @Override
        public MirrorList<V,List<V>,R,List<R>> valueToReflection(final K key, final List<R> value) {
          return Mirror.this.reflectionToValue(key, value);
        }

        @Override
        public MirrorList<R,List<R>,V,List<V>> reflectionToValue(final K key, final List<V> reflection) {
          return Mirror.this.valueToReflection(key, reflection);
        }
      };
    }
  }

  /**
   * Casts the specified {@code map} of type <b>{@link Map Map&lt;K,E&gt;}</b> to type <b>{@link Map Map&lt;K,E&gt;} &amp;
   * {@link Cloneable}</b>.
   *
   * @param <C> The type parameter for {@link Map Map&lt;K,E&gt;} & {@link Cloneable}.
   * @param <K> The type of keys maintained by the specified {@link Map Map&lt;K,E&gt;}.
   * @param <V> The type of mapped values in the specified {@link Map Map&lt;K,E&gt;}.
   * @param map The {@code map} of type <b>{@link Map Map&lt;K,E&gt;}</b> to cast to type <b>{@link Map Map&lt;K,E&gt;} &
   *          {@link Cloneable}</b>.
   * @return The specified {@code map} of type <b>{@link Map Map&lt;K,E&gt;}</b> cast to type <b>{@link Map Map&lt;K,E&gt;} &amp;
   *         {@link Cloneable}</b>.
   */
  @SuppressWarnings("unchecked")
  static <C extends Map<K,V> & Cloneable,K,V>C toCloneable(final Map<K,V> map) {
    return (C)map;
  }

  protected Mirror<K,R,V> reverse;

  /**
   * Creates a new {@link MirrorMultivaluedArrayMap} with the specified target maps and {@link Mirror}. The specified target maps are
   * meant to be empty, as they become the underlying maps of the new {@link MirrorMultivaluedArrayMap} instance. The specified
   * {@link Mirror} provides the {@link Mirror#valueToReflection(Object,List) V -&gt; R} and
   * {@link Mirror#reflectionToValue(Object,List) R -&gt; V} methods, which are used to reflect object values from one
   * {@link MirrorMultivaluedArrayMap} to the other.
   *
   * @param <CloneableValues> The type parameter constraining the {@code values} argument to {@link Map Map&lt;K,V&gt;} &amp;
   *          {@link Cloneable}.
   * @param <CloneableReflections> The type parameter constraining the {@code values} argument to {@link Map Map&lt;K,R&gt;} &amp;
   *          {@link Cloneable}.
   * @param values The underlying map of type {@code <K,List<V>> & Cloneable}.
   * @param reflections The underlying map of type {@code <K,List<R>> & Cloneable}.
   * @param mirror The {@link Mirror} specifying the {@link Mirror#valueToReflection(Object,List) V -&gt; R} and
   *          {@link Mirror#reflectionToValue(Object,List) R -&gt; V} methods.
   * @throws NullPointerException If any of the specified parameters is null.
   */
  <CloneableValues extends Map<K,List<V>> & Cloneable,CloneableReflections extends Map<K,List<R>> & Cloneable>MirrorMultivaluedArrayMap(final CloneableValues values, final CloneableReflections reflections, final Mirror<K,V,R> mirror) {
    super(values, reflections, mirror);
  }

  /**
   * Creates a new {@link MirrorMultivaluedArrayMap} with the specified maps and mirror. This method is specific for the construction of
   * a reflected {@link MirrorMultivaluedArrayMap} instance.
   *
   * @param mirrorMap The {@link MirrorMultivaluedArrayMap} for which {@code this} map will be a reflection. Likewise, {@code this} map
   *          will be a reflection for {@code mirrorMap}.
   * @param values The underlying map of type {@code <K,List<V>>}, which is implicitly assumed to also be {@link Cloneable}.
   * @param mirror The {@link Mirror} specifying the {@link Mirror#valueToReflection(Object,List) V -&gt; R} and
   *          {@link Mirror#reflectionToValue(Object,List) R -&gt; V} methods.
   */
  protected MirrorMultivaluedArrayMap(final MirrorMultivaluedArrayMap<K,R,V> mirrorMap, final Map<K,List<V>> values, final Mirror<K,V,R> mirror) {
    super(mirrorMap, values, mirror);
  }

  @Override
  protected MirrorMap<K,List<V>,List<R>> newInstance(final Map<K,List<V>> values, final Map<K,List<R>> reflections) {
    return new MirrorMultivaluedArrayMap<>(toCloneable(values), toCloneable(reflections), getMirror());
  }

  @Override
  protected MirrorMap<K,List<R>,List<V>> newMirrorInstance(final Map<K,List<R>> values) {
    return new MirrorMultivaluedArrayMap<>(this, values, getReverseMirror());
  }

  @Override
  public MirrorMultivaluedArrayMap<K,R,V> getMirrorMap() {
    return (MirrorMultivaluedArrayMap<K,R,V>)super.getMirrorMap();
  }

  @Override
  public Mirror<K,V,R> getMirror() {
    return (Mirror<K,V,R>)super.getMirror();
  }

  /**
   * Returns the reverse {@link Mirror}, and caches it for subsequent retrieval, avoiding reinstantiation.
   *
   * @return The reverse {@link Mirror}.
   */
  @Override
  protected Mirror<K,R,V> getReverseMirror() {
    return reverse == null ? reverse = getMirror().reverse() : reverse;
  }

  @Override
  @SuppressWarnings("unchecked")
  public MirrorList<V,List<V>,R,List<R>> get(final Object key) {
    return (MirrorList<V,List<V>,R,List<R>>)super.get(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public MirrorList<V,List<V>,R,List<R>> put(final K key, final List<V> value) {
    return (MirrorList<V,List<V>,R,List<R>>)super.put(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public MirrorList<V,List<V>,R,List<R>> putIfAbsent(final K key, final List<V> value) {
    return (MirrorList<V,List<V>,R,List<R>>)super.putIfAbsent(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public MirrorList<V,List<V>,R,List<R>> remove(final Object key) {
    return (MirrorList<V,List<V>,R,List<R>>)super.remove(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public MirrorList<V,List<V>,R,List<R>> replace(final K key, final List<V> value) {
    return (MirrorList<V,List<V>,R,List<R>>)super.replace(key, value);
  }

  /**
   * Returns the list associated to the specified key, creating one via {@link Mirror#reflectionToValue(Object,List)} if a list does
   * not exist.
   *
   * @param key The key.
   * @return The list associated to the specified key, creating one via {@link Mirror#reflectionToValue(Object,List)} if a list does
   *         not exist.
   */
  @Override
  public final MirrorList<V,List<V>,R,List<R>> getValues(final K key) {
    MirrorList<V,List<V>,R,List<R>> values = get(key);
    if (values == null)
      put(key, values, values = getMirror().reflectionToValue(key, null));

    return values;
  }

  @Override
  public V getFirst(final K key) {
    final List<V> value = get(key);
    return value == null || value.size() == 0 ? null : value.get(0);
  }

  @Override
  public void putSingle(final K key, final V value) {
    MirrorList<V,List<V>,R,List<R>> values = get(key);
    put(key, values, values = getMirror().reflectionToValue(key, null));
    values.add(value);
  }

  @Override
  public void add(final K key, final V value) {
    getValues(key).add(value);
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

    if (size() > 0) {
      for (final Map.Entry<K,List<V>> entry : entrySet()) { // [S]
        final List<V> otherValue = otherMap.get(entry.getKey());
        if (otherValue.size() != entry.getValue().size() || !otherValue.containsAll(entry.getValue()))
          return false;
      }
    }

    return true;
  }

  @SuppressWarnings("unchecked")
  private MirrorList<V,List<V>,R,List<R>> ensureMirrorList(final K key, final List<V> value) {
    return value instanceof MirrorList ? (MirrorList<V,List<V>,R,List<R>>)value : getMirror().valueToReflection(key, value).reverse();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected MirrorList<V,List<V>,R,List<R>> put(final K key, final List<V> oldValue, final List<V> newValue) {
    return (MirrorList<V,List<V>,R,List<R>>)super.put(key, oldValue, ensureMirrorList(key, newValue));
  }

  @SuppressWarnings("unchecked")
  private MirrorMultivaluedArrayMap<K,V,R> clone$() {
    try {
      final MirrorMultivaluedArrayMap<K,V,R> clone = (MirrorMultivaluedArrayMap<K,V,R>)super.clone();
      clone.entrySet = null;
      clone.keySet = null;
      clone.values = null;
      clone.target = (Map<?,?>)ObjectUtil.clone((Cloneable)target);
      return clone;
    }
    catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public MirrorMultivaluedArrayMap<K,V,R> clone() {
    final MirrorMultivaluedArrayMap<K,V,R> clone = clone$();
    clone.mirrorMap = getMirrorMap().clone$();
    clone.getMirrorMap().mirrorMap = clone;
    return clone;
  }
}