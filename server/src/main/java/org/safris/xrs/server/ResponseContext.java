/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

public class ResponseContext {
  private final HttpHeaders httpHeaders;
  private final HttpServletResponse httpServletResponse;
  private final ContainerResponseContext containerResponseContext;

  public ResponseContext(final HttpHeaders httpHeaders, final HttpServletResponse httpServletResponse, final ContainerResponseContext containerResponseContext) {
    this.httpHeaders = httpHeaders;
    this.httpServletResponse = httpServletResponse;
    this.containerResponseContext = containerResponseContext;
  }

  private Response response;

  public Response getResponse() {
    return response;
  }

  public void setResponse(final Response response) {
    this.response = response;
  }

  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  public void writeHeader() {
    final MultivaluedMap<String,String> containerResponseHeaders = containerResponseContext.getStringHeaders();
    if (getResponse() != null) {
      if (getResponse().hasEntity())
        containerResponseContext.setEntity(getResponse().getEntity());

      containerResponseContext.setStatusInfo(getResponse().getStatusInfo());

      final MultivaluedMap<String,String> responseHeaders = getResponse().getStringHeaders();
      for (final Map.Entry<String,List<String>> entry : responseHeaders.entrySet())
        for (final String header : entry.getValue())
          containerResponseHeaders.add(entry.getKey(), header);
    }

    for (final Map.Entry<String,List<String>> entry : containerResponseHeaders.entrySet())
      for (final String header : entry.getValue())
        httpServletResponse.addHeader(entry.getKey(), header);

    httpServletResponse.setStatus(containerResponseContext.getStatus());
  }

  private ByteArrayOutputStream outputStream = null;

  private ByteArrayOutputStream getOutputStream() {
    if (outputStream == null)
      outputStream = new ByteArrayOutputStream();

    return outputStream;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void writeBody(final EntityProviders entityProviders) throws IOException {
    final Object entity = containerResponseContext.getEntity();
    if (entity != null) {
      final MessageBodyWriter messageBodyWriter = entityProviders.getWriter(containerResponseContext.getMediaType(), entity.getClass());
      if (messageBodyWriter != null) {
        messageBodyWriter.writeTo(entity, entity.getClass(), entity.getClass().getGenericSuperclass(), entity.getClass().getAnnotations(), httpHeaders.getMediaType(), httpHeaders.getRequestHeaders(), getOutputStream());
      }
      else {
        throw new WebApplicationException("Could not find MessageBodyWriter for type: " + entity.getClass().getName());
      }
    }
  }

  public void commit() throws IOException {
    if (outputStream != null)
      httpServletResponse.getOutputStream().write(outputStream.toByteArray());

    // @see ServletResponse#getOutputStream :: "Calling flush() on the ServletOutputStream commits the response."
    httpServletResponse.getOutputStream().flush();
  }
}