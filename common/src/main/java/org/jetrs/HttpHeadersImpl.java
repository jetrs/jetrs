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

package org.jetrs;

import static org.libj.lang.Assertions.*;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

import org.libj.lang.Numbers;
import org.libj.util.CollectionUtil;
import org.libj.util.MirrorMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link HttpHeadersMap} that implements the {@link HttpHeaders} interface, providing a mirrored multi-valued map of headers in
 * {@link String} and {@link Object} representations.
 */
class HttpHeadersImpl extends HttpHeadersMap<String,Object> implements HttpHeaders {
  private static final Logger logger = LoggerFactory.getLogger(HttpHeadersImpl.class);

  private static final List<Locale> WILDCARD_LOCALE = Arrays.asList(new Locale("*"));
  private static final List<MediaType> WILDCARD_ACCEPT = Arrays.asList(MediaType.WILDCARD_TYPE);

  static char[] getHeaderValueDelimiters(final String headerName) {
    final AbstractMap.SimpleEntry<HttpHeader<?>,HeaderDelegateImpl<?>> entry = HeaderDelegateImpl.lookup(headerName);
    return entry.getKey() != null ? entry.getKey().getDelimiters() : HttpHeader.none;
  }

  private static void parseHeaderValuesFromString(final List<String> values, final String headerValue, final char[] delimiters) {
    if (delimiters != null)
      parseMultiHeaderNoSort(values, headerValue, delimiters);
    else
      values.add(headerValue);
  }

  private static char checkDel(final char ch, final char[] delimiters, final char found) {
    if (found != '\0')
      return ch == found ? ch : '\0';

    for (int i = 0, i$ = delimiters.length; i < i$; ++i) // [A]
      if (ch == delimiters[i])
        return ch;

    return '\0';
  }

  static void parseMultiHeaderNoSort(final List<String> values, final String headerValue, final char ... delimiters) {
    char ch = '\0';
    char checkDel = '\0', foundDel = '\0';
    for (int i = 0, start = -1, end = -1, i$ = headerValue.length(); i <= i$; ++i) { // [N]
      if (i == i$ || (checkDel = checkDel(ch = headerValue.charAt(i), delimiters, foundDel)) != '\0') {
        if (foundDel == '\0')
          foundDel = checkDel;

        values.add(headerValue.substring(start + 1, end + 1));
        end = -1;
      }
      else if (ch != ' ') {
        end = i;
      }

      if (end == -1)
        start = i;
    }
  }

  /**
   * Creates a new {@link HttpHeadersImpl} with the specified map of headers as lists of strings.
   *
   * @param headers The map of headers as lists of strings.
   * @throws IllegalArgumentException If the specified map is null.
   */
  HttpHeadersImpl(final Map<String,List<String>> headers) {
    this();
    if (assertNotNull(headers).size() > 0)
      for (final Map.Entry<String,List<String>> entry : headers.entrySet()) // [S]
        addAll(entry.getKey(), entry.getValue());
  }

  /**
   * Creates a new {@link HttpHeadersImpl} with the specified map of headers as lists of objects.
   *
   * @param headers The map of headers as lists of objects.
   * @throws IllegalArgumentException If the specified map is null.
   */
  HttpHeadersImpl(final MultivaluedMap<String,Object> headers) {
    this();
    final MirrorMultivaluedMap<String,Object,String> mirrorMap = getMirrorMap();
    if (assertNotNull(headers).size() > 0)
      for (final Map.Entry<String,List<Object>> entry : headers.entrySet()) // [S]
      mirrorMap.addAll(entry.getKey(), entry.getValue());
  }

  /**
   * Creates a new {@link HttpHeadersImpl} with the specified {@link HttpServletRequest} as the source from which to initialize the
   * header values.
   *
   * @param request The {@link HttpServletRequest} from which to initialize the header values.
   */
  HttpHeadersImpl(final HttpServletRequest request) {
    this();
    if (request == null)
      return;

    final Enumeration<String> headerNames = request.getHeaderNames();
    if (headerNames == null)
      return;

    while (headerNames.hasMoreElements()) {
      final String headerName = headerNames.nextElement();
      final Enumeration<String> headerValues = request.getHeaders(headerName);
      if (headerValues == null || !headerValues.hasMoreElements())
        continue;

      final List<String> values = getValues(headerName);
      final char[] delimiters = getHeaderValueDelimiters(headerName);
      do {
        parseHeaderValuesFromString(values, headerValues.nextElement(), delimiters);
      }
      while (headerValues.hasMoreElements());
    }
  }

