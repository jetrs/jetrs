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

package org.jetrs.common.ext.provider;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.libj.util.function.BiObjBiLongConsumer;

public class FileProviderTest {
  private static final File thisClassResource = new File(FileProviderTest.class.getProtectionDomain().getCodeSource().getLocation().getFile() + "/" + FileProviderTest.class.getName().replace('.', '/').concat(".class"));

  private static void testRange(final long[][] expected, final Object range) throws IOException {
    final AtomicInteger index = new AtomicInteger();
    FileProvider.writeTo(range, thisClassResource, new OutputStream() {
      @Override
      public void write(final int b) {
      }
    }, new BiObjBiLongConsumer<RandomAccessFile,OutputStream>() {
      @Override
      public void accept(final RandomAccessFile raf, final OutputStream out, final long from, final long to) {
        final long[] exp = expected[index.getAndIncrement()];
        assertEquals(exp[0], from);
        System.err.print(from);
        if (to != Long.MAX_VALUE) {
          System.err.print(" " + to);
          assertEquals(exp[1], to);
        }

        System.err.println();
      }
    });

    assertEquals(expected != null ? expected.length : 0, index.get());
  }

  @Test
  public void testNullRange() throws IOException {
    testRange(null, null);
    testRange(null, "bytes=");
    testRange(null, "bytes=-");
    testRange(null, "bytes=-,");
    testRange(null, "bytes=,-");
    testRange(null, "bytes=,-,");
    testRange(null, "bytes=,s-f,");
    testRange(null, "bytes=499-0");
  }

  @Test
  public void testRange() throws IOException {
    testRange(new long[][] {new long[] {0, 499}}, "bytes=0-499");
    testRange(new long[][] {new long[] {500, 999}}, "bytes=500 - 999");
    testRange(new long[][] {new long[] {-500}}, "bytes=- 500");
    testRange(new long[][] {new long[] {9500}}, "bytes=9500 -");
    testRange(new long[][] {new long[] {0}}, "bytes=0- ");
    testRange(new long[][] {new long[] {0, 0}, new long[] {-1}}, "bytes= 0-0,-1");
    testRange(new long[][] {new long[] {500, 600}, new long[] {601, 999}}, "bytes=500 -600,601-999 ");
    testRange(new long[][] {new long[] {500, 700}, new long[] {601, 999}}, "bytes=500- 700, 601-999");
    testRange(new long[][] {new long[] {500, 700}, new long[] {550, 600}, new long[] {550, 600}}, "bytes=500- 700, 550-600, 550-600");
  }
}