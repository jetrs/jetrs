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

package org.jetrs.common.ext;

import java.util.ServiceConfigurationError;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant.VariantListBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jetrs.common.core.UriBuilderImpl;
import org.jetrs.provider.ext.header.Delegate;

public abstract class RuntimeDelegateImpl extends RuntimeDelegate {
  private static final String serverRuntimeDelegateClassName = "org.jetrs.server.ext.ServerRuntimeDelegate";
  private static final String serverRuntimeDelegateResource = "org/jetrs/server/ext/ServerRuntimeDelegate.class";

  private static boolean isServerPresent(final ClassLoader classLoader) {
    return classLoader.getResource(serverRuntimeDelegateResource) != null;
  }

  private boolean isServerRuntimeDelegate() {
    Class<?> cls = getClass();
    do {
      if (serverRuntimeDelegateClassName.equals(cls.getName()))
        return true;
    }
    while ((cls = cls.getSuperclass()) != RuntimeDelegateImpl.class);

    return false;
  }

  public RuntimeDelegateImpl() {
    if (!isServerRuntimeDelegate() && (isServerPresent(Thread.currentThread().getContextClassLoader()) || isServerPresent(getClass().getClassLoader()))) {
      System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, getClass().getName());
      throw new ServiceConfigurationError("Server is present and should be loaded instead");
    }
  }

  @Override
  public <T>T createEndpoint(final Application application, final Class<T> endpointType) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public <T>HeaderDelegate<T> createHeaderDelegate(final Class<T> type) {
    if (type == null)
      throw new IllegalArgumentException("type is null");

    return Delegate.lookup(type);
  }

  @Override
  public Link.Builder createLinkBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder createResponseBuilder() {
    throw new UnsupportedOperationException();
  }

  @Override
  public UriBuilder createUriBuilder() {
    return new UriBuilderImpl();
  }

  @Override
  public VariantListBuilder createVariantListBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }
}