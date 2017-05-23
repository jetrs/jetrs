/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server.ext;

import java.util.Date;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant.VariantListBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.libx4j.xrs.server.core.ResponseBuilderImpl;

public class RuntimeDelegateImpl extends RuntimeDelegate {
  @Override
  public UriBuilder createUriBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder createResponseBuilder() {
    return new ResponseBuilderImpl();
  }

  @Override
  public VariantListBuilder createVariantListBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public <T>T createEndpoint(final Application application, final Class<T> endpointType) throws IllegalArgumentException, UnsupportedOperationException {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T>HeaderDelegate<T> createHeaderDelegate(final Class<T> type) throws IllegalArgumentException {
    if (type == MediaType.class)
      return (HeaderDelegate<T>)new MediaTypeHeaderDelegate();

    if (type == Date.class)
      return (HeaderDelegate<T>)new DateHeaderDelegate();

    throw new UnsupportedOperationException("Unexpected header object type: " + type.getName());
  }

  @Override
  public Builder createLinkBuilder() {
    // TODO:
    throw new UnsupportedOperationException();
  }
}