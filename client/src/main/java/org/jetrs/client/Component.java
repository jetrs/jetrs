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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;

class Component {
  private static int getPriority(final Class<?> cls) {
    final Priority priority = cls.getAnnotation(Priority.class);
    return priority != null ? priority.value() : Priorities.USER;
  }

  final Class<?> cls;
  final Object instance;
  final int priority;
  final Map<Class<?>,Integer> contracts;

  Component(final Class<?> cls, final Object instance) {
    this(cls, instance, getPriority(cls != null ? cls : instance.getClass()));
  }

  Component(final Class<?> cls, final Object instance, final int priority) {
    this.cls = cls;
    this.instance = instance;
    this.priority = priority;
    this.contracts = null;
  }

  Component(final Class<?> cls, final Object instance, final Map<Class<?>,Integer> contracts) {
    this.cls = cls;
    this.instance = instance;
    this.priority = getPriority(cls != null ? cls : instance.getClass());
    if (contracts != null) {
      this.contracts = new HashMap<>(contracts.size());
      for (final Map.Entry<Class<?>,Integer> entry : contracts.entrySet())
        this.contracts.put(entry.getKey(), entry.getValue());
    }
    else {
      this.contracts = null;
    }
  }

  Component(final Class<?> cls, final Object instance, final Class<?>[] contracts) {
    this.cls = cls;
    this.instance = instance;
    this.priority = getPriority(cls != null ? cls : instance.getClass());
    if (contracts != null) {
      this.contracts = new HashMap<>(contracts.length);
      for (final Class<?> contract : contracts)
        this.contracts.put(contract, getPriority(contract));
    }
    else {
      this.contracts = null;
    }
  }

  @Override
  public int hashCode() {
    return cls.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof Component))
      return false;

    return cls.equals(((Component)obj).cls);
  }
}