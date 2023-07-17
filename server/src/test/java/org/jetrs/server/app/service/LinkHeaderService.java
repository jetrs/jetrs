/* Copyright (c) 2023 JetRS
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

import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

@Path("link")
public class LinkHeaderService {
  private static String getSenderLink(final UriInfo info) {
    final String basePath = info.getMatchedURIs().get(0);
    final UriBuilder builder = info.getBaseUriBuilder();
    builder.path(basePath);
    builder.path("sender");
    return "<" + builder.build().toString() + ">; rel=\"sender\"; title=\"sender\"";
  }

  private static String getTopLink(final UriInfo info) {
    final String basePath = info.getMatchedURIs().get(0);
    final UriBuilder builder = info.getBaseUriBuilder();
    builder.path(basePath);
    builder.path("poller");
    return "<" + builder.build().toString() + ">; rel=\"top-message\"; title=\"top-message\"";
  }

  @POST
  @Path("link")
  public Response postLink(@HeaderParam(HttpHeaders.LINK) final Link link) {
    return Response.noContent().header(HttpHeaders.LINK, link).build();
  }

  @POST
  @Path("links")
  public Response postLinks(@HeaderParam(HttpHeaders.LINK) final Link[] links) {
    return Response.noContent().header(HttpHeaders.LINK, links).build();
  }

  @POST
  @Path("str")
  public Response postString(@HeaderParam(HttpHeaders.LINK) final String string) {
    return Response.noContent().header(HttpHeaders.LINK, string).build();
  }

  @HEAD
  @Path("topic")
  public Response head(@Context final UriInfo uriInfo) {
    return Response.ok().header(HttpHeaders.LINK, getSenderLink(uriInfo)).header(HttpHeaders.LINK, getTopLink(uriInfo)).build();
  }
}