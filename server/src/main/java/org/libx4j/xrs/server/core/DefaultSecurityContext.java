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

package org.libx4j.xrs.server.core;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;

public final class DefaultSecurityContext implements SecurityContext {
  private final HttpServletRequest request;

  public DefaultSecurityContext(final HttpServletRequest request) {
    this.request = request;
  }

  @Override
  public final Principal getUserPrincipal() {
    return null;
  }

  @Override
  public final boolean isUserInRole(final String role) {
    return false;
  }

  @Override
  public final boolean isSecure() {
    return request.isSecure();
  }

  @Override
  public final String getAuthenticationScheme() {
    return null;
  }
}