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

import java.io.IOException;

import org.libj.util.DirectByteArrayOutputStream;

abstract class SafeDirectByteArrayOutputStream extends DirectByteArrayOutputStream {
  protected final int size;
  protected int totalCount;

  SafeDirectByteArrayOutputStream(final int size) {
    super(size);
    this.size = size;
  }

  SafeDirectByteArrayOutputStream(final byte[] buf) {
    super(buf);
    this.size = buf.length;
  }

  protected abstract boolean beforeOverflow(int b, byte[] buf, int off, int len) throws IOException;

  @Override
  public void write(final int b) throws IOException {
    if (count >= size && !beforeOverflow(b, null, -1, -1))
      return;

    super.write(b);
    ++totalCount;
  }

  @Override
  public void write(final byte[] b, int off, int len) throws IOException {
    do {
      final int overflow = count + len - size;
      if (overflow <= 0) {
        super.write(b, off, len);
        totalCount += len;
        return;
      }

      final int part = len - overflow;
      if (part > 0) {
        super.write(b, off, part);
        totalCount += part;
        off += part;
        len -= part;
      }
    }
    while (beforeOverflow(-1, b, off, len));
  }
}