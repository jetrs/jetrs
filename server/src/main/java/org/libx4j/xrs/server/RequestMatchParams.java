/* Copyright (c) 2017 lib4j
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

package org.libx4j.xrs.server;

import java.util.Arrays;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.libx4j.xrs.server.util.MediaTypes;

public class RequestMatchParams {
  public static RequestMatchParams forContext(final ContainerRequestContext containerRequestContext) {
    final MediaType[] accept = MediaTypes.parse(containerRequestContext.getHeaders().get(HttpHeaders.ACCEPT));
    final MediaType[] contentType = MediaTypes.parse(containerRequestContext.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    return new RequestMatchParams(containerRequestContext.getMethod(), containerRequestContext.getUriInfo().getPath(), accept == null ? null : accept, contentType);
  }

  private final String method;
  private final String path;
  private final MediaType[] accept;
  private final MediaType[] contentType;

  public RequestMatchParams(final String method, final String path, final MediaType[] accept, final MediaType[] contentType) {
    this.method = method.toUpperCase();
    this.path = path;
    this.accept = accept;
    this.contentType = contentType;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public MediaType[] getAccept() {
    return accept;
  }

  public MediaType[] getContentType() {
    return contentType;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof RequestMatchParams))
      return false;

    final RequestMatchParams that = (RequestMatchParams)obj;
    return method.equals(that.method) && path.equals(that.path) && (accept != null ? that.accept != null && Arrays.equals(accept, that.accept) : that.accept == null) && (contentType != null ? that.contentType != null && Arrays.equals(contentType, that.contentType) : contentType == null);
  }

  @Override
  public int hashCode() {
    int hashCode = 9;
    hashCode ^= 31 * method.hashCode();
    hashCode ^= 31 * path.hashCode();
    if (accept != null)
      hashCode ^= 31 * accept.hashCode();

    if (contentType != null)
      hashCode ^= 31 * contentType.hashCode();

    return hashCode;
  }
}