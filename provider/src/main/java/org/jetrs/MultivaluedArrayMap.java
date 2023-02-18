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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.function.Predicate;

import javax.ws.rs.core.MultivaluedMap;

import org.libj.util.CollectionUtil;

/**
 * A {@link Map} with keys mapped to zero or more values.
 *
 * @param <K> The type of keys maintained by this map.
 * @param <V> The type of mapped values.
 */
interface MultivaluedArrayMap<K,V> extends MultivaluedMap<K,V> {
  /**
   * Returns a new instance of the value {@link List}.
   *
   * @return A new instance of the value {@link List}.
   */
  default List<V> newList() {
    return new ArrayList<>();
  }

  /**
   * Return a non-null list of values for a given key. The returned list may be empty.
   * <p>
   * If there is no entry for the key in the map, a new empty {@link List} instance is created, registered within the map to hold
   * the values of the key and returned from the method.
   *
   * @param key The key.
   * @return value The value {@link List} registered with the key. The method is guaranteed to never return {@code null}.
   */
  default List<V> getValues(final K key) {
    List<V> l = get(key);
    if (l == null)
      put(key, l = newList());

    return l;
  }

  /**
   * Set the value for the key to be a one item {@link List} consisting of the supplied value. Any existing values will be replaced.
   *
   * @param key The key.
   * @param value The single value of the key.
   */
  @Override
  default void putSingle(final K key, final V value) {
    final List<V> values = getValues(key);
    values.clear();
    values.add(value);
  }

  /**
   * Add a value to the current {@link List} of values for the supplied key.
   *
   * @param key The key.
   * @param value The value to be added.
   */
  @Override
  default void add(final K key, final V value) {
    final List<V> values = getValues(key);
    values.add(value);
  }

  /**
   * Add multiple values to the current list of values for the supplied key. If the supplied array of new values is empty, method
   * returns immediately. Method throws a {@link IllegalArgumentException} if the supplied array of values is null.
   *
   * @param key The key.
   * @param newValues The values to be added.
   * @throws NullPointerException If the supplied array of new values is null.
   */
  @Override
  @SuppressWarnings("unchecked")
  default void addAll(final K key, final V ... newValues) {
    final List<V> values = getValues(key);
    for (final V value : newValues) // [A]
      values.add(value);
  }

  /**
   * Add all values from the supplied value {@link List} to the current {@link List} of values for the supplied key. If the supplied
   * value list is empty, method returns immediately. Method throws a {@link IllegalArgumentException} if the supplied array of
   * values is null.
   *
   * @param key The key.
   * @param newValues The list of values to be added.
   * @throws NullPointerException If the supplied value list is null.
   */
  @Override
  default void addAll(final K key, final List<V> newValues) {
    CollectionUtil.addAll(getValues(key), newValues);
  }

  /**
   * Returns the first value for the provided key, or {@code null} if the keys does not map to a value.
   *
   * @param key The key.
   * @return The first value for the provided key, or {@code null} if the keys does not map to a value.
   */
  @Override
  default V getFirst(final K key) {
    final List<V> values = get(key);
    return values == null || values.size() == 0 ? null : values.get(0);
  }

  /**
   * Add a value to the first position in the current list of values for the supplied key. If the type of the value {@link List}
   * does not extend {@link List}, a {@link UnsupportedOperationException} is thrown.
   *
   * @param key The key
   * @param value The value to be added.
   * @throws UnsupportedOperationException If the type of the value {@link List} does not extend {@link List}.
   */
  @Override
  default void addFirst(final K key, final V value) {
    getValues(key).add(0, value);
  }

  /**
   * Removes the first value in the value {@link List} for the given key.
   *
   * @param key The key.
   * @param value The value in the value {@link List} to remove.
   * @return {@code true} if the method resulted in a change to the map.
   */
  default boolean removeValue(final K key, final V value) {
    final List<V> values = getValues(key);
    return values != null && values.remove(value);
  }

  /**
   * Removes the first value in the value {@link List} for the given key for which the provided {@link Predicate} returns
   * {@code true}.
   *
   * @param key The key.
   * @param test The {@link Predicate} to test whether a value is to be removed.
   * @return {@code true} if the method resulted in a change to the map.
   */
  default boolean removeIf(final K key, final Predicate<? super V> test) {
    final List<V> values = get(key);
    final int i$;
    if (values == null || (i$ = values.size()) == 0)
      return false;

    if (values instanceof RandomAccess) {
      int i = 0; do { // [RA]
        if (test.test(values.get(i))) {
          values.remove(i);
          return true;
        }
      }
      while (++i < i$);
    }
    else {
      final Iterator<V> i = values.iterator(); do { // [I]
        if (test.test(i.next())) {
          i.remove();
          return true;
        }
      }
      while (i.hasNext());
    }

    return false;
  }

  /**
   * Removes the all values in the value {@link Collection} for the given key for which the provided {@link Predicate} returns
   * {@code true}.
   *
   * @param key The key.
   * @param test The {@link Predicate} to test whether a value is to be removed.
   * @return {@code true} if the method resulted in a change to the map.
   * @throws NullPointerException If {@code test} is null.
   * @throws UnsupportedOperationException If the value {@link Collection} for the given key does not support the {@code remove}
   *           operation.
   */
  default boolean removeAllIf(final K key, final Predicate<? super V> test) {
    final List<V> values = get(key);
    final int i$;
    if (values == null || (i$ = values.size()) == 0)
      return false;

    boolean changed = false;
    if (values instanceof RandomAccess) {
      int i = 0; do { // [RA]
        if (test.test(values.get(i))) {
          values.remove(i);
          changed = true;
        }
      }
      while (++i < i$);
    }
    else {
      final Iterator<V> i = values.iterator(); do { // [I]
        if (test.test(i.next())) {
          i.remove();
          changed = true;
        }
      }
      while (i.hasNext());
    }

    return changed;
  }

  /**
   * Compare the specified map with this map for equality modulo the order of values for each key. Specifically, the values
   * associated with each key are compared as if they were ordered lists.
   *
   * @param otherMap Map to be compared to this one.
   * @return {@code true} if the maps are equal modulo value ordering.
   */
  @Override
  default boolean equalsIgnoreValueOrder(final MultivaluedMap<K,V> otherMap) {
    if (otherMap == this)
      return true;

    if (!keySet().equals(otherMap.keySet()))
      return false;

    if (size() > 0) {
      for (final Entry<K,List<V>> entry : entrySet()) { // [S]
        final List<V> olist = otherMap.get(entry.getKey());
        final List<V> value = entry.getValue();
        final int size = value.size();
        if (size != olist.size() || size > 0 && !CollectionUtil.containsAll(olist, value))
          return false;
      }
    }

    return true;
  }
}