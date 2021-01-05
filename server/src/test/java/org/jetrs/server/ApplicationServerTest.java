/* Copyright (c) 2021 JetRS
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import javax.ws.rs.core.MediaType;

import org.jetrs.server.app.ApplicationServer;
import org.junit.AfterClass;
import org.junit.Test;

public class ApplicationServerTest {
  private static final Random random = new Random();
  private static final ApplicationServer server = new ApplicationServer();

  @Test
  public void testUpload() throws IOException {
    final int len = Math.abs(random.nextInt() / 100);
    final URL url = new URL("http://localhost:" + server.getContainerPort() + "/upload?len=" + len);
    final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    connection.addRequestProperty("Content-Type", MediaType.MULTIPART_FORM_DATA);
    try (final OutputStream out = connection.getOutputStream()) {
      for (int i = 0; i < len; ++i)
        out.write((byte)random.nextInt());

      out.close();
      connection.getInputStream();
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    server.close();
  }
}