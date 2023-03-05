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

package org.jetrs;

import static org.libj.lang.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.jetrs.StrictCacheControl.Directive;
import org.libj.lang.Numbers;
import org.libj.lang.Strings;
import org.libj.util.Locales;
import org.libj.util.SimpleDateFormats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: Multiple message-header fields with the same field-name MAY be present in a message if and only if the entire field-value for
// FIXME: that header field is defined as a comma-separated list [i.e., #(values)]. It MUST be possible to combine the multiple header
// FIXME: fields into one "field-name: field-value" pair, without changing the semantics of the message, by appending each subsequent
// FIXME: field-value to the first, each separated by a comma. The order in which header fields with the same field-name are received is
// FIXME: therefore significant to the interpretation of the combined field value, and thus a proxy MUST NOT change the order of these field
// FIXME: values when a message is forwarded. http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
// FIXME: Except for COOKIE: https://datatracker.ietf.org/doc/html/rfc6265#section-4.2.1
abstract class HeaderDelegateImpl<T> implements RuntimeDelegate.HeaderDelegate<T> {
  private static final Logger logger = LoggerFactory.getLogger(HeaderDelegateImpl.class);
  private static final HashMap<Class<?>,HeaderDelegateImpl<?>> classToDelegates = new HashMap<>();

  static class HeaderDelegateComposite<T> extends HeaderDelegateImpl<T> {
    private final RuntimeDelegate.HeaderDelegate<T>[] delegates;

    @SafeVarargs
    HeaderDelegateComposite(final Class<T> type, final boolean add, final HeaderDelegate<T> ... delegates) {
      super(type, add);
      this.delegates = delegates;
    }

    @Override
    T valueOf(final String value) {
      for (final HeaderDelegate<T> delegate : delegates) { // [A]
        final T obj = delegate.fromString(value);
        if (obj != null)
          return obj;
      }

      return null;
    }

    @Override
    public String toString(final T value) {
      for (final HeaderDelegate<T> delegate : delegates) { // [A]
        final String str = delegate.toString(value);
        if (str != null)
          return str;
      }

      return null;
    }
  }

  public static final HeaderDelegateImpl<Byte> BYTE;
  public static final HeaderDelegateImpl<Short> SHORT;
  public static final HeaderDelegateImpl<Integer> INTEGER;
  public static final HeaderDelegateImpl<Long> LONG;
  public static final HeaderDelegateImpl<BigInteger> BIG_INTEGER;
  public static final HeaderDelegateImpl<BigDecimal> BIG_DECIMAL;

  public static final HeaderDelegateImpl<Boolean> BOOLEAN_1;
  public static final HeaderDelegateImpl<Boolean> BOOLEAN_1_0;
  public static final HeaderDelegateImpl<Boolean> BOOLEAN_ON_OFF;
  public static final HeaderDelegateImpl<Boolean> BOOLEAN_TRUE_FALSE;
  public static final HeaderDelegateImpl<Boolean> BOOLEAN;
  public static final HeaderDelegateImpl<CacheControl> CACHE_CONTROL;
  public static final HeaderDelegateImpl<Charset> CHARSET;
  public static final HeaderDelegateImpl<Cookie> COOKIE;
  public static final HeaderDelegateImpl<Date> DATE;
  public static final HeaderDelegateImpl<Double> DOUBLE;
  public static final HeaderDelegateImpl<EntityTag> ENTITY_TAG;
  public static final HeaderDelegateImpl<Float> FLOAT;
  public static final HeaderDelegateImpl<LocalDateTime> LOCAL_DATE_TIME;
  public static final HeaderDelegateImpl<Locale> LOCALE;
  public static final HeaderDelegateImpl<MediaType> MEDIA_TYPE;
  public static final HeaderDelegateImpl<NewCookie> NEW_COOKIE;
  public static final HeaderDelegateImpl<Object> DEFAULT_COMMA;
  public static final HeaderDelegateImpl<Object> DEFAULT_NONE;
  public static final HeaderDelegateImpl<Response.StatusType> STATUS_TYPE;
  public static final HeaderDelegateImpl<String> STRING;
  public static final HeaderDelegateImpl<String[]> STRING_ARRAY;
  public static final HeaderDelegateImpl<Tk> TK;
  public static final HeaderDelegateImpl<java.net.URI> URI;

