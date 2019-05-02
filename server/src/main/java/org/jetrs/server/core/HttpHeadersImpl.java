/* Copyright (c) 2016 JetRS
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

package org.jetrs.server.core;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jetrs.server.ext.DateHeaderDelegateImpl;
import org.jetrs.server.util.MediaTypes;
import org.jetrs.server.util.MirrorMultivaluedMap;
import org.jetrs.server.util.Responses;
import org.openjax.util.FastCollections;
import org.openjax.util.Locales;
import org.openjax.util.Numbers;

public class HttpHeadersImpl extends MirrorMultivaluedMap<String,String,Object> implements HttpHeaders {
  private static final long serialVersionUID = -5881222274060155846L;

  // https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
  private static Object parseRequestHeader(final String key, final String value) {
    // Standard Headers...
    if (HttpHeaders.ACCEPT.equalsIgnoreCase(key)) {
      try {
        return MediaTypes.parse(value);
      }
      catch (final ParseException e) {
        throw new IllegalArgumentException(e);
      }
    }

    if (HttpHeaders.ACCEPT_CHARSET.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.ACCEPT_ENCODING.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.ACCEPT_LANGUAGE.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Accept-Datetime".equalsIgnoreCase(key))
      return DateHeaderDelegateImpl.parse(value);

    if ("Access-Control-Request-Method".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.CACHE_CONTROL.equalsIgnoreCase(key))
      return CacheControl.valueOf(value);

    if ("Connection".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(key))
      return Integer.valueOf(value);

    if ("Content-MD5".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(key)) {
      try {
        return MediaTypes.parse(value);
      }
      catch (final ParseException e) {
        throw new IllegalArgumentException(e);
      }
    }

    if (HttpHeaders.COOKIE.equalsIgnoreCase(key))
      return Cookie.valueOf(value);

    if (HttpHeaders.DATE.equalsIgnoreCase(key))
      return DateHeaderDelegateImpl.parse(value);

    if ("Expect".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Forwarded".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("From".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.HOST.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("HTTP2-Settings".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.IF_MATCH.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.IF_MODIFIED_SINCE.equalsIgnoreCase(key))
      return DateHeaderDelegateImpl.parse(value);

    if (HttpHeaders.IF_NONE_MATCH.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.IF_UNMODIFIED_SINCE.equalsIgnoreCase(key))
      return DateHeaderDelegateImpl.parse(value);

    if ("Max-Forwards".equalsIgnoreCase(key))
      return Integer.valueOf(value);

    if ("Origin".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Pragma".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Proxy-Authorization".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Range".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Referrer".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("TE".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.USER_AGENT.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Upgrade".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Via".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Warning".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    // Non-standard headers...

    if ("Upgrade-Insecure-Requests".equalsIgnoreCase(key))
      return Integer.parseInt(value);

    if ("X-Requested-With".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("DNT".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Forwarded-For".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Forwarded-Host".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Forwarded-Proto".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Front-End-Https".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Http-Method-Override".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-ATT-DeviceId".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Wap-Profile".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Proxy-Connection".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-UIDH".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Csrf-Token".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Request-ID".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Correlation-ID".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Save-Data".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    return null;
  }

  private static Object parseResponseHeader(final String key, final String value) {
    if ("Access-Control-Allow-Origin".equalsIgnoreCase(key) || "Access-Control-Allow-Credentials".equalsIgnoreCase(key) || "Access-Control-Expose-Headers".equalsIgnoreCase(key) || "Access-Control-Max-Age".equalsIgnoreCase(key) || "Access-Control-Allow-Methods".equalsIgnoreCase(key) || "Access-Control-Allow-Headers".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Accept-Patch".equalsIgnoreCase(key)) {
      try {
        return MediaTypes.parse(value);
      }
      catch (final ParseException e) {
        throw new IllegalArgumentException(e);
      }
    }

    if ("Accept-Ranges".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Age".equalsIgnoreCase(key))
      return Integer.parseInt(value);

    if (HttpHeaders.ALLOW.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Alt-Svc".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.CONTENT_DISPOSITION.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.CONTENT_LANGUAGE.equalsIgnoreCase(key))
      return Locales.parse(value);

    if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(key))
      return Integer.parseInt(value);

    if (HttpHeaders.CONTENT_LOCATION.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Content-Range".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Delta-Base".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.ETAG.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.EXPIRES.equalsIgnoreCase(key))
      return DateHeaderDelegateImpl.parse(value);

    if ("IM".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.LAST_MODIFIED.equalsIgnoreCase(key))
      return DateHeaderDelegateImpl.parse(value);

    if (HttpHeaders.LINK.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.LOCATION.equalsIgnoreCase(key))
      return URI.create(value);

    if ("P3P".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Proxy-Authenticate".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Public-Key-Pins".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.RETRY_AFTER.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Server".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(key))
      return Cookie.valueOf(value);

    if ("Strict-Transport-Security".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Trailer".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Transfer-Encoding".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Tk".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.VARY.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.WWW_AUTHENTICATE.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Frame-Options".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Content-Security-Policy".equalsIgnoreCase(key) || "X-Content-Security-Policy".equalsIgnoreCase(key) || "X-WebKit-CSP".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Refresh".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Status".equalsIgnoreCase(key))
      return Responses.from(value);

    if ("Timing-Allow-Origin".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Content-Duration".equalsIgnoreCase(key))
      return new BigDecimal(value);

    if ("X-Content-Type-Options".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Powered-By".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-Request-ID".equalsIgnoreCase(key) || "X-Correlation-ID".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-UA-Compatible".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("X-XSS-Protection".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    return null;
  }

  private static Object parseHeaderByType(final String value) {
    if (value == null)
      return null;

    try {
      return DateHeaderDelegateImpl.parse(value);
    }
    catch (final DateTimeParseException e) {
    }

    try {
      return MediaType.valueOf(value);
    }
    catch (final IllegalArgumentException e) {
    }

    try {
      return CacheControl.valueOf(value);
    }
    catch (final IllegalArgumentException e) {
    }

    final Locale locale = Locales.parse(value);
    if (Locales.isIso(locale))
      return locale;

    try {
      return new URI(value);
    }
    catch (final URISyntaxException e) {
    }

    return value;
  }

  public HttpHeadersImpl(final HttpServletRequest request) {
    this();
    if (request != null) {
      final Enumeration<String> headerNames = request.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        final String headerName = headerNames.nextElement();
        final Enumeration<String> enumeration = request.getHeaders(headerName);
        while (enumeration.hasMoreElements())
          add(headerName, enumeration.nextElement());
      }
    }
  }

  public HttpHeadersImpl(final HttpServletResponse response) {
    this();
    if (response != null)
      for (final String header : response.getHeaders(HttpHeaders.ALLOW))
        add(HttpHeaders.ALLOW, header);
  }

  public HttpHeadersImpl() {
    super(HashMap::new, ArrayList::new, new BiFunction<String,String,Object>() {
      @Override
      public Object apply(final String key, final String value) {
        if (value == null)
          return null;

        final Object requestHeader = parseRequestHeader(key, value);
        if (requestHeader != null)
          return requestHeader;

        final Object responseHeader = parseResponseHeader(key, value);
        if (responseHeader != null)
          return responseHeader;

        return parseHeaderByType(value);
      }
    }, new BiFunction<String,Object,String>() {
      @Override
      public String apply(final String key, final Object value) {
        if (value == null)
          return null;

        if (value instanceof String)
          return (String)value;

        if (value instanceof MediaType)
          return value.toString();

        if (value instanceof Locale)
          return value.toString();

        if (value instanceof Date)
          return DateHeaderDelegateImpl.format((Date)value);

        if (value instanceof URI)
          return value.toString();

        if (value instanceof CacheControl)
          return value.toString();

        throw new UnsupportedOperationException("Unsupported type: " + value.getClass());
      }
    });
  }

  @Override
  public List<String> getRequestHeader(final String name) {
    return get(name);
  }

  @Override
  public String getHeaderString(final String name) {
    return FastCollections.toString(getRequestHeader(name), ",");
  }

  @Override
  public MultivaluedMap<String,String> getRequestHeaders() {
    return this;
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
    try {
      return MediaTypes.parse(getFirst(HttpHeaders.CONTENT_TYPE));
    }
    catch (final ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Locale getLanguage() {
    final String language = getFirst(HttpHeaders.CONTENT_LANGUAGE);
    return language == null ? null : Locales.parse(language);
  }

  @Override
  public Map<String,Cookie> getCookies() {
    // TODO:
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getDate() {
    final String date = getFirst(HttpHeaders.DATE);
    return date == null ? null : DateHeaderDelegateImpl.parse(date);
  }

  @Override
  public int getLength() {
    final String contentLength = getFirst(HttpHeaders.CONTENT_LENGTH);
    return Numbers.isNumber(contentLength) ? Integer.parseInt(contentLength) : null;
  }

  public Set<String> getAllowedMethods() {
    return new HashSet<>(get(HttpHeaders.ALLOW));
  }

  public Date getLastModified() {
    final String lastModified = getFirst(HttpHeaders.LAST_MODIFIED);
    return lastModified == null ? null : DateHeaderDelegateImpl.parse(lastModified);
  }

  public URI getLocation() {
    final String location = getFirst(HttpHeaders.LOCATION);
    return location == null ? null : URI.create(location);
  }

  public String getString(final String header) {
    final List<String> values = get(header);
    if (values == null)
      return null;

    if (values.size() == 0)
      return "";

    final StringBuilder builder = new StringBuilder();
    for (final String value : values)
      builder.append(',').append(value);

    return builder.substring(1);
  }

  @Override
  public HttpHeadersImpl clone() {
    return (HttpHeadersImpl)super.clone();
  }
}