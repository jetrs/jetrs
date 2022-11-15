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
import java.io.OutputStream;

abstract class EntityOutputStream extends OutputStream {
  OutputStream entityOutputStream;

  abstract void onWrite(byte[] bs, int off, int len, int b) throws IOException;

  @Override
  public void write(final int b) throws IOException {
    onWrite(null, -1, -1, b);
    entityOutputStream.write(b);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    onWrite(b, 0, -1, -1);
    entityOutputStream.write(b);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    onWrite(b, off, len, -1);
    entityOutputStream.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    if (entityOutputStream != null)
      entityOutputStream.flush();
  }

  @Override
  public void close() throws IOException {
    if (entityOutputStream != null) {
      entityOutputStream.flush();
      entityOutputStream.close();
    }
  }
}