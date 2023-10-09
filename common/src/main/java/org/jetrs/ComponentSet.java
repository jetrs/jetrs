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

import org.libj.logging.LoggerUtil;
import org.libj.util.SortedSetArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

abstract class ComponentSet<T extends Component<?>> extends SortedSetArrayList<T> {
  private static final Logger logger = LoggerFactory.getLogger(ComponentSet.class);
  private static final Comparator<TypeComponent<?>> typeComponentComparator = Comparator.nullsFirst((o1, o2) -> o1.getType() == o2.getType() ? Integer.compare(o1.getPriority(), o2.getPriority()) : o1.getType().isAssignableFrom(o2.getType()) ? 1 : -1);
  private static final Comparator<Component<?>> componentComparator = Comparator.nullsFirst((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority()));
  @SuppressWarnings("rawtypes")
  public static final ComponentSet EMPTY = new ComponentSet<Component<?>>(null) {};

  static class Typed<T extends TypeComponent<?>> extends ComponentSet<T> {
    Typed() {
      super(typeComponentComparator);
    }
  }

  static class Untyped<T extends Component<?>> extends ComponentSet<T> {
    Untyped() {
      super(componentComparator);
    }
  }

  private ComponentSet(final Comparator<? super T> comparator) {
    super(comparator);
  }

  boolean register(final T e) {
    for (int i = size() - 1; i >= 0; --i) { // [RA]
      final Component<?> component = get(i);
      if (e.clazz.equals(component.clazz)) {
        if (e.isDefaultProvider) {
          LoggerUtil.log(logger, component.isDefaultProvider ? Level.WARN : Level.DEBUG, e.instance != null ? "A default provider instance component of default provider class " + component.clazz.getName() + " is already present in this configuration" : "A default provider class component of class " + component.clazz.getName() + " is already present in this configuration"); // FIXME: "this configuration"?
          return false;
        }

        LoggerUtil.log(logger, component.isDefaultProvider ? Level.WARN : Level.DEBUG, e.instance != null ? "An instance component of %sclass " + component.clazz.getName() + " is already present in this configuration" : "A class component of %sclass " + component.clazz.getName() + " is already present in this configuration"); // FIXME: "this configuration"?
        if (!component.isDefaultProvider)
          return false;

        // An explicitly set (custom) provider can take the place of a default (standard) provider.
        remove(i);
        break;
      }
    }

    return super.add(e);
  }

  @Override
  public boolean add(final T e) {
    throw new UnsupportedOperationException();
  }

  boolean containsComponent(final Class<?> clazz) {
    for (final T component : this)
      if (clazz.equals(component.clazz))
        return true;

    return false;
  }

  boolean containsComponent(final Object instance) {
    for (final T component : this)
      if (instance.equals(component.instance))
        return true;

    return false;
  }

  Map<Class<?>,Integer> getContracts(final Class<?> componentClass) {
    for (final T component : this)
      if (componentClass.equals(component.clazz))
        return component.contracts;

    return Collections.EMPTY_MAP;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ComponentSet<T> clone() {
    return (ComponentSet<T>)super.clone();
  }
}