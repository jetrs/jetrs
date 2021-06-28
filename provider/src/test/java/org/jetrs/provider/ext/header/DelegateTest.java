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

package org.jetrs.provider.ext.header;

import static org.junit.Assert.*;

import org.junit.Test;

public class DelegateTest {
  @Test
  public void testParseStringArray() {
    assertArrayEquals(new String[0], Delegate.STRING_ARRAY.fromString(""));
    assertArrayEquals(new String[] {"one"}, Delegate.STRING_ARRAY.fromString("one"));
    assertArrayEquals(new String[] {"one", "two"}, Delegate.STRING_ARRAY.fromString("one, two"));
    assertArrayEquals(new String[] {"one", "two", "three"}, Delegate.STRING_ARRAY.fromString("one, two, three"));
    assertArrayEquals(new String[] {"one", "two", "three"}, Delegate.STRING_ARRAY.fromString("one,, two,, three"));
    assertArrayEquals(new String[] {"one", "two", "three"}, Delegate.STRING_ARRAY.fromString(",, , one,, , ,,two, three, ,,"));
  }
}