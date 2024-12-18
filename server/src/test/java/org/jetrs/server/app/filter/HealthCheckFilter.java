/* Copyright (c) 2024 JetRS
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

package org.jetrs.server.app.filter;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
@PreMatching
@Priority(0)
public class HealthCheckFilter implements ContainerRequestFilter {
  @Override
  public void filter(final ContainerRequestContext requestContext) {
    if (requestContext.getUriInfo().getPath().length() == 0)
      requestContext.abortWith(Response.status(HttpMethod.GET.equals(requestContext.getMethod()) ? Response.Status.OK : Response.Status.NO_CONTENT).build());
  }
}