/* Copyright (c) 2019 JetRS
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
import org.libj.lang.Numbers;

public class HeaderUtilTest {
  private static void assertComposite(final float quality, final int index, final long actual) {
    assertEquals(quality, Numbers.Composite.decodeFloat(actual, 0), 0.0000001f);
    assertEquals(index, Numbers.Composite.decodeShort(actual, 3));
  }

  @Test
  public void testQualityFunction() {
    assertComposite(0.1f, 6, HeaderUtil.getQualityFromString("q=0.1;", 0));
    assertComposite(0.8f, 12, HeaderUtil.getQualityFromString("en-GB;q= .8 ,en;q=0.8", 3));
    assertComposite(0f, 14, HeaderUtil.getQualityFromString("en-GB;q =0.0; ,fr;q=0.4", 5));
    assertComposite(1f, 11, HeaderUtil.getQualityFromString("en-GB; q=1 ; q=0  ;;, fr;q=0.4", 1));
    assertComposite(0f, 15, HeaderUtil.getQualityFromString("en-GB; q =0    ; foo=bar,,,", 7));
    assertComposite(0.4f, 25, HeaderUtil.getQualityFromString("en-GB;q= 0.8 0 ; fr;q=0.4,", 6));
    assertComposite(1f, -1, HeaderUtil.getQualityFromString("en-GB;q= 0.8 0 , fr;q=0.4,", 6));
    assertComposite(1f, -1, HeaderUtil.getQualityFromString("en-GB;rq=0.8;;;;;", 5));
  }
}