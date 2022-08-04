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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

final class ReaderInterceptorProviders {
  static class Factory extends MessageBodyProviderFactory<ReaderInterceptor> implements ReaderInterceptor {
    Factory(final Class<ReaderInterceptor> clazz, final ReaderInterceptor singleton) throws IllegalAccessException, InstantiationException, InvocationTargetException {
      super(clazz, singleton, ReaderInterceptor.class);
    }

    @Override
    public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
      throw new UnsupportedOperationException();
    }
  }

  static class FactoryList extends org.jetrs.FactoryList<Factory,ReaderInterceptor> {
    static final FactoryList EMPTY_LIST = new FactoryList();

    @Override
    ContextList newContextList(final RequestContext requestContext) {
      return new ContextList(requestContext, this);
    }
  }

  static class ContextList extends org.jetrs.ContextList<Factory,ReaderInterceptor> {
    ContextList(final RequestContext requestContext, final FactoryList factories) {
      super(requestContext, factories);
    }
  }

  private ReaderInterceptorProviders() {
  }
}