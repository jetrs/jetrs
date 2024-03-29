/* Copyright (c) 2018 JetRS
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

package org.jetrs.provider.ext.mapper;

import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.openjax.json.JsonUtil;

@Singleton
public class ThrowableMapper<T extends Throwable> implements ExceptionMapper<T> {
  private final boolean verbose;

  public ThrowableMapper(final boolean verbose) {
    this.verbose = verbose;
  }

  public ThrowableMapper() {
    this(true);
  }

  @Override
  public Response toResponse(final T exception) {
    return toResponse(exception, Response.serverError().build());
  }

  protected Response toResponse(final T exception, final Response response) {
    if (response.hasEntity())
      return response;

    final int status = response.getStatus();
    final StringBuilder builder = new StringBuilder("{\"status\":").append(status);
    if (verbose) {
      final String message = exception.getMessage();
      if (message != null) {
        builder.append(",\"message\":\"");
        final String prefix = "HTTP " + status + " ";
        JsonUtil.escape(builder, message.startsWith(prefix) ? message.substring(prefix.length()) : message);
        builder.append('"');
      }
    }

    return Response
      .fromResponse(response)
      .entity(builder.append('}').toString())
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
      .build();
  }
}