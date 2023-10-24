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

package org.jetrs;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.ProcessingException;

import org.libj.lang.Strings;
import org.libj.util.ArrayUtil;

abstract class ParamPlurality<T> {
  private static final ConcurrentHashMap<Class<?>,Object> componentTypeToEmptyArray = new ConcurrentHashMap<>();

  static {
    componentTypeToEmptyArray.put(Object.class, ArrayUtil.EMPTY_ARRAY);
    componentTypeToEmptyArray.put(String.class, Strings.EMPTY_ARRAY);
    componentTypeToEmptyArray.put(boolean.class, ArrayUtil.EMPTY_ARRAY_BOOLEAN);
    componentTypeToEmptyArray.put(byte.class, ArrayUtil.EMPTY_ARRAY_BYTE);
    componentTypeToEmptyArray.put(char.class, ArrayUtil.EMPTY_ARRAY_CHAR);
    componentTypeToEmptyArray.put(short.class, ArrayUtil.EMPTY_ARRAY_SHORT);
    componentTypeToEmptyArray.put(int.class, ArrayUtil.EMPTY_ARRAY_INT);
    componentTypeToEmptyArray.put(long.class, ArrayUtil.EMPTY_ARRAY_LONG);
    componentTypeToEmptyArray.put(float.class, ArrayUtil.EMPTY_ARRAY_FLOAT);
    componentTypeToEmptyArray.put(double.class, ArrayUtil.EMPTY_ARRAY_DOUBLE);
  }

  static final ParamPlurality<Object[]> ARRAY;
  static final ParamPlurality<Collection<?>> COLLECTION;
  static final ParamPlurality<Void> SINGLE;

  private static final ParamPlurality<?>[] values = {
    ARRAY = new ParamPlurality<Object[]>() {
      @Override
      Object[] newContainer(final Class<?> rawType, final int size) {
        return (Object[])Array.newInstance(rawType.getComponentType(), size);
      }

      @Override
      Object[] newSingleton(final Class<?> rawType, final Object obj) {
        final Object[] a = newContainer(rawType, 1);
        a[0] = obj;
        return a;
      }

      @Override
      Class<?> getMemberClass(final Class<?> rawType, final Type genericType) {
        final Class<?> cls = rawType.getComponentType();
        return cls != null ? cls : ((Class<?>)genericType).getComponentType();
      }

      @Override
      Object getNullValue(Class<?> rawType) {
        rawType = rawType.getComponentType();
        Object a = componentTypeToEmptyArray.get(rawType);
        if (a == null)
          componentTypeToEmptyArray.put(rawType, a = Array.newInstance(rawType, 0));

        return a;
      }
    },
    COLLECTION = new ParamPlurality<Collection<?>>() {
      @Override
      @SuppressWarnings("unchecked")
      Collection<?> newContainer(final Class<?> rawType, final int size) {
        final Class<?> cls;
        if (rawType.isAssignableFrom(List.class))
          cls = ArrayList.class;
        else if (rawType.isAssignableFrom(Set.class))
          cls = HashSet.class;
        else if (rawType.isAssignableFrom(SortedSet.class))
          cls = TreeSet.class;
        else
          cls = rawType;

        try {
          return ((Class<? extends Collection<?>>)cls).getDeclaredConstructor().newInstance();
        }
        catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e) {
          throw new RuntimeException(e);
        }
        catch (final InvocationTargetException e) {
          final Throwable cause = e.getCause();
          if (cause instanceof RuntimeException)
            throw (RuntimeException)cause;

          throw new ProcessingException(cause);
        }
      }

      @Override
      Collection<?> newSingleton(final Class<?> rawType, final Object obj) {
        if (rawType.isAssignableFrom(List.class))
          return Collections.singletonList(obj);

        if (rawType.isAssignableFrom(Set.class))
          return Collections.singleton(obj);

        if (rawType.isAssignableFrom(SortedSet.class)) {
          final TreeSet<Object> set = new TreeSet<>();
          set.add(obj);
          return set;
        }

        return null;
      }

      @Override
      Class<?> getMemberClass(final Class<?> rawType, final Type genericType) {
        return (Class<?>)((ParameterizedType)genericType).getActualTypeArguments()[0];
      }

      @Override
      Object getNullValue(final Class<?> rawType) {
        if (rawType.isAssignableFrom(List.class))
          return Collections.emptyList();

        if (rawType.isAssignableFrom(Set.class))
          return Collections.emptySet();

        if (rawType.isAssignableFrom(SortedSet.class))
          return Collections.emptySortedSet();

        return null;
      }
    },
    SINGLE = new ParamPlurality<Void>() {
      @Override
      Void newContainer(final Class<?> rawType, final int size) {
        return null;
      }

      @Override
      Void newSingleton(final Class<?> rawType, final Object obj) {
        return null;
      }

      @Override
      Class<?> getMemberClass(final Class<?> rawType, final Type genericType) {
        return rawType;
      }

      @Override
      Object getNullValue(final Class<?> rawType) {
        if (rawType == int.class)
          return 0;

        if (rawType == long.class)
          return 0L;

        if (rawType == double.class)
          return 0d;

        if (rawType == boolean.class)
          return false;

        if (rawType == float.class)
          return 0f;

        if (rawType == char.class)
          return '\u0000';

        if (rawType == short.class)
          return (short)0;

        if (rawType == byte.class)
          return (byte)0;

        return null;
      }
    }
  };

  abstract T newContainer(Class<?> rawType, int size);
  abstract T newSingleton(Class<?> rawType, Object obj);
  abstract Class<?> getMemberClass(Class<?> rawType, Type genericType);
  abstract Object getNullValue(Class<?> rawType);

  ParamPlurality<?>[] values() {
    return values;
  }

  static ParamPlurality<?> fromClass(final Class<?> clazz) {
    if (List.class.isAssignableFrom(clazz) || Set.class.isAssignableFrom(clazz) || SortedSet.class.isAssignableFrom(clazz))
      return COLLECTION;

    if (clazz.isArray())
      return ARRAY;

    return SINGLE;
  }
}