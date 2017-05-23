/* Copyright (c) 2017 lib4j
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

package org.libx4j.xrs.server.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.ws.rs.Encoded;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.safris.commons.util.Collections;

public final class ParameterUtil {
  private static final String[] forEnums = new String[] {"fromString", "valueOf"};
  private static final String[] forOther = new String[] {"valueOf", "fromString"};

  private static Method findToString(final Class<?> type) throws NoSuchMethodException {
    final String[] methodNames = type.isEnum() ? forEnums : forOther;
    for (final String methodName : methodNames) {
      final Method method = type.getMethod(methodName, String.class);
      if (method != null)
        return method;
    }

    return null;
  }

  private static <T>ParamConverter<T> lookupParamConverter(final List<ParamConverterProvider> paramConverterProviders, final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
    for (final ParamConverterProvider paramConverterProvider : paramConverterProviders) {
      final ParamConverter<T> paramConverter = paramConverterProvider.getConverter(rawType, genericType, annotations);
      if (paramConverter != null)
        return paramConverter;
    }

    return null;
  }

  // http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-eval-spec/jsr339-jaxrs-2.0-final-spec.pdf Section 3.2
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Object convertParameter(final Class<?> parameterType, final Type genericType, final Annotation[] annotations, final List<String> values, final List<ParamConverterProvider> paramConverterProviders) throws ReflectiveOperationException {
    if (values == null || values.size() == 0)
      return null;

    final ParamConverter<?> paramConverter = lookupParamConverter(paramConverterProviders, parameterType, genericType, annotations);
    if (paramConverter != null)
      return paramConverter.fromString(Collections.toString(values, ';'));

    if (parameterType == String.class)
      return values.get(0);

    if (parameterType == Long.class || parameterType == long.class)
      return Long.parseLong(values.get(0));

    if (parameterType == Double.class || parameterType == double.class)
      return Double.parseDouble(values.get(0));

    if (parameterType == Float.class || parameterType == float.class)
      return Float.parseFloat(values.get(0));

    if (parameterType == Integer.class || parameterType == int.class)
      return Integer.parseInt(values.get(0));

    if (parameterType == Short.class || parameterType == short.class)
      return Short.parseShort(values.get(0));

    // FIXME: What if it's out of range of char?
    if (parameterType == Character.class || parameterType == char.class)
      return Character.valueOf((char)Integer.parseInt(values.get(0)));

    if (parameterType == Byte.class || parameterType == byte.class)
      return Byte.valueOf(values.get(0));

    if (parameterType == Set.class || parameterType == List.class || parameterType == SortedSet.class) {
      final Collection collection = Collections.asCollection((Class<? extends Collection>)parameterType, values);
      final Class<?> type = (Class<?>)((ParameterizedType)parameterType.getGenericInterfaces()[0]).getActualTypeArguments()[0];
      if (type == String.class) {
        for (final String value : values) {
          collection.add(value);
        }
      }
      else {
        final Method method = findToString(type);
        for (final String value : values) {
          collection.add(method.invoke(null, value));
        }
      }

      return collection;
    }

    final Method method = findToString(parameterType);
    return method.invoke(null, Collections.toString(values, ';'));
  }

  public static boolean decode(final Annotation[] annotations) {
    for (final Annotation annotation : annotations)
      if (annotation instanceof Encoded)
        return false;

    return true;
  }

  private ParameterUtil() {
  }
}