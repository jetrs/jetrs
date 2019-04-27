/* Copyright (c) 2019 OpenJAX
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

package org.openjax.xrs.server.core;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.openjax.ext.net.URLs;

public class PathSegmentImpl implements PathSegment {
  private final String path;
  private final String matrix;

  public PathSegmentImpl(final String path) {
    final int semi = path.indexOf(';');
    if (semi != -1) {
      this.path = path.substring(0, semi);
      this.matrix = path.substring(semi + 1);
    }
    else {
      this.path = path;
      this.matrix = null;
    }
  }

  @Override
  public String getPath() {
    return path;
  }

  private MultivaluedMap<String,String> matrixParameters;

  @Override
  public MultivaluedMap<String,String> getMatrixParameters() {
    if (matrix == null || matrixParameters != null)
      return matrixParameters;

    final MultivaluedMap<String,String> matrixParameters = new MultivaluedHashMap<>();
    int semi = -1;
    int equals = -1;
    for (int i = 0; i < matrix.length(); ++i) {
      final char ch = matrix.charAt(i);
      if (ch == '=') {
        equals = i;
      }
      else if (ch == ';' || i == matrix.length() - 1) {
        if (equals == -1)
          throw new BadRequestException();

        final String key = matrix.substring(semi + 1, equals);
        final String value = matrix.substring(equals + 1, i);
        matrixParameters.putSingle(URLs.decodePath(key), URLs.decodePath(value));
        semi = i;
        equals = -1;
      }
    }

    return this.matrixParameters = matrixParameters;
  }
}