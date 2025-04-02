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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeDelegateImpl extends RuntimeDelegate {
  private static final Logger logger = LoggerFactory.getLogger(RuntimeDelegateImpl.class);
  private static final String endpointFactoryClassName = "org.jetrs.EndpointFactory";

  private final BiFunction<Application,Class<?>,HttpServlet> endpointFactory;
  private RuntimeContext runtimeContext;

  @SuppressWarnings("unchecked")
  public RuntimeDelegateImpl() {
    final Class<?> cls = Classes.forNameOrNull(endpointFactoryClassName, true, Thread.currentThread().getContextClassLoader());
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

    if (logger.isDebugEnabled()) { logger.debug("cls: " + cls + ", endpointFactory: " + System.identityHashCode(endpointFactory)); }
  }

  void setRuntimeContext(final RuntimeContext runtimeContext) {
    this.runtimeContext = runtimeContext;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T createEndpoint(final Application application, final Class<T> endpointType) {
    assertNotNull(application, "application is null");
    if (endpointFactory == null)
      throw new UnsupportedOperationException("No endpoint types are supported: No container available");

    return (T)endpointFactory.apply(application, endpointType);
  }

  @Override
  public <T> HeaderDelegate<T> createHeaderDelegate(final Class<T> type) {
    return HeaderDelegateImpl.lookup(assertNotNull(type, "type is null"));
  }

  @Override
  public ResponseBuilder createResponseBuilder() {
    return new ResponseBuilderImpl(runtimeContext == null ? null : assertNotNull(runtimeContext.localRequestContext()));
  }

  @Override
  public UriBuilder createUriBuilder() {
    return new UriBuilderImpl();
  }

  @Override
  public Link.Builder createLinkBuilder() {
    return new LinkImpl.BuilderImpl();
  }

  @Override
  public VariantListBuilder createVariantListBuilder() {
    return new VariantListBuilderImpl();
  }
}