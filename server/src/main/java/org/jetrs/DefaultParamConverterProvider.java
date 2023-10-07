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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverter.Lazy;
import javax.ws.rs.ext.ParamConverterProvider;

import org.libj.lang.Classes;
import org.libj.lang.Numbers;
import org.libj.util.ConcurrentNullHashMap;

class DefaultParamConverterProvider implements ParamConverterProvider {
  private static final String[] forEnums = {"fromString", "valueOf"};
  private static final String[] forOther = {"valueOf", "fromString"};
  private static final DefaultParamConverterProvider instance = new DefaultParamConverterProvider();

  private static Method findParseMethod(final Class<?> type) {
    final String[] methodNames = type.isEnum() ? forEnums : forOther;
    for (final String methodName : methodNames) { // [A]
      final Method method = Classes.getMethod(type, methodName, String.class);
      if (method != null)
        return method;
    }

    return null;
  }

  private static Class<?> getGenericClassArgument(final Type genericType) {
    if (genericType instanceof ParameterizedType) {
      final Type[] typeArguments = ((ParameterizedType)genericType).getActualTypeArguments();
      return typeArguments.length > 0 && typeArguments[0] instanceof Class<?> ? (Class<?>)typeArguments[0] : String.class;
    }

    return String.class;
  }

  private static Boolean parseBoolean(final String str, final Boolean defaultValue) {
    return str == null || "true".equalsIgnoreCase(str) ? Boolean.TRUE : "false".equalsIgnoreCase(str) ? Boolean.FALSE : defaultValue;
  }

  private static char parseChar(final String str) {
    final int value;
    return str.length() == 0 || (value = str.charAt(0)) < Character.MIN_VALUE || value > Character.MAX_VALUE ? '\u0000' : (char)value;
  }

  private static Character parseCharacter(final String str, final Character defaultValue) {
    final int value;
    return str.length() == 0 || (value = str.charAt(0)) < Character.MIN_VALUE || value > Character.MAX_VALUE ? defaultValue : Character.valueOf((char)value);
  }

  private static <T> ParamConverter<T> lookupParamConverter(final ArrayList<Component<ParamConverterProvider>> paramConverterProviderFactories, final RequestContext<?> requestContext, final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
    for (int i = 0, i$ = paramConverterProviderFactories.size(); i < i$; ++i) { // [RA]
      final Component<ParamConverterProvider> factory = paramConverterProviderFactories.get(i);
      // FIXME: Is there a way to detect whether the ParamConverterProvider can convert the parameter without instantiating the
      // ParamConverterProvider?
      final ParamConverter<T> paramConverter = factory.getSingletonOrFromRequestContext(requestContext).getConverter(rawType, genericType, annotations);
      if (paramConverter != null)
        return paramConverter;
    }

    return instance.getConverter(rawType, genericType, annotations);
  }

