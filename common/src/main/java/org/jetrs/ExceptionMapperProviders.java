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

import java.lang.reflect.InvocationTargetException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

final class ExceptionMapperProviders {
  static class Factory extends TypeProviderFactory<ExceptionMapper<?>> implements ExceptionMapper<Throwable> {
    Factory(final Class<ExceptionMapper<?>> clazz, final ExceptionMapper<?> singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
      super(clazz, singleton, getGenericInterfaceFirstTypeArgument(clazz, ExceptionMapper.class, Throwable.class));
    }

    @Override
    public Response toResponse(final Throwable exception) {
      throw new UnsupportedOperationException();
    }
  }

  static class FactoryList extends org.jetrs.FactoryList<Factory,ExceptionMapper<?>> {
    static final FactoryList EMPTY_LIST = new FactoryList();

    @Override
    ContextList newContextList(final RequestContext requestContext) {
      return new ContextList(requestContext, this);
    }
  }

  static class ContextList extends org.jetrs.ContextList<Factory,ExceptionMapper<?>> {
    ContextList(final RequestContext requestContext, final FactoryList factories) {
      super(requestContext, factories);
    }
  }

  private ExceptionMapperProviders() {
  }
}