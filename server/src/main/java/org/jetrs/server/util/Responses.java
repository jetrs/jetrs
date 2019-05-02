/* Copyright (c) 2016 OpenJAX
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

package org.jetrs.server.util;

import java.util.Arrays;
import java.util.Comparator;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

/**
 * Utility functions for operations pertaining to {@link Response}.
 */
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

    for (int i = 0; i < statuses.length; ++i)
      statusCodes[i] = statuses[i].getStatusCode();
  }

  /**
   * Convert a numerical status code into the corresponding {@code Status}.
   *
   * @param status The status header string.
   * @return The matching {@link Status} or {@code null} if the specified string
   *         is null.
   */
  public static Response.StatusType from(final String status) {
    if (status == null)
      return null;

    final int index = status.indexOf(' ');
    return index == -1 ? from(Integer.parseInt(status)) : from(Integer.parseInt(status.substring(0, index)), status.substring(index + 1));
  }

  /**
   * Convert a numerical status code into the corresponding {@code StatusType}.
   *
   * @param statusCode The numerical status code.
   * @return The matching {@link Status} or null if there is no match.
   */
  public static Response.StatusType from(final int statusCode) {
    final int index = Arrays.binarySearch(statusCodes, statusCode);
    return index < 0 ? null : statuses[index];
  }

  /**
   * Convert a numerical status code into the corresponding {@code StatusType}.
   *
   * @param statusCode The numerical status code.
   * @param reasonPhrase The reason phrase.
   * @return The matching {@link StatusType} or null if there is no match.
   */
  public static Response.StatusType from(final int statusCode, final String reasonPhrase) {
    final Response.StatusType statusType = from(statusCode);
    if (statusType != null && statusType.getReasonPhrase().equals(reasonPhrase))
      return statusType;

    return new Response.StatusType() {
      @Override
      public int getStatusCode() {
        return statusCode;
      }

      @Override
      public Family getFamily() {
        return from(statusCode).getFamily();
      }

      @Override
      public String getReasonPhrase() {
        return reasonPhrase;
      }
    };
  }

  private Responses() {
  }
}