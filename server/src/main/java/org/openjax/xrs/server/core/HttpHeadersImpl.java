/* Copyright (c) 2016 OpenJAX
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

package org.openjax.xrs.server.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.fastjax.util.FastCollections;
import org.fastjax.util.Numbers;
import org.openjax.xrs.server.util.HttpServletRequestUtil;
import org.openjax.xrs.server.util.MediaTypes;

public class HttpHeadersImpl implements HttpHeaders {
  private final MultivaluedMap<String,String> headers;

  public HttpHeadersImpl(final MultivaluedMap<String,String> headers) {
    this.headers = headers;
  }

  public HttpHeadersImpl(final HttpServletRequest request) {
    this(HttpServletRequestUtil.getHeaders(request));
  }

  @Override
  public List<String> getRequestHeader(final String name) {
    return headers.get(name);
  }

  @Override
  public String getHeaderString(final String name) {
    return FastCollections.toString(getRequestHeader(name), ",");
  }

  @Override
  public MultivaluedMap<String,String> getRequestHeaders() {
    return headers;
  }

  @Override
  public List<MediaType> getAcceptableMediaTypes() {
    final List<String> accepts = getRequestHeader(HttpHeaders.ACCEPT);
    if (accepts == null)
      return java.util.Collections.unmodifiableList(java.util.Collections.singletonList(MediaType.WILDCARD_TYPE));

    final List<MediaType> mediaTypes = new ArrayList<>();
    // FIXME: MediaType.valueOf(), subtype, charset
    for (final String accept : accepts)
      mediaTypes.add(new MediaType(accept, null));

    return mediaTypes;
  }

  @Override
  public List<Locale> getAcceptableLanguages() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public MediaType getMediaType() {
    return MediaTypes.parse(headers.getFirst(HttpHeaders.CONTENT_TYPE));
  }

  @Override
  public Locale getLanguage() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String,Cookie> getCookies() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getDate() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public int getLength() {
    final String contentLength = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
    return contentLength != null && Numbers.isNumber(contentLength) ? Integer.parseInt(contentLength) : null;
  }
}