  /**
   * Creates a new {@link HttpHeadersImpl} with the specified {@link HttpServletResponse} as the source from which to initialize the
   * header values.
   *
   * @param response The {@link HttpServletResponse} from which to initialize the header values.
   */
  HttpHeadersImpl(final HttpServletResponse response) {
    this();
    if (response == null)
      return;

    final Collection<String> headerNames = response.getHeaderNames();
    if (headerNames == null || headerNames.size() == 0)
      return;

    for (final String headerName : headerNames) { // [C]
      final Collection<String> headerValues = response.getHeaders(headerName);
      if (headerValues != null && headerValues.size() > 0) {
        final List<String> values = getValues(headerName);
        final char[] delimiters = getHeaderValueDelimiters(headerName);
        for (final String headerValue : headerValues) // [C]
          parseHeaderValuesFromString(values, headerValue, delimiters);
      }
    }
  }

  /**
   * Creates an empty {@link HttpHeadersImpl}.
   */
  HttpHeadersImpl() {
    super(new MirrorMap.Mirror<String,String,Object>() {
      @Override
      public Object valueToReflection(final String key, final String value) {
        if (value == null)
          return null;

        final Object reflection = HeaderDelegateImpl.lookup(key).getValue().fromString(value);
        if (reflection == null)
          logger.warn("Got null reflection for header name: \"" + key + "\"");

        return reflection;
      }

      @Override
      public String reflectionToValue(final String key, final Object value) {
        return value == null ? null : HeaderDelegateImpl.lookup(key, value.getClass()).toString(value);
      }
    }, new MirrorQualityList.Qualifier<String,Object>() {
      @Override
      public long valueToQuality(final String reflection, final int index) {
        return HeaderUtil.getQualityFromString(reflection, index);
      }

      /**
       * Gets the quality attribute from a strongly typed header object (i.e. {@link MediaType#getParameters()
       * mediaType.getParameters().get("q")}), and returns a {@link org.libj.lang.Numbers.Composite composite} {@code long}
       * containing the {@code float} quality value and {@code int} ending index of the attribute in the string.
       *
       * @param value The object to parse.
       * @param index The index from which to start parsing (ignored).
       * @return A {@link org.libj.lang.Numbers.Composite composite} {@code long} containing the {@code float} quality value and
       *         {@code int} ending index of the attribute in the string.
       */
      @Override
      public long reflectionToQuality(final Object value, final int index) {
        if (value == null)
          return Numbers.Composite.encode(1f, index);

        if (value instanceof String || !(value instanceof Qualified))
          return HeaderUtil.getQualityFromString(value.toString(), index);

        final Float quality = ((Qualified)value).getQuality();
        return Numbers.Composite.encode(quality != null ? quality : 1f, index);
      }
    });
  }

  @Override
  public List<String> getRequestHeader(final String name) {
    return get(name);
  }

  @Override
  public String getHeaderString(final String name) {
    return CollectionUtil.toString(get(name), getHeaderValueDelimiters(name)[0]);
  }

