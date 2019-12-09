/* Copyright (c) 2019 JetRS
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

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.libj.util.ArrayFloatList;
import org.libj.util.CollectionUtil;
import org.libj.util.FloatComparator;
import org.libj.util.MirrorList;
import org.libj.util.Numbers;
import org.libj.util.ObjectUtil;

/**
 * A {@link MirrorList} that performs live sorting of values based on a quality
 * factor. The quality of a value is dereferenced by an instance of the
 * {@link Qualifier} interface.
 *
 * @param <V> The type of value elements in this list.
 * @param <R> The type of reflected value elements in the mirror list.
 */
public class MirrorQualityList<V,R> extends MirrorList<V,R> implements Cloneable {
  /**
   * Interface providing methods for the determination of quality from value
   * objects.
   *
   * @param <V> The type of value object of this {@link Qualifier}.
   * @param <R> The type of reflected value object of this {@link Qualifier}.
   */
  public interface Qualifier<V,R> {
    /**
     * Returns a {@link org.libj.util.Numbers.Compound#encode(float,int)
     * compound} quality-and-index {@code long} comprised of the quality
     * dereferenced from the value object of type {@code <V>}, and some index of
     * type {@code int} that could be used to further direct downstream parsing.
     *
     * @param value The value of type {@code <V>} from which to dereference the
     *          quality.
     * @param index An index of type {@code int}.
     * @return A {@link org.libj.util.Numbers.Compound#encode(float,int)
     *         compound} quality-and-index {@code long} comprised of the quality
     *         dereferenced from the value object of type {@code <V>}, and some
     *         index of type {@code int} that could be used to further direct
     *         downstream parsing.
     */
    long valueToQuality(V value, int index);

    /**
     * Returns a {@link org.libj.util.Numbers.Compound#encode(float,int)
     * compound} quality-and-index {@code long} comprised of the quality
     * dereferenced from the reflection value object of type {@code <R>}, and
     * some index of type {@code int} that could be used to further direct
     * downstream parsing.
     *
     * @param reflection The reflection value of type {@code <R>} from which to
     *          dereference the quality.
     * @param index An index of type {@code int}.
     * @return A {@link org.libj.util.Numbers.Compound#encode(float,int)
     *         compound} quality-and-index {@code long} comprised of the quality
     *         dereferenced from the value object of type {@code <R>}, and some
     *         index of type {@code int} that could be used to further direct
     *         downstream parsing.
     */
    long reflectionToQuality(R reflection, int index);

    /**
     * Returns the reverse representation of this {@link Qualifier}, whereby the
     * value object type {@code <V>} and reflected value object of type
     * {@code <R>} are swapped.
     *
     * @return The reverse representation of this {@link Qualifier}.
     */
    default Qualifier<R,V> reverse() {
      return new Qualifier<R,V>() {
        @Override
        public long valueToQuality(final R value, final int index) {
          return Qualifier.this.reflectionToQuality(value, index);
        }

        @Override
        public long reflectionToQuality(final V reflection, final int index) {
          return Qualifier.this.valueToQuality(reflection, index);
        }
      };
    }
  }

  /**
   * Casts the specified {@code list} of type <b>{@link List List&lt;T&gt;}</b>
   * to type <b>{@link List List&lt;T&gt;} &amp; {@link Cloneable}</b>.
   *
   * @param <C> The type parameter for {@link List List&lt;T&gt;} &
   *          {@link Cloneable}.
   * @param <T> The type of elements in the specified {@link List
   *          List&lt;T&gt;}.
   * @param list The {@code list} of type <b>{@link List List&lt;T&gt;}</b> to
   *          cast to type <b>{@link List List&lt;T&gt;} &
   *          {@link Cloneable}</b>.
   * @return The specified {@code list} of type <b>{@link List
   *         List&lt;T&gt;}</b> cast to type <b>{@link List List&lt;T&gt;} &
   *         {@link Cloneable}</b>.
   */
  @SuppressWarnings("unchecked")
  static <C extends List<T> & Cloneable,T>C toCloneable(final List<T> list) {
    return (C)list;
  }

  private Qualifier<V,R> qualifier;
  private Qualifier<R,V> reverse;
  private ArrayFloatList qualities;

