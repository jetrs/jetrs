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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("root1/")
@SuppressWarnings("unused")
public class RootService1 {
  @GET
  @Path("1/")
  @Produces("text/plain")
  public Response get1() {
    return Response.ok("GET").build();
  }

  @OPTIONS
  @Path("/1")
  public Response options1() {
    return Response.ok().header("Access-Control-Allow-Methods", "GET,POST").build();
  }

  @HEAD
  @Path("2/")
  public Response head2() {
    return Response.ok("HEAD").build();
  }

  @GET
  @Path("/2/")
  public Response get2() {
    return Response.ok("GET").build();
  }

  @POST
  @Path("/1/{id:[\\da-z]{32}}")
  @Consumes("text/plain")
  public Response post1(@PathParam("id") final String uuid, final String data) {
    return Response.ok("POST").build();
  }

  @POST
  @Path("1/{id:[\\d]+}/")
  @Consumes("text/plain")
  public Response post1(@PathParam("id") final int id, final String data) {
    return Response.ok("POST").build();
  }

  @POST
  @Path("/1/{id:[\\d]+}/")
  @Consumes("text/plain")
  public Response post1(@PathParam("id") final long id, final String data) {
    return Response.ok("POST").build();
  }
}