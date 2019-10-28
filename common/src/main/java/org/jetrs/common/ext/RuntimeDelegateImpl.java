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

public class RuntimeDelegateImpl extends RuntimeDelegate {
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

  @Override
  public <T>T createEndpoint(final Application application, final Class<T> endpointType) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T>HeaderDelegate<T> createHeaderDelegate(final Class<T> type) {
    if (type == MediaType.class)
      return (HeaderDelegate<T>)new MediaTypeHeaderDelegate();

    if (type == Date.class)
      return (HeaderDelegate<T>)new DateHeaderDelegateImpl();

    if (type == String[].class)
      return (HeaderDelegate<T>)new StringArrayHeaderDelegate();

    if (type == String.class)
      return (HeaderDelegate<T>)new StringHeaderDelegate();

    if (type == CacheControl.class)
      return (HeaderDelegate<T>)new CacheControlHeaderDelegate();

    if (type == Cookie.class)
      return (HeaderDelegate<T>)new CookieHeaderDelegate();

    if (type == NewCookie.class)
      return (HeaderDelegate<T>)new NewCookieHeaderDelegate();

    if (type == EntityTag.class)
      throw new UnsupportedOperationException();

    if (type == Link.class)
      throw new UnsupportedOperationException();

    return null;
  }

  @Override
  public Link.Builder createLinkBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }
}