  // http://download.oracle.com/otn-pub/jcp/jaxrs-2_0_rev_A-mrel-eval-spec/jsr339-jaxrs-2.0-final-spec.pdf Section 3.2
  @SuppressWarnings({"null", "rawtypes", "unchecked"})
  static Object convertParameter(final Class<?> rawType, Type genericType, final Annotation[] annotations, final ParamPlurality paramPlurality, String firstValue, final List<String> values, final boolean onlyIfEager, final ArrayList<Component<ParamConverterProvider>> paramConverterProviderFactories, final RequestContext<?> requestContext) {
    final int size;
    if (values == null)
      size = firstValue != null ? 1 : 0;
    else if ((size = values.size()) > 0 && firstValue == null)
      firstValue = values.get(0);

    if (size == 0)
      return paramPlurality.getNullValue(rawType);

    Class<?> componentType;
    if (paramPlurality == ParamPlurality.COLLECTION) {
      componentType = getGenericClassArgument(genericType);
      genericType = null;
    }
    else if (paramPlurality == ParamPlurality.ARRAY) {
      componentType = rawType.getComponentType();
      genericType = null;
    }
    else {
      componentType = rawType;
    }

    ParamConverter<?> paramConverter = lookupParamConverter(paramConverterProviderFactories, requestContext, componentType, genericType, annotations);
    if (onlyIfEager && paramConverter != null && AnnotationUtil.isAnnotationPresent(paramConverter.getClass(), Lazy.class))
      return null;

    if (paramPlurality == ParamPlurality.ARRAY) {
      if (componentType.isPrimitive()) {
        if (size == 0)
          return ParamPlurality.ARRAY.getNullValue(rawType);

        if (componentType == int.class) {
          final int[] a = new int[size];

          if (size == 1) {
            a[0] = paramConverter != null ? (Integer)paramConverter.fromString(firstValue) : Numbers.parseInt(firstValue, 0);
          }
          else if (paramConverter != null) {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = (Integer)paramConverter.fromString(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = (Integer)paramConverter.fromString(it.next());
              while (it.hasNext());
            }
          }
          else {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = Numbers.parseInt(values.get(i), 0);
              while (++i < size);
            }
            else {
              final Iterator<String> iterator = values.iterator();
              do // [I]
                a[i++] = Numbers.parseInt(iterator.next(), 0);
              while (iterator.hasNext());
            }
          }

          return a;
        }

        if (componentType == long.class) {
          final long[] a = new long[size];

          if (size == 1) {
            a[0] = paramConverter != null ? (Long)paramConverter.fromString(firstValue) : Numbers.parseLong(firstValue, 0L);
          }
          else if (paramConverter != null) {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = (Long)paramConverter.fromString(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = (Long)paramConverter.fromString(it.next());
              while (it.hasNext());
            }
          }
          else {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = Numbers.parseLong(values.get(i), 0L);
              while (++i < size);
            }
            else {
              final Iterator<String> iterator = values.iterator();
              do // [I]
                a[i++] = Numbers.parseLong(iterator.next(), 0L);
              while (iterator.hasNext());
            }
          }

          return a;
        }

        if (componentType == double.class) {
          final double[] a = new double[size];

          if (size == 1) {
            a[0] = paramConverter != null ? (Double)paramConverter.fromString(firstValue) : Numbers.parseDouble(firstValue, 0d);
          }
          else if (paramConverter != null) {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = (Double)paramConverter.fromString(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = (Double)paramConverter.fromString(it.next());
              while (it.hasNext());
            }
          }
          else {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = Numbers.parseDouble(values.get(i), 0d);
              while (++i < size);
            }
            else {
              final Iterator<String> iterator = values.iterator();
              do // [I]
                a[i++] = Numbers.parseDouble(iterator.next(), 0d);
              while (iterator.hasNext());
            }
          }

          return a;
        }

        if (componentType == float.class) {
          final float[] a = new float[size];

          if (size == 1) {
            a[0] = paramConverter != null ? (Float)paramConverter.fromString(firstValue) : Numbers.parseFloat(firstValue, 0f);
          }
          else if (paramConverter != null) {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = (Float)paramConverter.fromString(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = (Float)paramConverter.fromString(it.next());
              while (it.hasNext());
            }
          }
          else {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = Numbers.parseFloat(values.get(i), 0f);
              while (++i < size);
            }
            else {
              final Iterator<String> iterator = values.iterator();
              do // [I]
                a[i++] = Numbers.parseFloat(iterator.next(), 0f);
              while (iterator.hasNext());
            }
          }

