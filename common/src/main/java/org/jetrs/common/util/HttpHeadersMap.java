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

package org.jetrs.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetrs.common.util.MirrorQualityList.Qualifier;
import org.libj.util.MirrorList;
import org.libj.util.MirrorMap;

/**
 * A {@link MirrorMultivaluedMap} that uses lists of type
 * {@link MirrorQualityList}, which automatically sort header values based on
 * quality (i.e. {@code "q=0.2"}).
 *
 * @param <V> The type of value elements in this map.
 * @param <R> The type of reflected value elements in the mirror map.
 */
public class HttpHeadersMap<V,R> extends MirrorMultivaluedMap<String,V,R> {
  @SuppressWarnings("unchecked")
  private static <C extends List<T> & Cloneable,T>C ensureCloneable(final List<T> list) {
    if (list == null)
      return (C)new ArrayList<>();

    if (list instanceof Cloneable)
      return (C)list;

    return (C)new ArrayList<>(list);
  }

  /**
   * Creates a new {@link HttpHeadersMap} with a default
   * {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror} that instantiates
   * lists of type {@link MirrorQualityList}.
   *
   * @param mirror The {@link org.libj.util.MirrorMap.Mirror} specifying the
   *          {@link org.libj.util.MirrorMap.Mirror#valueToReflection(Object,Object)
   *          V -> R} and
   *          {@link org.libj.util.MirrorMap.Mirror#reflectionToValue(Object,Object)
   *          R -> V} methods.
   * @param qualifier {@link Qualifier} providing methods for the determination
   *          of quality from value objects.
   * @throws NullPointerException If any of the specified parameters is null.
   */
  public HttpHeadersMap(final MirrorMap.Mirror<String,V,R> mirror, final Qualifier<V,R> qualifier) {
    super(new HashMap<>(), new HashMap<>(), new MirrorMultivaluedMap.Mirror<String,V,R>() {
      @Override
      @SuppressWarnings("unchecked")
      public MirrorQualityList<R,V> valueToReflection(final String key, final List<V> value) {
        if (value instanceof MirrorQualityList)
          return ((MirrorQualityList<V,R>)value).getMirrorList();

        return new MirrorQualityList<>(new ArrayList<>(), ensureCloneable(value), new MirrorList.Mirror<R,V>() {
          @Override
          public V valueToReflection(final R value) {
            return mirror.reflectionToValue(key, value);
          }

          @Override
          public R reflectionToValue(final V reflection) {
            return mirror.reverse().reflectionToValue(key, reflection);
          }
        }, qualifier.reverse());
      }

      @Override
      @SuppressWarnings("unchecked")
      public MirrorQualityList<V,R> reflectionToValue(final String key, final List<R> reflection) {
        if (reflection instanceof MirrorQualityList)
          return ((MirrorQualityList<R,V>)reflection).getMirrorList();

        return new MirrorQualityList<>(new ArrayList<>(), ensureCloneable(reflection), new MirrorList.Mirror<V,R>() {
          @Override
          public R valueToReflection(final V value) {
            return mirror.valueToReflection(key, value);
          }

          @Override
          public V reflectionToValue(final R reflection) {
            return mirror.reflectionToValue(key, reflection);
          }
        }, qualifier);
      }
    });
    Objects.requireNonNull(mirror);
    Objects.requireNonNull(qualifier);
  }

  /**
   * Creates a new {@link HttpHeadersMap} with the specified maps and mirror.
   * This method is specific for the construction of a reflected
   * {@link HttpHeadersMap} instance.
   *
   * @param mirrorMap The {@link HttpHeadersMap} for which {@code this} map will
   *          be a reflection. Likewise, {@code this} map will be a reflection
   *          for {@code mirrorMap}.
   * @param values The underlying map of type {@code <K,List<V>>}, which is
   *          implicitly assumed to also be {@link Cloneable}.
   * @param mirror The {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror}
   *          specifying the
   *          {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror#valueToReflection(Object,List) V -&gt; R} and
   *          {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror#reflectionToValue(Object,List) R -&gt; V} methods.
   */
  protected HttpHeadersMap(final HttpHeadersMap<R,V> mirrorMap, final Map<String,List<V>> values, final HttpHeadersMap.Mirror<String,V,R> mirror) {
    super(mirrorMap, values, mirror);
  }

