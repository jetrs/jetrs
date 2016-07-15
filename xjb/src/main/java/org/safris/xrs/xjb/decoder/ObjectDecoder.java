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
import java.lang.reflect.Array;

import org.safris.commons.util.StringBuilderReader;
import org.safris.xrs.xjb.DecodeException;
import org.safris.xrs.xjb.JSObject;
import org.safris.xrs.xjb.JSObjectUtil;

public class ObjectDecoder extends JSObjectUtil {
  public JSObject decode(final StringBuilderReader reader, char ch, final Class<?> clazz) throws DecodeException, IOException {
    try {
      final JSObject value = (JSObject)clazz.newInstance();
      JSObjectUtil.decode(reader, ch, value);
      return value;
    }
    catch (final ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public JSObject[] recurse(final StringBuilderReader reader, final Class<?> clazz, final int depth) throws DecodeException, IOException {
    char ch = JSObjectUtil.next(reader);
    final JSObject value;
    if (ch != '{') {
      if (JSObjectUtil.isNull(ch, reader))
        value = null;
      else
        throw new IllegalArgumentException("Malformed JSON");
    }
    else {
      try {
        value = (JSObject)clazz.newInstance();
      }
      catch (final ReflectiveOperationException e) {
        throw new Error(e);
      }

      JSObjectUtil.decode(reader, ch, value);
    }

    ch = JSObjectUtil.next(reader);
    if (ch == ',') {
      final JSObject[] array = recurse(reader, clazz, depth + 1);
      array[depth] = value;
      return array;
    }

    if (ch == ']') {
      final JSObject[] array = (JSObject[])Array.newInstance(clazz, depth + 1);
      array[depth] = value;
      return array;
    }

    throw new IllegalArgumentException("Malformed JSON");
  }
}