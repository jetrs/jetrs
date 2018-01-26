package org.libx4j.xrs.server.ext;

import org.junit.Assert;
import org.junit.Test;

public class StringArrayHeaderDelegateTest {
  @Test
  public void testParse() {
    Assert.assertNull(StringArrayHeaderDelegate.parse(null));
    Assert.assertArrayEquals(new String[0], StringArrayHeaderDelegate.parse(""));
    Assert.assertArrayEquals(new String[] {"one"}, StringArrayHeaderDelegate.parse("one"));
    Assert.assertArrayEquals(new String[] {"one", "two"}, StringArrayHeaderDelegate.parse("one, two"));
    Assert.assertArrayEquals(new String[] {"one", "two", "three"}, StringArrayHeaderDelegate.parse("one, two, three"));
    Assert.assertArrayEquals(new String[] {"one", "two", "three"}, StringArrayHeaderDelegate.parse("one,, two,, three"));
    Assert.assertArrayEquals(new String[] {"one", "two", "three"}, StringArrayHeaderDelegate.parse(",, , one,, , ,,two, three, ,,"));
  }
}