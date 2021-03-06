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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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

import org.jetrs.common.util.HttpHeadersMap;
import org.jetrs.common.util.MirrorMultivaluedMap;
import org.jetrs.common.util.MirrorQualityList;
import org.jetrs.provider.ext.header.Delegate;
import org.jetrs.provider.ext.header.HeaderUtil;
import org.jetrs.provider.ext.header.Qualified;
import org.libj.lang.Numbers;
import org.libj.util.CollectionUtil;
import org.libj.util.MirrorMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link HttpHeadersMap} that implements the {@link HttpHeaders} interface,
 * providing a mirrored multi-valued map of headers in {@link String} and
 * {@link Object} representations.
 */
public class HttpHeadersImpl extends HttpHeadersMap<String,Object> implements HttpHeaders {
  private static final Logger logger = LoggerFactory.getLogger(HttpHeadersImpl.class);

  private static final List<Locale> WILDCARD_LOCALE = Arrays.asList(new Locale("*"));
  private static final List<MediaType> WILDCARD_ACCEPT = Arrays.asList(MediaType.WILDCARD_TYPE);
  private static final HashMap<String,char[]> headerWithNonCommaDelimiters = new HashMap<>();
  private static final char[] comma = {','};
  private static final char[] semiComma = {';', ','};

  static {
    headerWithNonCommaDelimiters.put(HttpHeaders.COOKIE, semiComma);
  }

  public static char[] getHeaderValueDelimiters(final String headerName) {
    final char[] delimiters = headerWithNonCommaDelimiters.get(headerName);
    return delimiters != null ? delimiters : comma;
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

    for (int i = 0; i < delimiters.length; ++i)
      if (ch == delimiters[i])
        return ch;

    return '\0';
  }

  static void parseMultiHeaderNoSort(final List<String> values, final String headerValue, final char ... delimiters) {
    char ch = '\0';
    char checkDel = '\0', foundDel = '\0';
    for (int i = 0, start = -1, end = -1, len = headerValue.length(); i <= len; ++i) {
      if (i == len || (checkDel = checkDel(ch = headerValue.charAt(i), delimiters, foundDel)) != '\0') {
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
   * Creates a new {@link HttpHeadersImpl} with the specified
   * {@link HttpServletResponse} as the source from which to initialize the
   * header values.
   *
   * @param response The {@link HttpServletResponse} from which to initialize
   *          the header values.
   */
  public HttpHeadersImpl(final HttpServletResponse response) {
    this();
    if (response == null)
      return;

    final Collection<String> headerNames = response.getHeaderNames();
    if (headerNames == null)
      return;

    for (final String headerName : headerNames) {
      final Collection<String> headerValues = response.getHeaders(headerName);
      if (headerValues == null || headerValues.size() == 0)
        continue;

      final List<String> values = getValues(headerName);
      final char[] delimiters = getHeaderValueDelimiters(headerName);
      for (final String headerValue : headerValues)
        parseHeaderValuesFromString(values, headerValue, delimiters);
    }
  }

  /**
   * Creates an empty {@link HttpHeadersImpl}.
   */
  public HttpHeadersImpl() {
    super(new MirrorMap.Mirror<String,String,Object>() {
      @Override
      public Object valueToReflection(final String key, final String value) {
        if (value == null)
          return null;

        final Object reflection = Delegate.lookup(key).fromString(value);
        if (reflection == null)
          logger.warn("Got null reflection for header name: \"" + key + "\"");

        return reflection;
      }

      @Override
      public String reflectionToValue(final String key, final Object value) {
        return value == null ? null : Delegate.lookup(key, value.getClass()).toString(value);
      }
    }, new MirrorQualityList.Qualifier<String,Object>() {
      @Override
      public long valueToQuality(final String reflection, final int index) {
        return HeaderUtil.getQualityFromString(reflection, index);
      }

      /**
       * Gets the quality attribute from a strongly typed header object (i.e.
       * {@link MediaType#getParameters() mediaType.getParameters().get("q")}),
       * and returns a {@link org.libj.lang.Numbers.Compound compound}
       * {@code long} containing the {@code float} quality value and {@code int}
       * ending index of the attribute in the string.
       *
       * @param value The object to parse.
       * @param index The index from which to start parsing (ignored).
       * @return A {@link org.libj.lang.Numbers.Compound compound} {@code long}
       *         containing the {@code float} quality value and {@code int}
       *         ending index of the attribute in the string.
       */
      @Override
      public long reflectionToQuality(final Object value, final int index) {
        if (value == null)
          return Numbers.Compound.encode(1f, index);

        if (value instanceof String || !(value instanceof Qualified))
          return HeaderUtil.getQualityFromString(value.toString(), index);

        final Float quality = ((Qualified)value).getQuality();
        return Numbers.Compound.encode(quality != null ? quality : 1f, index);
      }
    });
  }

  @Override
  public List<String> getRequestHeader(final String name) {
    return get(name);
  }

  @Override
  public String getHeaderString(final String name) {
    return CollectionUtil.toString(getRequestHeader(name), getHeaderValueDelimiters(name)[0]);
  }

  @Override
  public HttpHeadersImpl getRequestHeaders() {
    return this;
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public List<MediaType> getAcceptableMediaTypes() {
    final MirrorQualityList<?,String> headers = getMirrorMap().get(HttpHeaders.ACCEPT);
    return headers == null || headers.size() == 0 ? WILDCARD_ACCEPT : (List<MediaType>)headers;
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public List<Locale> getAcceptableLanguages() {
    final MirrorQualityList<?,String> headers = getMirrorMap().get(HttpHeaders.ACCEPT_LANGUAGE);
    return headers == null || headers.size() == 0 ? WILDCARD_LOCALE : (List<Locale>)headers;
  }

  @Override
  public MediaType getMediaType() {
    return (MediaType)getMirrorMap().getFirst(HttpHeaders.CONTENT_TYPE);
  }

  @Override
  public Locale getLanguage() {
    return (Locale)getMirrorMap().getFirst(HttpHeaders.CONTENT_LANGUAGE);
  }

  @Override
  public Map<String,Cookie> getCookies() {
    final MirrorQualityList<?,String> headers = getMirrorMap().get(HttpHeaders.COOKIE);
    if (headers == null || headers.size() == 0)
      return Collections.emptyMap();

    final Map<String,Cookie> cookies = new LinkedHashMap<>();
    for (final Object header : headers) {
      final Cookie cookie = (Cookie)header;
      cookies.put(cookie.getName(), cookie);
    }

    return cookies;
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
    return (Date)getMirrorMap().getFirst(HttpHeaders.LAST_MODIFIED);
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
   * with the specified {@code headerName}, or {@code null} if no value exists.
   *
   * @param headerName The name of the header.
   * @return A string representation of the list of header strings associated
   *         with the specified {@code header} name, or {@code null} if no value
   *         exists
   */
  String getString(final String headerName) {
    final List<String> values = get(headerName);
    if (values == null)
      return null;

    if (values.size() == 0)
      return "";

    final char delimiter = getHeaderValueDelimiters(headerName)[0];
    final StringBuilder builder = new StringBuilder();
    for (int i = 0, len = values.size(); i < len; ++i) {
      if (i > 0)
        builder.append(delimiter);

      builder.append(values.get(i));
    }

    return builder.toString();
  }

  @Override
  public HttpHeadersImpl clone() {
    return (HttpHeadersImpl)super.clone();
  }
}