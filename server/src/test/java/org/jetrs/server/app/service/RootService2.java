/* Copyright (c) 2021 JetRS
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

package org.jetrs.server.app.service;

import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Singleton
@Path("root2/")
@SuppressWarnings("unused")
public class RootService2 {
  private static String toString(final ContainerRequestContext containerRequestContext) {
    final UriInfo uriInfo = containerRequestContext.getUriInfo();
    return uriInfo.getMatchedURIs() + "\n" + uriInfo.getMatchedResources().stream().map(o -> o.getClass().getName()).collect(Collectors.joining(", ")) + "\n" + uriInfo.getPathParameters();
  }

  @GET
  @Path("/2/filter")
  @Produces(MediaType.TEXT_PLAIN)
  public String get2Filter(@Context final ContainerRequestContext containerRequestContext) {
    return toString(containerRequestContext);
  }

  @GET
  @Path("2/")
  @Produces(MediaType.TEXT_PLAIN)
  public String get2(@Context final ContainerRequestContext containerRequestContext) {
    return toString(containerRequestContext);
  }

  @GET
  @Path("/2/{id:\\d+}/")
  @Produces(MediaType.TEXT_PLAIN)
  public String get2(@Context final ContainerRequestContext containerRequestContext, @PathParam("id") final int id) {
    return toString(containerRequestContext);
  }

  @GET
  @Path("2/{id1:\\d+}/{id2:\\d+}")
  @Produces(MediaType.TEXT_PLAIN)
  public String get1(@Context final ContainerRequestContext containerRequestContext, @PathParam("id1") final int id, @PathParam("id2") final int id2) {
    return toString(containerRequestContext);
  }
}