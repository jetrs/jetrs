/* Copyright (c) 2016 JetRS
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

import java.util.Date;
import java.util.List;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

final class RequestImpl implements Request {
  private final String method;

  RequestImpl(final String method) {
    this.method = method;
  }

  @Override
  public String getMethod() {
    return method;
  }

  @Override
  public Variant selectVariant(final List<Variant> variants) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder evaluatePreconditions(final EntityTag eTag) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder evaluatePreconditions(final Date lastModified) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder evaluatePreconditions(final Date lastModified, final EntityTag eTag) {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }

  @Override
  public ResponseBuilder evaluatePreconditions() {
    // TODO: Implement this.
    throw new UnsupportedOperationException();
  }
}