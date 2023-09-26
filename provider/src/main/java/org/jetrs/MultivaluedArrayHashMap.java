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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultivaluedArrayHashMap<K,V> extends HashMap<K,List<V>> implements MultivaluedArrayMap<K,V> {
  /**
   * Constructs an empty {@link MultivaluedArrayHashMap} with the default initial capacity ({@code 16}) and the default load factor
   * ({@code 0.75}).
   */
  public MultivaluedArrayHashMap() {
    super();
  }

  /**
   * Constructs an empty {@link MultivaluedArrayHashMap} with the specified initial capacity and the default load factor (
   * {@code 0.75}).
   *
   * @param initialCapacity The initial capacity.
   * @throws IllegalArgumentException If the initial capacity is negative.
   */
  public MultivaluedArrayHashMap(final int initialCapacity) {
    super(initialCapacity);
  }

  /**
   * Constructs an empty {@link MultivaluedArrayHashMap} with the specified initial capacity and load factor.
   *
   * @param initialCapacity The initial capacity.
   * @param loadFactor The load factor.
   * @throws IllegalArgumentException If the initial capacity is negative or the load factor is nonpositive.
   */
  public MultivaluedArrayHashMap(final int initialCapacity, final float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  /**
   * Constructs a new {@link MultivaluedArrayHashMap} with the same mappings as the specified {@link Map}. The {@link List} instances
   * holding the values of each key are created anew instead of being reused.
   *
   * @param map The multivalued map whose mappings are to be placed in this multivalued map.
   * @throws NullPointerException If the specified map is null.
   */
  public MultivaluedArrayHashMap(final Map<? extends K,? extends List<V>> map) {
    super(map);
  }
}