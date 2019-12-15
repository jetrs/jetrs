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

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;

import org.jetrs.common.ext.delegate.CookieHeaderDelegate;
import org.jetrs.common.ext.delegate.DateHeaderDelegate;
import org.jetrs.common.util.MediaTypes;
import org.jetrs.common.util.MirrorQualityList;
import org.jetrs.common.util.Responses;
import org.libj.io.Charsets;
import org.libj.util.CollectionUtil;
import org.libj.util.Locales;
import org.libj.util.MirrorList;
import org.libj.util.Numbers;
import org.libj.util.primitive.ArrayFloatList;
import org.libj.util.primitive.FloatComparator;

class HttpHeadersUtil {
  static Object valueToReflection(final String key, final String value, final boolean single) {
    Objects.requireNonNull(key);
    if (value == null)
      return null;

    final Object requestHeader = parseRequestHeader(key, value, single);
    if (requestHeader != NOT_FOUND)
      return requestHeader;

    final Object responseHeader = parseResponseHeader(key, value);
    if (responseHeader != NOT_FOUND)
      return responseHeader;

    return value;
  }

  static String reflectionToValue(final Object value) {
    if (value == null)
      return null;

    if (value instanceof String)
      return (String)value;

    if (value instanceof MediaType)
      return value.toString();

    if (value instanceof Locale)
      return value.toString();

    if (value instanceof Charset)
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
  }

  /**
   * Gets the quality attribute from a strongly typed header object (i.e.
   * {@link MediaType#getParameters() mediaType.getParameters().get("q")}), and
   * returns a {@link org.libj.util.Numbers.Compound compound} {@code long}
   * containing the {@code float} quality value and {@code int} ending index of
   * the attribute in the string.
   *
   * @param obj The object to parse.
   * @param i The index from which to start parsing (ignored).
   * @return A {@link org.libj.util.Numbers.Compound compound} {@code long}
   *         containing the {@code float} quality value and {@code int} ending
   *         index of the attribute in the string.
   */
  static long getQualityFromObject(final Object obj, int i) {
    final float quality = obj instanceof MediaType ? Numbers.parseFloat(((MediaType)obj).getParameters().get("q"), 1f) : 1f;
    return Numbers.Compound.encode(quality, i);
  };

  /**
   * Parses the quality attribute from a raw header string (i.e.
   * {@code "fr-CH;q=0.8"}), and returns a {@link org.libj.util.Numbers.Compound
   * compound} {@code long} containing the {@code float} quality value and
   * {@code int} ending index of the attribute in the string.
   *
   * @param str The string to parse.
   * @param i The index from which to start parsing.
   * @return A {@link org.libj.util.Numbers.Compound compound} {@code long}
   *         containing the {@code float} quality value and {@code int} ending
   *         index of the attribute in the string.
   */
  static long getQualityFromString(final String str, int i) {
    boolean dotSeen = false;
    boolean qFinished = false;
    StringBuilder builder = null;
    final int len = str.length();
    for (int stage = 1; i <= len; ++i) {
      char ch;
      if (i == len || (ch = str.charAt(i)) == ',' || qFinished && ch == ';')
        break;

      if (ch == ';') {
        stage = 1;
      }
      else if (stage == 1) {
        if (ch == 'q')
          stage = 2;
        else if (ch != ' ')
          stage = 0;
      }
      else if (stage == 2) {
        if (ch == '=')
          stage = 3;
        else if (ch != ' ')
          stage = 0;
      }
      else if (stage == 3) {
        if ('0' <= ch && ch <= '9' || (dotSeen = ch == '.' && !dotSeen)) {
          if (!qFinished) {
            if (builder == null)
              builder = new StringBuilder();

            builder.append(ch);
            continue;
          }
        }
        else if (ch == ' ') {
          qFinished |= builder != null && builder.length() > 0;
          continue;
        }

        stage = 0;
        dotSeen = false;
        qFinished = false;
        if (builder != null)
          builder.setLength(0);
      }
    }

    // FIXME: Can we avoid Float.parseFloat?
    final float quality = builder == null || builder.length() == 0 ? Float.NaN : Float.parseFloat(builder.toString());
    return Numbers.Compound.encode(quality, i);
  };

  static <T>ArrayFloatList parseMultiHeaderNoSort(final List<String> values, ArrayFloatList qualities, final String header, final Function<String,T> stringToObjectFunction, final boolean hasQuality) {
    final int len = header.length();
    String value = null;
    char ch;
    for (int i = 0, start = -1, end = -1; i <= len; ++i) {
      if (i == len || (ch = header.charAt(i)) == ',') {
        if (value == null)
          value = header.substring(start + 1, end + 1);

        values.add(value);
        start = end = -1;
        value = null;
      }
      else if (ch == ' ') {
        if (end == -1)
          start = i;
      }
      else {
        end = i;
      }
    }

    return qualities;
  };

