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

import java.util.Map;

import javax.ws.rs.core.Configurable;

@SuppressWarnings("unchecked")
public interface ClientConfigurable<C extends Configurable<? super C>> extends Configurable<C> {
  @Override
  public default C property(final String name, final Object value) {
    getConfiguration().getProperties().put(name, value);
    return (C)this;
  }

  @Override
  public default C register(final Object component) {
    ((ClientConfiguration)getConfiguration()).components().add(new Component(component.getClass(), component));
    return (C)this;
  }

  @Override
  public default C register(final Object component, final int priority) {
    ((ClientConfiguration)getConfiguration()).components().add(new Component(component.getClass(), component, priority));
    return (C)this;
  }

  @Override
  public default C register(final Object component, final Class<?> ... contracts) {
    ((ClientConfiguration)getConfiguration()).components().add(new Component(component.getClass(), component, contracts));
    return (C)this;
  }

  @Override
  public default C register(final Object component, final Map<Class<?>,Integer> contracts) {
    ((ClientConfiguration)getConfiguration()).components().add(new Component(component.getClass(), component, contracts));
    return (C)this;
  }

  @Override
  public default C register(final Class<?> componentClass) {
    ((ClientConfiguration)getConfiguration()).components().add(new Component(componentClass, null));
    return (C)this;
  }

  @Override
  public default C register(final Class<?> componentClass, final int priority) {
    ((ClientConfiguration)getConfiguration()).components().add(new Component(componentClass, null, priority));
    return (C)this;
  }

  @Override
  public default C register(final Class<?> componentClass, final Class<?> ... contracts) {
    ((ClientConfiguration)getConfiguration()).components().add(new Component(componentClass, null, contracts));
    return (C)this;
  }

  @Override
  public default C register(final Class<?> componentClass, final Map<Class<?>,Integer> contracts) {
    ((ClientConfiguration)getConfiguration()).components().add(new Component(componentClass, null, contracts));
    return (C)this;
  }
}
