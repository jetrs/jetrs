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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.libj.lang.ToArrayList;

/**
 * Utility functions for operations pertaining to {@link Response}.
 */
final class Responses {
  private static final Status[] statuses = Status.values();
  private static final int[] statusCodes = new int[statuses.length];
  private static final ConcurrentHashMap<ToArrayList<Object>,StatusType> codeReasonToStatus = new ConcurrentHashMap<>();

  static {
    for (int i = 0, i$ = statuses.length; i < i$; ++i) // [A]
      statusCodes[i] = statuses[i].getStatusCode();
  }

  /**
   * Convert a numerical status code into the corresponding {@link StatusType}.
   *
   * @param status The status header string.
   * @return The matching {@link StatusType} or {@code null} if the specified string is null or the parsed {@code statusCode == -1}.
   * @throws IllegalArgumentException If the parsed {@code statusCode < -1}.
   */
  static StatusType from(final String status) {
    if (status == null)
      return null;

    final int index = status.indexOf(' ');
    return index == -1 ? from(Integer.parseInt(status)) : from(Integer.parseInt(status.substring(0, index)), status.substring(index + 1));
  }

  /**
   * Convert a numerical status code into the corresponding {@link StatusType}.
   *
   * @param statusCode The numerical status code.
   * @return The matching {@link StatusType} or {@code null} if {@code statusCode == -1}.
   * @throws IllegalArgumentException If {@code statusCode < -1}.
   */
  static StatusType from(final int statusCode) {
    if (statusCode < -1)
      throw new IllegalArgumentException("statusCode = " + statusCode);

    if (statusCode == -1)
      return null;

    final int index = Arrays.binarySearch(statusCodes, statusCode);
    return index < 0 ? fromMap(statusCode, null) : statuses[index];
  }

  /**
   * Convert a numerical status code into a {@link StatusType}.
   *
   * @param statusCode The numerical status code.
   * @param reasonPhrase The reason phrase.
   * @return The {@link StatusType} or {@code null} if {@code statusCode == -1}.
   * @throws IllegalArgumentException If {@code statusCode < -1}.
   */
  static StatusType from(final int statusCode, final String reasonPhrase) {
    if (statusCode < -1)
      throw new IllegalArgumentException("statusCode = " + statusCode);

    if (statusCode == -1)
      return null;

    final int index = Arrays.binarySearch(statusCodes, statusCode);
    if (index >= 0) {
      final StatusType status = statuses[index];
      if (Objects.equals(reasonPhrase, status.getReasonPhrase()))
        return status;
    }

    return fromMap(statusCode, reasonPhrase);
  }

  private static StatusType fromMap(final int statusCode, final String reasonPhrase) {
    final ToArrayList<Object> codeReason = new ToArrayList<>(statusCode, reasonPhrase);
    StatusType status = codeReasonToStatus.get(codeReason);
    if (status == null) {
      codeReasonToStatus.put(codeReason, status = new StatusType() {
        @Override
        public int getStatusCode() {
          return statusCode;
        }

        @Override
        public String getReasonPhrase() {
          return reasonPhrase;
        }

        @Override
        public Family getFamily() {
          return Family.familyOf(statusCode);
        }

        @Override
        public Status toEnum() {
          final int index = Arrays.binarySearch(statusCodes, statusCode);
          return index < 0 ? null : statuses[index];
        }

        @Override
        public String toString() {
          final StringBuilder b = new StringBuilder("HTTP ").append(statusCode);
          if (reasonPhrase != null)
            b.append(' ').append(reasonPhrase);

          return b.toString();
        }
      });
    }

    return status;
  }

  private Responses() {
  }
}