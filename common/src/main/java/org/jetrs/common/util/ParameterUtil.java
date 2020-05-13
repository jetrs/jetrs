/* Copyright (c) 2017 JetRS
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

package org.jetrs.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.ws.rs.Encoded;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.jetrs.common.ProviderResource;
import org.libj.lang.Classes;
import org.libj.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParameterUtil {
  private static final Logger logger = LoggerFactory.getLogger(ParameterUtil.class);
  private static final String[] forEnums = {"fromString", "valueOf"};
  private static final String[] forOther = {"valueOf", "fromString"};

  private static Method findToString(final Class<?> type) {
    final String[] methodNames = type.isEnum() ? forEnums : forOther;
    for (final String methodName : methodNames) {
      final Method method = Classes.getMethod(type, methodName, String.class);
      if (method != null)
        return method;
    }

    return null;
  }

  private static <T>ParamConverter<T> lookupParamConverter(final List<? extends ProviderResource<ParamConverterProvider>> paramConverterProviders, final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
    for (final ProviderResource<ParamConverterProvider> paramConverterProvider : paramConverterProviders) {
      final ParamConverter<T> paramConverter = paramConverterProvider.getMatchInstance().getConverter(rawType, genericType, annotations);
      if (paramConverter != null)
        return paramConverter;
    }

    return null;
  }

  // http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-eval-spec/jsr339-jaxrs-2.0-final-spec.pdf Section 3.2
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Object convertParameter(final Class<?> parameterType, final Type genericType, final Annotation[] annotations, final List<String> values, final List<? extends ProviderResource<ParamConverterProvider>> paramConverterProviders) {
    if (values == null || values.size() == 0)
      return null;

    final ParamConverter<?> paramConverter = lookupParamConverter(paramConverterProviders, parameterType, genericType, annotations);
    if (paramConverter != null)
      return paramConverter.fromString(CollectionUtil.toString(values, ';'));

    if (parameterType == String.class)
      return values.get(0);

    if (parameterType == BigInteger.class)
      return new BigInteger(values.get(0));

    if (parameterType == BigDecimal.class)
      return new BigDecimal(values.get(0));

    if (parameterType == Long.class || parameterType == long.class)
      return Long.valueOf(values.get(0));

    if (parameterType == Double.class || parameterType == double.class)
      return Double.valueOf(values.get(0));

    if (parameterType == Float.class || parameterType == float.class)
      return Float.valueOf(values.get(0));

    if (parameterType == Integer.class || parameterType == int.class)
      return Integer.valueOf(values.get(0));

    if (parameterType == Short.class || parameterType == short.class)
      return Short.valueOf(values.get(0));

    // FIXME: What if it's out of range of char?
    if (parameterType == Character.class || parameterType == char.class)
      return Character.valueOf((char)Integer.parseInt(values.get(0)));

    if (parameterType == Byte.class || parameterType == byte.class)
      return Byte.valueOf(values.get(0));

    try {
      if (parameterType == Set.class || parameterType == List.class || parameterType == SortedSet.class) {
        final Collection collection = CollectionUtil.concat(((Class<? extends Collection>)parameterType).getDeclaredConstructor().newInstance(), values);
        final Class<?> type = (Class<?>)((ParameterizedType)parameterType.getGenericInterfaces()[0]).getActualTypeArguments()[0];
        if (type == String.class) {
          collection.addAll(values);
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
      return method.invoke(null, CollectionUtil.toString(values, ';'));
    }
    catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
      // FIXME: This error is kinda hidden in the logs, but it should somehow be highlighted to be fixed?!
      logger.error(e.getMessage(), e);
      return e instanceof InvocationTargetException ? e.getCause() : e;
    }
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