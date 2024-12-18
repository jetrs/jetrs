/* Copyright (c) 2022 JetRS
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class EntityUtilTest {
  private static final byte[] data = "hello".getBytes();
  private static final InputStream empty = new ByteArrayInputStream("".getBytes());
  private static final InputStream available = new ByteArrayInputStream(data) {
    @Override
    public boolean markSupported() {
      return false;
    }
  };
  private static final InputStream unbuffered = new ByteArrayInputStream(data) {
    @Override
    public synchronized int available() {
      return 0;
    }

    @Override
    public boolean markSupported() {
      return false;
    }
  };
  private static final InputStream buffered = new ByteArrayInputStream(data) {
    @Override
    public synchronized int available() {
      return 0;
    }
  };

  @Test
  public void testDisallowEmpty() throws IOException {
    assertNull(EntityUtil.makeConsumableNonEmptyOrNull(empty, false, false));
    assertNull(EntityUtil.makeConsumableNonEmptyOrNull(empty, true, false));
  }

  @Test
  public void testAllowEmpty() throws IOException {
    final InputStream in1 = EntityUtil.makeConsumableNonEmptyOrNull(empty, false, true);
    assertNotNull(in1);
    assertEquals(-1, in1.read());
    final InputStream in2 = EntityUtil.makeConsumableNonEmptyOrNull(empty, true, true);
    assertNotNull(in2);
    assertEquals(-1, in2.read());
  }

  @Test
  public void testAvailable() throws IOException {
    assertSame(available, EntityUtil.makeConsumableNonEmptyOrNull(available, false, false));
  }

  @Test
  public void testUnbuffered() throws IOException {
    InputStream in = EntityUtil.makeConsumableNonEmptyOrNull(unbuffered, false, false);
    assertNotSame(unbuffered, in);
    assertFalse(in instanceof Consumable);
    assertTrue(in instanceof BufferedInputStream);

    in = EntityUtil.makeConsumableNonEmptyOrNull(unbuffered, true, false);
    assertNotSame(unbuffered, in);
    assertTrue(in instanceof Consumable);
    assertTrue(in instanceof BufferedInputStream);
  }

  @Test
  public void testBuffered() throws IOException {
    InputStream in = EntityUtil.makeConsumableNonEmptyOrNull(buffered, false, false);
    assertSame(buffered, in);
    assertFalse(in instanceof Consumable);

    in = EntityUtil.makeConsumableNonEmptyOrNull(buffered, true, false);
    assertNotSame(buffered, in);
    assertTrue(in instanceof Consumable);
    assertTrue(in instanceof FilterInputStream);
  }
}