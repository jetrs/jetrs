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
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

@Path("type")
public class CoreTypeService {
  private static void assertPathSegments(final List<String> pathParameters, final Object[] v, final PathSegment s, final PathSegment[] a, final List<PathSegment> l) {
    assertEquals("a" + v[0], s.getPath());
    assertEquals("a" + v[0], a[0].getPath());
    assertEquals(v[1] + "b", a[1].getPath());
    assertEquals("c" + v[2] + "d", a[2].getPath());
    assertEquals(3, pathParameters.size());
    assertEquals(3, a.length);
    assertEquals(3, l.size());
    for (int i = 0, i$ = v.length; i < i$; ++i) { // [A]
      assertEquals(pathParameters.get(i), String.valueOf(v[i]));
      assertEquals(a[i], l.get(i));
    }
  }

  private @Context UriInfo uriInfo;
  private @PathParam("p") PathSegment firstSegment;
  private @PathParam("p") PathSegment[] segmentArray;
  private @PathParam("p") List<PathSegment> segmentList;

  @POST
  @Path("boolean/a{p::.+}/{p::.+}b/c{p::.+}d")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public String getBoolean(@Context final UriInfo uriInfo, @PathParam("p") final boolean firstPrimitive, @PathParam("p") final Boolean firstObject, @PathParam("p") final boolean[] arrayPrivimite, @PathParam("p") final Boolean[] arrayObject, @PathParam("p") final List<Boolean> list, @PathParam("p") final PathSegment firstSegment, @PathParam("p") final PathSegment[] segmentArray, @PathParam("p") final List<PathSegment> segmentList, @QueryParam("q") final boolean queryParam, @QueryParam("Q") final Boolean queryParamObj, final MultivaluedMap<String,String> body) {
    assertEquals(firstPrimitive, queryParam);
    assertEquals(firstObject, queryParamObj);
    assertEquals(firstPrimitive, firstObject.booleanValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    String total = "";
    for (int i = 0, i$ = arrayPrivimite.length; i < i$; ++i) { // [A]
      assertEquals(arrayPrivimite[i], arrayObject[i].booleanValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("p").size());
    assertPathSegments(uriInfo.getPathParameters().get("p"), arrayObject, firstSegment, segmentArray, segmentList);

    assertEquals(body.toString(), 3, body.size());
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("p"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("q"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("Q"));
    return total;
  }

  @POST
  @Path("char/a{p::.+}/{p::.+}b/c{p::.+}d")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public String getCharacter(@Context final UriInfo uriInfo, @PathParam("p") final char firstPrimitive, @PathParam("p") final Character firstObject, @PathParam("p") final char[] arrayPrivimite, @PathParam("p") final Character[] arrayObject, @PathParam("p") final List<Character> list, @PathParam("p") final PathSegment firstSegment, @PathParam("p") final PathSegment[] segmentArray, @PathParam("p") final List<PathSegment> segmentList, @QueryParam("q") final char queryParam, @QueryParam("Q") final Character queryParamObj, final MultivaluedMap<String,String> body) {
    assertEquals(firstPrimitive, queryParam);
    assertEquals(firstObject, queryParamObj);
    assertEquals(firstPrimitive, firstObject.charValue(), 0);
    assertEquals(firstPrimitive, arrayPrivimite[0], 0);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    String total = "";
    for (int i = 0, i$ = arrayPrivimite.length; i < i$; ++i) { // [A]
      assertEquals(arrayPrivimite[i], arrayObject[i].charValue(), 0);
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("p").size());
    assertPathSegments(uriInfo.getPathParameters().get("p"), arrayObject, firstSegment, segmentArray, segmentList);

    assertEquals(body.toString(), 3, body.size());
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("p"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("q"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("Q"));
    return total;
  }

  @POST
  @Path("byte/a{p::-?\\d{1,3}}/{p::-?\\d{1,3}}b/c{p::-?\\d{1,3}}d")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public int getByte(@Context final UriInfo uriInfo, @PathParam("p") final byte firstPrimitive, @PathParam("p") final Byte firstObject, @PathParam("p") final byte[] arrayPrivimite, @PathParam("p") final Byte[] arrayObject, @PathParam("p") final List<Byte> list, @PathParam("p") final PathSegment firstSegment, @PathParam("p") final PathSegment[] segmentArray, @PathParam("p") final List<PathSegment> segmentList, @QueryParam("q") final byte queryParam, @QueryParam("Q") final Byte queryParamObj, final MultivaluedMap<String,String> body) {
    assertEquals(firstPrimitive, queryParam);
    assertEquals(firstObject, queryParamObj);
    assertEquals(firstPrimitive, firstObject.byteValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    int total = 0;
    for (int i = 0, i$ = arrayPrivimite.length; i < i$; ++i) { // [A]
      assertEquals(arrayPrivimite[i], arrayObject[i].byteValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("p").size());
    assertPathSegments(uriInfo.getPathParameters().get("p"), arrayObject, firstSegment, segmentArray, segmentList);

    assertEquals(body.toString(), 3, body.size());
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("p"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("q"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("Q"));
    return total;
  }

  @POST
  @Path("short/a{p::-?\\d{1,5}}/{p::-?\\d{1,5}}b/c{p::-?\\d{1,5}}d")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public int getShort(@Context final UriInfo uriInfo, @PathParam("p") final short firstPrimitive, @PathParam("p") final Short firstObject, @PathParam("p") final short[] arrayPrivimite, @PathParam("p") final Short[] arrayObject, @PathParam("p") final List<Short> list, @PathParam("p") final PathSegment firstSegment, @PathParam("p") final PathSegment[] segmentArray, @PathParam("p") final List<PathSegment> segmentList, @QueryParam("q") final short queryParam, @QueryParam("Q") final Short queryParamObj, final MultivaluedMap<String,String> body) {
    assertEquals(firstPrimitive, queryParam);
    assertEquals(firstObject, queryParamObj);
    assertEquals(firstPrimitive, firstObject.shortValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    int total = 0;
    for (int i = 0, i$ = arrayPrivimite.length; i < i$; ++i) { // [A]
      assertEquals(arrayPrivimite[i], arrayObject[i].shortValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("p").size());
    assertPathSegments(uriInfo.getPathParameters().get("p"), arrayObject, firstSegment, segmentArray, segmentList);

    assertEquals(body.toString(), 3, body.size());
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("p"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("q"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("Q"));
    return total;
  }

  @POST
  @Path("int/a{p::-?\\d{1,10}}/{p::-?\\d{1,10}}b/c{p::-?\\d{1,10}}d")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public int getInt(@Context final UriInfo uriInfo, @PathParam("p") final int firstPrimitive, @PathParam("p") final Integer firstObject, @PathParam("p") final int[] arrayPrivimite, @PathParam("p") final Integer[] arrayObject, @PathParam("p") final List<Integer> list, @PathParam("p") final PathSegment firstSegment, @PathParam("p") final PathSegment[] segmentArray, @PathParam("p") final List<PathSegment> segmentList, @QueryParam("q") final int queryParam, @QueryParam("Q") final Integer queryParamObj, final MultivaluedMap<String,String> body) {
    assertEquals(firstPrimitive, queryParam);
    assertEquals(firstObject, queryParamObj);
    assertEquals(firstPrimitive, firstObject.intValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    int total = 0;
    for (int i = 0, i$ = arrayPrivimite.length; i < i$; ++i) { // [A]
      assertEquals(arrayPrivimite[i], arrayObject[i].intValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("p").size());
    assertPathSegments(uriInfo.getPathParameters().get("p"), arrayObject, firstSegment, segmentArray, segmentList);

    assertEquals(body.toString(), 3, body.size());
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("p"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("q"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("Q"));
    return total;
  }

  @POST
  @Path("long/a{p::-?\\d{1,19}}/{p::-?\\d{1,19}}b/c{p::-?\\d{1,19}}d")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public long getLong(@Context final UriInfo uriInfo, @PathParam("p") final long firstPrimitive, @PathParam("p") final Long firstObject, @PathParam("p") final long[] arrayPrivimite, @PathParam("p") final Long[] arrayObject, @PathParam("p") final List<Long> list, @PathParam("p") final PathSegment firstSegment, @PathParam("p") final PathSegment[] segmentArray, @PathParam("p") final List<PathSegment> segmentList, @QueryParam("q") final long queryParam, @QueryParam("Q") final Long queryParamObj, final MultivaluedMap<String,String> body) {
    assertEquals(firstPrimitive, queryParam);
    assertEquals(firstObject, queryParamObj);
    assertEquals(firstPrimitive, firstObject.longValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    long total = 0;
    for (int i = 0, i$ = arrayPrivimite.length; i < i$; ++i) { // [A]
      assertEquals(arrayPrivimite[i], arrayObject[i].longValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("p").size());
    assertPathSegments(uriInfo.getPathParameters().get("p"), arrayObject, firstSegment, segmentArray, segmentList);

    assertEquals(body.toString(), 3, body.size());
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("p"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("q"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("Q"));
    return total;
  }

  @POST
  @Path("float/a{p::-?\\d*\\.?\\d{1,10}(E-?\\d+)?}/{p::-?\\d*\\.?\\d{1,10}(E-?\\d+)?}b/c{p::-?\\d*\\.?\\d{1,10}(E-?\\d+)?}d")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public float getFloat(@Context final UriInfo uriInfo, @PathParam("p") final float firstPrimitive, @PathParam("p") final Float firstObject, @PathParam("p") final float[] arrayPrivimite, @PathParam("p") final Float[] arrayObject, @PathParam("p") final List<Float> list, @PathParam("p") final PathSegment firstSegment, @PathParam("p") final PathSegment[] segmentArray, @PathParam("p") final List<PathSegment> segmentList, @QueryParam("q") final float queryParam, @QueryParam("Q") final Float queryParamObj, final MultivaluedMap<String,String> body) {
    assertEquals(firstPrimitive, queryParam, 0);
    assertEquals(firstObject, queryParamObj);
    assertEquals(firstPrimitive, firstObject.floatValue(), 0);
    assertEquals(firstPrimitive, arrayPrivimite[0], 0);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    float total = 0;
    for (int i = 0, i$ = arrayPrivimite.length; i < i$; ++i) { // [A]
      assertEquals(arrayPrivimite[i], arrayObject[i].floatValue(), 0);
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("p").size());
    assertPathSegments(uriInfo.getPathParameters().get("p"), arrayObject, firstSegment, segmentArray, segmentList);

    assertEquals(body.toString(), 3, body.size());
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("p"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("q"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("Q"));
    return total;
  }

  @POST
  @Path("double/a{p::-?\\d*\\.?\\d{1,19}(E-?\\d+)?}/{p::-?\\d*\\.?\\d{1,19}(E-?\\d+)?}b/c{p::-?\\d*\\.?\\d{1,19}(E-?\\d+)?}d")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.TEXT_PLAIN)
  public double getDouble(@Context final UriInfo uriInfo, @PathParam("p") final double firstPrimitive, @PathParam("p") final Double firstObject, @PathParam("p") final double[] arrayPrivimite, @PathParam("p") final Double[] arrayObject, @PathParam("p") final List<Double> list, @PathParam("p") final PathSegment firstSegment, @PathParam("p") final PathSegment[] segmentArray, @PathParam("p") final List<PathSegment> segmentList, @QueryParam("q") final double queryParam, @QueryParam("Q") final Double queryParamObj, final MultivaluedMap<String,String> body) {
    assertEquals(firstPrimitive, queryParam, 0);
    assertEquals(firstObject, queryParamObj);
    assertEquals(firstPrimitive, firstObject.doubleValue(), 0);
    assertEquals(firstPrimitive, arrayPrivimite[0], 0);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    double total = 0;
    for (int i = 0, i$ = arrayPrivimite.length; i < i$; ++i) { // [A]
      assertEquals(arrayPrivimite[i], arrayObject[i].doubleValue(), 0);
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("p").size());
    assertPathSegments(uriInfo.getPathParameters().get("p"), arrayObject, firstSegment, segmentArray, segmentList);

    assertEquals(body.toString(), 3, body.size());
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("p"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("q"));
    assertEquals(body.toString(), String.valueOf(firstObject), body.getFirst("Q"));
    return total;
  }
}