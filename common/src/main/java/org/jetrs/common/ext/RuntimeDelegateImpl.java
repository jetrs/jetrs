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

import java.util.Date;
import java.util.ServiceConfigurationError;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant.VariantListBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jetrs.common.core.UriBuilderImpl;
import org.jetrs.common.ext.delegate.CacheControlHeaderDelegate;
import org.jetrs.common.ext.delegate.CookieHeaderDelegate;
import org.jetrs.common.ext.delegate.DateHeaderDelegate;
import org.jetrs.common.ext.delegate.MediaTypeHeaderDelegate;
import org.jetrs.common.ext.delegate.NewCookieHeaderDelegate;
import org.jetrs.common.ext.delegate.StringArrayHeaderDelegate;
import org.jetrs.common.ext.delegate.StringHeaderDelegate;

public abstract class RuntimeDelegateImpl extends RuntimeDelegate {
  private static final String serverRuntimeDelegateClassName = "org.jetrs.server.ext.ServerRuntimeDelegate";

  private static boolean isServerPresent(final ClassLoader classLoader) {
    return classLoader.getResource(serverRuntimeDelegateClassName.replace('.', '/').concat(".class")) != null;
  }

  public RuntimeDelegateImpl() {
    if (!serverRuntimeDelegateClassName.equals(getClass().getName()) && (isServerPresent(Thread.currentThread().getContextClassLoader()) || isServerPresent(getClass().getClassLoader()))) {
      System.setProperty(RuntimeDelegate.JAXRS_RUNTIME_DELEGATE_PROPERTY, serverRuntimeDelegateClassName);
      throw new ServiceConfigurationError("Server is present and should be loaded instead");
    }
  }

  @Override
  public <T>T createEndpoint(final Application application, final Class<T> endpointType) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T>HeaderDelegate<T> createHeaderDelegate(final Class<T> type) {
    if (MediaType.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)new MediaTypeHeaderDelegate();

    if (Date.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)new DateHeaderDelegate();

    if (String[].class.isAssignableFrom(type))
      return (HeaderDelegate<T>)new StringArrayHeaderDelegate();

    if (String.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)new StringHeaderDelegate();

    if (CacheControl.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)new CacheControlHeaderDelegate();

    if (NewCookie.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)new NewCookieHeaderDelegate();

    if (Cookie.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)new CookieHeaderDelegate();

    if (EntityTag.class.isAssignableFrom(type))
      throw new UnsupportedOperationException();

    if (Link.class.isAssignableFrom(type))
      throw new UnsupportedOperationException();

    return null;
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