  static <T>ArrayFloatList parseMultiHeader(final List<T> values, ArrayFloatList qualities, final String header, final Function<String,T> stringToObjectFunction, final boolean hasQuality) {
    final int len = header.length();
    String value = null;
    float quality = 1f;
    char ch;
    for (int i = 0, start = -1, end = -1; i <= len; ++i) {
      if (i == len || (ch = header.charAt(i)) == ',') {
        if (value == null)
          value = header.substring(start + 1, end + 1);

        final T obj = stringToObjectFunction.apply(value);
        if (quality == 1f) {
          values.add(qualities != null ? values.size() - qualities.size() : values.size(), obj);
        }
        else if (qualities == null) {
          values.add(obj);
          qualities = new ArrayFloatList(quality);
        }
        else {
          final int index = CollectionUtil.binaryClosestSearch(qualities, quality, FloatComparator.REVERSE);
          values.add(index + values.size() - qualities.size(), obj);
          qualities.add(index, quality);
        }

        start = end = -1;
        value = null;
        quality = 1f;
      }
      else if (ch == ';') {
        value = header.substring(++start, ++end);
        if (hasQuality) {
          final long qualityAndIndex = getQualityFromString(header, end + 1);
          quality = Numbers.Compound.decodeFloat(qualityAndIndex, 0);
          if (Float.isNaN(quality))
            quality = 1f;

          i = Numbers.Compound.decodeInt(qualityAndIndex, 1) - 1;
        }

        start = end = -1;
      }
      else if (ch == ' ') {
        if (end == -1)
          start = i;
      }
      else {
        end = i;
      }
    }

    return qualities;
  }

  static <T>List<T> parseMultiHeaders(final List<String> headers, final Function<String,T> stringToObjectFunction, final boolean hasQuality, final List<T> dflt) {
    if (headers == null)
      return dflt;

    final List<T> values = new ArrayList<>();
    ArrayFloatList qualities = null;
    for (final String header : headers)
      qualities = parseMultiHeader(values, qualities, header, stringToObjectFunction, hasQuality);

    return values;
  }

  static final MirrorQualityList.Qualifier<String,Object> qualifier = new MirrorQualityList.Qualifier<String,Object>() {
    @Override
    public long valueToQuality(final String reflection, final int index) {
      return HttpHeadersUtil.getQualityFromString(reflection, index);
    }

    @Override
    public long reflectionToQuality(final Object value, final int index) {
      return HttpHeadersUtil.getQualityFromObject(value, index);
    }
  };

  @SuppressWarnings("rawtypes")
  private static <T,X extends List & Cloneable>MirrorList<Object,String> makeMirrorList(final String key, final String header, final Function<String,Object> stringToObjectFunction, final boolean hasQuality, final Supplier<X> listSupplier) {
    final MirrorList.Mirror<String,Object> mirror = HttpHeaderMirrors.getMirrorForward(key);
    final MirrorList<String,Object> mirrorList;
    if (hasQuality)
      mirrorList = new MirrorQualityList<>(listSupplier.get(), listSupplier.get(), mirror, qualifier);
    else
      mirrorList = new MirrorList<String,Object>(listSupplier.get(), listSupplier.get(), mirror);

    parseMultiHeaderNoSort(mirrorList, null, header, stringToObjectFunction, hasQuality);
    return mirrorList.getMirrorList();
  }

  // FIXME: Oh man, this is me redoing parsing that's already been done in MirrorQualityList
  private static String removeQuality(final String value) {
    final int colon = value.indexOf(';');
    if (colon == -1)
      return value;

    final long qualityAndIndex = getQualityFromString(value, colon);
    final int index = Numbers.Compound.decodeInt(qualityAndIndex, 1);
    final String trimmed = value.substring(0, colon);
    return index <= value.length() ? trimmed + value.substring(index, value.length()) : trimmed;
  }

  private static final Object NOT_FOUND = new Object();

  /**
   * Parses the specified HTTP header {@code key} and {@code value} by matching
   * the {@code key} to standard request header names.
   *
   * @param key The key (i.e. the name of the HTTP header).
   * @param value The value (i.e. the value of the HTTP header).
   * @return A strongly-typed representation of a HTTP header specified by
   *         {@code key} and {@code value}.
   */
  // https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
  static Object parseRequestHeader(final String key, String value, final boolean single) {
    // Standard headers...

    if (HttpHeaders.ACCEPT.equalsIgnoreCase(key)) {
      return MediaTypes.parse(value);
    }

    if (HttpHeaders.ACCEPT_CHARSET.equalsIgnoreCase(key)) {
      return single ? Charsets.lookup(removeQuality(value)) : makeMirrorList(key, value, Charsets::lookup, true, ArrayList::new);
    }

    if (HttpHeaders.ACCEPT_ENCODING.equalsIgnoreCase(key)) {
      return single ? removeQuality(value) : makeMirrorList(key, value, s -> s, true, ArrayList::new);
    }

    if (HttpHeaders.ACCEPT_LANGUAGE.equalsIgnoreCase(key)) {
      return single ? Locales.fromRFC1766(removeQuality(value)) : makeMirrorList(key, value, Locales::fromRFC1766, true, ArrayList::new);
    }

    if ("Accept-Datetime".equalsIgnoreCase(key)) {
      return DateHeaderDelegate.parse(value);
    }

    if ("Access-Control-Request-Method".equalsIgnoreCase(key)) {
      return value;
    }

    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(key)) {
      return value;
    }

