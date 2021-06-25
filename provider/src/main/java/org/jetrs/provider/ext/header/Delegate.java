/* Copyright (c) 2021 JetRS
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

package org.jetrs.provider.ext.header;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jetrs.provider.util.Responses;
import org.libj.lang.Numbers;
import org.libj.lang.Strings;
import org.libj.util.Locales;
import org.libj.util.SimpleDateFormats;

public abstract class Delegate<T> implements RuntimeDelegate.HeaderDelegate<T> {
  private static final HashMap<String,Delegate<?>> headerNameToDelegate = new HashMap<>();
  private static final ArrayList<Delegate<?>> delegates = new ArrayList<>();

  static final Delegate<Object> RETRY_AFTER = new Delegate<Object>(false, "Retry-After") {
    @Override
    Class<Object> getType() {
      return null;
    }

    @Override
    public Object fromString(final String value) {
      return Numbers.isNumber(value) ? Long.parseLong(value) : Delegate.DATE.fromString(value);
    }

    @Override
    public String toString(final Object value) {
      return value.toString();
    }
  };

  static final Delegate<BigDecimal> BIG_DECIMAL = new Delegate<BigDecimal>(true, "X-Content-Duration") {
    @Override
    Class<BigDecimal> getType() {
      return BigDecimal.class;
    }

    @Override
    public BigDecimal fromString(final String value) {
      return value == null ? null : new BigDecimal(value);
    }

    @Override
    public String toString(final BigDecimal value) {
      return value.toString();
    }
  };

  static final Delegate<Boolean> BOOLEAN = new Delegate<Boolean>(true, "Access-Control-Allow-Credentials") {
    @Override
    Class<Boolean> getType() {
      return Boolean.class;
    }

    @Override
    public Boolean fromString(final String value) {
      return Boolean.parseBoolean(value);
    }

    @Override
    public String toString(final Boolean value) {
      return value.toString();
    }
  };

  static final Delegate<CacheControl> CACHE_CONTROL = new Delegate<CacheControl>(true, HttpHeaders.CACHE_CONTROL) {
    @Override
    Class<CacheControl> getType() {
      return CacheControl.class;
    }

    @Override
    public StrictCacheControl fromString(final String value) {
      return StrictCacheControl.parse(value);
    }

    @Override
    public String toString(final CacheControl value) {
      final StringBuilder builder = new StringBuilder();
      if (value instanceof StrictCacheControl) {
        final StrictCacheControl cacheControl = (StrictCacheControl)value;
        for (final StrictCacheControl.Directive directive : cacheControl.order)
          directive.toString(cacheControl, builder);

        StrictCacheControl.Directive.EXTENSION.toString(cacheControl, builder);
        builder.setLength(builder.length() - 1);
      }
      else {
        for (final StrictCacheControl.Directive directive : StrictCacheControl.Directive.values())
          directive.toString(value, builder);
      }

      return builder.toString();
    }
  };

  static final Delegate<Charset> CHARSET = new Delegate<Charset>(true, HttpHeaders.ACCEPT_CHARSET) {
    @Override
    Class<Charset> getType() {
      return Charset.class;
    }

    @Override
    public QualifiedCharset fromString(final String value) {
      return value == null ? null : QualifiedCharset.valueOf(value);
    }

    @Override
    public String toString(final Charset value) {
      return QualifiedCharset.toString(value);
    }
  };

  static final Delegate<NewCookie> NEW_COOKIE = new Delegate<NewCookie>(true, HttpHeaders.SET_COOKIE) {
    @Override
    Class<NewCookie> getType() {
      return NewCookie.class;
    }

    // FIXME: This should be re-implemented with a char-by-char algorithm
    @Override
    public NewCookie fromString(final String string) {

      final String[] parts = string.split(";");
      final String part0 = parts[0];
      int index = part0.indexOf('=');
      if (index == -1)
        return null;

      final DirectiveList<StrictNewCookie.Directive> order = new DirectiveList<>();
      final String name = Strings.trim(part0.substring(0, index).trim(), '"');
      final String value = Strings.trim(part0.substring(index + 1).trim(), '"');

      String path = null;
      String domain = null;
      int version = -1;
      String comment = null;
      int maxAge = -1;
      Date expires = null;
      boolean secure = false;
      boolean httpOnly = false;
      for (int i = 1; i < parts.length; ++i) {
        final String part = parts[i].trim();
        if (part.startsWith("Path")) {
          if ((index = part.indexOf('=')) != -1) {
            path = Strings.trim(part.substring(index + 1).trim(), '"');
            order.addLast(StrictNewCookie.Directive.PATH);
          }
        }
        else if (part.startsWith("Domain")) {
          if ((index = part.indexOf('=')) != -1) {
            domain = part.substring(index + 1).trim();
            order.addLast(StrictNewCookie.Directive.DOMAIN);
          }
        }
        else if (part.startsWith("Version")) {
          if ((index = part.indexOf('=')) != -1) {
            version = Integer.parseInt(Strings.trim(part.substring(index + 1).trim(), '"'));
            order.addLast(StrictNewCookie.Directive.VERSION);
          }
        }
        else if (part.startsWith("Comment")) {
          if ((index = part.indexOf('=')) != -1) {
            comment = part.substring(index + 1).trim();
            order.addLast(StrictNewCookie.Directive.COMMENT);
          }
        }
        else if (part.startsWith("Max-Age")) {
          if ((index = part.indexOf('=')) != -1) {
            maxAge = Integer.parseInt(part.substring(index + 1).trim());
            order.addLast(StrictNewCookie.Directive.MAX_AGE);
          }
        }
        else if (part.startsWith("Expires")) {
          if ((index = part.indexOf('=')) != -1) {
            try {
              expires = SimpleDateFormats.ISO_1123.get().parse(part.substring(index + 1).trim());
              order.addLast(StrictNewCookie.Directive.EXPIRY);
            }
            catch (final ParseException e) {
            }
          }
        }
        else if (part.startsWith("Secure")) {
          secure = true;
          order.addLast(StrictNewCookie.Directive.SECURE);
        }
        else if (part.startsWith("HttpOnly")) {
          httpOnly = true;
          order.addLast(StrictNewCookie.Directive.HTTP_ONLY);
        }
      }

      return new StrictNewCookie(order, name, value, path, domain, version, comment, maxAge, expires, secure, httpOnly);
    }

    @Override
    public String toString(final NewCookie value) {
      final StringBuilder builder = new StringBuilder();
      builder.append(value.getName()).append('=').append(value.getValue());
      if (value instanceof StrictNewCookie) {
        final StrictNewCookie cacheControl = (StrictNewCookie)value;
        for (final StrictNewCookie.Directive directive : cacheControl.order)
          directive.toString(cacheControl, builder);
      }
      else {
        for (final StrictNewCookie.Directive directive : StrictNewCookie.Directive.values())
          directive.toString(value, builder);
      }

      return builder.toString();
    }
  };

  static final Delegate<Cookie> COOKIE = new Delegate<Cookie>(true, HttpHeaders.COOKIE) {
    @Override
    Class<Cookie> getType() {
      return Cookie.class;
    }

    @Override
    public Cookie fromString(final String value) {
      final int index = value.indexOf('=');
      return index == -1 ? null : new Cookie(value.substring(0, index).trim(), value.substring(index + 1).trim());
    }

    @Override
    public String toString(final Cookie value) {
      return value.getName() + "=" + value.getValue();
    }
  };

  static final Delegate<Date> DATE = new Delegate<Date>(true, "Accept-Datetime", HttpHeaders.DATE, HttpHeaders.EXPIRES, HttpHeaders.IF_MODIFIED_SINCE, HttpHeaders.IF_UNMODIFIED_SINCE, HttpHeaders.LAST_MODIFIED) {
    @Override
    Class<Date> getType() {
      return Date.class;
    }

    @Override
    public Date fromString(final String value) {
      try {
        return SimpleDateFormats.ISO_1123.get().parse(value);
      }
      catch (final ParseException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String toString(final Date value) {
      return SimpleDateFormats.ISO_1123.get().format(value);
    }
  };

  static final Delegate<Locale> LOCALE = new Delegate<Locale>(true, HttpHeaders.ACCEPT_LANGUAGE, HttpHeaders.CONTENT_LANGUAGE) {
    @Override
    Class<Locale> getType() {
      return Locale.class;
    }

    @Override
    public Locale fromString(final String value) {
      return value == null ? null : Locales.fromRFC1766(value);
    }

    @Override
    public String toString(final Locale value) {
      return value.toString().replace('_', '-');
    }
  };

  static final Delegate<Long> LONG = new Delegate<Long>(true, "Access-Control-Max-Age", "Age", "Age", "Max-Forwards", "Upgrade-Insecure-Requests", HttpHeaders.CONTENT_LENGTH) {
    @Override
    Class<Long> getType() {
      return Long.class;
    }

    @Override
    public Long fromString(final String value) {
      return Numbers.parseLong(value);
    }

    @Override
    public String toString(final Long value) {
      return value.toString();
    }
  };

  static final Delegate<MediaType> MEDIA_TYPE = new Delegate<MediaType>(true, "Accept-Patch", HttpHeaders.ACCEPT, HttpHeaders.CONTENT_TYPE) {
    @Override
    Class<MediaType> getType() {
      return MediaType.class;
    }

    @Override
    public QualifiedMediaType fromString(final String value) {
      return MediaTypes.parse(value);
    }

    @Override
    public String toString(final MediaType value) {
      return MediaTypes.toString(value);
    }
  };

  static final Delegate<Response.StatusType> STATUS_TYPE = new Delegate<Response.StatusType>(true, "Status") {
    @Override
    Class<Response.StatusType> getType() {
      return Response.StatusType.class;
    }

    @Override
    public Response.StatusType fromString(final String value) {
      return value == null ? null : Responses.from(value);
    }

    @Override
    public String toString(final Response.StatusType value) {
      return value.getStatusCode() + " " + value.getReasonPhrase();
    }
  };

  static final Delegate<String> STRING = new Delegate<String>(true) {
    @Override
    Class<String> getType() {
      return String.class;
    }

    @Override
    public String fromString(final String value) {
      return value;
    }

    @Override
    public String toString(final String value) {
      return value;
    }
  };

  static final Delegate<Tk> TK = new Delegate<Tk>(true, "Tk") {
    @Override
    Class<Tk> getType() {
      return Tk.class;
    }

    @Override
    public Tk fromString(final String value) {
      return Tk.fromString(value);
    }

    @Override
    public String toString(final Tk value) {
      return value.toString();
    }
  };

  static final Delegate<String[]> STRING_ARRAY = new Delegate<String[]>(true) {
    @Override
    Class<String[]> getType() {
      return String[].class;
    }

    private String[] fromString(final String value, final int start, final int depth) {
      if (start >= value.length() - 1)
        return new String[depth];

      int end = value.indexOf(',', start);
      if (start == end)
        return fromString(value, end + 1, depth);

      if (end == -1)
        end = value.length();

      final String token = value.substring(start, end).trim();
      if (token.length() == 0)
        return fromString(value, end + 1, depth);

      final String[] array = fromString(value, end + 1, depth + 1);
      array[depth] = token;
      return array;
    }

    @Override
    public String[] fromString(final String value) {
      return value == null ? null : value.length() == 0 ? new String[0] : fromString(value, 0, 0);
    }

    @Override
    public String toString(final String[] value) {
      if (value == null)
        return null;

      if (value.length == 0)
        return "";

      final StringBuilder builder = new StringBuilder();
      builder.append(value[0]);
      for (int i = 1; i < value.length; ++i)
        builder.append(',').append(value[i]);

      return builder.toString();
    }
  };

  static final Delegate<java.net.URI> URI = new Delegate<java.net.URI>(true, HttpHeaders.LOCATION) {
    @Override
    Class<java.net.URI> getType() {
      return java.net.URI.class;
    }

    @Override
    public java.net.URI fromString(final String value) {
      return value == null ? null : java.net.URI.create(value);
    }

    @Override
    public String toString(final java.net.URI value) {
      return value.toString();
    }
  };

  public static final Delegate<Object> DEFAULT = new Delegate<Object>(false) {
    @Override
    Class<Object> getType() {
      return null;
    }

    @Override
    public Object fromString(final String value) {
      return value;
    }

    @Override
    public String toString(final Object value) {
      return value == null ? null : value instanceof String ? (String)value : value.toString();
    }
  };

  @SuppressWarnings("rawtypes")
  public static <T>Delegate lookup(final String headerName) {
    final Delegate delegate = headerNameToDelegate.get(headerName.toLowerCase());
    return delegate != null ? delegate : DEFAULT;
  }

  @SuppressWarnings("unchecked")
  public static <T>Delegate<T> lookup(final Class<T> type) {
    if (type == null)
      return (Delegate<T>)DEFAULT;

    for (final Delegate<?> delegate : delegates)
      if (delegate.getType().isAssignableFrom(type))
        return (Delegate<T>)delegate;

    return (Delegate<T>)DEFAULT;
  }

  @SuppressWarnings("rawtypes")
  public static <T>Delegate lookup(final String headerName, final Class<T> type) {
    final Delegate delegate = headerNameToDelegate.get(headerName.toLowerCase());
    return delegate != null && delegate.getClass().isAssignableFrom(type) ? delegate : lookup(type);
  }

  abstract Class<T> getType();

  private Delegate(final boolean add, final String ... headerNames) {
    if (add) {
      delegates.add(this);
      for (final String headerName : headerNames)
        headerNameToDelegate.put(headerName.toLowerCase(), this);
    }
  }
}