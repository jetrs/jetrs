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

import static org.libj.lang.Assertions.*;

import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.Configurable;

@SuppressWarnings("unchecked")
interface ConfigurableImpl<C extends Configurable<? super C>> extends Configurable<C> {
  @Override
  default C property(final String name, final Object value) {
    ((ConfigurationImpl)getConfiguration()).getOrCreateProperties().put(name, value);
    return (C)this;
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code component} is null.
   */
  @Override
  default C register(final Object component) {
    ((ConfigurationImpl)getConfiguration()).getOrCreateComponents().register(component.getClass(), component, false, null, -1);
    return (C)this;
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code component} is null.
   * @throws IllegalArgumentException If {@code priority} is negative.
   */
  @Override
  default C register(final Object component, final int priority) {
    ((ConfigurationImpl)getConfiguration()).getOrCreateComponents().register(component.getClass(), component, false, null, assertNotNegative(priority));
    return (C)this;
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code component} is null.
   */
  @Override
  default C register(final Object component, final Class<?> ... contracts) {
    final Class<? extends Object> clazz = component.getClass();
    ((ConfigurationImpl)getConfiguration()).getOrCreateComponents().register(clazz, component, false, Component.filterAssignable(clazz, contracts), -1);
    return (C)this;
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code component} is null.
   */
  @Override
  default C register(final Object component, final Map<Class<?>,Integer> contracts) {
    final Class<? extends Object> clazz = component.getClass();
    ((ConfigurationImpl)getConfiguration()).getOrCreateComponents().register(clazz, component, false, Component.filterAssignable(clazz, contracts), -1);
    return (C)this;
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code componentClass} is null.
   */
  @Override
  default C register(final Class<?> componentClass) {
    ((ConfigurationImpl)getConfiguration()).getOrCreateComponents().register(Objects.requireNonNull(componentClass), null, false, null, -1);
    return (C)this;
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code componentClass} is null.
   * @throws IllegalArgumentException If {@code priority} is negative.
   */
  @Override
  default C register(final Class<?> componentClass, final int priority) {
    ((ConfigurationImpl)getConfiguration()).getOrCreateComponents().register(Objects.requireNonNull(componentClass), null, false, null, assertNotNegative(priority));
    return (C)this;
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code componentClass} is null.
   */
  @Override
  default C register(final Class<?> componentClass, final Class<?> ... contracts) {
    ((ConfigurationImpl)getConfiguration()).getOrCreateComponents().register(Objects.requireNonNull(componentClass), null, false, Component.filterAssignable(componentClass, contracts), -1);
    return (C)this;
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException If {@code componentClass} is null.
   */
  @Override
  default C register(final Class<?> componentClass, final Map<Class<?>,Integer> contracts) {
    ((ConfigurationImpl)getConfiguration()).getOrCreateComponents().register(Objects.requireNonNull(componentClass), null, false, Component.filterAssignable(componentClass, contracts), -1);
    return (C)this;
  }
}