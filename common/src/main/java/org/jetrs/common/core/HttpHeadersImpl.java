/* Copyright (c) 2019 JetRS
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

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jetrs.common.ext.delegate.CookieHeaderDelegate;
import org.jetrs.common.ext.delegate.DateHeaderDelegate;
import org.jetrs.common.util.HttpHeadersMap;
import org.jetrs.common.util.MirrorMultivaluedMap;
import org.jetrs.common.util.MirrorQualityList;
import org.libj.lang.Numbers;
import org.libj.util.CollectionUtil;
import org.libj.util.Locales;
import org.libj.util.MirrorMap;

/**
 * A {@link HttpHeadersMap} that implements the {@link HttpHeaders} interface,
 * providing a mirrored multi-valued map of headers in {@link String} and
 * {@link Object} representations.
 */
public class HttpHeadersImpl extends HttpHeadersMap<String,String,Object> implements HttpHeaders {
  private static final List<Locale> WILDCARD_LOCALE = Collections.unmodifiableList(Collections.singletonList(new Locale("*")));
  private static final List<MediaType> WILDCARD_ACCEPT = Collections.unmodifiableList(Collections.singletonList(MediaType.WILDCARD_TYPE));

  /**
   * Creates a new {@link HttpHeadersImpl} with the specified map of headers as
   * lists of strings.
   *
   * @param headers The map of headers as lists of strings.
   * @throws NullPointerException If the specified map is null.
   */
  public HttpHeadersImpl(final Map<String,List<String>> headers) {
    this();
    for (final Map.Entry<String,List<String>> entry : headers.entrySet())
      addAll(entry.getKey(), entry.getValue());
  }

  /**
   * Creates a new {@link HttpHeadersImpl} with the specified map of headers as
   * lists of objects.
   *
   * @param headers The map of headers as lists of objects.
   * @throws NullPointerException If the specified map is null.
   */
  public HttpHeadersImpl(final MultivaluedMap<String,Object> headers) {
    this();
    final MirrorMultivaluedMap<String,Object,String> mirrorMap = getMirrorMap();
    for (final Map.Entry<String,List<Object>> entry : headers.entrySet())
      mirrorMap.addAll(entry.getKey(), entry.getValue());
  }

  /**
   * Creates a new {@link HttpHeadersImpl} with the specified
   * {@link HttpServletRequest} as the source from which to initialize the
   * header values.
   *
   * @param request The {@link HttpServletRequest} from which to initialize the
   *          header values.
   */
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

  /**
   * Creates a new {@link HttpHeadersImpl} with the specified
   * {@link HttpServletResponse} as the source from which to initialize the
   * header values.
   *
   * @param response The {@link HttpServletResponse} from which to initialize
   *          the header values.
   */
  public HttpHeadersImpl(final HttpServletResponse response) {
    this();
    if (response != null)
      for (final String header : response.getHeaders(HttpHeaders.ALLOW))
        add(HttpHeaders.ALLOW, header);
  }

  /**
   * Creates an empty {@link HttpHeadersImpl}.
   */
  public HttpHeadersImpl() {
    super(new MirrorMap.Mirror<String,String,Object>() {
      @Override
      public Object valueToReflection(final String key, final String value) {
        return HttpHeadersUtil.fromString(key, value, false);
      }

      @Override
      public String reflectionToValue(final String key, final Object value) {
        return HttpHeadersUtil.toString(value);
      }
    }, HttpHeadersUtil.qualifier);
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
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<MediaType> getAcceptableMediaTypes() {
    final List header = getMirrorMap().get(HttpHeaders.ACCEPT);
    return header == null || header.size() == 0 ? WILDCARD_ACCEPT : Collections.unmodifiableList(header);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public List<Locale> getAcceptableLanguages() {
    // FIXME: This is a lot of processing to collate all "Accept-Language" headers.
    // FIXME: Need to add a `isDirty` flag to see if this work needs to be redone.
    final MirrorQualityList<?,String> headers = getMirrorMap().get(HttpHeaders.ACCEPT_LANGUAGE);
    if (headers == null || headers.size() == 0)
      return WILDCARD_LOCALE;

    final MirrorQualityList<?,String> all = ((MirrorQualityList<?,String>)headers.get(0)).clone();
    for (int i = 1; i < headers.size(); ++i) {
      all.addAll((MirrorQualityList)headers.get(i));
    }

    return (List<Locale>)all;
  }

  @Override
  public MediaType getMediaType() {
    return (MediaType)getMirrorMap().getFirst(HttpHeaders.CONTENT_TYPE);
  }

  @Override
  public Locale getLanguage() {
    final String language = getFirst(HttpHeaders.CONTENT_LANGUAGE);
    return language == null ? null : Locales.parse(language);
  }

  @Override
  public Map<String,Cookie> getCookies() {
    final List<String> cookies = getRequestHeader(HttpHeaders.COOKIE);
    return cookies == null || cookies.size() == 0 ? null : CookieHeaderDelegate.parse(cookies.toArray(new String[cookies.size()]));
  }

  @Override
  public Date getDate() {
    final String date = getFirst(HttpHeaders.DATE);
    return date == null ? null : DateHeaderDelegate.parse(date);
  }

  @Override
  public int getLength() {
    return Numbers.parseInt(getFirst(HttpHeaders.CONTENT_LENGTH), -1);
  }

  /**
   * Returns the allowed HTTP methods from the {@code "Allow"} HTTP header,
   * otherwise {@code null} if not present.
   *
   * @return The allowed HTTP methods from the {@code "Allow"} HTTP header,
   *         otherwise {@code null} if not present.
   */
  public Set<String> getAllowedMethods() {
    return new HashSet<>(getRequestHeader(HttpHeaders.ALLOW));
  }

  /**
   * Returns last modified date from the {@code "Last-Modified"} HTTP header,
   * otherwise {@code null} if not present.
   *
   * @return The last modified date from the {@code "Last-Modified"} HTTP header,
   *         otherwise {@code null} if not present.
   */
  public Date getLastModified() {
    final String lastModified = getFirst(HttpHeaders.LAST_MODIFIED);
    return lastModified == null ? null : DateHeaderDelegate.parse(lastModified);
  }

  /**
   * Returns location from the {@code "Location"} HTTP header, otherwise
   * {@code null} if not present.
   *
   * @return The location from the {@code "Location"} HTTP header, otherwise
   *         {@code null} if not present.
   */
  public URI getLocation() {
    final String location = getFirst(HttpHeaders.LOCATION);
    return location == null ? null : URI.create(location);
  }

  /**
   * Returns a string representation of the list of header strings associated
   * with the specified {@code header} name, or {@code null} if no value exists.
   *
   * @param header The name of the header.
   * @return A string representation of the list of header strings associated
   *         with the specified {@code header} name, or {@code null} if no value
   *         exists
   */
  String getString(final String header) {
    final List<String> values = get(header);
    if (values == null)
      return null;

    if (values.size() == 0)
      return "";

    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.size(); ++i) {
      if (i > 0)
        builder.append(',');

      builder.append(values.get(i));
    }

    return builder.toString();
  }

  @Override
  public HttpHeadersImpl clone() {
    return (HttpHeadersImpl)super.clone();
  }
}