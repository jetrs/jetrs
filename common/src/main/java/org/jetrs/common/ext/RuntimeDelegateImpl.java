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
import org.jetrs.provider.ext.delegate.CacheControlHeaderDelegate;
import org.jetrs.provider.ext.delegate.CookieHeaderDelegate;
import org.jetrs.provider.ext.delegate.DateHeaderDelegate;
import org.jetrs.provider.ext.delegate.DefaultHeaderDelegate;
import org.jetrs.provider.ext.delegate.MediaTypeHeaderDelegate;
import org.jetrs.provider.ext.delegate.NewCookieHeaderDelegate;
import org.jetrs.provider.ext.delegate.StringArrayHeaderDelegate;
import org.jetrs.provider.ext.delegate.StringHeaderDelegate;

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

  private static final MediaTypeHeaderDelegate mediaTypeHeaderDelegate = new MediaTypeHeaderDelegate();
  private static final DateHeaderDelegate dateHeaderDelegate = new DateHeaderDelegate();
  private static final StringArrayHeaderDelegate stringArrayHeaderDelegate = new StringArrayHeaderDelegate();
  private static final StringHeaderDelegate stringHeaderDelegate = new StringHeaderDelegate();
  private static final CacheControlHeaderDelegate cacheControlHeaderDelegate = new CacheControlHeaderDelegate();
  private static final NewCookieHeaderDelegate newCookieHeaderDelegate = new NewCookieHeaderDelegate();
  private static final CookieHeaderDelegate cookieHeaderDelegate = new CookieHeaderDelegate();
  private static final DefaultHeaderDelegate defaultHeaderDelegate = new DefaultHeaderDelegate();

  @Override
  @SuppressWarnings("unchecked")
  public <T>HeaderDelegate<T> createHeaderDelegate(final Class<T> type) {
    if (type == null)
      throw new IllegalArgumentException("type is null");

    if (MediaType.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)mediaTypeHeaderDelegate;

    if (Date.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)dateHeaderDelegate;

    if (String[].class.isAssignableFrom(type))
      return (HeaderDelegate<T>)stringArrayHeaderDelegate;

    if (String.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)stringHeaderDelegate;

    if (CacheControl.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)cacheControlHeaderDelegate;

    if (NewCookie.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)newCookieHeaderDelegate;

    if (Cookie.class.isAssignableFrom(type))
      return (HeaderDelegate<T>)cookieHeaderDelegate;

    if (EntityTag.class.isAssignableFrom(type))
      throw new UnsupportedOperationException();

    if (Link.class.isAssignableFrom(type))
      throw new UnsupportedOperationException();

    return (HeaderDelegate<T>)defaultHeaderDelegate;
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