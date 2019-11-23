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

package org.jetrs.common.core;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import org.jetrs.common.ext.delegate.CookieHeaderDelegate;
import org.jetrs.common.ext.delegate.DateHeaderDelegate;
import org.jetrs.common.util.MediaTypes;
import org.jetrs.common.util.MirrorMultivaluedMap;
import org.jetrs.common.util.Responses;
import org.libj.util.ArrayDoubleList;
import org.libj.util.CollectionUtil;
import org.libj.util.Locales;
import org.libj.util.Matched;
import org.libj.util.Numbers;

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
      return DateHeaderDelegate.parse(value);

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
      return CookieHeaderDelegate.parse(value.split(";"));

    if (HttpHeaders.DATE.equalsIgnoreCase(key))
      return DateHeaderDelegate.parse(value);

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
      return DateHeaderDelegate.parse(value);

    if (HttpHeaders.IF_NONE_MATCH.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.IF_UNMODIFIED_SINCE.equalsIgnoreCase(key))
      return DateHeaderDelegate.parse(value);

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

    if (HttpHeaders.EXPIRES.equalsIgnoreCase(key)) {
      if (Numbers.isNumber(value))
        return new Date(System.currentTimeMillis() + 1000 * Integer.parseInt(value));

      return DateHeaderDelegate.parse(value);
    }

    if ("IM".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.LAST_MODIFIED.equalsIgnoreCase(key))
      return DateHeaderDelegate.parse(value);

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
      return NewCookie.valueOf(value);

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
      return DateHeaderDelegate.parse(value);
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

  public HttpHeadersImpl(final Map<String,List<String>> headers) {
    this();
    for (final Map.Entry<String,List<String>> entry : headers.entrySet())
      addAll(entry.getKey(), entry.getValue());
  }

  public HttpHeadersImpl(final MultivaluedMap<String,Object> headers) {
    this();
    for (final Map.Entry<String,List<Object>> entry : headers.entrySet())
      mirroredMap.addAll(entry.getKey(), entry.getValue());
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
    super(HashMap::new, ArrayList::new, (key, value) -> {
      if (value == null)
        return null;

      final Object requestHeader = parseRequestHeader(key, value);
      if (requestHeader != null)
        return requestHeader;

      final Object responseHeader = parseResponseHeader(key, value);
      if (responseHeader != null)
        return responseHeader;

      return parseHeaderByType(value);
    }, (key, value) -> {
      if (value == null)
        return null;

      if (value instanceof String)
        return (String)value;

      if (value instanceof MediaType)
        return value.toString();

      if (value instanceof Locale)
        return value.toString();

      if (value instanceof Date)
        return DateHeaderDelegate.format((Date)value);

      if (value instanceof URI)
        return value.toString();

      if (value instanceof CacheControl)
        return value.toString();

      if (value instanceof NewCookie)
        return value.toString();

      // NOTE: It is assumed that the only Map in here is a Map of cookies
      if (value instanceof Map) {
        final StringBuilder builder = new StringBuilder();
        for (final Object cookie : ((Map<?,?>)value).values())
          builder.append(cookie).append(';');

        if (builder.length() > 0)
          builder.setLength(builder.length() - 1);

        return builder.toString();
      }

      throw new UnsupportedOperationException("Unsupported type: " + value.getClass());
    });
  }

  @Override
  public List<String> getRequestHeader(final String name) {
    return get(name);
  }

  @Override
  public String getHeaderString(final String name) {
    return CollectionUtil.toString(getRequestHeader(name), ",");
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

  private static final Locale WILDCARD_LOCALE = new Locale("*");

  private static Locale getLocale(final String value) {
    if (value == null)
      return null;

    final int dash = value.indexOf('-');
    if (dash == 0 || dash == value.length() - 1)
      throw new IllegalArgumentException("Illegal  locale: " + value);

    return dash == -1 ? new Locale(value) : new Locale(value.substring(0, dash), value.substring(dash + 1));
  }

  @Override
  public List<Locale> getAcceptableLanguages() {
    final List<String> acceptLanguages = getRequestHeader(HttpHeaders.ACCEPT_LANGUAGE);
    if (acceptLanguages == null || acceptLanguages.isEmpty())
      return Collections.singletonList(WILDCARD_LOCALE);

    final List<Locale> languages = new ArrayList<>();
    final ArrayDoubleList qualities = new ArrayDoubleList();
    for (final String accepterLanguage : acceptLanguages) {
      final int sc = accepterLanguage.indexOf(';');

      final double quality;
      final String language;
      if (sc > 0) {
        language = accepterLanguage.substring(0, sc).trim();
        final int len = accepterLanguage.length();
        boolean qSeen = false;
        int i = sc + 1;
        for (char ch; i < len; ++i) {
          ch = accepterLanguage.charAt(i);
          if (ch == 'q')
            qSeen = true;
          else if (ch == '=')
            break;
          else if (ch != ' ')
            throw new IllegalArgumentException("Illegal language: " + accepterLanguage);
        }

        quality = qSeen ? Numbers.parseDouble(accepterLanguage.substring(i + 1).trim(), 1d) : 1d;
      }
      else {
        language = accepterLanguage;
        quality = 1d;
      }

      languages.add(getLocale(language));
      qualities.add(1 - quality);
    }

    Matched.sort(languages, qualities);
    return languages;
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
    final List<String> cookies = get(HttpHeaders.COOKIE);
    return cookies == null || cookies.size() == 0 ? null : CookieHeaderDelegate.parse(cookies.toArray(new String[cookies.size()]));
  }

  @Override
  public Date getDate() {
    final String date = getFirst(HttpHeaders.DATE);
    return date == null ? null : DateHeaderDelegate.parse(date);
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
    return lastModified == null ? null : DateHeaderDelegate.parse(lastModified);
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