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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.container.ContainerRequestContext;

class HttpServletRequestImpl extends HttpServletRequestWrapper {
  HttpServletRequestImpl(final HttpServletRequest request) {
    super(request);
  }

  // FIXME: Try to remove these...

  private ContainerRequestContext requestContext;

  ContainerRequestContext getRequestContext() {
    return requestContext;
  }

  void setRequestContext(final ContainerRequestContext requestContext) {
    this.requestContext = requestContext;
  }

  private ResourceManifest resourceManifest;

  ResourceManifest getResourceManifest() {
    return resourceManifest;
  }

  void setResourceManifest(final ResourceManifest resourceManifest) {
    this.resourceManifest = resourceManifest;
  }
}