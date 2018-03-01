package org.libx4j.xrs.server.ext;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
  @Override
  public Response toResponse(final WebApplicationException exception) {
    final StringBuilder builder = new StringBuilder("{\"status\":");
    builder.append(exception.getResponse().getStatus());

    final String message = exception.getMessage();
    if (message != null) {
      final String prefix = "HTTP " + exception.getResponse().getStatus() + " ";
      builder.append(",\"message\":\"");
      builder.append((message.startsWith(prefix) ? message.substring(prefix.length()) : message));
      builder.append('"');
    }

    builder.append('}');
    return Response.fromResponse(exception.getResponse()).entity(builder.toString()).build();
  }
}