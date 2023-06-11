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

import java.util.Map;

import javax.ws.rs.core.Configurable;

@SuppressWarnings("unchecked")
interface ConfigurableImpl<C extends Configurable<? super C>> extends Configurable<C> {
  @Override
  default C property(final String name, final Object value) {
    getConfiguration().getProperties().put(name, value);
    return (C)this;
  }

  @Override
  default C register(final Object component) {
    ((ConfigurationImpl)getConfiguration()).components().add(new Component(component.getClass(), component));
    return (C)this;
  }

  @Override
  default C register(final Object component, final int priority) {
    ((ConfigurationImpl)getConfiguration()).components().add(new Component(component.getClass(), component, priority));
    return (C)this;
  }

  @Override
  default C register(final Object component, final Class<?> ... contracts) {
    ((ConfigurationImpl)getConfiguration()).components().add(new Component(null, component, contracts));
    return (C)this;
  }

  @Override
  default C register(final Object component, final Map<Class<?>,Integer> contracts) {
    ((ConfigurationImpl)getConfiguration()).components().add(new Component(null, component, contracts));
    return (C)this;
  }

  @Override
  default C register(final Class<?> componentClass) {
    ((ConfigurationImpl)getConfiguration()).components().add(new Component(componentClass, null));
    return (C)this;
  }

  @Override
  default C register(final Class<?> componentClass, final int priority) {
    ((ConfigurationImpl)getConfiguration()).components().add(new Component(componentClass, null, priority));
    return (C)this;
  }

  @Override
  default C register(final Class<?> componentClass, final Class<?> ... contracts) {
    ((ConfigurationImpl)getConfiguration()).components().add(new Component(componentClass, null, contracts));
    return (C)this;
  }

  @Override
  default C register(final Class<?> componentClass, final Map<Class<?>,Integer> contracts) {
    ((ConfigurationImpl)getConfiguration()).components().add(new Component(componentClass, null, contracts));
    return (C)this;
  }
}