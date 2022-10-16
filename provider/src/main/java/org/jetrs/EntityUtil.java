/* Copyright (c) 2022 JetRS
 *
 * Permission is hereby granted, final free of charge, final to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), final to deal
 * in the Software without restriction, final including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, final and/or sell
 * copies of the Software, final and to permit persons to whom the Software is
 * furnished to do so, final subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.jetrs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Encoded;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.libj.net.URIs;

public class EntityUtil {
  // NOTE: This was copy+pasted from jetty-server `ContextHandler`
  private static final String MAX_FORM_KEYS_KEY = "org.eclipse.jetty.server.Request.maxFormKeys";
  private static final String MAX_FORM_CONTENT_SIZE_KEY = "org.eclipse.jetty.server.Request.maxFormContentSize";
  private static final int DEFAULT_MAX_FORM_KEYS = 1000;
  private static final int DEFAULT_MAX_FORM_CONTENT_SIZE = 200000;

  private static final int _maxFormKeys = Integer.getInteger(MAX_FORM_KEYS_KEY, DEFAULT_MAX_FORM_KEYS);
  private static final int _maxFormContentSize = Integer.getInteger(MAX_FORM_CONTENT_SIZE_KEY, DEFAULT_MAX_FORM_CONTENT_SIZE);

  static final MultivaluedMap<String,String> EMPTY_MAP = new MultivaluedHashMap<>(0); // FIXME: Make this unmodifiable

  static class MultivaluedLinkedHashMap<V> extends MultiMap<V> implements MultivaluedMap<String,V> {
    @Override
    public void putSingle(final String key, final V value) {
      super.put(key, value);
    }

    @Override
    public V getFirst(final String key) {
      final List<V> values = super.get(key);
      return values == null ? null : values.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addAll(final String key, final V ... newValues) {
      super.addValues(key, newValues);
    }

    @Override
    public void addAll(final String key, final List<V> valueList) {
      super.addValues(key, valueList);
    }

    @Override
    public void addFirst(final String key, final V value) {
      super.add(key, value);
    }

    @Override
    public boolean equalsIgnoreValueOrder(final MultivaluedMap<String,V> otherMap) {
      return otherMap != null && otherMap.equalsIgnoreValueOrder(this);
    }
  }

  public static Map<String,String[]> toStringArrayMap(final Map<String,List<String>> multiMap) {
    final Map<String,String[]> map = new LinkedHashMap<String,String[]>(multiMap.size() * 3 / 2) {
      @Override
      public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append('{');
        final Iterator<Map.Entry<String,String[]>> iterator = super.entrySet().iterator();
        for (int i = 0; iterator.hasNext();) {
          final Map.Entry<String,String[]> entry = iterator.next();
          if (i > 0)
            b.append(',');

          b.append(entry.getKey()).append("=[");
          for (final String value : entry.getValue())
            b.append(value).append(',');

          b.setCharAt(b.length() - 1, ']');
        }

        b.append('}');
        return b.toString();
      }
    };

    for (final Map.Entry<String,List<String>> entry : multiMap.entrySet()) {
      final List<String> value = entry.getValue();
      map.put(entry.getKey(), value == null ? null : value.toArray(new String[value.size()]));
    }

    return map;
  }

  public static int getMaxFormContentSize() {
    return _maxFormContentSize;
  }

  public static <T>T checktNotNull(final T entity, final Annotation[] annotations) {
    if (entity == null)
      for (final Annotation annotation : annotations) // [A]
        if (annotation.annotationType() == NotNull.class)
          throw new BadRequestException("Entity is null");

    return entity;
  }

  public static boolean shouldDecode(final Annotation[] annotations) {
    for (final Annotation annotation : annotations) // [A]
      if (annotation.annotationType().equals(Encoded.class))
        return false;

    return true;
  }

  static MultivaluedHashMap<String,String> readFormParamsEncoded(final InputStream in, final Charset encoding) throws IOException {
    final StringBuilder b = new StringBuilder();
    String name = null;
    final MultivaluedHashMap<String,String> map = new MultivaluedHashMap<>();
    final Reader r = new InputStreamReader(in, encoding);
    try {
      for (int ch; (ch = r.read()) != -1;) { // [ST]
        if (ch == '&') {
          map.add(name, b.toString());
          b.setLength(0);
        }
        else if (ch == '=') {
          name = b.toString();
          b.setLength(0);
        }
        else {
          b.append((char)ch);
        }
      }

      map.add(name, b.toString());
      return map;
    }
    catch (final IllegalArgumentException e) {
      throw new BadRequestException(e);
    }
  }

  public static MultivaluedMap<String,String> readFormParams(final InputStream in, final Charset encoding, final boolean decode) throws IOException {
    if (in instanceof FormServletInputStream)
      return ((FormServletInputStream)in).getFormParameterMap(decode);

    if (!decode)
      return readFormParamsEncoded(in, encoding);

    final MultivaluedLinkedHashMap<String> params = new MultivaluedLinkedHashMap<>();
    UrlEncoded.decodeTo(in, params, encoding, _maxFormContentSize, _maxFormKeys);
    return params;
  }

  static MultivaluedMap<String,String> readQueryString(final String queryString, final Charset encoding) {
    if (queryString == null || queryString.length() == 0)
      return EMPTY_MAP;

    if (encoding == null) {
      final MultivaluedHashMap<String,String> parameters = new MultivaluedHashMap<>();
      URIs.parseParameters(parameters, queryString);
      return parameters;
    }

    final MultivaluedLinkedHashMap<String> parameters = new MultivaluedLinkedHashMap<>();
    if (StandardCharsets.UTF_8.equals(encoding))
      UrlEncoded.decodeUtf8To(queryString, parameters);
    else
      UrlEncoded.decodeTo(queryString, parameters, encoding);

    return parameters;
  }

  public static void writeFormParams(final MultivaluedMap<String,String> t, final MediaType mediaType, final OutputStream entityStream) throws IOException {
    if (t.size() == 0)
      return;

    final Charset charset = MediaTypes.getCharset(mediaType);
    final OutputStreamWriter writer = new OutputStreamWriter(entityStream, charset);
    final Iterator<Map.Entry<String,List<String>>> i1 = t.entrySet().iterator();
    for (int i = 0; i1.hasNext();) { // [I]
      final Map.Entry<String,List<String>> entity = i1.next();
      final String key = entity.getKey();
      final List<String> values = entity.getValue();
      if (values.size() == 0)
        continue;

      final Iterator<String> i2 = values.iterator();
      do {
        if (i++ > 0)
          writer.write('&');

        writer.write(UrlEncoded.encodeString(key, charset));
        final String value = i2.next();
        if (value != null && value.length() > 0) {
          writer.write('=');
          writer.write(UrlEncoded.encodeString(value, charset));
        }
      }
      while (i2.hasNext());
    }

    writer.flush();
  }

  private static void consumeAndClose(final InputStream in) throws IOException {
    IOException suppressed = null;
    try {
      while (in.read() > 0);
    }
    catch (final IOException e) {
      suppressed = e;
    }
    try {
      in.close();
    }
    catch (final IOException e) {
      if (suppressed != null)
        e.addSuppressed(suppressed);

      throw e;
    }
  }

  private static class ConsumableFilterInputStream extends FilterInputStream implements Consumable {
    private boolean isConsumed;

    private ConsumableFilterInputStream(final InputStream in) {
      super(in);
    }

    @Override
    public int read() throws IOException {
      final int r = super.read();
      if (r == -1)
        isConsumed = true;

      return r;
    }

    @Override
    public boolean isConsumed() {
      return isConsumed;
    }

    @Override
    public void close() throws IOException {
      consumeAndClose(in);
    }
  }

  private static class ConsumableBufferedInputStream extends BufferedInputStream implements Consumable {
    private boolean isConsumed;

    private ConsumableBufferedInputStream(final InputStream in, final int size) {
      super(in, size);
    }

    @Override
    public synchronized int read() throws IOException {
      final int r = super.read();
      if (r == -1)
        isConsumed = true;

      return r;
    }

    @Override
    public boolean isConsumed() {
      return isConsumed;
    }

    @Override
    public void close() throws IOException {
      consumeAndClose(in);
    }
  }

  static class ConsumableByteArrayInputStream extends ByteArrayInputStream implements Consumable {
    private boolean isConsumed;

    ConsumableByteArrayInputStream(final byte[] buf) {
      super(buf);
    }

    @Override
    public synchronized int read() {
      final int r = super.read();
      if (r == -1)
        isConsumed = true;

      return r;
    }

    @Override
    public boolean isConsumed() {
      return isConsumed;
    }

    @Override
    public void close() throws IOException {
      pos = count;
    }
  }

  private static boolean hasData(final InputStream in) throws IOException {
    in.mark(1);
    if (in.read() == -1) {
      in.close();
      return false;
    }

    in.reset();
    return true;
  }

  static InputStream makeReadAwareNonEmptyOrNull(InputStream in, final boolean readAware) throws IOException {
    final boolean hasAvailable = in.available() > 0;
    if (readAware) {
      if (hasAvailable) {
        return new ConsumableFilterInputStream(in);
      }
      else if (!in.markSupported()) {
        in = new ConsumableBufferedInputStream(in, 1);
      }
      else if (!hasData(in)) {
        return null;
      }
      else {
        in = new ConsumableFilterInputStream(in);
      }
    }
    else if (hasAvailable) {
      return in;
    }
    else if (!in.markSupported()) {
      in = new BufferedInputStream(in, 1);
    }

    return hasData(in) ? in : null;
  }
}