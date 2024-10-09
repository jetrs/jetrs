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

package org.jetrs;

import static org.junit.Assert.*;

import java.util.List;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;

import org.junit.Test;

public class CookieParamPrimitiveTest extends SingleServiceTest {
  private static final String entity = "content";
  private static final String MATH_PI = "3.14159265358979323846";
  private static final String LONG_MAX_VALUE = "9223372036854775807";
  private static final String INT_MAX_VALUE = "2147483647";
  private static final String SHORT_MAX_VALUE = "32767";
  private static final String CHAR_STRING_VALUE = "z";
  private static final char CHAR_VALUE = 'z';
  // FIXME: For char values larger than 255 (I think), the client converts
  // FIXME: unicode to multi-byte chars, which then need to be recomposed.
  // private static final String CHAR_STRING_VALUE = "\uFFFF";
  // private static final char CHAR_VALUE = Character.MAX_VALUE;
  private static final String BYTE_MAX_VALUE = "127";
  private static final String BOOLEAN_MAX_VALUE = "true";

  public CookieParamPrimitiveTest() {
    startServer(
      ResourceHeaderPrimitive.class,
      ResourceHeaderPrimitiveDefault.class,
      ResourceHeaderPrimitiveDefaultNull.class,
      ResourceHeaderPrimitiveDefaultOverride.class,
      ResourceHeaderPrimitiveBoxed.class,
      ResourceHeaderPrimitiveBoxedDefault.class,
      ResourceHeaderPrimitiveBoxedDefaultNull.class,
      ResourceHeaderPrimitiveBoxedDefaultOverride.class,
      ResourceHeaderPrimitiveList.class,
      ResourceHeaderPrimitiveListDefault.class,
      ResourceHeaderPrimitiveListDefaultNull.class,
      ResourceHeaderPrimitiveListDefaultOverride.class);
  }

  void assertEach(final String type, final String value) {
    assertCookies("/", HttpMethod.GET, entity, "application/" + type, new Cookie(type, value));
    assertCookies("/boxed", HttpMethod.GET, entity, "application/" + type, new Cookie(type, value));
    assertCookies("/list", HttpMethod.GET, entity, "application/" + type, new Cookie(type, value));
  }

  void assertDefault(final String base, final String type, final String value) {
    assertEquals(entity, target(base + "default/null")
      .accept("application/" + type)
      .get()
      .readEntity(String.class));

    assertEquals(entity, target(base + "default")
      .accept("application/" + type)
      .get()
      .readEntity(String.class));

    assertEquals(entity, target(base + "default/override")
      .accept("application/" + type)
      .cookie(new Cookie(type, value))
      .get()
      .readEntity(String.class));
  }

  void assertDefault(final String type, final String value) {
    assertDefault("/", type, value);
  }

  void assertBoxedDefault(final String type, final String value) {
    assertDefault("/boxed/", type, value);
  }

  void assertListDefault(final String type, final String value) {
    assertDefault("/list/", type, value);
  }

