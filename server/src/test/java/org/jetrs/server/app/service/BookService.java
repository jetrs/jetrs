/* Copyright (c) 2022 JetRS
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jetrs.server.ApplicationServerTest;

@Path("/books")
@Singleton
public class BookService {
  private static String toString(final Object ... args) {
    final StringBuilder builder = new StringBuilder();
    for (final Object arg : args) { // [A]
      if (arg instanceof Collection) {
        builder.append(toString(((Collection<?>)arg).toArray()));
      }
      else if (arg instanceof PathSegment) {
        final PathSegment pathSegment = (PathSegment)arg;
        builder.append("{" + pathSegment.getPath() + "}: " + ApplicationServerTest.encodeLexicographically(pathSegment.getMatrixParameters()));
      }
      else {
        builder.append(arg);
      }

      builder.append(" | ");
    }

    return builder.length() == 0 ? "" : builder.substring(0, builder.length() - 3);
  }

  @GET
  @Path("/aaa{param1:b+}/{param2:xxx}/{param2:.*}/stuff")
  public Response getIt(@PathParam("param1") final List<PathSegment> seg1, @PathParam("param1") final String param1, @PathParam("param2") final String param2, @PathParam("param2") final List<PathSegment> seg2) {
    return Response.status(200).entity(seg1.stream().map(PathSegment::getPath).collect(Collectors.joining("|")) + ":" + param1 + ":" + param2 + ":" + seg2.stream().map(PathSegment::getPath).collect(Collectors.joining("|"))).build();
  }

  @GET
  @Path("{years:\\d+(/\\d+)*}")
  public Response getBooks(@PathParam("years") final String[] years, @PathParam("years") final PathSegment[] segs, @MatrixParam("author") final String author, @MatrixParam("country") final String country) {
    assertEquals(1, years.length);
    return Response.status(200).entity("getBooks is called, years: " + Arrays.toString(years) + ", segs: " + Arrays.toString(segs) + ", author: " + author + ", country: " + country).build();
  }

  @GET
  @Path("/query/a{a}/{a}a{b}b/c{c}c/b{b:[^/]+(/+[^/]+)*}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getListOfBooksOrderBy(@PathParam("a") final String a0, @PathParam("a") final String[] a1, @PathParam("a") final List<String> a2, @PathParam("a") final PathSegment aSeg0, @PathParam("a") final PathSegment[] aSeg1, @PathParam("a") final List<PathSegment> aSeg2, @PathParam("b") final String b0, @PathParam("b") final String[] b1, @PathParam("b") final List<String> b2, @PathParam("b") final PathSegment bSeg0, @PathParam("b") final PathSegment[] bSeg1, @PathParam("b") final List<PathSegment> bSeg2, @MatrixParam("orderBy") final List<String> orderBy) {
    assertEquals(2, a1.length);
    assertEquals(2, a2.size());
    assertEquals(a0, a1[0]);
    assertEquals(a1[0], a2.get(0));
    assertEquals(a1[1], a2.get(1));
    assertEquals(2, aSeg1.length);
    assertEquals(2, aSeg2.size());
    assertEquals(aSeg0, aSeg1[0]);
    assertEquals(aSeg1[0], aSeg2.get(0));
    assertEquals(aSeg1[1], aSeg2.get(1));
    assertEquals("a" + a1[0], aSeg1[0].getPath());
    assertEquals(a1[1] + "a" + b1[0] + "b", aSeg1[1].getPath());
    for (int i = 0, i$ = aSeg1.length; i < i$; ++i) // [A]
      assertEquals(aSeg1[i], aSeg2.get(i));

    assertEquals(2, b1.length);
    assertEquals(2, b2.size());
    assertEquals(b0, b1[0]);
    assertEquals(b1[0], b2.get(0));
    assertEquals(b1[1], b2.get(1));
    assertEquals(bSeg1.length, bSeg2.size());
    assertEquals(bSeg0, bSeg1[0]);
    assertEquals(bSeg1[0], bSeg2.get(0));
    assertEquals(bSeg1[1], bSeg2.get(1));
    assertEquals(a1[1] + "a" + b1[0] + "b", bSeg1[0].getPath());
    for (int i = 0, i$ = bSeg1.length; i < i$; ++i) // [A]
      assertEquals(bSeg1[i], bSeg2.get(i));


    return Response.status(200).entity("List of Books order by: " + aSeg2 + " :: " + bSeg2 + " :: " + orderBy).build();
  }

  @GET
  @Path("header")
  public long header(@HeaderParam(HttpHeaders.IF_UNMODIFIED_SINCE) final Date ifModifiedSince) {
    return ifModifiedSince.getTime();
  }

  @GET
  @Path("{categoriesParam:categories\\d}/static/{objectsParam:objects\\d}")
  public String objectsByCategory(@Context final UriInfo uriInfo, @PathParam("categoriesParam") final PathSegment categoriesParamSegment, @PathParam("objectsParam") final PathSegment objectsParamSegment, @MatrixParam("name") final String firstObjectName, @MatrixParam("name") final List<String> allObjectNames) {
    return toString(uriInfo.getPathSegments(), categoriesParamSegment, objectsParamSegment, firstObjectName, allObjectNames);
  }
}