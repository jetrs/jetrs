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

package org.jetrs.server.ext;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.jetrs.common.ext.RuntimeDelegateImpl;
import org.jetrs.server.EndpointFactory;
import org.jetrs.server.ResourceContext;
import org.jetrs.server.core.ResponseBuilderImpl;

public class ServerRuntimeDelegate extends RuntimeDelegateImpl {
  private final ResourceContext resourceContext;

  public ServerRuntimeDelegate(final ResourceContext resourceContext) {
    this.resourceContext = resourceContext;
  }

  public ServerRuntimeDelegate() {
    this(null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T>T createEndpoint(final Application application, final Class<T> endpointType) {
    if (!HttpServlet.class.equals(endpointType))
      throw new IllegalArgumentException("Only " + HttpServlet.class.getName() + " endpoint type is supported");

    return (T)EndpointFactory.createEndpoint(application);
  }

  @Override
  public ResponseBuilder createResponseBuilder() {
    if (resourceContext == null)
      throw new IllegalStateException("Server environment has not yet initialized");

    return new ResponseBuilderImpl(resourceContext);
  }
}