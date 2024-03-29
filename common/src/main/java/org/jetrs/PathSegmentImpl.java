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

package org.jetrs;

import java.util.List;

import javax.ws.rs.core.PathSegment;

import org.libj.net.URLs;

final class PathSegmentImpl implements PathSegment {
  private final String pathEncoded;
  private final String path;
  private final MultivaluedArrayHashMap<String,String> matrixParameters;

  static void parseMatrixParams(final MultivaluedArrayHashMap<String,String> matrixParameters, final String pathEncoded, int j) {
    int i = j;
    final int len = pathEncoded.length();

    do {
      final String param;
      j = pathEncoded.indexOf(';', ++i);
      if (j < 0) {
        param = pathEncoded.substring(i);
        i = len;
      }
      else {
        param = pathEncoded.substring(i, j);
        i = j;
      }

      if (param.length() > 0) {
        final int eq = param.indexOf('=');
        final String name;
        final String value;
        if (eq < 0) {
          name = param;
          value = null;
        }
        else {
          name = param.substring(0, eq);
          value = param.substring(eq + 1);
        }

        matrixParameters.add(name, value);
      }
    }
    while (i < len);
  }

  PathSegmentImpl(final String pathEncoded, final boolean decode) {
    if (decode) {
      final String path = URLs.decodePath(pathEncoded);
      final int s = path.indexOf(';');
      if (s < 0) {
        this.path = path;
        this.pathEncoded = pathEncoded;
        this.matrixParameters = EntityUtil.EMPTY_MAP;
      }
      else {
        this.path = path.substring(0, s);
        this.pathEncoded = pathEncoded.substring(0, pathEncoded.indexOf(';'));
        parseMatrixParams(this.matrixParameters = new MultivaluedArrayHashMap<>(), path, s);
      }
    }
    else {
      final String path = pathEncoded;
      final int s = path.indexOf(';');
      if (s < 0) {
        this.path = path;
        this.pathEncoded = pathEncoded;
        this.matrixParameters = EntityUtil.EMPTY_MAP;
      }
      else {
        this.path = this.pathEncoded = path.substring(0, s);
        parseMatrixParams(this.matrixParameters = new MultivaluedArrayHashMap<>(), path, s);
      }
    }
  }

  String getPathEncoded() {
    return pathEncoded;
  }

  boolean hasMatrixParams() {
    return matrixParameters.size() > 0;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public MultivaluedArrayHashMap<String,String> getMatrixParameters() {
    return matrixParameters;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    if (path != null)
      builder.append(path);

    if (matrixParameters.size() > 0) {
      for (final String name : matrixParameters.keySet()) { // [S]
        final List<String> values = matrixParameters.get(name);
        for (int i = 0, i$ = values.size(); i < i$; ++i) // [RA]
          builder.append(';').append(UriEncoder.MATRIX.encode(name)).append('=').append(UriEncoder.MATRIX.encode(values.get(i)));
      }
    }

    return builder.toString();
  }
}