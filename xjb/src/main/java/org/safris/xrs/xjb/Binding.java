/* Copyright (c) 2015 Seva Safris
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

package org.safris.xrs.xjb;

import java.lang.reflect.Field;
import java.util.Collection;

import org.safris.xrs.xjb.validator.Validator;

public class Binding<T> {
  public final String name;
  public final Field property;
  public final Class<?> type;
  public final boolean isAbstract;
  public final boolean array;
  public final boolean required;
  public final boolean notNull;
  public final Validator<?>[] validators;

  @SafeVarargs
  public Binding(final String name, final Field property, final Class<?> type, final boolean isAbstract, final boolean array, final boolean required, final boolean notNull, final Validator<?> ... validators) {
    property.setAccessible(true);
    this.name = name;
    this.property = property;
    this.type = type;
    this.isAbstract = isAbstract;
    this.array = array;
    this.required = required;
    this.notNull = notNull;
    this.validators = validators;
  }

  private String errorsToString(final String[] errors) {
    if (errors == null || errors.length == 0)
      return null;

    final StringBuilder message = new StringBuilder();
    for (final String error : errors)
      message.append("\n\"").append(name).append("\" ").append(error);

    return message.substring(1);
  }

  @SuppressWarnings("unchecked")
  protected String validate(final Object value) {
    return errorsToString(value instanceof Collection ? Validator.validate((Validator<T>[])validators, (Collection<T>)value) : Validator.validate((Validator<T>[])validators, (T)value));
  }
}