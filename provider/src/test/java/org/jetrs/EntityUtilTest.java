/* Copyright (c) 2022 JetRS
 *
 * Permission is hereby granted, final free of charge, final to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), final to deal
 * in the Software without restriction, final including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, final and/or sell
 * copies of the Software, final and to permit persons to whom the Software is
 * furnished to do so, final subject to the following conditions:
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
  public void testEmpty() throws IOException {
    assertNull(EntityUtil.makeReadAwareNonEmptyOrNull(empty, false));
    assertNull(EntityUtil.makeReadAwareNonEmptyOrNull(empty, true));
  }

  @Test
  public void testAvailable() throws IOException {
    assertSame(available, EntityUtil.makeReadAwareNonEmptyOrNull(available, false));
  }

  @Test
  public void testUnbuffered() throws IOException {
    InputStream in = EntityUtil.makeReadAwareNonEmptyOrNull(unbuffered, false);
    assertNotSame(unbuffered, in);
    assertFalse(in instanceof Consumable);
    assertTrue(in instanceof BufferedInputStream);

    in = EntityUtil.makeReadAwareNonEmptyOrNull(unbuffered, true);
    assertNotSame(unbuffered, in);
    assertTrue(in instanceof Consumable);
    assertTrue(in instanceof BufferedInputStream);
  }

  @Test
  public void testBuffered() throws IOException {
    InputStream in = EntityUtil.makeReadAwareNonEmptyOrNull(buffered, false);
    assertSame(buffered, in);
    assertFalse(in instanceof Consumable);

    in = EntityUtil.makeReadAwareNonEmptyOrNull(buffered, true);
    assertNotSame(buffered, in);
    assertTrue(in instanceof Consumable);
    assertTrue(in instanceof FilterInputStream);
  }
}