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

package org.jetrs.server.app.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.libj.io.Streams;

@Singleton
public class FileUploadService {
  @PUT
  @Path("/upload")
  @Consumes(MediaType.WILDCARD)
  public void upload(final InputStream in, @QueryParam("len") final int len) throws IOException {
    final java.nio.file.Path path = Files.createTempFile("jetrs", null);
    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);

    final File file = path.toFile();
    assertEquals(len, file.length());
    file.deleteOnExit();
  }

  @PUT
  @Path("/upload/echo")
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.WILDCARD)
  public StreamingOutput uploadEcho(final InputStream in, @QueryParam("error") final boolean error) {
    return new StreamingOutput() {
      @Override
      public void write(final OutputStream output) throws IOException, WebApplicationException {
        if (error)
          throw new IOException();

        Streams.pipe(in, output);
      }
    };
  }
}