/* Copyright (c) 2016 JetRS
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

import org.libj.lang.ToArrayList;

class PathSegmentList extends ToArrayList<PathSegmentImpl> {
  private static PathSegmentImpl[] parseSegments(final String pathEncoded, final int i, final int depth, final boolean decode) {
    final String segment;
    final int j = pathEncoded.indexOf('/', i);
    final PathSegmentImpl[] pathSegments;
    if (j < 0) {
      segment = pathEncoded.substring(i);
      pathSegments = new PathSegmentImpl[depth + 1];
    }
    else {
      segment = pathEncoded.substring(i, j);
      pathSegments = parseSegments(pathEncoded, j + 1, depth + 1, decode);
    }

    pathSegments[depth] = new PathSegmentImpl(segment, decode);
    return pathSegments;
  }

  PathSegmentList(final String pathEncoded, final boolean decode) {
    super(parseSegments(pathEncoded, 0, 0, decode));
  }

  PathSegmentList(final PathSegmentList pathSegmentList, final boolean decode) {
    super(new PathSegmentImpl[pathSegmentList.elementData.length]);
    final PathSegmentImpl[] pathSegments = pathSegmentList.elementData;
    for (int i = 0, i$ = elementData.length; i < i$; ++i) // [A]
      elementData[i] = new PathSegmentImpl(pathSegments[i].getPathEncoded(), decode);
  }
}