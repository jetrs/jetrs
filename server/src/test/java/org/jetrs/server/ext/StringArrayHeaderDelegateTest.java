package org.jetrs.server.ext;

import static org.junit.Assert.*;

import org.jetrs.common.ext.StringArrayHeaderDelegate;
import org.junit.Test;

public class StringArrayHeaderDelegateTest {
  @Test
  public void testParse() {
    assertNull(StringArrayHeaderDelegate.parse(null));
    assertArrayEquals(new String[0], StringArrayHeaderDelegate.parse(""));
    assertArrayEquals(new String[] {"one"}, StringArrayHeaderDelegate.parse("one"));
    assertArrayEquals(new String[] {"one", "two"}, StringArrayHeaderDelegate.parse("one, two"));
    assertArrayEquals(new String[] {"one", "two", "three"}, StringArrayHeaderDelegate.parse("one, two, three"));
    assertArrayEquals(new String[] {"one", "two", "three"}, StringArrayHeaderDelegate.parse("one,, two,, three"));
    assertArrayEquals(new String[] {"one", "two", "three"}, StringArrayHeaderDelegate.parse(",, , one,, , ,,two, three, ,,"));
  }
}