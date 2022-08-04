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

abstract class ContextList<F extends ProviderFactory<? super T>,T> {
  private final RequestContext requestContext;
  private final FactoryList<F,T> factories;
  private final Object[] instances;
  private final int size;

  ContextList(final RequestContext requestContext, final FactoryList<F,T> factories) {
    this.requestContext = requestContext;
    this.factories = factories;
    this.size = factories.size();
    this.instances = new Object[size];
  }

  F getFactory(final int i) {
    return factories.get(i);
  }

  @SuppressWarnings("unchecked")
  T getInstance(final int i) {
    Object instance = instances[i];
    if (instance != null)
      return (T)instance;

    return (T)(instances[i] = getFactory(i).getSingletonOrNewInstance(requestContext));
  }

  public int size() {
    return size;
  }
}