          return a;
        }

        if (componentType == boolean.class) {
          final boolean[] a = new boolean[size];

          if (size == 1) {
            a[0] = firstValue == null || paramConverter != null && (Boolean)paramConverter.fromString(firstValue) || Boolean.parseBoolean(firstValue);
          }
          else if (paramConverter != null) {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = (Boolean)paramConverter.fromString(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = (Boolean)paramConverter.fromString(it.next());
              while (it.hasNext());
            }
          }
          else {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = Boolean.parseBoolean(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> iterator = values.iterator();
              do // [I]
                a[i++] = Boolean.parseBoolean(iterator.next());
              while (iterator.hasNext());
            }
          }

          return a;
        }

        if (componentType == byte.class) {
          final byte[] a = new byte[size];

          if (size == 1) {
            a[0] = paramConverter != null ? (Byte)paramConverter.fromString(firstValue) : Numbers.parseByte(firstValue, (byte)0);
          }
          else if (paramConverter != null) {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = (Byte)paramConverter.fromString(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = (Byte)paramConverter.fromString(it.next());
              while (it.hasNext());
            }
          }
          else {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = Numbers.parseByte(values.get(i), (byte)0);
              while (++i < size);
            }
            else {
              final Iterator<String> iterator = values.iterator();
              do // [I]
                a[i++] = Numbers.parseByte(iterator.next(), (byte)0);
              while (iterator.hasNext());
            }
          }

          return a;
        }

        if (componentType == char.class) {
          final char[] a = new char[size];

          if (size == 1) {
            a[0] = paramConverter != null ? (Character)paramConverter.fromString(firstValue) : parseChar(firstValue);
          }
          else if (paramConverter != null) {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = (Character)paramConverter.fromString(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = (Character)paramConverter.fromString(it.next());
              while (it.hasNext());
            }
          }
          else {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = parseChar(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> iterator = values.iterator();
              do // [I]
                a[i++] = parseChar(iterator.next());
              while (iterator.hasNext());
            }
          }

          return a;
        }

        if (componentType == short.class) {
          final short[] a = new short[size];

          if (size == 1) {
            a[0] = paramConverter != null ? (Short)paramConverter.fromString(firstValue) : Numbers.parseShort(firstValue, (short)0);
          }
          else if (paramConverter != null) {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = (Short)paramConverter.fromString(values.get(i));
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = (Short)paramConverter.fromString(it.next());
              while (it.hasNext());
            }
          }
          else {
            int i = 0;
            if (values instanceof RandomAccess) {
              do // [RA]
                a[i] = Numbers.parseShort(values.get(i), (short)0);
              while (++i < size);
            }
            else {
              final Iterator<String> it = values.iterator();
              do // [I]
                a[i++] = Numbers.parseShort(it.next(), (short)0);
              while (it.hasNext());
            }
          }

          return a;
        }
      }
    }

    if (paramConverter == null && componentType.isPrimitive()) {
      componentType = Classes.box(componentType);
      paramConverter = lookupParamConverter(paramConverterProviderFactories, requestContext, componentType, genericType, annotations);
      if (onlyIfEager && paramConverter != null && AnnotationUtil.isAnnotationPresent(paramConverter.getClass(), Lazy.class))
        return null;
    }

    if (paramConverter != null) {
      if (paramPlurality == ParamPlurality.SINGLE)
        return paramConverter.fromString(firstValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, paramConverter.fromString(firstValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(paramConverter.fromString(values.get(i)));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(paramConverter.fromString(it.next()));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = paramConverter.fromString(values.get(i));
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = paramConverter.fromString(it.next());
        while (it.hasNext());
      }

      return a;
    }

    if (componentType == Integer.class) {
      final Integer defaultValue = rawType.isPrimitive() ? 0 : null;
      if (paramPlurality == ParamPlurality.SINGLE)
        return Numbers.parseInteger(firstValue, defaultValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, Numbers.parseInteger(firstValue, defaultValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(Numbers.parseInteger(values.get(i), defaultValue));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(Numbers.parseInteger(it.next(), defaultValue));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = Numbers.parseInteger(values.get(i), defaultValue);
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = Numbers.parseInteger(it.next(), defaultValue);
        while (it.hasNext());
      }

      return a;
    }

    if (componentType == Long.class) {
      final Long defaultValue = rawType.isPrimitive() ? 0L : null;
      if (paramPlurality == ParamPlurality.SINGLE)
        return Numbers.parseLong(firstValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, Numbers.parseLong(firstValue, defaultValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(Numbers.parseLong(values.get(i), defaultValue));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(Numbers.parseLong(it.next(), defaultValue));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = Numbers.parseLong(values.get(i), defaultValue);
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = Numbers.parseLong(it.next(), defaultValue);
        while (it.hasNext());
      }

      return a;
    }

    if (componentType == Double.class) {
      final Double defaultValue = rawType.isPrimitive() ? 0d : null;
      if (paramPlurality == ParamPlurality.SINGLE)
        return Numbers.parseDouble(firstValue, defaultValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, Numbers.parseDouble(firstValue, defaultValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(Numbers.parseDouble(values.get(i), defaultValue));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(Numbers.parseDouble(it.next(), defaultValue));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = Numbers.parseDouble(values.get(i), defaultValue);
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = Numbers.parseDouble(it.next(), defaultValue);
        while (it.hasNext());
      }

      return a;
    }

    if (componentType == Float.class) {
      final Float defaultValue = rawType.isPrimitive() ? 0f : null;
      if (paramPlurality == ParamPlurality.SINGLE)
        return Numbers.parseFloat(firstValue, defaultValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, Numbers.parseFloat(firstValue, defaultValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(Numbers.parseFloat(values.get(i), defaultValue));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(Numbers.parseFloat(it.next(), defaultValue));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = Numbers.parseFloat(values.get(i), defaultValue);
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = Numbers.parseFloat(it.next(), defaultValue);
        while (it.hasNext());
      }

      return a;
    }

    if (componentType == Boolean.class) {
      final Boolean defaultValue = rawType.isPrimitive() ? Boolean.FALSE : null;
      if (paramPlurality == ParamPlurality.SINGLE)
        return parseBoolean(firstValue, defaultValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, parseBoolean(firstValue, defaultValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(parseBoolean(values.get(i), defaultValue));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(parseBoolean(it.next(), defaultValue));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = parseBoolean(values.get(i), defaultValue);
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = parseBoolean(it.next(), defaultValue);
        while (it.hasNext());
      }

      return a;
    }

    if (componentType == Byte.class) {
      final Byte defaultValue = rawType.isPrimitive() ? (byte)0 : null;
      if (paramPlurality == ParamPlurality.SINGLE)
        return Numbers.parseByte(firstValue, defaultValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, Numbers.parseByte(firstValue, defaultValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(Numbers.parseByte(values.get(i), defaultValue));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(Numbers.parseByte(it.next(), defaultValue));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = Numbers.parseByte(values.get(i), defaultValue);
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = Numbers.parseByte(it.next(), defaultValue);
        while (it.hasNext());
      }

      return a;
    }

    if (componentType == Character.class) {
      final Character defaultValue = rawType.isPrimitive() ? '\u0000' : null;
      if (paramPlurality == ParamPlurality.SINGLE)
        return parseCharacter(firstValue, defaultValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, parseCharacter(firstValue, defaultValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(parseCharacter(values.get(i), defaultValue));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(parseCharacter(it.next(), defaultValue));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = parseCharacter(values.get(i), defaultValue);
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = parseCharacter(it.next(), defaultValue);
        while (it.hasNext());
      }

      return a;
    }

    if (componentType == Short.class) {
      final Short defaultValue = rawType.isPrimitive() ? (short)0 : null;
      if (paramPlurality == ParamPlurality.SINGLE)
        return Numbers.parseShort(firstValue, defaultValue);

      if (size == 1)
        return paramPlurality.newSingleton(rawType, Numbers.parseShort(firstValue, defaultValue));

      if (paramPlurality == ParamPlurality.COLLECTION) {
        final Collection c = ParamPlurality.COLLECTION.newContainer(rawType, size);
        if (values instanceof RandomAccess) {
          int i = 0;
          do // [RA]
            c.add(Numbers.parseShort(values.get(i), defaultValue));
          while (++i < size);
        }
        else {
          final Iterator<String> it = values.iterator();
          do // [I]
            c.add(Numbers.parseShort(it.next(), defaultValue));
          while (it.hasNext());
        }

        return c;
      }

      final Object[] a = ParamPlurality.ARRAY.newContainer(rawType, size);
      int i = 0;
      if (values instanceof RandomAccess) {
        do // [RA]
          a[i] = Numbers.parseShort(values.get(i), defaultValue);
        while (++i < size);
      }
      else {
        final Iterator<String> it = values.iterator();
        do // [I]
          a[i++] = Numbers.parseShort(it.next(), defaultValue);
        while (it.hasNext());
      }

      return a;
    }

    return null;
  }

  private abstract class TypedParamConverter<T> implements ParamConverter<T> {
    final Class<?> type;

    TypedParamConverter(final Class<T> cls) {
      this.type = cls;
    }

    @Override
    public final String toString(final T value) {
      return String.valueOf(value);
    }
  }

  private final ConcurrentNullHashMap<Class<?>,ParamConverter<?>> dynamicParamConverters = new ConcurrentNullHashMap<>();
  private final TypedParamConverter<?>[] paramConverters = {
    new TypedParamConverter<String>(String.class) {
      @Override
      public String fromString(final String value) {
        return value;
      }
    },
    new TypedParamConverter<BigInteger>(BigInteger.class) {
      @Override
      public BigInteger fromString(final String value) {
        return new BigInteger(value);
      }
    },
    new TypedParamConverter<BigDecimal>(BigDecimal.class) {
      @Override
      public BigDecimal fromString(final String value) {
        return new BigDecimal(value);
      }
    }
  };

  @Override
  @SuppressWarnings("unchecked")
  public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
    if (rawType.isPrimitive() || rawType == Byte.class || rawType == Short.class || rawType == Character.class || rawType == Integer.class || rawType == Long.class || rawType == Float.class || rawType == Double.class || rawType == Boolean.class)
      return null;

    TypedParamConverter<T> paramConverter = null;
    for (int i = 0, i$ = paramConverters.length; i < i$; ++i) { // [RA]
      final TypedParamConverter<?> converter = paramConverters[i];
      if (rawType.isAssignableFrom(converter.type)) {
        paramConverter = (TypedParamConverter<T>)converter;
        break;
      }
    }

    if (dynamicParamConverters.containsKey(rawType))
      return (ParamConverter<T>)dynamicParamConverters.get(rawType);

    final Method method = findParseMethod(rawType);
    if (method != null) {
      paramConverter = new TypedParamConverter<T>(rawType) {
        @Override
        public T fromString(final String value) {
          try {
            return (T)method.invoke(method, value);
          }
          catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
          }
          catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
              throw (RuntimeException)cause;

            throw new ProcessingException(cause);
          }
        }
      };
    }

    dynamicParamConverters.put(rawType, paramConverter);
    return paramConverter;
  }

  private DefaultParamConverterProvider() {
  }
}