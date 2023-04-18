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

package org.jetrs.server;

import static org.junit.Assert.*;

import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jetrs.HttpHeaders;

public final class AssertServer {
  private static void assertHeadersEqual(final boolean shouldEqualOnError, final Response headResponse, final MultivaluedMap<?,?> expected, final MultivaluedMap<?,?> actual) {
    for (final Map.Entry<?,?> entry : expected.entrySet()) { // [S]
      final String headerName = entry.getKey().toString();
      if (HttpHeaders.DATE.equalsIgnoreCase(headerName))
        continue;

      final boolean isErrorAndShouldEqual = headResponse.getStatus() == 200 || shouldEqualOnError;
      if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName) || HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(headerName))
        assertEquals(headerName, isErrorAndShouldEqual, entry.getValue().equals(actual.get(entry.getKey())));
      else if (!isErrorAndShouldEqual && HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName))
        continue;
      else
        assertEquals(headerName, entry.getValue(), actual.get(entry.getKey()));
    }
  }

  public static void assertGetHead(final Response getResponse, final Response headResponse) {
    assertGetHead(true, getResponse, headResponse);
  }

  public static void assertGetHead(final boolean shouldEqualOnError, final Response getResponse, final Response headResponse) {
    assertHeadersEqual(shouldEqualOnError, headResponse, getResponse.getHeaders(), headResponse.getHeaders());
    assertHeadersEqual(shouldEqualOnError, headResponse, getResponse.getStringHeaders(), headResponse.getStringHeaders());
  }

  private AssertServer() {
  }
}