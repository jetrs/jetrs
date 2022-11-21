package org.jetrs;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

@SuppressWarnings("resource")
public class SafeDirectByteArrayOutputStreamTest {
  @Test
  public void testOverflowByte() throws IOException {
    final int valid = 123;
    final int overflow = 123;
    final SafeDirectByteArrayOutputStream out = new SafeDirectByteArrayOutputStream(valid) {
      private int oveflowIndex = overflow;
      @Override
      protected boolean beforeOverflow(final int b, final byte[] buf, final int off, final int len) {
        assertEquals(oveflowIndex++, b);
        return false;
      }
    };

    for (int i = 0; i < valid; ++i)
      out.write(i);

    for (int i = valid; i < overflow; ++i)
      out.write(i);
  }

  @Test
  public void testOverflowByteArray() throws IOException {
    final byte[] bytes = new byte[3];
    for (int i = 0; i < bytes.length; ++i)
      bytes[i] = (byte)i;

    final AtomicInteger noOverflows = new AtomicInteger();
    final SafeDirectByteArrayOutputStream out = new SafeDirectByteArrayOutputStream(10) {
      @Override
      protected boolean beforeOverflow(final int b, final byte[] buf, final int off, final int len) throws IOException {
        assertArrayEquals(bytes, buf);
        final int overflows = noOverflows.getAndIncrement();
        if (overflows == 0) {
          assertEquals(1, off);
          assertEquals(2, len);
          return false;
        }

        if (overflows == 1) {
          assertEquals(2, off);
          assertEquals(1, len);
          return false;
        }

        if (overflows == 2) {
          assertEquals(0, off);
          assertEquals(3, len);
          reset();
          return true;
        }

        return false;
      }
    };

    out.write(bytes);
    out.write(bytes);
    out.write(bytes);

    out.write(bytes); // Overflow 1, 2

    assertArrayEquals(new byte[] {0, 1, 2, 0, 1, 2, 0, 1, 2, 0}, out.toByteArray());
    assertEquals(noOverflows.get(), 1);

    out.reset();

    out.write(bytes);
    out.write(bytes);
    out.write(0);
    out.write(0);

    out.write(bytes); // Overflow 2, 1

    assertArrayEquals(new byte[] {0, 1, 2, 0, 1, 2, 0, 0, 0, 1}, out.toByteArray());
    assertEquals(noOverflows.get(), 2);

    out.reset();

    out.write(bytes);
    out.write(bytes);
    out.write(0);
    out.write(bytes);

    assertArrayEquals(new byte[] {0, 1, 2, 0, 1, 2, 0, 0, 1, 2}, out.toByteArray());
    out.write(bytes); // Overflow 0, 3

    assertArrayEquals(new byte[] {0, 1, 2}, out.toByteArray());
    assertEquals(noOverflows.get(), 3);
  }
}