  @SuppressWarnings("rawtypes")
  private static final HeaderDelegateImpl[] values = {
    BYTE = new HeaderDelegateImpl<Byte>(Byte.class, true) {
      @Override
      Byte valueOf(final String value) {
        return Numbers.parseByte(value);
      }

      @Override
      public String toString(final Byte value) {
        return value.toString();
      }
    },
    SHORT = new HeaderDelegateImpl<Short>(Short.class, true) {
      @Override
      Short valueOf(final String value) {
        return Numbers.parseShort(value);
      }

      @Override
      public String toString(final Short value) {
        return value.toString();
      }
    },
    INTEGER = new HeaderDelegateImpl<Integer>(Integer.class, true) {
      @Override
      Integer valueOf(final String value) {
        return Numbers.parseInteger(value);
      }

      @Override
      public String toString(final Integer value) {
        return value.toString();
      }
    },
    LONG = new HeaderDelegateImpl<Long>(Long.class, true) {
      @Override
      Long valueOf(final String value) {
        return Numbers.parseLong(value);
      }

      @Override
      public String toString(final Long value) {
        return value.toString();
      }
    },
    BIG_INTEGER = new HeaderDelegateImpl<BigInteger>(BigInteger.class, true) {
      @Override
      BigInteger valueOf(final String value) {
        return Numbers.isNumber(value) ? new BigInteger(value) : null;
      }

      @Override
      public String toString(final BigInteger value) {
        return value.toString();
      }
    },
    BIG_DECIMAL = new HeaderDelegateImpl<BigDecimal>(BigDecimal.class, true) {
      @Override
      BigDecimal valueOf(final String value) {
        return new BigDecimal(value);
      }

      @Override
      public String toString(final BigDecimal value) {
        return value.toString();
      }
    },
    BOOLEAN_1 = new HeaderDelegateImpl<Boolean>(Boolean.class, false) {
      @Override
      Boolean valueOf(final String value) {
        return value.equals("1") ? Boolean.TRUE : null;
      }

      @Override
      public String toString(final Boolean value) {
        return value == null ? null : value ? "1" : null;
      }
    },
    BOOLEAN_1_0 = new HeaderDelegateImpl<Boolean>(Boolean.class, false) {
      @Override
      Boolean valueOf(final String value) {
        return value == null ? null : "1".equals(value) ? Boolean.TRUE : "0".equals(value) ? Boolean.FALSE : null;
      }

      @Override
      public String toString(final Boolean value) {
        return value == null ? null : value ? "1" : "0";
      }
    },
    BOOLEAN_ON_OFF = new HeaderDelegateImpl<Boolean>(Boolean.class, false) {
      @Override
      Boolean valueOf(final String value) {
        return value == null ? null : "on".equalsIgnoreCase(value) ? Boolean.TRUE : "off".equalsIgnoreCase(value) ? Boolean.FALSE : null;
      }

      @Override
      public String toString(final Boolean value) {
        return value == null ? null : value ? "on" : "off";
      }
    },
    BOOLEAN_TRUE_FALSE = new HeaderDelegateImpl<Boolean>(Boolean.class, false) {
      @Override
      Boolean valueOf(final String value) {
        return Boolean.parseBoolean(value);
      }

      @Override
      public String toString(final Boolean value) {
        return value.toString();
      }
    },
    BOOLEAN = new HeaderDelegateComposite<>(Boolean.class, true, BOOLEAN_TRUE_FALSE, BOOLEAN_1_0, BOOLEAN_ON_OFF),
    CACHE_CONTROL = new HeaderDelegateImpl<CacheControl>(CacheControl.class, true) {
      @Override
      StrictCacheControl valueOf(final String value) {
        return StrictCacheControl.parse(value);
      }

      @Override
      public String toString(final CacheControl value) {
        final StringBuilder builder = new StringBuilder();
        if (value instanceof StrictCacheControl) {
          final StrictCacheControl cacheControl = (StrictCacheControl)value;
          final DirectiveList<Directive> directives = cacheControl.order;
          for (int i = 0, i$ = directives.size(); i < i$; ++i) // [RA]
            directives.get(i).toString(cacheControl, builder);

          StrictCacheControl.Directive.EXTENSION.toString(cacheControl, builder);
          builder.setLength(builder.length() - 1);
        }
        else {
          for (final StrictCacheControl.Directive directive : StrictCacheControl.Directive.values()) // [A]
            directive.toString(value, builder);
        }

        return builder.toString();
      }
    },
    CHARSET = new HeaderDelegateImpl<Charset>(Charset.class, true) {
      @Override
      QualifiedCharset valueOf(final String value) {
        return QualifiedCharset.valueOf(value);
      }

      @Override
      public String toString(final Charset value) {
        return QualifiedCharset.toString(value);
      }
    },
    COOKIE = new HeaderDelegateImpl<Cookie>(Cookie.class, true) {
      @Override
      Cookie valueOf(final String value) {
        final int index = value.indexOf('=');
        return index == -1 ? null : new Cookie(value.substring(0, index).trim(), value.substring(index + 1).trim());
      }

      @Override
      public String toString(final Cookie value) {
        return value.getName() + "=" + value.getValue();
      }
    },
    DATE = new HeaderDelegateImpl<Date>(Date.class, true) {
      @Override
      Date valueOf(final String value) throws Exception {
        return SimpleDateFormats.RFC_1123.get().parse(value);
      }

      @Override
      public String toString(final Date value) {
        return SimpleDateFormats.RFC_1123.get().format(value);
      }
    },
    DOUBLE = new HeaderDelegateImpl<Double>(Double.class, true) {
      @Override
      Double valueOf(final String value) throws Exception {
        return Numbers.parseDouble(value);
      }

      @Override
      public String toString(final Double value) {
        return value.toString();
      }
    },
    ENTITY_TAG = new HeaderDelegateImpl<EntityTag>(EntityTag.class, true) {
      @Override
      EntityTag valueOf(final String string) {
        if ("*".equals(string))
          return wildcardEntityTag;

        final int i$ = string.length();
        if (i$ <= 2)
          return null;

        int i = 0;
        char ch = string.charAt(i);
        final boolean weak = ch == 'W';
        if (weak) {
          if (string.charAt(++i) != '/')
            return null;

          ch = string.charAt(++i);
        }

        if (ch != '"')
          return null;

        final StringBuilder b = new StringBuilder();
        b.append(string, ++i, i = i$ - 1);
        ch = string.charAt(i);

        if (ch != '"')
          return null;

        return new EntityTag(b.toString(), weak);
      }

      @Override
      public String toString(final EntityTag value) {
        if (value == wildcardEntityTag)
          return "*";

        final String valueValue = value.getValue();
        if ("*".equals(valueValue))
          return "*";

        final StringBuilder builder = new StringBuilder((value.isWeak() ? 4 : 2) + valueValue.length());
        if (value.isWeak())
          builder.append("W/");

        builder.append('"');
        builder.append(valueValue);
        builder.append('"');
        return builder.toString();
      }
    },
    FLOAT = new HeaderDelegateImpl<Float>(Float.class, true) {
      @Override
      Float valueOf(final String value) throws Exception {
        return Numbers.parseFloat(value);
      }

      @Override
      public String toString(final Float value) {
        return value.toString();
      }
    },
    LOCAL_DATE_TIME = new HeaderDelegateImpl<LocalDateTime>(LocalDateTime.class, true) {
      @Override
      LocalDateTime valueOf(final String value) throws Exception {
        return LocalDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
      }

      @Override
      public String toString(final LocalDateTime value) {
        return value.format(DateTimeFormatter.RFC_1123_DATE_TIME);
      }
    },
    LOCALE = new HeaderDelegateImpl<Locale>(Locale.class, true) {
      @Override
      Locale valueOf(final String value) {
        return Locales.fromRFC1766(value);
      }

      @Override
      public String toString(final Locale value) {
        return value.toString().replace('_', '-');
      }
    },
    MEDIA_TYPE = new HeaderDelegateImpl<MediaType>(MediaType.class, true) {
      @Override
      QualifiedMediaType valueOf(final String value) {
        return MediaTypes.parse(value);
      }

      @Override
      public String toString(final MediaType value) {
        return MediaTypes.toString(value);
      }
    },
    NEW_COOKIE = new HeaderDelegateImpl<NewCookie>(NewCookie.class, true) {
      // FIXME: This should be re-implemented with a char-by-char algorithm
      @Override
      NewCookie valueOf(final String string) {
        final String[] parts = Strings.split(string, ';');
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
        for (int i = 1, i$ = parts.length; i < i$; ++i) { // [A]
          final String part = parts[i].trim();
          if (part.startsWith("Path")) {
            if ((index = part.indexOf('=')) != -1) {
              path = Strings.trim(part.substring(index + 1).trim(), '"');
              order.add(StrictNewCookie.Directive.PATH);
            }
          }
          else if (part.startsWith("Domain")) {
            if ((index = part.indexOf('=')) != -1) {
              domain = part.substring(index + 1).trim();
              order.add(StrictNewCookie.Directive.DOMAIN);
            }
          }
          else if (part.startsWith("Version")) {
            if ((index = part.indexOf('=')) != -1) {
              version = Integer.parseInt(Strings.trim(part.substring(index + 1).trim(), '"'));
              order.add(StrictNewCookie.Directive.VERSION);
            }
          }
          else if (part.startsWith("Comment")) {
            if ((index = part.indexOf('=')) != -1) {
              comment = part.substring(index + 1).trim();
              order.add(StrictNewCookie.Directive.COMMENT);
            }
          }
          else if (part.startsWith("Max-Age")) {
            if ((index = part.indexOf('=')) != -1) {
              maxAge = Integer.parseInt(part.substring(index + 1).trim());
              order.add(StrictNewCookie.Directive.MAX_AGE);
            }
          }
          else if (part.startsWith("Expires")) {
            if ((index = part.indexOf('=')) != -1) {
              try {
                expires = SimpleDateFormats.RFC_1123.get().parse(part.substring(index + 1).trim());
                order.add(StrictNewCookie.Directive.EXPIRY);
              }
              catch (final ParseException e) {
              }
            }
          }
          else if (part.startsWith("Secure")) {
            secure = true;
            order.add(StrictNewCookie.Directive.SECURE);
          }
          else if (part.startsWith("HttpOnly")) {
            httpOnly = true;
            order.add(StrictNewCookie.Directive.HTTP_ONLY);
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
          final DirectiveList<StrictNewCookie.Directive> directives = cacheControl.order;
          for (int i = 0, i$ = directives.size(); i < i$; ++i) // [RA]
            directives.get(i).toString(cacheControl, builder);
        }
        else {
          for (final StrictNewCookie.Directive directive : StrictNewCookie.Directive.values()) // [A]
            directive.toString(value, builder);
        }

        return builder.toString();
      }
    },
    DEFAULT_COMMA = new HeaderDelegateImpl<Object>(Object.class, false) {
      @Override
      Object valueOf(final String value) {
        return value;
      }

      @Override
      public String toString(final Object value) {
        return value instanceof String ? (String)value : value.toString();
      }
    },
    DEFAULT_NONE = new HeaderDelegateImpl<Object>(Object.class, true) {
      @Override
      Object valueOf(final String value) {
        return value;
      }

      @Override
      public String toString(final Object value) {
        return value instanceof String ? (String)value : value.toString();
      }
    },
    STATUS_TYPE = new HeaderDelegateImpl<Response.StatusType>(Response.StatusType.class, true) {
      @Override
      Response.StatusType valueOf(final String value) {
        return Responses.from(value);
      }

      @Override
      public String toString(final Response.StatusType value) {
        return value.getStatusCode() + " " + value.getReasonPhrase();
      }
    },
    STRING = new HeaderDelegateImpl<String>(String.class, false) {
      @Override
      String valueOf(final String value) {
        return value;
      }

      @Override
      public String toString(final String value) {
        return value;
      }
    },
    STRING_ARRAY = new HeaderDelegateImpl<String[]>(String[].class, true) {
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
      String[] valueOf(final String value) {
        return value.length() == 0 ? Strings.EMPTY_ARRAY : fromString(value, 0, 0);
      }

      @Override
      public String toString(final String[] value) {
        if (value.length == 0)
          return "";

        final StringBuilder builder = new StringBuilder();
        builder.append(value[0]);
        for (int i = 1, i$ = value.length; i < i$; ++i) // [A]
          builder.append(',').append(value[i]);

        return builder.toString();
      }
    },
    TK = new HeaderDelegateImpl<Tk>(Tk.class, true) {
      @Override
      Tk valueOf(final String value) {
        return Tk.valueOf(value);
      }

      @Override
      public String toString(final Tk value) {
        return value.toString();
      }
    },
    URI = new HeaderDelegateImpl<java.net.URI>(java.net.URI.class, true) {
      @Override
      java.net.URI valueOf(final String value) {
        return java.net.URI.create(value);
      }

      @Override
      public String toString(final java.net.URI value) {
        return value.toString();
      }
    }
  };

  static HeaderDelegateImpl<?>[] values() {
    return values;
  }

  @SuppressWarnings("unchecked")
  static <T>HeaderDelegateImpl<T> lookup(final Class<T> type) {
    if (type == null)
      return (HeaderDelegateImpl<T>)DEFAULT_COMMA;

    Class<?> cls = type;
    do {
      final HeaderDelegateImpl<?> delegate = classToDelegates.get(cls);
      if (delegate != null)
        return (HeaderDelegateImpl<T>)delegate;
    }
    while ((cls = cls.getSuperclass()) != null);

    return (HeaderDelegateImpl<T>)DEFAULT_COMMA;
  }

  static AbstractMap.SimpleEntry<HttpHeader<?>,HeaderDelegateImpl<?>> lookup(final String headerName) {
    final AbstractMap.SimpleEntry<HttpHeader<?>,HeaderDelegateImpl<?>> entry = HttpHeader.headerNameToDelegate.get(headerName.toLowerCase());
    return entry != null ? entry : defaultHeaderDelegate;
  }

  @SuppressWarnings("rawtypes")
  static <T>RuntimeDelegate.HeaderDelegate lookup(final String headerName, final Class<T> type) {
    final AbstractMap.SimpleEntry<HttpHeader<?>,HeaderDelegateImpl<?>> entry = HttpHeader.headerNameToDelegate.get(headerName.toLowerCase());
    return entry != null && (type == null || entry.getValue().getType().isAssignableFrom(type)) ? entry.getValue() : lookup(type);
  }

  private static final EntityTag wildcardEntityTag = new EntityTag("*");
  private static final AbstractMap.SimpleEntry<HttpHeader<?>,HeaderDelegateImpl<?>> defaultHeaderDelegate = new AbstractMap.SimpleEntry<>(null, DEFAULT_COMMA);

  Class<T> getType() {
    return type;
  }

  abstract T valueOf(String value) throws Exception;

  @Override
  public final T fromString(final String value) {
    if (value == null)
      return null;

    try {
      return valueOf(value);
    }
    catch (final Exception e) {
      if (logger.isDebugEnabled()) logger.debug("Exception parsing header value: \"" + value + "\"", e);
      return null;
    }
  }

  private final Class<T> type;

  HeaderDelegateImpl(final Class<T> type, final boolean add) {
    this.type = type;
    if (add)
      classToDelegates.put(assertNotNull(type), this);
  }

  @Override
  public String toString() {
    return "Delegate<" + (getType() == null ? "null" : getType().getSimpleName()) + ">";
  }
}