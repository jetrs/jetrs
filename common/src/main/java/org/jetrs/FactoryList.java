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
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

abstract class FactoryList<F extends ProviderFactory<? super T>,T> extends ArrayList<F> {
  boolean superAdd(final F element) {
    return super.add(element);
  }

  void superSort(final Comparator<? super F> c) {
    super.sort(c);
  }

  abstract ContextList<F,T> newContextList(final RequestContext requestContext);

  @SuppressWarnings("unchecked")
  T getInstance(final int i, final RequestContext requestContext) {
    return (T)get(i).getSingletonOrNewInstance(requestContext);
  }

  @Override
  public F set(final int index, final F element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(final F element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(final int index, final F element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public F remove(final int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(final int index, final Collection<? extends F> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replaceAll(final UnaryOperator<F> operator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sort(final Comparator<? super F> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<F> listIterator() {
    return listIterator(0);
  }

  @Override
  public ListIterator<F> listIterator(final int index) {
    return new ListIterator<F>() {
      private final ListIterator<? extends F> iterator = FactoryList.super.listIterator(index);

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public F next() {
        return iterator.next();
      }

      @Override
      public boolean hasPrevious() {
        return iterator.hasPrevious();
      }

      @Override
      public F previous() {
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
      public void set(final F e) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void add(final F e) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void forEachRemaining(final Consumer<? super F> action) {
        iterator.forEachRemaining(action);
      }
    };
  }

  @Override
  public List<F> subList(final int fromIndex, final int toIndex) {
    throw new UnsupportedOperationException();
  }
}