    if (HttpHeaders.CACHE_CONTROL.equalsIgnoreCase(key)) {
      return CacheControl.valueOf(value);
    }

    if ("Connection".equalsIgnoreCase(key)) {
      return value;
    }

    if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(key)) {
      return Numbers.parseLong(value);
    }

    if ("Content-MD5".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(key)) {
      return MediaTypes.parse(value);
    }

    if (HttpHeaders.COOKIE.equalsIgnoreCase(key)) {
      return CookieHeaderDelegate.parse(value.split(";"));
    }

    if (HttpHeaders.DATE.equalsIgnoreCase(key)) {
      return DateHeaderDelegate.parse(value);
    }

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

    if (HttpHeaders.IF_MODIFIED_SINCE.equalsIgnoreCase(key)) {
      return DateHeaderDelegate.parse(value);
    }

    if (HttpHeaders.IF_NONE_MATCH.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.IF_UNMODIFIED_SINCE.equalsIgnoreCase(key)) {
      return DateHeaderDelegate.parse(value);
    }

    if ("Max-Forwards".equalsIgnoreCase(key)) {
      return Numbers.parseLong(value);
    }

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
      // TRANSFER ENCODING: Multiple acceptable with q value
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
      // Multiple acceptable, but no q value (order matters)
      // FIXME: Does this have a strong type?
      return single ? removeQuality(value) : makeMirrorList(key, value, s -> s, false, ArrayList::new);
    }

    // Warning: <warn-code> <warn-agent> <warn-text> [<warn-date>]
    if ("Warning".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    // Non-standard headers...

    if ("Upgrade-Insecure-Requests".equalsIgnoreCase(key)) {
      return Numbers.parseLong(value);
    }

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

    return NOT_FOUND;
  }

  /**
   * Parses the specified HTTP header {@code key} and {@code value} by matching
   * the {@code key} to standard response header names.
   *
   * @param key The key (i.e. the name of the HTTP header).
   * @param value The value (i.e. the value of the HTTP header).
   * @return A strongly-typed representation of a HTTP header specified by
   *         {@code key} and {@code value}.
   */
  static Object parseResponseHeader(final String key, final String value) {
    if ("Access-Control-Allow-Origin".equalsIgnoreCase(key) || "Access-Control-Allow-Credentials".equalsIgnoreCase(key) || "Access-Control-Expose-Headers".equalsIgnoreCase(key) || "Access-Control-Max-Age".equalsIgnoreCase(key) || "Access-Control-Allow-Methods".equalsIgnoreCase(key) || "Access-Control-Allow-Headers".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Accept-Patch".equalsIgnoreCase(key)) {
      return MediaTypes.parse(value);
    }

    if ("Accept-Ranges".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Age".equalsIgnoreCase(key)) {
      return Numbers.parseLong(value);
    }

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
      // multiple acceptable, but no q value (order matters)
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.CONTENT_LANGUAGE.equalsIgnoreCase(key)) {
      return Locale.forLanguageTag(value);
    }

    if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(key)) {
      return Long.parseLong(value);
    }

    if (HttpHeaders.CONTENT_LOCATION.equalsIgnoreCase(key)) {
      // multiple acceptable, but no q value (order matters)
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
        return new Date((Integer.parseInt(value) + System.currentTimeMillis() / 1000) * 1000);

      return DateHeaderDelegate.parse(value);
    }

    if ("IM".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.LAST_MODIFIED.equalsIgnoreCase(key)) {
      return DateHeaderDelegate.parse(value);
    }

    if (HttpHeaders.LINK.equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if (HttpHeaders.LOCATION.equalsIgnoreCase(key)) {
      return URI.create(value);
    }

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

    if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(key)) {
      return NewCookie.valueOf(value);
    }

    if ("Strict-Transport-Security".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Trailer".equalsIgnoreCase(key)) {
      // FIXME: Does this have a strong type?
      return value;
    }

    if ("Transfer-Encoding".equalsIgnoreCase(key)) {
      // TRANSFER ENCODING: Multiple acceptable, but no q value (order matters)
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

    return NOT_FOUND;
  }

  private HttpHeadersUtil() {
  }
}