/* Copyright (c) 2016 lib4j
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

package org.libx4j.xrs.server.core;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public class ResponseImpl extends Response {
  private final Response.Status status;
  private final HeaderMap headers;
  private final Object entity;
  private boolean closed;

  // FIXME: annotations are not being used.. there's no API to get them out of this class
  public ResponseImpl(final Response.Status status, final HeaderMap headers, final Object entity, final Annotation[] annotations) {
    this.status = status;
    this.headers = headers;
    this.entity = entity;
  }

  public ResponseImpl(final Response.Status status, final HeaderMap headers) {
    this(status, headers, null, null);
  }

  @Override
  public int getStatus() {
    return status.getStatusCode();
  }

  @Override
  public StatusType getStatusInfo() {
    return status;
  }

  @Override
  public Object getEntity() {
    if (closed)
      throw new IllegalStateException("response has been closed");

    return entity;
  }

  @Override
  public <T>T readEntity(final Class<T> entityType) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public <T>T readEntity(final GenericType<T> entityType) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public <T>T readEntity(final Class<T> entityType, final Annotation[] annotations) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public <T>T readEntity(final GenericType<T> entityType, final Annotation[] annotations) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEntity() {
    if (closed)
      throw new IllegalStateException("response has been closed");

    return entity != null;
  }

  @Override
  public boolean bufferEntity() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    this.closed = true;
  }

  @Override
  public MediaType getMediaType() {
    return (MediaType)getMetadata().getFirst(HttpHeaders.CONTENT_TYPE);
  }

  @Override
  public Locale getLanguage() {
    return headers.getLanguage();
  }

  @Override
  public int getLength() {
    return headers.getLength();
  }

  @Override
  public Set<String> getAllowedMethods() {
    return headers.getAllowedMethods();
  }

  @Override
  public Map<String,NewCookie> getCookies() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public EntityTag getEntityTag() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getDate() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getLastModified() {
    return headers.getLastModified();
  }

  @Override
  public URI getLocation() {
    return headers.getLocation();
  }

  @Override
  public Set<Link> getLinks() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasLink(final String relation) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Link getLink(final String relation) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Builder getLinkBuilder(final String relation) {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String,Object> getMetadata() {
    return headers.getMirroredMap();
  }

  @Override
  public MultivaluedMap<String,String> getStringHeaders() {
    return headers;
  }

  @Override
  public String getHeaderString(final String name) {
    return headers.getString(name);
  }
}