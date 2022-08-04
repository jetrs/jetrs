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

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.libj.net.URLs;

class PathSegmentImpl implements PathSegment {
  private static final MultivaluedHashMap<String,String> emptyMap = new MultivaluedHashMap<>(); // FIXME: Make this unmodifiable

  private final String pathEncoded;
  private final String path;
  private final MultivaluedMap<String,String> matrixParameters;

  PathSegmentImpl(final String pathEncoded, final boolean decode) {
    final int semicolon = pathEncoded.indexOf(';');
    if (semicolon < 0) {
      this.matrixParameters = emptyMap;
      this.pathEncoded = pathEncoded;
    }
    else {
      this.matrixParameters = new MultivaluedHashMap<>();

      int i = semicolon;
      final int len = pathEncoded.length();

      do {
        final String param;
        final int j = pathEncoded.indexOf(';', ++i);
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

          if (decode)
            matrixParameters.add(URLs.decodePath(name), value == null ? null : URLs.decodePath(value));
          else
            matrixParameters.add(name, value);
        }
      }
      while (i < len);

      this.pathEncoded = pathEncoded.substring(0, semicolon);
    }

    this.path = decode ? URLs.decodePath(this.pathEncoded) : this.pathEncoded;
  }

  String getPathEncoded() {
    return this.pathEncoded;
  }

  boolean hasMatrixParams() {
    return matrixParameters != null;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public MultivaluedMap<String,String> getMatrixParameters() {
    return matrixParameters;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    if (path != null)
      builder.append(path);

    if (matrixParameters != null)
      for (final String name : matrixParameters.keySet())
        for (final String value : matrixParameters.get(name))
          builder.append(';').append(name).append('=').append(value);

    return builder.toString();
  }
}