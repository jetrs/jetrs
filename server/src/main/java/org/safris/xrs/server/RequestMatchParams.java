/* Copyright (c) 2017 Seva Safris
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

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.safris.commons.util.Collections;
import org.safris.xrs.server.util.MediaTypes;

public class RequestMatchParams {
  public static RequestMatchParams forContext(final ContainerRequestContext containerRequestContext) {
    final MediaType[] accept = MediaTypes.parse(containerRequestContext.getHeaders().get(HttpHeaders.ACCEPT));
    final MediaType[] contentType = MediaTypes.parse(containerRequestContext.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    return new RequestMatchParams(containerRequestContext.getMethod(), containerRequestContext.getUriInfo().getPath(), accept == null ? null : Collections.asCollection(HashSet.class, accept), contentType == null ? null : Collections.asCollection(HashSet.class, contentType));
  }

  private final String method;
  private final String path;
  private final Set<MediaType> accept;
  private final Set<MediaType> contentType;

  public RequestMatchParams(final String method, final String path, final Set<MediaType> accept, final Set<MediaType> contentType) {
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

  public Set<MediaType> getAccept() {
    return accept;
  }

  public Set<MediaType> getContentType() {
    return contentType;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof RequestMatchParams))
      return false;

    final RequestMatchParams that = (RequestMatchParams)obj;
    return method.equals(that.method) && path.equals(that.path) && (accept != null ? that.accept != null && accept.containsAll(that.accept) : that.accept == null) && (contentType != null ? that.contentType != null && contentType.containsAll(that.contentType) : contentType == null);
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