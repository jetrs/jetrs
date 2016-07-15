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

public class BooleanDecoder extends Decoder<Boolean> {
  @Override
  protected Boolean[] newInstance(final int depth) {
    return new Boolean[depth];
  }

  @Override
  public Boolean decode(final Reader reader, char ch) throws IOException {
    if (ch != 'f' && ch != 't') {
      if (JSObjectUtil.isNull(ch, reader))
        return null;

      throw new IllegalArgumentException("Malformed JSON");
    }

    final StringBuilder value = new StringBuilder(5);
    value.append(ch);
    do
      value.append(JSObjectUtil.next(reader));
    while ((value.length() != 4 || !"true".equals(value.toString())) && (value.length() != 5 || !"false".equals(value.toString())));

    return Boolean.parseBoolean(value.toString());
  }
}