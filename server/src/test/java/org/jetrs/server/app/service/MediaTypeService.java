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

import static org.junit.Assert.*;

import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

@Path("mime")
public class MediaTypeService {
  private static void assertContentType(final String json, final Object ... mediaTypes) {
    final String expectedContentType = json.substring(2, json.length() - 2);
    for (final Object mediaType : mediaTypes)
      if(expectedContentType.equalsIgnoreCase(mediaType.toString()))
        return;

    fail(expectedContentType + " is not in " + Arrays.toString(mediaTypes));
  }

  @POST
  @Path("ca")
  @Consumes("*/*")
  public void ca(@Context final ContainerRequestContext requextContext, final String json) {
    assertContentType(json, requextContext.getMediaType());
  }

  @POST
  @Path("cb")
  @Consumes({"application/json", "text/html"})
  public void cb(@Context final ContainerRequestContext requextContext, final String json) {
    assertContentType(json, requextContext.getMediaType());
  }

  @POST
  @Path("cc")
  @Consumes({"application/*+json;charset=utf-8;qs=.5", "application/json;charset=ascii;qs=.6", "text/json", "text/html"})
  public void cc(@Context final ContainerRequestContext requextContext, final String json) {
    assertContentType(json, requextContext.getMediaType());
  }

  @POST
  @Path("pa")
  @Consumes("*/*")
  @Produces("*/*")
  public String pa(@Context final ContainerRequestContext requextContext, final String json) {
    assertContentType(json, requextContext.getAcceptableMediaTypes().toArray());
    return json;
  }

  @POST
  @Path("pb")
  @Consumes("*/*")
  @Produces({"application/json", "text/html"})
  public String pb(@Context final ContainerRequestContext requextContext, final String json) {
    assertContentType(json, requextContext.getAcceptableMediaTypes().toArray());
    return json;
  }

  @POST
  @Path("pc")
  @Consumes("*/*")
  @Produces({"application/*+json;charset=utf-8;qs=.5", "application/json;charset=ascii;qs=.6", "text/json", "text/html"})
  public String pc(@Context final ContainerRequestContext requextContext, final String json) {
    assertContentType(json, requextContext.getAcceptableMediaTypes().toArray());
    return json;
  }
}