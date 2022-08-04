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

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant.VariantListBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.libj.lang.Classes;

public class RuntimeDelegateImpl extends RuntimeDelegate {
  private static final String endpointFactoryClass = "org.jetrs.EndpointFactory";

  private final RuntimeContext runtimeContext;
  private final BiFunction<Application,Class<?>,HttpServlet> endpointFactory;

  @SuppressWarnings("unchecked")
  RuntimeDelegateImpl(final RuntimeContext runtimeContext) {
    this.runtimeContext = runtimeContext;
    final Class<?> cls = Classes.forNameOrNull(endpointFactoryClass, true, Thread.currentThread().getContextClassLoader());
    if (cls == null) {
      this.endpointFactory = null;
    }
    else {
      try {
        this.endpointFactory = (BiFunction<Application,Class<?>,HttpServlet>)cls.getDeclaredConstructor().newInstance();
      }
      catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public RuntimeDelegateImpl() {
    this(null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T>T createEndpoint(final Application application, final Class<T> endpointType) {
    assertNotNull(application, "application is null");
    if (endpointFactory == null)
      throw new UnsupportedOperationException("No endpoint types are supported: No container available");

    return (T)endpointFactory.apply(application, endpointType);
  }

  @Override
  public <T>HeaderDelegate<T> createHeaderDelegate(final Class<T> type) {
    return Delegate.lookup(assertNotNull(type, "type is null"));
  }

  @Override
  public ResponseBuilder createResponseBuilder() {
    // FIXME: Need to figure out the logic if `runtimeContext == null`.
    if (runtimeContext == null)
      return new ResponseBuilderImpl(null, null);

    final RequestContext serviceContext = runtimeContext.newRequestContext(null);
    return new ResponseBuilderImpl(serviceContext.getProviders(), serviceContext.getReaderInterceptorFactoryList().newContextList(serviceContext));
  }

  @Override
  public UriBuilder createUriBuilder() {
    return new UriBuilderImpl();
  }

  @Override
  public Link.Builder createLinkBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public VariantListBuilder createVariantListBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }
}