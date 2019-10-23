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

package org.jetrs.client;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.libj.util.TransSet;

class ComponentSet implements Cloneable {
  private static final Comparator<Component> comparator = new Comparator<Component>() {
    @Override
    public int compare(final Component o1, final Component o2) {
      return Integer.compare(o1.priority, o2.priority);
    }
  };
  private TreeSet<Component> components = new TreeSet<>(comparator);

  void add(final Component component) {
    components.add(component);
  }

  private Set<Class<?>> classes;

  Set<Class<?>> classes() {
    return classes == null ? classes = new TransSet<>(components, c -> c.cls, null) : classes;
  }

  private Set<Object> instances;

  Set<Object> instances() {
    return instances == null ? instances = new TransSet<>(components, c -> c.instance, null) : instances;
  }

  public boolean contains(final Class<?> componentClass) {
    return classes().contains(componentClass);
  }

  public boolean contains(final Object component) {
    return instances().contains(component);
  }

  public Map<Class<?>,Integer> getContracts(final Class<?> componentClass) {
    for (final Component component : components)
      if (componentClass.equals(component.cls))
        return component.contracts;

    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected ComponentSet clone() {
    try {
      final ComponentSet clone = (ComponentSet)super.clone();
      clone.components = (TreeSet<Component>)components.clone();
      clone.classes = null;
      clone.instances = null;
      return clone;
    }
    catch (final CloneNotSupportedException e) {
      throw new IllegalStateException(e);
    }
  }
}