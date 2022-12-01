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

import org.jetrs.provider.ext.BytesProvider;
import org.jetrs.provider.ext.CharacterProvider;
import org.jetrs.provider.ext.FormMultivaluedMapProvider;
import org.jetrs.provider.ext.FormProvider;
import org.jetrs.provider.ext.NumberProvider;
import org.jetrs.provider.ext.StringProvider;
import org.jetrs.provider.ext.interceptor.GZipCodec;
import org.jetrs.server.app.ApplicationServer;
import org.jetrs.server.app.service.FlushResponseService;
import org.junit.AfterClass;
import org.junit.Test;
import org.libj.lang.Strings;

public class FlushResponseTest {
  private static final ApplicationServer server = new ApplicationServer(null, null);
  private static final String serviceUrl = "http://localhost:" + server.getContainerPort() + ApplicationServer.applicationPath;
  private static final Client client;

  static {
    client = ClientBuilder.newClient();
    client.register(new CharacterProvider());
    client.register(new NumberProvider());
    client.register(new BytesProvider());
    client.register(new StringProvider());
    client.register(new FormProvider());
    client.register(new FormMultivaluedMapProvider());
    client.register(GZipCodec.class);
  }

  public static String s(final Object obj) {
    return obj == null ? "" : obj.toString();
  }

  private static long test(final String data, final int mul, final Boolean chunked, final boolean gzip) throws Exception {
    final byte[] expected = FlushResponseService.expand(data.getBytes(), mul);
    final int expectedContentLength = gzip ? FlushResponseService.gzip(expected).length : expected.length;

    WebTarget webTarget = client.target(serviceUrl + "/flush/" + mul).queryParam("d", data);
    if (chunked != null)
      webTarget = webTarget.queryParam("q", chunked);

    Invocation.Builder request = webTarget.request();
    if (gzip)
      request = request.header(HttpHeaders.ACCEPT_ENCODING, "gzip");

    long time = System.currentTimeMillis();
    final Response getResponse = request.get();
    time = System.currentTimeMillis() - time;

    final byte[] actual = getResponse.readEntity(byte[].class);
    if (gzip)
      assertEquals("gzip", getResponse.getHeaderString(HttpHeaders.CONTENT_ENCODING));

    assertArrayEquals(expected, actual);
    final MultivaluedMap<String,Object> headers = getResponse.getHeaders();
    final Object contentLength = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
    final List<Object> transferEncoding = headers.get(HttpHeaders.TRANSFER_ENCODING);
    final boolean bufferOverflow = expectedContentLength > ContainerResponseContextImpl.bufferSize;
    final boolean shouldBeChunked = (chunked != null ? chunked : bufferOverflow) || gzip && bufferOverflow;
    if (shouldBeChunked) {
      assertNull("Content-Length: " + contentLength, contentLength);
      assertNotNull("Transfer-Encoding: " + transferEncoding, transferEncoding);
      assertTrue("Transfer-Encoding: " + transferEncoding, transferEncoding.contains("chunked"));
    }
    else {
      assertNotNull("Content-Length: " + contentLength, contentLength);
      assertNull("Transfer-Encoding: " + transferEncoding, transferEncoding);
      assertEquals("Content-Length: " + contentLength, expectedContentLength, ((Number)contentLength).intValue());
    }

    final Response headResponse = request.head();
    assertGetHead(getResponse, headResponse);
    return time;
  }

  private static long test(final Boolean chunked, final boolean gzip) throws Exception {
    int m = 1;
    for (; m < 10000000; m *= 2) { // [N]
      test(Strings.getRandomAlphaNumeric(3), m, chunked, gzip);
    }

    test(Strings.getRandomAlphaNumeric(3), ContainerResponseContextImpl.bufferSize, chunked, gzip);
    test(Strings.getRandomAlphaNumeric(3), ContainerResponseContextImpl.bufferSize + 1, chunked, gzip);

    long time = 0;
    for (int i = 0; i < 10; ++i) // [N]
      time += test(Strings.getRandomAlphaNumeric(3), m, chunked, gzip);

    return time;
  }

  @Test
  public void testBuffered() throws Exception {
    System.err.println("Buffered: " + test(null, false));
    System.err.println("Buffered (gzip): " + test(null, true));
  }

  @Test
  public void testChunked() throws Exception {
    System.err.println("Chunked: " + test(true, false));
    System.err.println("Chunked (gzip): " + test(true, true));
  }

  @Test
  public void testContentLength() throws Exception {
    System.err.println("Content-Length: " + test(false, false));
    System.err.println("Content-Length (gzip): " + test(false, true));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }
}