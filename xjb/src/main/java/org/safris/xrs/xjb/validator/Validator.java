/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.xjb.validator;

import java.util.Collection;
import java.util.Iterator;

public abstract class Validator<T> {
  public static <T>String[] validate(final Validator<T>[] validators, final T value) {
    return validators.length > 0 ? validate(validators, value, 0, 0) : null;
  }

  public static <T>String[] validate(final Validator<T>[] validators, final Collection<T> values) {
    return validators.length > 0 ? validate(validators, values, values.iterator(), 0, 0) : null;
  }

  private static <T>String[] validate(final Validator<T>[] validators, final T value, final int index, final int depth) {
    if (index == validators.length)
      return new String[depth];

    final String error = validators[index].validate(value);
    final String[] errors = validate(validators, value, index + 1, error != null ? depth + 1 : depth);
    if (error != null)
      errors[depth] = error;

    return errors;
  }

  private static <T>String[] validate(final Validator<T>[] validators, final Collection<T> values, final Iterator<T> iterator, final int index, final int depth) {
    if (!iterator.hasNext()) {
      if (index + 1 == validators.length)
        return new String[depth];

      return validate(validators, values, values.iterator(), index + 1, depth);
    }

    final String error = validators[index].validate(iterator.next());
    final String[] errors = validate(validators, values, iterator, index, error != null ? depth + 1 : depth);
    if (error != null)
      errors[depth] = error;

    return errors;
  }

  public abstract String validate(final T value);
}