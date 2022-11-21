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
  OutputStream socketOutputStream;

  abstract void onWrite(int b, byte[] bs, int off, int len) throws IOException;

  @Override
  public void write(final int b) throws IOException {
    onWrite(b, null, -1, -1);
    socketOutputStream.write(b);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    onWrite(-1, b, 0, -1);
    socketOutputStream.write(b);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    onWrite(-1, b, off, len);
    socketOutputStream.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    if (socketOutputStream != null)
      socketOutputStream.flush();
  }

  @Override
  public void close() throws IOException {
    if (socketOutputStream != null)
      socketOutputStream.close();
  }
}