  /**
   * Creates a new {@link HttpHeadersMap} with the specified target maps and
   * {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror}. The specified
   * target maps are meant to be empty, as they become the underlying maps of
   * the new {@link HttpHeadersMap} instance. The specified
   * {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror} provides the
   * {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror#valueToReflection(Object,List) V -&gt; R} and
   * {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror#reflectionToValue(Object,List) R -&gt; V} methods,
   * which are used to reflect object values from one {@link HttpHeadersMap} to the other.
   *
   * @param values The underlying map of type {@code <K,List<V>>}, which is
   *          implicitly assumed to also be {@link Cloneable}.
   * @param reflections The underlying map of type {@code <K,List<R>>}, which is
   *          implicitly assumed to also be {@link Cloneable}.
   * @param mirror The {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror}
   *          specifying the
   *          {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror#valueToReflection(Object,List) V -&gt; R} and
   *          {@link org.jetrs.common.util.MirrorMultivaluedMap.Mirror#reflectionToValue(Object,List) R -&gt; V} methods.
   * @throws NullPointerException If any of the specified parameters is null.
   */
  private HttpHeadersMap(final Map<String,List<V>> values, final Map<String,List<R>> reflections, final Mirror<String,V,R> mirror) {
    super(toCloneable(values), toCloneable(reflections), mirror);
  }

  private static String toLowerCase(final Object key) {
    return key == null ? null : key.toString().toLowerCase();
  }

  @Override
  protected Object beforeGet(final Object key) {
    return super.beforeGet(toLowerCase(key));
  }

  @Override
  public boolean containsKey(final Object key) {
    return super.containsKey(toLowerCase(key));
  }

  @Override
  public void add(final String key, final V value) {
    super.add(toLowerCase(key), value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addAll(final String key, final V ... newValues) {
    super.addAll(toLowerCase(key), newValues);
  }

  @Override
  public void addAll(final String key, final List<V> valueList) {
    super.addAll(toLowerCase(key), valueList);
  }

  @Override
  public void addFirst(final String key, final V value) {
    super.addFirst(toLowerCase(key), value);
  }

  @Override
  public void putSingle(final String key, final V value) {
    super.putSingle(toLowerCase(key), value);
  }

  @Override
  protected MirrorList<V,R> put(final String key, final List<V> oldValue, final List<V> newValue) {
    return super.put(toLowerCase(key), oldValue, newValue);
  }

  @Override
  public void putAll(final Map<? extends String,? extends List<V>> m) {
    for (final Map.Entry<? extends String,? extends List<V>> entry : m.entrySet())
      put(toLowerCase(entry.getKey()), entry.getValue());
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    return super.remove(toLowerCase(key), value);
  }

  @Override
  protected final HttpHeadersMap<V,R> newInstance(final Map<String,List<V>> values, final Map<String,List<R>> reflections) {
    return new HttpHeadersMap<>(values, reflections, getMirror());
  }

  @Override
  protected final HttpHeadersMap<R,V> newMirrorInstance(final Map<String,List<R>> values) {
    return new HttpHeadersMap<>(this, values, getReverseMirror());
  }

  @Override
  public final HttpHeadersMap<R,V> getMirrorMap() {
    return (HttpHeadersMap<R,V>)super.getMirrorMap();
  }

  @Override
  @SuppressWarnings("unlikely-arg-type")
  public MirrorQualityList<V,R> get(final Object key) {
    return (MirrorQualityList<V,R>)super.get(key);
  }

  @Override
  public MirrorQualityList<V,R> put(final String key, final List<V> value) {
    return (MirrorQualityList<V,R>)super.put(key, value);
  }

  @Override
  public MirrorQualityList<V,R> putIfAbsent(final String key, final List<V> value) {
    return (MirrorQualityList<V,R>)super.putIfAbsent(key, value);
  }

  @Override
  @SuppressWarnings("unlikely-arg-type")
  public MirrorQualityList<V,R> remove(final Object key) {
    return (MirrorQualityList<V,R>)super.remove(key);
  }

  @Override
  public MirrorQualityList<V,R> replace(final String key, final List<V> value) {
    return (MirrorQualityList<V,R>)super.replace(key, value);
  }

  @Override
  public HttpHeadersMap<V,R> clone() {
    return (HttpHeadersMap<V,R>)super.clone();
  }
}