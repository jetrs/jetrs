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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
    for (int i = 0; i < v.length; ++i) {
      assertEquals(pathParameters.get(i), String.valueOf(v[i]));
      assertEquals(a[i], l.get(i));
    }
  }

  private @Context UriInfo uriInfo;
  private @PathParam("param") PathSegment firstSegment;
  private @PathParam("param") PathSegment[] segmentArray;
  private @PathParam("param") List<PathSegment> segmentList;

  @GET
  @Path("boolean/a{param::.+}/{param::.+}b/c{param::.+}d")
  @Produces(MediaType.TEXT_PLAIN)
  public String getBoolean(@Context final UriInfo uriInfo, @PathParam("param") final boolean firstPrimitive, @PathParam("param") final Boolean firstObject, @PathParam("param") final boolean[] arrayPrivimite, @PathParam("param") final Boolean[] arrayObject, @PathParam("param") final List<Boolean> list, @PathParam("param") final PathSegment firstSegment, @PathParam("param") final PathSegment[] segmentArray, @PathParam("param") final List<PathSegment> segmentList) {
    assertEquals(firstPrimitive, firstObject.booleanValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    String total = "";
    for (int i = 0; i < arrayPrivimite.length; ++i) {
      assertEquals(arrayPrivimite[i], arrayObject[i].booleanValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("param").size());
    assertPathSegments(uriInfo.getPathParameters().get("param"), arrayObject, firstSegment, segmentArray, segmentList);
    return total;
  }

  @GET
  @Path("char/a{param::.+}/{param::.+}b/c{param::.+}d")
  @Produces(MediaType.TEXT_PLAIN)
  public String getCharacter(@Context final UriInfo uriInfo, @PathParam("param") final char firstPrimitive, @PathParam("param") final Character firstObject, @PathParam("param") final char[] arrayPrivimite, @PathParam("param") final Character[] arrayObject, @PathParam("param") final List<Character> list, @PathParam("param") final PathSegment firstSegment, @PathParam("param") final PathSegment[] segmentArray, @PathParam("param") final List<PathSegment> segmentList) {
    assertEquals(firstPrimitive, firstObject.charValue(), 0);
    assertEquals(firstPrimitive, arrayPrivimite[0], 0);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    String total = "";
    for (int i = 0; i < arrayPrivimite.length; ++i) {
      assertEquals(arrayPrivimite[i], arrayObject[i].charValue(), 0);
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("param").size());
    assertPathSegments(uriInfo.getPathParameters().get("param"), arrayObject, firstSegment, segmentArray, segmentList);
    return total;
  }

  @GET
  @Path("byte/a{param::-?\\d{1,3}}/{param::-?\\d{1,3}}b/c{param::-?\\d{1,3}}d")
  @Produces(MediaType.TEXT_PLAIN)
  public int getByte(@Context final UriInfo uriInfo, @PathParam("param") final byte firstPrimitive, @PathParam("param") final Byte firstObject, @PathParam("param") final byte[] arrayPrivimite, @PathParam("param") final Byte[] arrayObject, @PathParam("param") final List<Byte> list, @PathParam("param") final PathSegment firstSegment, @PathParam("param") final PathSegment[] segmentArray, @PathParam("param") final List<PathSegment> segmentList) {
    assertEquals(firstPrimitive, firstObject.byteValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    int total = 0;
    for (int i = 0; i < arrayPrivimite.length; ++i) {
      assertEquals(arrayPrivimite[i], arrayObject[i].byteValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("param").size());
    assertPathSegments(uriInfo.getPathParameters().get("param"), arrayObject, firstSegment, segmentArray, segmentList);
    return total;
  }

  @GET
  @Path("short/a{param::-?\\d{1,5}}/{param::-?\\d{1,5}}b/c{param::-?\\d{1,5}}d")
  @Produces(MediaType.TEXT_PLAIN)
  public int getShort(@Context final UriInfo uriInfo, @PathParam("param") final short firstPrimitive, @PathParam("param") final Short firstObject, @PathParam("param") final short[] arrayPrivimite, @PathParam("param") final Short[] arrayObject, @PathParam("param") final List<Short> list, @PathParam("param") final PathSegment firstSegment, @PathParam("param") final PathSegment[] segmentArray, @PathParam("param") final List<PathSegment> segmentList) {
    assertEquals(firstPrimitive, firstObject.shortValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    int total = 0;
    for (int i = 0; i < arrayPrivimite.length; ++i) {
      assertEquals(arrayPrivimite[i], arrayObject[i].shortValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("param").size());
    assertPathSegments(uriInfo.getPathParameters().get("param"), arrayObject, firstSegment, segmentArray, segmentList);
    return total;
  }

  @GET
  @Path("int/a{param::-?\\d{1,10}}/{param::-?\\d{1,10}}b/c{param::-?\\d{1,10}}d")
  @Produces(MediaType.TEXT_PLAIN)
  public int getInt(@Context final UriInfo uriInfo, @PathParam("param") final int firstPrimitive, @PathParam("param") final Integer firstObject, @PathParam("param") final int[] arrayPrivimite, @PathParam("param") final Integer[] arrayObject, @PathParam("param") final List<Integer> list, @PathParam("param") final PathSegment firstSegment, @PathParam("param") final PathSegment[] segmentArray, @PathParam("param") final List<PathSegment> segmentList) {
    assertEquals(firstPrimitive, firstObject.intValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    int total = 0;
    for (int i = 0; i < arrayPrivimite.length; ++i) {
      assertEquals(arrayPrivimite[i], arrayObject[i].intValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("param").size());
    assertPathSegments(uriInfo.getPathParameters().get("param"), arrayObject, firstSegment, segmentArray, segmentList);
    return total;
  }

  @GET
  @Path("long/a{param::-?\\d{1,19}}/{param::-?\\d{1,19}}b/c{param::-?\\d{1,19}}d")
  @Produces(MediaType.TEXT_PLAIN)
  public long getLong(@Context final UriInfo uriInfo, @PathParam("param") final long firstPrimitive, @PathParam("param") final Long firstObject, @PathParam("param") final long[] arrayPrivimite, @PathParam("param") final Long[] arrayObject, @PathParam("param") final List<Long> list, @PathParam("param") final PathSegment firstSegment, @PathParam("param") final PathSegment[] segmentArray, @PathParam("param") final List<PathSegment> segmentList) {
    assertEquals(firstPrimitive, firstObject.longValue());
    assertEquals(firstPrimitive, arrayPrivimite[0]);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    long total = 0;
    for (int i = 0; i < arrayPrivimite.length; ++i) {
      assertEquals(arrayPrivimite[i], arrayObject[i].longValue());
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("param").size());
    assertPathSegments(uriInfo.getPathParameters().get("param"), arrayObject, firstSegment, segmentArray, segmentList);
    return total;
  }

  @GET
  @Path("float/a{param::-?\\d*\\.?\\d{1,10}(E-?\\d+)?}/{param::-?\\d*\\.?\\d{1,10}(E-?\\d+)?}b/c{param::-?\\d*\\.?\\d{1,10}(E-?\\d+)?}d")
  @Produces(MediaType.TEXT_PLAIN)
  public float getFloat(@Context final UriInfo uriInfo, @PathParam("param") final float firstPrimitive, @PathParam("param") final Float firstObject, @PathParam("param") final float[] arrayPrivimite, @PathParam("param") final Float[] arrayObject, @PathParam("param") final List<Float> list, @PathParam("param") final PathSegment firstSegment, @PathParam("param") final PathSegment[] segmentArray, @PathParam("param") final List<PathSegment> segmentList) {
    assertEquals(firstPrimitive, firstObject.floatValue(), 0);
    assertEquals(firstPrimitive, arrayPrivimite[0], 0);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    float total = 0;
    for (int i = 0; i < arrayPrivimite.length; ++i) {
      assertEquals(arrayPrivimite[i], arrayObject[i].floatValue(), 0);
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("param").size());
    assertPathSegments(uriInfo.getPathParameters().get("param"), arrayObject, firstSegment, segmentArray, segmentList);
    return total;
  }

  @GET
  @Path("double/a{param::-?\\d*\\.?\\d{1,19}(E-?\\d+)?}/{param::-?\\d*\\.?\\d{1,19}(E-?\\d+)?}b/c{param::-?\\d*\\.?\\d{1,19}(E-?\\d+)?}d")
  @Produces(MediaType.TEXT_PLAIN)
  public double getDouble(@Context final UriInfo uriInfo, @PathParam("param") final double firstPrimitive, @PathParam("param") final Double firstObject, @PathParam("param") final double[] arrayPrivimite, @PathParam("param") final Double[] arrayObject, @PathParam("param") final List<Double> list, @PathParam("param") final PathSegment firstSegment, @PathParam("param") final PathSegment[] segmentArray, @PathParam("param") final List<PathSegment> segmentList) {
    assertEquals(firstPrimitive, firstObject.doubleValue(), 0);
    assertEquals(firstPrimitive, arrayPrivimite[0], 0);
    assertEquals(3, arrayPrivimite.length);
    assertEquals(3, arrayObject.length);
    assertEquals(3, list.size());
    assertEquals(Arrays.asList(arrayObject), list);
    double total = 0;
    for (int i = 0; i < arrayPrivimite.length; ++i) {
      assertEquals(arrayPrivimite[i], arrayObject[i].doubleValue(), 0);
      total += arrayPrivimite[i];
    }

    assertSame(this.uriInfo, uriInfo);
    assertSame(this.firstSegment, firstSegment);
    assertSame(this.segmentArray, segmentArray);
    assertSame(this.segmentList, segmentList);

    assertEquals(1, uriInfo.getPathParameters().size());
    assertEquals(3, uriInfo.getPathParameters().get("param").size());
    assertPathSegments(uriInfo.getPathParameters().get("param"), arrayObject, firstSegment, segmentArray, segmentList);
    return total;
  }
}