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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.ext.ParamConverterProvider;

import org.libj.lang.Classes;

class ResourceInfos extends ArrayList<ResourceInfoImpl> {
  private final HashMap<Class<?>,HashMap<AnnotatedElement,DefaultValueImpl>> classToDefaultValues = new HashMap<>();

  static DefaultValueImpl digestDefaultValue(final DefaultValue defaultValue, final Class<?> clazz, final Type type, final Annotation[] annotations, final ComponentSet<Component<ParamConverterProvider>> paramConverterComponents) throws IOException {
    final String annotatedValue = defaultValue.value();
    final Object convertedValue = DefaultParamConverterProvider.convertParameter(clazz, type, annotations, ParamPlurality.fromClass(clazz), annotatedValue, null, true, paramConverterComponents, null);
    if (!EntityUtil.validateNotNull(convertedValue, annotations))
      throw new NullPointerException("ParamConverter.fromString(String) returned null for @NotNull @DefaultValue(" + defaultValue.value() + ")");

    return new DefaultValueImpl(convertedValue != null, annotatedValue, convertedValue);
  }

  void initDefaultValues(final Class<?> cls, final ComponentSet<Component<ParamConverterProvider>> paramConverterComponents) throws IOException {
    if (!classToDefaultValues.containsKey(cls)) {
      HashMap<AnnotatedElement,DefaultValueImpl> defaultValues = null;
      DefaultValue defaultValue = AnnotationUtil.getAnnotation(cls, DefaultValue.class);
      if (defaultValue != null) {
        classToDefaultValues.put(cls, defaultValues = new HashMap<>());
        defaultValues.put(cls, digestDefaultValue(defaultValue, cls, cls.getGenericSuperclass(), AnnotationUtil.getAnnotations(cls), paramConverterComponents)); // FIXME: Is cls.getGenericSuperclass() correct here?
      }

      for (final Field field : ContainerRequestContextImpl.getContextFields(cls)) { // [A]
        defaultValue = field.getAnnotation(DefaultValue.class);
        if (defaultValue != null) {
          if (defaultValues == null)
            classToDefaultValues.put(cls, defaultValues = new HashMap<>());

          defaultValues.put(field, digestDefaultValue(defaultValue, field.getType(), field.getGenericType(), Classes.getAnnotations(field), paramConverterComponents));
        }
      }
    }
  }

  HashMap<AnnotatedElement,DefaultValueImpl> getDefaultValues(final Class<?> cls) {
    return classToDefaultValues.get(cls);
  }
}