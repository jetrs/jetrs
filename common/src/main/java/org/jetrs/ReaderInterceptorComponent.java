/* Copyright (c) 2020 JetRS
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

import javax.ws.rs.ext.ReaderInterceptor;

final class ReaderInterceptorComponent extends MessageBodyComponent<ReaderInterceptor> {
  static ComponentSet<MessageBodyComponent<ReaderInterceptor>> register(ComponentSet<MessageBodyComponent<ReaderInterceptor>> components, final Class<ReaderInterceptor> clazz, final ReaderInterceptor instance, final boolean isDefaultProvider, final Map<Class<?>,Integer> contracts, final int priority) {
    if (components == null)
      components = new ComponentSet.Typed<>();
    else if (components.contains(clazz, isDefaultProvider))
      return components;

    components.add(new ReaderInterceptorComponent(clazz, instance, isDefaultProvider, contracts, priority));
    return components;
  }

  ReaderInterceptorComponent(final Class<ReaderInterceptor> clazz, final ReaderInterceptor instance, final boolean isDefaultProvider, final Map<Class<?>,Integer> contracts, final int priority) {
    super(clazz, instance, isDefaultProvider, contracts, priority, ReaderInterceptor.class, null);
  }
}