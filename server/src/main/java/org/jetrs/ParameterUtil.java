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

package org.jetrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.Encoded;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.libj.lang.Classes;
import org.libj.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ParameterUtil {
  private static final Logger logger = LoggerFactory.getLogger(ParameterUtil.class);
  private static final String[] forEnums = {"fromString", "valueOf"};
  private static final String[] forOther = {"valueOf", "fromString"};
  @SuppressWarnings("rawtypes")
  private static final Map<Class<? extends Collection>,Class<? extends Collection>> defaultCollectionClasses = new HashMap<>();

  static {
    defaultCollectionClasses.put(List.class, ArrayList.class);
    defaultCollectionClasses.put(Set.class, HashSet.class);
    defaultCollectionClasses.put(SortedSet.class, TreeSet.class);
  }

  private static Method findParseMethod(final Class<?> type) {
    final String[] methodNames = type.isEnum() ? forEnums : forOther;
    for (final String methodName : methodNames) { // [A]
      final Method method = Classes.getMethod(type, methodName, String.class);
      if (method != null)
        return method;
    }

    return null;
  }

  private static <T>ParamConverter<T> lookupParamConverter(final List<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories, final RequestContext requestContext, final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
    for (int i = 0, len = paramConverterProviderFactories.size(); i < len; ++i) { // [L]
      final ProviderFactory<ParamConverterProvider> factory = paramConverterProviderFactories.get(i);
      // FIXME: Is there a way to detect whether the ParamConverterProvider can convert the parameter without instantiating the ParamConverterProvider?
      final ParamConverter<T> paramConverter = factory.getSingletonOrFromRequestContext(requestContext).getConverter(rawType, genericType, annotations);
      if (paramConverter != null)
        return paramConverter;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  static Collection<?> newCollection(final Class<?> parameterType) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
    final Class<?> cls = parameterType.isInterface() ? defaultCollectionClasses.get(parameterType) : parameterType;
    return ((Class<? extends Collection<?>>)cls).getDeclaredConstructor().newInstance();
  }

  // http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-eval-spec/jsr339-jaxrs-2.0-final-spec.pdf Section 3.2
  @SuppressWarnings("unchecked")
  static Object convertParameter(final Class<?> parameterType, final Type genericType, final Annotation[] annotations, final List<String> values, final List<ProviderFactory<ParamConverterProvider>> paramConverterProviderFactories, final RequestContext requestContext) {
    if (values == null || values.size() == 0)
      return null;

    try {
      final Object container;
      final Class<?> memberClass;
      if (Set.class.isAssignableFrom(parameterType) || List.class.isAssignableFrom(parameterType) || SortedSet.class.isAssignableFrom(parameterType)) {
        memberClass = (Class<?>)((ParameterizedType)genericType).getActualTypeArguments()[0];
        container = newCollection(parameterType);
      }
      else if (parameterType.isArray()) {
        memberClass = parameterType.getComponentType();
        container = Array.newInstance(memberClass, values.size());
      }
      else {
        memberClass = parameterType;
        container = null;
      }

      final ParamConverter<?> paramConverter = lookupParamConverter(paramConverterProviderFactories, requestContext, memberClass, genericType, annotations);
      if (paramConverter != null)
        return paramConverter.fromString(CollectionUtil.toString(values, ';')); // FIXME: Is `;` the delimiter?

      if (memberClass == String.class) {
        if (container == null)
          return values.get(0);

        if (container.getClass().isArray())
          return values.toArray((Object[])container);

        ((Collection<Object>)container).addAll(values);
        return container;
      }

      if (memberClass == BigInteger.class) {
        if (container == null)
          return new BigInteger(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(new BigInteger(values.get(i)));

          return c;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = new BigInteger(values.get(i));

        return a;
      }

      if (memberClass == BigDecimal.class) {
        if (container == null)
          return new BigDecimal(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(new BigDecimal(values.get(i)));

          return c;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = new BigDecimal(values.get(i));

        return a;
      }

      if (memberClass == Long.class || memberClass == long.class) {
        if (container == null)
          return Long.valueOf(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(Long.valueOf(values.get(i)));

          return c;
        }

        if (container instanceof long[]) {
          final long[] a = (long[])container;
          for (int i = 0; i < a.length; ++i) // [A]
            a[i] = Long.parseLong(values.get(i));

          return a;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = Long.valueOf(values.get(i));

        return a;
      }

      if (memberClass == Integer.class || memberClass == int.class) {
        if (container == null)
          return Integer.valueOf(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(Integer.valueOf(values.get(i)));

          return c;
        }

        if (container instanceof int[]) {
          final int[] a = (int[])container;
          for (int i = 0; i < a.length; ++i) // [A]
            a[i] = Integer.parseInt(values.get(i));

          return a;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = Integer.valueOf(values.get(i));

        return a;
      }

      if (memberClass == Double.class || memberClass == double.class) {
        if (container == null)
          return Double.valueOf(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(Double.valueOf(values.get(i)));

          return c;
        }

        if (container instanceof double[]) {
          final double[] a = (double[])container;
          for (int i = 0; i < a.length; ++i) // [A]
            a[i] = Double.parseDouble(values.get(i));

          return a;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = Double.valueOf(values.get(i));

        return a;
      }

      if (memberClass == Float.class || memberClass == float.class) {
        if (container == null)
          return Float.valueOf(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(Float.valueOf(values.get(i)));

          return c;
        }

        if (container instanceof float[]) {
          final float[] a = (float[])container;
          for (int i = 0; i < a.length; ++i) // [A]
            a[i] = Float.parseFloat(values.get(i));

          return a;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = Float.valueOf(values.get(i));

        return a;
      }

      if (memberClass == Short.class || memberClass == short.class) {
        if (container == null)
          return Short.valueOf(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(Short.valueOf(values.get(i)));

          return c;
        }

        if (container instanceof short[]) {
          final short[] a = (short[])container;
          for (int i = 0; i < a.length; ++i) // [A]
            a[i] = Short.parseShort(values.get(i));

          return a;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = Short.valueOf(values.get(i));

        return a;
      }

      if (memberClass == Character.class || memberClass == char.class) {
        if (container == null)
          return parseChar(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(parseChar(values.get(i)));

          return c;
        }

        if (container instanceof char[]) {
          final char[] a = (char[])container;
          for (int i = 0; i < a.length; ++i) // [A]
            a[i] = parseChar(values.get(i));

          return a;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = parseChar(values.get(i));

        return a;
      }

      if (memberClass == Byte.class || memberClass == byte.class) {
        if (container == null)
          return Byte.valueOf(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(Byte.valueOf(values.get(i)));

          return c;
        }

        if (container instanceof byte[]) {
          final byte[] a = (byte[])container;
          for (int i = 0; i < a.length; ++i) // [A]
            a[i] = Byte.parseByte(values.get(i));

          return a;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = Byte.valueOf(values.get(i));

        return a;
      }

      if (memberClass == Boolean.class || memberClass == boolean.class) {
        if (container == null)
          return Boolean.valueOf(values.get(0));

        if (container instanceof Collection) {
          final Collection<Object> c = (Collection<Object>)container;
          for (int i = 0, len = values.size(); i < len; ++i) // [L]
            c.add(Boolean.valueOf(values.get(i)));

          return c;
        }

        if (container instanceof boolean[]) {
          final boolean[] a = (boolean[])container;
          for (int i = 0; i < a.length; ++i) // [A]
            a[i] = Boolean.parseBoolean(values.get(i));

          return a;
        }

        final Object[] a = (Object[])container;
        for (int i = 0; i < a.length; ++i) // [A]
          a[i] = Boolean.valueOf(values.get(i));

        return a;
      }

      final Method method = findParseMethod(memberClass);
      return method == null ? null : method.invoke(null, values.get(0));
    }
    catch (final IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
      // FIXME: This error is kind of hidden in the logs, but it should somehow be highlighted to be fixed?!
      logger.error(e.getMessage(), e);
      return e instanceof InvocationTargetException ? e.getCause() : e;
    }
  }

  private static Character parseChar(final String str) {
    final int value = str.charAt(0);
    if (value < Character.MIN_VALUE)
      throw new NumberFormatException(str + " is less than Character.MIN_VALUE");

    if (value > Character.MAX_VALUE)
      throw new NumberFormatException(str + " is greater than Character.MAX_VALUE");

    return Character.valueOf((char)value);
  }

  static boolean decode(final Annotation[] annotations) {
    for (final Annotation annotation : annotations)
      if (annotation instanceof Encoded)
        return false;

    return true;
  }

  private ParameterUtil() {
  }
}