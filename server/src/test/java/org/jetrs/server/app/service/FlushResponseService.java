package org.jetrs.server.app.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.libj.util.UnsynchronizedByteArrayOutputStream;

@Singleton
@Path("flush")
public class FlushResponseService {
  public static byte[] expand(final byte[] bytes, final int mul) {
    final byte[] out = new byte[bytes.length * mul];
    for (int i = 0; i < mul; ++i) // [N]
      System.arraycopy(bytes, 0, out, i * bytes.length, bytes.length);

    return out;
  }

  public static byte[] gzip(final byte[] bytes) throws IOException {
    final UnsynchronizedByteArrayOutputStream o = new UnsynchronizedByteArrayOutputStream();
    try (final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(o)) {
      gzipOutputStream.write(bytes);
    }

    return o.toByteArray();
  }

  @GET
  @Path("{mul:[\\d]+}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response get(final @PathParam("mul") int mul, final @QueryParam("d") String data, final @QueryParam("q") Boolean chunked, final @QueryParam("e") boolean exception) {
    final byte[] bytes = expand(data.getBytes(), mul);
    final Response.ResponseBuilder response = Response.ok(new StreamingOutput() {
      @Override
      public void write(final OutputStream output) throws IOException, WebApplicationException {
        final int len = bytes.length - 2;
        output.write(bytes, 0, len);
        if (exception)
          throw new IOException();

        output.write(bytes, len, bytes.length - len);
      }
    });

    if (chunked != null) {
      if (chunked)
        response.header(org.jetrs.HttpHeaders.TRANSFER_ENCODING, "chunked");
      else
        response.header(org.jetrs.HttpHeaders.CONTENT_LENGTH, bytes.length);
    }

    return response.build();
  }
}