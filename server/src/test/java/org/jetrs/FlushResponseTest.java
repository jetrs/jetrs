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

import static org.jetrs.server.AssertServer.*;
import static org.junit.Assert.*;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jetrs.provider.ext.interceptor.GZipCodecInterceptor;
import org.jetrs.server.app.TestAppServer;
import org.jetrs.server.app.service.FlushResponseService;
import org.junit.AfterClass;
import org.junit.Test;
import org.libj.lang.Strings;

public class FlushResponseTest {
  private static final TestAppServer server = new TestAppServer(null, null);
  private static final String serviceUrl = "http://localhost:" + server.getContainerPort();
  private static final Client client = ClientBuilder.newClient().register(GZipCodecInterceptor.class);

  public static String s(final Object obj) {
    return obj == null ? "" : obj.toString();
  }

  private static long test(final String data, final int mul, final Boolean chunked, final boolean gzip, final boolean exception, final boolean expectContentLengthEqualOnError) throws Exception {
    final byte[] expected = FlushResponseService.expand(data.getBytes(), mul);
    final int expectedContentLength = gzip ? FlushResponseService.gzip(expected).length : expected.length;

    WebTarget webTarget = client.target(serviceUrl + "/flush/" + mul)
      .queryParam("d", data)
      .queryParam("e", exception);

    if (chunked != null)
      webTarget = webTarget.queryParam("q", chunked);

    Invocation.Builder request = webTarget.request();
    if (gzip)
      request = request.header(HttpHeaders.ACCEPT_ENCODING, "gzip");

    long timeMs = System.currentTimeMillis();
    final Response getResponse = request.get();
    timeMs = System.currentTimeMillis() - timeMs;

    final byte[] actual = getResponse.readEntity(byte[].class);
    try {
      getResponse.readEntity(byte[].class);
      fail("Expected IllegalStateException");
    }
    catch (final IllegalStateException e) {
    }

    if (gzip)
      assertEquals("gzip", getResponse.getHeaderString(HttpHeaders.CONTENT_ENCODING));

    final boolean bufferOverflow = expectedContentLength > ContainerResponseContextImpl.bufferSize;

    // Expect error if buffered or Content-Length && gzip (with gzip, the Content-Length is removed by ContentCodecInterceptor)
    final boolean expectErrorResponse = exception && !bufferOverflow && (chunked == null || !chunked && gzip);
    assertEquals(expectErrorResponse ? 503 : 200, getResponse.getStatus());
    if (!exception)
      assertArrayEquals(expected, actual);

    final MultivaluedMap<String,Object> headers = getResponse.getHeaders();
    final Object contentLength = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
    final List<Object> transferEncoding = headers.get(HttpHeaders.TRANSFER_ENCODING);
    final boolean shouldBeChunked = (chunked != null ? chunked : bufferOverflow) || gzip && bufferOverflow;
    if (shouldBeChunked) {
      assertNull("Content-Length: " + contentLength, contentLength);
      assertNotNull("Transfer-Encoding: " + transferEncoding, transferEncoding);
      assertTrue("Transfer-Encoding: " + transferEncoding, transferEncoding.contains("chunked"));
    }
    else {
      assertNotNull("Content-Length: " + contentLength, contentLength);
      assertNull("Transfer-Encoding: " + transferEncoding, transferEncoding);
      if (expectContentLengthEqualOnError)
        assertEquals("Content-Length: " + contentLength, expectedContentLength, ((Number)contentLength).intValue());
    }

    final Response headResponse = request.head();
    assertGetHead(!exception, false, getResponse, headResponse);
    return timeMs;
  }

  private static long test(final Boolean chunked, final boolean gzip, final boolean expectContentLengthEqualOnError) throws Exception {
    test(chunked, gzip, true, expectContentLengthEqualOnError);
    return test(chunked, gzip, false, expectContentLengthEqualOnError);
  }

  private static long test(final Boolean chunked, final boolean gzip, final boolean exception, final boolean expectContentLengthEqualOnError) throws Exception {
    int m = 1;
    for (; m <= 8388608; m *= 2) { // [N]
      test(Strings.getRandomAlphaNumeric(3), m, chunked, gzip, exception, expectContentLengthEqualOnError);
    }

    test(Strings.getRandomAlphaNumeric(3), ContainerResponseContextImpl.bufferSize, chunked, gzip, exception, expectContentLengthEqualOnError);
    test(Strings.getRandomAlphaNumeric(3), ContainerResponseContextImpl.bufferSize + 1, chunked, gzip, exception, expectContentLengthEqualOnError);

    long timeMs = 0;
    for (int i = 0; i < 10; ++i) // [N]
      timeMs += test(Strings.getRandomAlphaNumeric(3), m, chunked, gzip, exception, expectContentLengthEqualOnError);

    return timeMs;
  }

  @Test
  public void testBuffered() throws Exception {
    System.err.println("Buffered: " + test(null, false, false));
    System.err.println("Buffered (gzip): " + test(null, true, false));
  }

  @Test
  public void testChunked() throws Exception {
    System.err.println("Chunked: " + test(true, false, true));
    System.err.println("Chunked (gzip): " + test(true, true, true));
  }

  @Test
  public void testContentLength() throws Exception {
    System.err.println("Content-Length: " + test(false, false, true));
    System.err.println("Content-Length (gzip): " + test(false, true, false));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }
}