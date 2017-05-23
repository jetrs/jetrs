/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server.util;

import java.util.Arrays;
import java.util.Comparator;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public final class Responses {
  private static final Response.Status[] statuses = Response.Status.values();
  private static final int[] statusCodes = new int[statuses.length];

  static {
    Arrays.sort(statuses, new Comparator<Response.Status>() {
      @Override
      public int compare(final Status o1, final Status o2) {
        return Integer.compare(o1.getStatusCode(), o2.getStatusCode());
      }
    });

    for (int i = 0; i < statuses.length; i++)
      statusCodes[i] = statuses[i].getStatusCode();
  }

  /**
   * Convert a numerical status code into the corresponding Status.
   *
   * @param statusCode the numerical status code.
   * @return the matching Status or null is no matching Status is defined.
   */
  public static Response.Status fromStatusCode(final int statusCode) {
    final int index = Arrays.binarySearch(statusCodes, statusCode);
    return index < 0 ? null : statuses[index];
  }


  private Responses() {
  }
}