  @Override
  public HttpHeadersImpl getRequestHeaders() {
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<MediaType> getAcceptableMediaTypes() {
    final MirrorQualityList<?,String> headers = getMirrorMap().get(HttpHeaders.ACCEPT);
    return headers == null || headers.size() == 0 ? WILDCARD_ACCEPT : (List<MediaType>)headers;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Locale> getAcceptableLanguages() {
    final MirrorQualityList<?,String> headers = getMirrorMap().get(HttpHeaders.ACCEPT_LANGUAGE);
    return headers == null || headers.size() == 0 ? WILDCARD_LOCALE : (List<Locale>)headers;
  }

  @Override
  public MediaType getMediaType() {
    return (MediaType)getMirrorMap().getFirst(HttpHeaders.CONTENT_TYPE);
  }

  void setMediaType(final MediaType mediaType) {
    if (mediaType != null)
      getMirrorMap().putSingle(HttpHeaders.CONTENT_TYPE, mediaType.getParameters().size() == 0 ? mediaType : MediaTypes.cloneWithoutParameters(mediaType));
    else
      remove(HttpHeaders.CONTENT_TYPE);
  }

  /**
   * FIXME: Is it correct to drop the parameters when setting the CONTENT_TYPE in the response?! If so, then what about
   * {@link ResponseBuilderImpl#header(String,Object)}.
   *
   * @param mediaType The {@link MediaType} string.
   */
  void setMediaType(final String mediaType) {
    final int i = mediaType.indexOf(';');
    putSingle(mediaType, i > -1 ? mediaType.substring(0, i) : mediaType);
  }

  @Override
  public Locale getLanguage() {
    return (Locale)getMirrorMap().getFirst(HttpHeaders.CONTENT_LANGUAGE);
  }

  @Override
  public Map<String,Cookie> getCookies() {
    final MirrorQualityList<?,String> cookies = getMirrorMap().get(HttpHeaders.COOKIE);
    if (cookies == null || cookies.size() == 0)
      return Collections.emptyMap();

    final Map<String,Cookie> map = new LinkedHashMap<>();
    if (cookies.isRandomAccess()) {
      for (int i = 0, i$ = cookies.size(); i < i$; ++i) { // [RA]
        final Cookie cookie = (Cookie)cookies.get(i);
        map.put(cookie.getName(), cookie);
      }
    }
    else {
      for (final Object obj : cookies) { // [L]
        final Cookie cookie = (Cookie)obj;
        map.put(cookie.getName(), cookie);
      }
    }

    return map;
  }

  @Override
  public Date getDate() {
    return (Date)getMirrorMap().getFirst(HttpHeaders.DATE);
  }

  @Override
  public int getLength() {
    return Numbers.parseInt(getFirst(HttpHeaders.CONTENT_LENGTH), -1);
  }

  /**
   * Returns the allowed HTTP methods from the {@code "Allow"} HTTP header, otherwise {@code null} if not present.
   *
   * @return The allowed HTTP methods from the {@code "Allow"} HTTP header, otherwise {@code null} if not present.
   */
  Set<String> getAllowedMethods() {
    return new HashSet<>(getRequestHeader(HttpHeaders.ALLOW));
  }

  /**
   * Returns last modified date from the {@code "Last-Modified"} HTTP header, otherwise {@code null} if not present.
   *
   * @return The last modified date from the {@code "Last-Modified"} HTTP header, otherwise {@code null} if not present.
   */
  Date getLastModified() {
    return (Date)getMirrorMap().getFirst(HttpHeaders.LAST_MODIFIED);
  }

  /**
   * Returns location from the {@code "Location"} HTTP header, otherwise {@code null} if not present.
   *
   * @return The location from the {@code "Location"} HTTP header, otherwise {@code null} if not present.
   */
  URI getLocation() {
    final String location = getFirst(HttpHeaders.LOCATION);
    return location == null ? null : URI.create(location);
  }

  /**
   * Returns a string representation of the list of header strings associated with the specified {@code headerName}, or {@code null}
   * if no value exists.
   *
   * @param headerName The name of the header.
   * @return A string representation of the list of header strings associated with the specified {@code header} name, or
   *         {@code null} if no value exists
   */
  String getString(final String headerName) {
    final List<String> values = get(headerName);
    if (values == null)
      return null;

    final int i$ = values.size();
    if (i$ == 0)
      return "";

    final char delimiter = getHeaderValueDelimiters(headerName)[0];
    final StringBuilder builder = new StringBuilder();
    if (CollectionUtil.isRandomAccess(values)) {
      for (int i = 0; i < i$; ++i) { // [RA]
        if (i > 0)
          builder.append(delimiter);

        builder.append(values.get(i));
      }
    }
    else {
      final Iterator<String> iterator = values.iterator();
      for (int i = 0; i < i$; ++i) { // [RA]
        if (i > 0)
          builder.append(delimiter);

        builder.append(iterator.next());
      }
    }

    return builder.toString();
  }

  @Override
  public HttpHeadersImpl clone() {
    return (HttpHeadersImpl)super.clone();
  }
}