  /**
   * Creates a new {@link MirrorQualityList} with the specified target lists,
   * {@link org.libj.util.MirrorList.Mirror}, and {@link Qualifier}. The
   * specified target lists are meant to be empty, as they become the underlying
   * lists of the new {@link MirrorQualityList} instance.
   * <p>
   * The specified {@link org.libj.util.MirrorList.Mirror} provides the
   * {@link org.libj.util.MirrorList.Mirror#valueToReflection(Object) V -> R}
   * and {@link org.libj.util.MirrorList.Mirror#reflectionToValue(Object) R ->
   * V} methods, which are used to reflect object values from one
   * {@link MirrorQualityList} to the other.
   * <p>
   * The specified {@link Qualifier} provides the
   * {@link Qualifier#valueToQuality(Object,int) V -> quality} and
   * {@link Qualifier#reflectionToQuality(Object,int) R -> quality} methods,
   * which are used to dereference the quality from object values and reflected
   * values.
   *
   * @param <CloneableValues> The type parameter constraining the {@code values}
   *          argument to {@link List List&lt;V&gt;} &amp; {@link Cloneable}.
   * @param <CloneableReflections> The type parameter constraining the
   *          {@code values} argument to {@link List List&lt;R&gt;} &amp;
   *          {@link Cloneable}.
   * @param values The underlying {@link Cloneable} list of type {@code <V>}.
   * @param reflections The underlying {@link Cloneable} list of type
   *          {@code <R>}.
   * @param mirror The {@link org.libj.util.MirrorList.Mirror} specifying the
   *          {@link org.libj.util.MirrorList.Mirror#valueToReflection(Object) V
   *          -> R} and
   *          {@link org.libj.util.MirrorList.Mirror#reflectionToValue(Object) R
   *          -> V} methods.
   * @param qualifier The {@link Qualifier} specifying the
   *          {@link Qualifier#valueToQuality(Object,int) V -> quality} and
   *          {@link Qualifier#reflectionToQuality(Object,int) R -> quality}
   *          methods.
   * @throws NullPointerException If any of the specified parameters is null.
   */
  public <CloneableValues extends List<V> & Cloneable,CloneableReflections extends List<R> & Cloneable>MirrorQualityList(final CloneableValues values, final CloneableReflections reflections, final Mirror<V,R> mirror, final Qualifier<V,R> qualifier) {
    super(values, reflections, mirror);
    this.qualifier = Objects.requireNonNull(qualifier);
  }

  /**
   * Creates a new {@link MirrorQualityList} with the specified lists and
   * mirror. This method is specific for the construction of a reflected
   * {@link MirrorQualityList} instance.
   *
   * @param mirrorList The {@link MirrorQualityList} for which {@code this} list
   *          will be a reflection. Likewise, {@code this} list will be a
   *          reflection for {@code mirrorList}.
   * @param values The underlying list of type {@code <V>}, which is implicitly
   *          assumed to also be {@link Cloneable}.
   * @param mirror The {@link org.libj.util.MirrorList.Mirror} specifying the
   *          {@link org.libj.util.MirrorList.Mirror#valueToReflection(Object) V -> R} and
   *          {@link org.libj.util.MirrorList.Mirror#reflectionToValue(Object) R -> V} methods.
   */
  protected MirrorQualityList(final MirrorQualityList<R,V> mirrorList, final List<V> values, final Mirror<V,R> mirror) {
    super(mirrorList, values, mirror);
    this.qualifier = mirrorList.qualifier.reverse();
  }

  @Override
  protected MirrorList<V,R> newInstance(final List<V> values, final List<R> reflections) {
    return new MirrorQualityList<>(toCloneable(values), toCloneable(reflections), getMirror(), qualifier);
  }

  @Override
  protected MirrorList<R,V> newMirrorInstance(final List<R> values) {
    return new MirrorQualityList<>(this, values, getReverseMirror());
  }

  @Override
  protected boolean unlock() {
    if (!inited) {
      inited = true;
      if (target.size() > 0)
        sort();

      super.unlock();
      return true;
    }

    return super.unlock();
  }

