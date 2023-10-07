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

package org.jetrs;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.libj.util.TransSet;

class Components implements Cloneable, Serializable {
  // FIXME: I don't think sorting by priority here is necessary
  private static final Comparator<Component<?>> comparator = new Comparator<Component<?>>() {
    @Override
    public int compare(final Component<?> o1, final Component<?> o2) {
      final int c = Integer.compare(o1.priority, o2.priority);
      return c != 0 ? c : o1.equals(o2) ? 0 : 1;
    }
  };

  private TreeSet<Component<?>> classComponents;
  private TreeSet<Component<?>> instanceComponents;

  void add(final Component<?> component) {
    if (classComponents != null)
      for (final Component<?> classComponent : classComponents)
        if (classComponent.clazz.equals(component.clazz))
          throw new IllegalArgumentException("A class component of class " + classComponent.clazz.getName() + " is already present in this configuration");

    if (instanceComponents != null)
      for (final Component<?> instanceComponent : instanceComponents)
        if (instanceComponent.clazz.equals(component.clazz))
          throw new IllegalArgumentException("An instance component of class " + instanceComponent.clazz.getName() + " is already present in this configuration");

    if (component.isSingleton) {
      if (instanceComponents == null)
        instanceComponents = new TreeSet<>(comparator);

      instanceComponents.add(component);
    }
    else {
      if (classComponents == null)
        classComponents = new TreeSet<>(comparator);

      classComponents.add(component);
    }
  }

  private Set<Class<?>> classes;

  Set<Class<?>> classes() {
    return classes != null ? classes : classComponents != null ? classes = new TransSet<>(classComponents, (final Component<?> c) -> c.clazz, null) : null;
  }

  private Set<Object> instances;

  Set<Object> instances() {
    return instances != null ? instances : instanceComponents != null ? instances = new TransSet<>(instanceComponents, (final Component<?> c) -> c.instance, null) : null;
  }

  boolean contains(final Class<?> componentClass) {
    if (classComponents.size() > 0)
      for (final Component<?> classComponent : classComponents) // [S]
        if (componentClass.equals(classComponent.clazz))
          return true;

    return false;
  }

  boolean contains(final Object component) {
    if (component instanceof Class)
      return contains((Class<?>)component);

    if (instanceComponents.size() > 0)
      for (final Component<?> instanceComponent : instanceComponents) // [S]
        if (component.equals(instanceComponent.instance))
          return true;

    return false;
  }

  Map<Class<?>,Integer> getContracts(final Class<?> componentClass) {
    if (classComponents.size() > 0)
      for (final Component<?> classComponent : classComponents) // [S]
        if (componentClass.equals(classComponent.clazz))
          return classComponent.contracts;

    return Collections.EMPTY_MAP;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Components clone() {
    try {
      final Components clone = (Components)super.clone();
      if (classComponents != null)
        clone.classComponents = (TreeSet<Component<?>>)classComponents.clone();

      if (instanceComponents != null)
        clone.instanceComponents = (TreeSet<Component<?>>)instanceComponents.clone();

      clone.classes = null;
      clone.instances = null;
      return clone;
    }
    catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}