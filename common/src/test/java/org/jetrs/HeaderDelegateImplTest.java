/* Copyright (c) 2021 JetRS
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

import static org.junit.Assert.*;

import org.junit.Test;
import org.libj.lang.Strings;

public class HeaderDelegateImplTest extends RuntimeDelegateTest {
  // NOTE: This was moved out of HeaderDelegateImpl class, because it's most likely not needed
  static final HeaderDelegateImpl<String[]> STRING_ARRAY = new HeaderDelegateImpl<String[]>(String[].class, true) {
    private String[] fromString(final String value, final int start, final int depth) {
      if (start >= value.length() - 1)
        return new String[depth];

      int end = value.indexOf(',', start);
      if (start == end)
        return fromString(value, end + 1, depth);

      if (end == -1)
        end = value.length();

      final String token = value.substring(start, end).trim();
      if (token.length() == 0)
        return fromString(value, end + 1, depth);

      final String[] array = fromString(value, end + 1, depth + 1);
      array[depth] = token;
      return array;
    }

    @Override
    String[] valueOf(final String value) {
      return value.length() == 0 ? Strings.EMPTY_ARRAY : fromString(value, 0, 0);
    }

    @Override
    public String toString(final String[] value) {
      if (value.length == 0)
        return "";

      final StringBuilder builder = new StringBuilder();
      builder.append(value[0]);
      for (int i = 1, i$ = value.length; i < i$; ++i) // [A]
        builder.append(',').append(value[i]);

      return builder.toString();
    }
  };

  @Test
  public void testParseStringArray() {
    assertArrayEquals(Strings.EMPTY_ARRAY, STRING_ARRAY.fromString(""));
    assertArrayEquals(new String[] {"one"}, STRING_ARRAY.fromString("one"));
    assertArrayEquals(new String[] {"one", "two"}, STRING_ARRAY.fromString("one, two"));
    assertArrayEquals(new String[] {"one", "two", "three"}, STRING_ARRAY.fromString("one, two, three"));
    assertArrayEquals(new String[] {"one", "two", "three"}, STRING_ARRAY.fromString("one,, two,, three"));
    assertArrayEquals(new String[] {"one", "two", "three"}, STRING_ARRAY.fromString(",, , one,, , ,,two, three, ,,"));
  }
}