  @Override
  public MirrorQualityList<R,V> getMirrorList() {
    return (MirrorQualityList<R,V>)super.getMirrorList();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void superReverse() {
    final Qualifier tempQualifier = qualifier;
    qualifier = (Qualifier)(reverse != null ? reverse : qualifier.reverse());
    reverse = tempQualifier;
  }

  @Override
  public MirrorQualityList<R,V> reverse() {
    superReverse();
    if (mirrorList != null)
      getMirrorList().superReverse();

    return (MirrorQualityList<R,V>)super.reverse();
  }

  /**
   * Returns the {@link Qualifier}.
   *
   * @return The {@link Qualifier}.
   */
  public Qualifier<V,R> getQualifier() {
    return this.qualifier;
  }

  /**
   * Returns the reverse {@link Qualifier}, and caches it for subsequent
   * retrieval, avoiding reinstantiation.
   *
   * @return The reverse {@link Qualifier}.
   */
  protected Qualifier<R,V> getReverseQualifier() {
    return reverse == null ? reverse = getQualifier().reverse() : reverse;
  }

  /**
   * Returns the quality for the value at the specified index.
   *
   * @param index The index at which the quality is to be returned.
   * @return The quality for the value at the specified index.
   */
  public float getQuality(final int index) {
    return qualities() == null ? 1f : qualities.get(indexToQualitiesIndex(index));
  }

  /**
   * Operation that sorts this list by recursively examining each element in
   * reverse sequential order (end to front of the list).
   * <p>
   * The initiating call into this operation is:
   *
   * <pre>
   * {@code sort(size() - 1, false)}
   * </pre>
   *
   * @param index The index at which the element is to be examined whether it
   *          needs to be sorted.
   * @param needsSort Whether sorting is to be performed, as dictated by the
   *          previous stack frame.
   * @return Whether sorting is to be performed, as dictated by this stack
   *         frame.
   * @see #sort()
   */
  private boolean sort(final int index, boolean needsSort) {
    final V value = get(index);
    final long qualityAndIndex = qualifier.valueToQuality(value, 0);
    final float quality = Numbers.Compound.decodeFloat(qualityAndIndex, 0);
    needsSort |= quality != 1f;
    if (index == 0 ? !needsSort : !sort(index - 1, needsSort))
      return false;

    remove(index);
    add(index, value, quality);
    return true;
  }

  /**
   * Sorts this list in descending order of the quality property for each
   * element. The quality property of each element is dereferenced via the
   * {@link #qualifier Qualifier}.
   *
   * @return {@code true} if sorting was performed on this list, otherwise
   *         {@code false} if this list is already sorted.
   * @see #sort(int,boolean)
   */
  private boolean sort() {
    final int size = size();
    if (size == 0)
      return false;

    final boolean unlocked = unlock();
    final boolean sorted = sort(size - 1, false);
    lock(unlocked && sorted);
    return sorted;
  }

  private ArrayFloatList qualities() {
    if (qualities != null || this.mirrorList == null)
      return qualities;

    return qualities = getMirrorList().qualities;
  }

  /**
   * Associates the specified {@code quality} to the provided {@code index}, and
   * returns the index at which the associated value object should be added to
   * {@code this} list.
   *
   * @param index The index at which the specified {@code quality} is desired to
   *          be associated. This method will overwrite this index in order to
   *          place the quality in proper sorted order.
   * @param quality The specified quality to be associated at the provided
   *          index.
   * @return The index at which the associated value object should be added to
   *         {@code this} list.
   */
  private int addQuality(int index, final float quality) {
    if (quality == 1f)
      return qualities() == null ? index : Math.min(index, size() - qualities.size());

    if (qualities() == null) {
      qualities = new ArrayFloatList(quality);
      if (mirrorList != null)
        getMirrorList().qualities = qualities;

      return size();
    }

    final int qIndex = CollectionUtil.binaryClosestSearch(qualities, quality, FloatComparator.REVERSE);
    index = qualitiesIndexToIndex(qIndex);

    qualities.add(qIndex, quality);
    return index;
  }

  private void add(int index, final V value, final float quality) {
    index = addQuality(index, quality);
    super.target.add(index, value);
    super.beforeAdd(index, value);
  }

  /**
   * Returns the translated index in the {@link #qualities} list that is
   * equivalent to the specified index of {@code this} list.
   *
   * @param index The index of {@code this} list which is to be translated to an
   *          index in the {@link #qualities} list.
   * @return The translated index in the {@link #qualities} list that is
   *         equivalent to the specified index of {@code this} list.
   */
  private int indexToQualitiesIndex(final int index) {
    return index + size() - qualities.size();
  }

  /**
   * Returns the translated index of {@code this} list that is equivalent to the
   * specified index in the {@link #qualities} list.
   *
   * @param index The index in the {@link #qualities} list which is to be
   *          translated to an index of {@code this} list.
   * @return The translated index of {@code this} list that is equivalent to the
   *         specified index in the {@link #qualities} list.
   */
  private int qualitiesIndexToIndex(final int index) {
    return size() - qualities.size() + index;
  }

  @Override
  protected boolean beforeAdd(int index, final V element) {
    final boolean unlocked = unlock();
    final long qualityAndIndex = qualifier.valueToQuality(element, 0);
    final float quality = Numbers.Compound.decodeFloat(qualityAndIndex, 0);
    add(index, element, quality);
    lock(unlocked);
    return false;
  }

  @Override
  protected boolean beforeSet(final int index, final V newElement) {
    final boolean unlocked = unlock();
    beforeRemove(index);
    beforeAdd(index, newElement);
    lock(unlocked);
    return false;
  }

  @Override
  protected boolean beforeRemove(final int index) {
    final boolean unlocked = unlock();
    if (qualities() != null)
      qualities.removeIndex(indexToQualitiesIndex(index));

    super.target.remove(index);
    super.beforeRemove(index);

    lock(unlocked);
    return false;
  }

  @Override
  public ListIterator<V> listIterator(final int index) {
    final boolean unlocked = unlock();
    final ListIterator<V> iterator = super.listIterator(index);
    lock(unlocked);
    return iterator;
  }

  @SuppressWarnings("unchecked")
  private MirrorQualityList<V,R> superClone() {
    try {
      final MirrorQualityList<V,R> clone = (MirrorQualityList<V,R>)super.clone();
      if (qualities != null)
        clone.qualities = qualities.clone();

      clone.target = (List<?>)ObjectUtil.clone((Cloneable)clone.target);
      clone.target.addAll(target);
      return clone;
    }
    catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public MirrorQualityList<V,R> clone() {
    final MirrorQualityList<V,R> clone = superClone();
    clone.mirrorList = getMirrorList().superClone();
    ((MirrorQualityList<R,V>)clone.mirrorList).mirrorList = clone;
    return clone;
  }
}