  @Path("/")
  public static class ResourceHeaderPrimitive {
    @GET
    @Produces("application/boolean")
    public String get(@CookieParam("boolean") final boolean v) {
      assertEquals(true, v);
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String get(@CookieParam("byte") final byte v) {
      assertEquals(Byte.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") final char v) {
      assertEquals(CHAR_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/short")
    public String get(@CookieParam("short") final short v) {
      assertEquals(Short.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/int")
    public String get(@CookieParam("int") final int v) {
      assertEquals(Integer.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/long")
    public String get(@CookieParam("long") final long v) {
      assertEquals(Long.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/float")
    public String get(@CookieParam("float") final float v) {
      assertEquals(3.14159265f, v, 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String get(@CookieParam("double") final double v) {
      assertEquals(Math.PI, v, 0);
      return entity;
    }
  }

  @Path("/default/null")
  public static class ResourceHeaderPrimitiveDefaultNull {
    @GET
    @Produces("application/boolean")
    public String get(@CookieParam("boolean") final boolean v) {
      assertEquals(false, v);
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String get(@CookieParam("byte") final byte v) {
      assertEquals(0, v);
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") final char v) {
      assertEquals('\0', v);
      return entity;
    }

    @GET
    @Produces("application/short")
    public String get(@CookieParam("short") final short v) {
      assertEquals(0, v);
      return entity;
    }

    @GET
    @Produces("application/int")
    public String get(@CookieParam("int") final int v) {
      assertEquals(0, v);
      return entity;
    }

    @GET
    @Produces("application/long")
    public String get(@CookieParam("long") final long v) {
      assertEquals(0L, v);
      return entity;
    }

    @GET
    @Produces("application/float")
    public String get(@CookieParam("float") final float v) {
      assertEquals(0.0f, v, 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String get(@CookieParam("double") final double v) {
      assertEquals(0.0d, v, 0);
      return entity;
    }
  }

  @Path("/default")
  public static class ResourceHeaderPrimitiveDefault {
    @GET
    @Produces("application/boolean")
    public String get(@CookieParam("boolean") @DefaultValue(BOOLEAN_MAX_VALUE) final boolean v) {
      assertEquals(true, v);
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String get(@CookieParam("byte") @DefaultValue(BYTE_MAX_VALUE) final byte v) {
      assertEquals(Byte.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") @DefaultValue(CHAR_STRING_VALUE) final char v) {
      assertEquals(CHAR_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/short")
    public String get(@CookieParam("short") @DefaultValue(SHORT_MAX_VALUE) final short v) {
      assertEquals(Short.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/int")
    public String get(@CookieParam("int") @DefaultValue(INT_MAX_VALUE) final int v) {
      assertEquals(Integer.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/long")
    public String get(@CookieParam("long") @DefaultValue(LONG_MAX_VALUE) final long v) {
      assertEquals(Long.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/float")
    public String get(@CookieParam("float") @DefaultValue(MATH_PI) final float v) {
      assertEquals(3.14159265f, v, 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String get(@CookieParam("double") @DefaultValue(MATH_PI) final double v) {
      assertEquals(Math.PI, v, 0);
      return entity;
    }
  }

  @Path("/default/override")
  public static class ResourceHeaderPrimitiveDefaultOverride {
    @GET
    @Produces("application/boolean")
    public String get(@CookieParam("boolean") @DefaultValue("false") final boolean v) {
      assertEquals(true, v);
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String get(@CookieParam("byte") @DefaultValue("1") final byte v) {
      assertEquals(Byte.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") @DefaultValue("a") final char v) {
      assertEquals(CHAR_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/short")
    public String get(@CookieParam("short") @DefaultValue("1") final short v) {
      assertEquals(Short.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/int")
    public String get(@CookieParam("int") @DefaultValue("1") final int v) {
      assertEquals(Integer.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/long")
    public String get(@CookieParam("long") @DefaultValue("1") final long v) {
      assertEquals(Long.MAX_VALUE, v);
      return entity;
    }

    @GET
    @Produces("application/float")
    public String get(@CookieParam("float") @DefaultValue("0.0") final float v) {
      assertEquals(3.14159265f, v, 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String get(@CookieParam("double") @DefaultValue("0.0") final double v) {
      assertEquals(Math.PI, v, 0);
      return entity;
    }
  }

  @Path("/boxed")
  public static class ResourceHeaderPrimitiveBoxed {
    @GET
    @Produces("application/boolean")
    public String get(@CookieParam("boolean") final Boolean v) {
      assertEquals(true, v);
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String get(@CookieParam("byte") final Byte v) {
      assertEquals(Byte.MAX_VALUE, v.byteValue());
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") final Character v) {
      assertEquals(CHAR_VALUE, v.charValue());
      return entity;
    }

    @GET
    @Produces("application/short")
    public String get(@CookieParam("short") final Short v) {
      assertEquals(Short.MAX_VALUE, v.shortValue());
      return entity;
    }

    @GET
    @Produces("application/int")
    public String get(@CookieParam("int") final Integer v) {
      assertEquals(Integer.MAX_VALUE, v.intValue());
      return entity;
    }

    @GET
    @Produces("application/long")
    public String get(@CookieParam("long") final Long v) {
      assertEquals(Long.MAX_VALUE, v.longValue());
      return entity;
    }

    @GET
    @Produces("application/float")
    public String get(@CookieParam("float") final Float v) {
      assertEquals(3.14159265f, v, 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String get(@CookieParam("double") final Double v) {
      assertEquals(Math.PI, v, 0);
      return entity;
    }
  }

  @Path("/boxed/default/null")
  public static class ResourceHeaderPrimitiveBoxedDefaultNull {
    @GET
    @Produces("application/boolean")
    public String get(@CookieParam("boolean") final Boolean v) {
      assertEquals(null, v);
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String get(@CookieParam("byte") final Byte v) {
      assertEquals(null, v);
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") final Character v) {
      assertEquals(null, v);
      return entity;
    }

    @GET
    @Produces("application/short")
    public String get(@CookieParam("short") final Short v) {
      assertEquals(null, v);
      return entity;
    }

    @GET
    @Produces("application/int")
    public String get(@CookieParam("int") final Integer v) {
      assertEquals(null, v);
      return entity;
    }

    @GET
    @Produces("application/long")
    public String get(@CookieParam("long") final Long v) {
      assertEquals(null, v);
      return entity;
    }

    @GET
    @Produces("application/float")
    public String get(@CookieParam("float") final Float v) {
      assertEquals(null, v);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String get(@CookieParam("double") final Double v) {
      assertEquals(null, v);
      return entity;
    }
  }

  @Path("/boxed/default")
  public static class ResourceHeaderPrimitiveBoxedDefault {
    @GET
    @Produces("application/boolean")
    public String get(@CookieParam("boolean") @DefaultValue(BOOLEAN_MAX_VALUE) final Boolean v) {
      assertEquals(true, v);
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String get(@CookieParam("byte") @DefaultValue(BYTE_MAX_VALUE) final Byte v) {
      assertEquals(Byte.MAX_VALUE, v.byteValue());
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") @DefaultValue(CHAR_STRING_VALUE) final Character v) {
      assertEquals(CHAR_VALUE, v.charValue());
      return entity;
    }

    @GET
    @Produces("application/short")
    public String get(@CookieParam("short") @DefaultValue(SHORT_MAX_VALUE) final Short v) {
      assertEquals(Short.MAX_VALUE, v.shortValue());
      return entity;
    }

    @GET
    @Produces("application/int")
    public String get(@CookieParam("int") @DefaultValue(INT_MAX_VALUE) final Integer v) {
      assertEquals(Integer.MAX_VALUE, v.intValue());
      return entity;
    }

    @GET
    @Produces("application/long")
    public String get(@CookieParam("long") @DefaultValue(LONG_MAX_VALUE) final Long v) {
      assertEquals(Long.MAX_VALUE, v.longValue());
      return entity;
    }

    @GET
    @Produces("application/float")
    public String get(@CookieParam("float") @DefaultValue(MATH_PI) final Float v) {
      assertEquals(3.14159265f, v, 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String get(@CookieParam("double") @DefaultValue(MATH_PI) final Double v) {
      assertEquals(Math.PI, v, 0);
      return entity;
    }
  }

  @Path("/boxed/default/override")
  public static class ResourceHeaderPrimitiveBoxedDefaultOverride {
    @GET
    @Produces("application/boolean")
    public String get(@CookieParam("boolean") @DefaultValue("false") final Boolean v) {
      assertEquals(true, v);
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String get(@CookieParam("byte") @DefaultValue("1") final Byte v) {
      assertEquals(Byte.MAX_VALUE, v.byteValue());
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") @DefaultValue("a") final Character v) {
      assertEquals(CHAR_VALUE, v.charValue());
      return entity;
    }

    @GET
    @Produces("application/short")
    public String get(@CookieParam("short") @DefaultValue("1") final Short v) {
      assertEquals(Short.MAX_VALUE, v.shortValue());
      return entity;
    }

    @GET
    @Produces("application/int")
    public String get(@CookieParam("int") @DefaultValue("1") final Integer v) {
      assertEquals(Integer.MAX_VALUE, v.intValue());
      return entity;
    }

    @GET
    @Produces("application/long")
    public String get(@CookieParam("long") @DefaultValue("1") final Long v) {
      assertEquals(Long.MAX_VALUE, v.longValue());
      return entity;
    }

    @GET
    @Produces("application/float")
    public String get(@CookieParam("float") @DefaultValue("0.0") final Float v) {
      assertEquals(3.14159265f, v, 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String get(@CookieParam("double") @DefaultValue("0.0") final Double v) {
      assertEquals(Math.PI, v, 0);
      return entity;
    }
  }

  @Path("/list")
  public static class ResourceHeaderPrimitiveList {
    @GET
    @Produces("application/boolean")
    public String getBoolean(@CookieParam("boolean") final List<Boolean> v) {
      assertEquals(true, v.get(0));
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String getByte(@CookieParam("byte") final List<Byte> v) {
      assertEquals(Byte.MAX_VALUE, v.get(0).byteValue());
      return entity;
    }

    @GET
    @Produces("application/char")
    public String get(@CookieParam("char") final List<Character> v) {
      assertEquals(CHAR_VALUE, v.get(0).charValue());
      return entity;
    }

    @GET
    @Produces("application/short")
    public String getShort(@CookieParam("short") final List<Short> v) {
      assertEquals(Short.MAX_VALUE, v.get(0).shortValue());
      return entity;
    }

    @GET
    @Produces("application/int")
    public String getInteger(@CookieParam("int") final List<Integer> v) {
      assertEquals(Integer.MAX_VALUE, v.get(0).intValue());
      return entity;
    }

    @GET
    @Produces("application/long")
    public String getLong(@CookieParam("long") final List<Long> v) {
      assertEquals(Long.MAX_VALUE, v.get(0).longValue());
      return entity;
    }

    @GET
    @Produces("application/float")
    public String getFloat(@CookieParam("float") final List<Float> v) {
      assertEquals(3.14159265f, v.get(0), 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String getDouble(@CookieParam("double") final List<Double> v) {
      assertEquals(Math.PI, v.get(0), 0);
      return entity;
    }
  }

  @Path("/list/default/null")
  public static class ResourceHeaderPrimitiveListDefaultNull {
    @GET
    @Produces("application/boolean")
    public String getBoolean(@CookieParam("boolean") final List<Boolean> v) {
      assertEquals(0, v.size());
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String getByte(@CookieParam("byte") final List<Byte> v) {
      assertEquals(0, v.size());
      return entity;
    }

    @GET
    @Produces("application/char")
    public String getChar(@CookieParam("char") final List<Character> v) {
      assertEquals(0, v.size());
      return entity;
    }

    @GET
    @Produces("application/short")
    public String getShort(@CookieParam("short") final List<Short> v) {
      assertEquals(0, v.size());
      return entity;
    }

    @GET
    @Produces("application/int")
    public String getInteger(@CookieParam("int") final List<Integer> v) {
      assertEquals(0, v.size());
      return entity;
    }

    @GET
    @Produces("application/long")
    public String getLong(@CookieParam("long") final List<Long> v) {
      assertEquals(0, v.size());
      return entity;
    }

    @GET
    @Produces("application/float")
    public String getFloat(@CookieParam("float") final List<Float> v) {
      assertEquals(0, v.size());
      return entity;
    }

    @GET
    @Produces("application/double")
    public String getDouble(@CookieParam("double") final List<Double> v) {
      assertEquals(0, v.size());
      return entity;
    }
  }

  @Path("/list/default")
  public static class ResourceHeaderPrimitiveListDefault {
    @GET
    @Produces("application/boolean")
    public String getBoolean(@CookieParam("boolean") @DefaultValue(BOOLEAN_MAX_VALUE) final List<Boolean> v) {
      assertEquals(true, v.get(0));
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String getByte(@CookieParam("byte") @DefaultValue(BYTE_MAX_VALUE) final List<Byte> v) {
      assertEquals(Byte.MAX_VALUE, v.get(0).byteValue());
      return entity;
    }

    @GET
    @Produces("application/char")
    public String getChar(@CookieParam("char") @DefaultValue(CHAR_STRING_VALUE) final List<Character> v) {
      assertEquals(CHAR_VALUE, v.get(0).charValue());
      return entity;
    }

    @GET
    @Produces("application/short")
    public String getShort(@CookieParam("short") @DefaultValue(SHORT_MAX_VALUE) final List<Short> v) {
      assertEquals(Short.MAX_VALUE, v.get(0).shortValue());
      return entity;
    }

    @GET
    @Produces("application/int")
    public String getInteger(@CookieParam("int") @DefaultValue(INT_MAX_VALUE) final List<Integer> v) {
      assertEquals(Integer.MAX_VALUE, v.get(0).intValue());
      return entity;
    }

    @GET
    @Produces("application/long")
    public String getLong(@CookieParam("long") @DefaultValue(LONG_MAX_VALUE) final List<Long> v) {
      assertEquals(Long.MAX_VALUE, v.get(0).longValue());
      return entity;
    }

    @GET
    @Produces("application/float")
    public String getFloat(@CookieParam("float") @DefaultValue(MATH_PI) final List<Float> v) {
      assertEquals(3.14159265f, v.get(0), 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String getDouble(@CookieParam("double") @DefaultValue(MATH_PI) final List<Double> v) {
      assertEquals(Math.PI, v.get(0), 0);
      return entity;
    }
  }

  @Path("/list/default/override")
  public static class ResourceHeaderPrimitiveListDefaultOverride {
    @GET
    @Produces("application/boolean")
    public String getBoolean(@CookieParam("boolean") @DefaultValue("false") final List<Boolean> v) {
      assertEquals(true, v.get(0));
      return entity;
    }

    @GET
    @Produces("application/byte")
    public String getByte(@CookieParam("byte") @DefaultValue("0") final List<Byte> v) {
      assertEquals(Byte.MAX_VALUE, v.get(0).byteValue());
      return entity;
    }

    @GET
    @Produces("application/char")
    public String getChar(@CookieParam("char") @DefaultValue("0") final List<Character> v) {
      assertEquals(CHAR_VALUE, v.get(0).charValue());
      return entity;
    }

    @GET
    @Produces("application/short")
    public String getShort(@CookieParam("short") @DefaultValue("0") final List<Short> v) {
      assertEquals(Short.MAX_VALUE, v.get(0).shortValue());
      return entity;
    }

    @GET
    @Produces("application/int")
    public String getInteger(@CookieParam("int") @DefaultValue("0") final List<Integer> v) {
      assertEquals(Integer.MAX_VALUE, v.get(0).intValue());
      return entity;
    }

    @GET
    @Produces("application/long")
    public String getLong(@CookieParam("long") @DefaultValue("0") final List<Long> v) {
      assertEquals(Long.MAX_VALUE, v.get(0).longValue());
      return entity;
    }

    @GET
    @Produces("application/float")
    public String getFloat(@CookieParam("float") @DefaultValue("0.0") final List<Float> v) {
      assertEquals(3.14159265f, v.get(0), 0);
      return entity;
    }

    @GET
    @Produces("application/double")
    public String getDouble(@CookieParam("double") @DefaultValue("0.0") final List<Double> v) {
      assertEquals(Math.PI, v.get(0), 0);
      return entity;
    }
  }

  @Test
  public void testGetBoolean() {
    assertEach("boolean", BOOLEAN_MAX_VALUE);
  }

  @Test
  public void testGetBooleanPrimitiveDefault() {
    assertDefault("boolean", BOOLEAN_MAX_VALUE);
  }

  @Test
  public void testGetBooleanPrimitiveWrapperDefault() {
    assertBoxedDefault("boolean", BOOLEAN_MAX_VALUE);
  }

  @Test
  public void testGetBooleanPrimitiveListDefault() {
    assertListDefault("boolean", BOOLEAN_MAX_VALUE);
  }

  @Test
  public void testGetByte() {
    assertEach("byte", BYTE_MAX_VALUE);
  }

  @Test
  public void testGetBytePrimitiveDefault() {
    assertDefault("byte", BYTE_MAX_VALUE);
  }

  @Test
  public void testGetBytePrimitiveBoxedDefault() {
    assertBoxedDefault("byte", BYTE_MAX_VALUE);
  }

  @Test
  public void testGetBytePrimitiveListDefault() {
    assertListDefault("byte", BYTE_MAX_VALUE);
  }

  @Test
  public void testGetChar() {
    assertEach("char", CHAR_STRING_VALUE);
  }

  @Test
  public void testGetCharPrimitiveDefault() {
    assertDefault("char", CHAR_STRING_VALUE);
  }

  @Test
  public void testGetCharPrimitiveBoxedDefault() {
    assertBoxedDefault("char", CHAR_STRING_VALUE);
  }

  @Test
  public void testGetCharPrimitiveListDefault() {
    assertListDefault("char", CHAR_STRING_VALUE);
  }

  @Test
  public void testGetShort() {
    assertEach("short", SHORT_MAX_VALUE);
  }

  @Test
  public void testGetShortPrimtivesDefault() {
    assertDefault("short", SHORT_MAX_VALUE);
  }

  @Test
  public void testGetShortPrimtiveBoxedDefault() {
    assertBoxedDefault("short", SHORT_MAX_VALUE);
  }

  @Test
  public void testGetShortPrimtiveListDefault() {
    assertListDefault("short", SHORT_MAX_VALUE);
  }

  @Test
  public void testGetInt() {
    assertEach("int", INT_MAX_VALUE);
  }

  @Test
  public void testGetIntPrimitiveDefault() {
    assertDefault("int", INT_MAX_VALUE);
  }

  @Test
  public void testGetIntPrimitiveBoxedDefault() {
    assertBoxedDefault("int", INT_MAX_VALUE);
  }

  @Test
  public void testGetIntPrimitiveListDefault() {
    assertListDefault("int", INT_MAX_VALUE);
  }

  @Test
  public void testGetLong() {
    assertEach("long", LONG_MAX_VALUE);
  }

  @Test
  public void testGetLongPrimitiveDefault() {
    assertDefault("long", LONG_MAX_VALUE);
  }

  @Test
  public void testGetLongPrimitiveBoxedDefault() {
    assertBoxedDefault("long", LONG_MAX_VALUE);
  }

  @Test
  public void testGetLongPrimitiveListDefault() {
    assertListDefault("long", LONG_MAX_VALUE);
  }

  @Test
  public void testGetFloat() {
    assertEach("float", MATH_PI);
  }

  @Test
  public void testGetFloatPrimitiveDefault() {
    assertDefault("float", MATH_PI);
  }

  @Test
  public void testGetFloatPrimitiveBoxedDefault() {
    assertBoxedDefault("float", MATH_PI);
  }

  @Test
  public void testGetFloatPrimitiveListDefault() {
    assertListDefault("float", MATH_PI);
  }

  @Test
  public void testGetDouble() {
    assertEach("double", MATH_PI);
  }

  @Test
  public void testGetDoublePrimitiveDefault() {
    assertDefault("double", MATH_PI);
  }

  @Test
  public void testGetDoublePrimitiveBoxedDefault() {
    assertBoxedDefault("double", MATH_PI);
  }

  @Test
  public void testGetDoublePrimitiveListDefault() {
    assertListDefault("double", MATH_PI);
  }
}