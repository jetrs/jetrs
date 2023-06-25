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
import java.util.Comparator;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class UnmodifiableArrayList<E> extends ArrayList<E> {
  boolean add$(final E element) {
    return super.add(element);
  }

  @Override
  public E set(final int index, final E element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(final E element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final int index, final E element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public E remove(final int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(final int index, final Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replaceAll(final UnaryOperator<E> operator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sort(final Comparator<? super E> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<E> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<E> listIterator(final int index) {
    return new ListIterator<E>() {
      private final ListIterator<? extends E> iterator = UnmodifiableArrayList.super.listIterator(index);

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public E next() {
        return iterator.next();
      }

      @Override
      public boolean hasPrevious() {
        return iterator.hasPrevious();
      }

      @Override
      public E previous() {
        return iterator.previous();
      }

      @Override
      public int nextIndex() {
        return iterator.nextIndex();
      }

      @Override
      public int previousIndex() {
        return iterator.previousIndex();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void set(final E e) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void add(final E e) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void forEachRemaining(final Consumer<? super E> action) {
        iterator.forEachRemaining(action);
      }
    };
  }

  @Override
  public ArrayList<E> subList(final int fromIndex, final int toIndex) {
    throw new UnsupportedOperationException();
  }
}