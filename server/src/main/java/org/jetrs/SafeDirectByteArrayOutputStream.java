package org.jetrs;

import java.io.IOException;

import org.libj.util.DirectByteArrayOutputStream;

public abstract class SafeDirectByteArrayOutputStream extends DirectByteArrayOutputStream {
  protected final int size;
  protected int totalCount;

  public SafeDirectByteArrayOutputStream(final int size) {
    super(size);
    this.size = size;
  }

  public SafeDirectByteArrayOutputStream(final byte[] buf) {
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
    while (true) {
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

      if (!beforeOverflow(-1, b, off, len))
        return;
    }
  }
}