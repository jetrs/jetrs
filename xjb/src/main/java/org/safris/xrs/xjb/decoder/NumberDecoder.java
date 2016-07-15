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

package org.safris.xrs.xjb.decoder;

import java.io.IOException;
import java.io.Reader;

import org.safris.xrs.xjb.JSObjectUtil;

public class NumberDecoder extends Decoder<Number> {
  @Override
  protected Number[] newInstance(final int depth) {
    return new Double[depth];
  }

  @Override
  public Number decode(final Reader reader, char ch) throws IOException {
    if (('0' > ch || ch > '9') && ch != '-') {
      if (JSObjectUtil.isNull(ch, reader))
        return null;

      throw new IllegalArgumentException("Malformed JSON");
    }

    final StringBuilder value = new StringBuilder();
    do {
      value.append(ch);
    }
    while ('0' <= (ch = JSObjectUtil.nextAny(reader)) && ch <= '9' || ch == '.' || ch == 'e' || ch == 'E' || ch == '+' || ch == '+');

    final String number = value.toString();
    return number.contains(".") || number.contains("e") || number.contains("E") ? Double.parseDouble(number) : Long.parseLong(number);
  }
}