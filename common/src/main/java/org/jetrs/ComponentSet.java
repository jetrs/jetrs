/* Copyright (c) 2023 JetRS
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

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.libj.util.SortedSetArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ComponentSet<T extends Component<?>> extends SortedSetArrayList<T> {
  private static final Logger logger = LoggerFactory.getLogger(ComponentSet.class);
  private static final Comparator<TypeComponent<?>> typeComponentComparator = Comparator.nullsFirst((o1, o2) -> o1.getType() == o2.getType() ? Integer.compare(o1.priority, o2.priority) : o1.getType().isAssignableFrom(o2.getType()) ? 1 : -1);
  private static final Comparator<Component<?>> componentComparator = Comparator.nullsFirst((o1, o2) -> Integer.compare(o1.priority, o2.priority));
  @SuppressWarnings("rawtypes")
  static final ComponentSet EMPTY = new ComponentSet<Component<?>>(null) {};

  static final class Typed<T extends TypeComponent<?>> extends ComponentSet<T> {
    Typed() {
      super(typeComponentComparator);
    }
  }

  static final class Untyped<T extends Component<?>> extends ComponentSet<T> {
    Untyped() {
      super(componentComparator);
    }
  }

  private ComponentSet(final Comparator<? super T> comparator) {
    super(comparator);
  }

  final boolean contains(final Class<?> clazz, final boolean isDefaultProvider) {
    for (int i = size() - 1; i >= 0; --i) { // [RA]
      final Component<?> component = get(i);
      if (clazz.equals(component.clazz)) {
        if (component.isDefaultProvider) {
          if (isDefaultProvider) {
            if (logger.isDebugEnabled()) { logger.debug("Skipped " + clazz.getName() + ", because a default provider component of class " + component.clazz.getName() + " is already present in this configuration"); }
            return true;
          }

          // An explicitly set (custom) provider can take the place of a default (standard) provider.
          remove(i);
          return false;
        }

        if (isDefaultProvider) {
          if (logger.isDebugEnabled()) { logger.debug("Skipped " + clazz.getName() + ", because a component of class " + component.clazz.getName() + " is already present in this configuration"); }
        }
        else {
          if (logger.isWarnEnabled()) { logger.warn("Skipped " + clazz.getName() + ", because a component of class " + component.clazz.getName() + " is already present in this configuration"); }
        }

        return true;
      }
    }

    return false;
  }

  final boolean containsComponent(final Class<?> clazz) {
    for (int i = 0, i$ = size(); i < i$; ++i) // [RA]
      if (clazz.equals(get(i).clazz))
        return true;

    return false;
  }

  final boolean containsComponent(final Object instance) {
    for (int i = 0, i$ = size(); i < i$; ++i) // [RA]
      if (instance.equals(get(i).singleton))
        return true;

    return false;
  }

  final Map<Class<?>,Integer> getContracts(final Class<?> componentClass) {
    for (int i = 0, i$ = size(); i < i$; ++i) { // [RA]
      final Component<?> component = get(i);
      if (componentClass.equals(component.clazz))
        return component.contracts;
    }

    return Collections.EMPTY_MAP;
  }

  @Override
  @SuppressWarnings("unchecked")
  public final ComponentSet<T> clone() {
    return (ComponentSet<T>)super.clone();
  }
}