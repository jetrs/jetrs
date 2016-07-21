/* Copyright (c) 2016 Seva Safris
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

package org.safris.xrs.xjb;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

import org.safris.commons.io.Readers;
import org.safris.commons.lang.Numbers;
import org.safris.commons.net.URIComponent;
import org.safris.commons.util.StringBuilderReader;

public class Property<T> {
  @SuppressWarnings("unchecked")
  private static <T>T encode(final T value, final JSObject jsObject) {
    if (value instanceof Number) {
      final Number number = (Number)value;
      return (T)(number.intValue() == number.doubleValue() ? String.valueOf(number.intValue()) : String.valueOf(number.doubleValue()));
    }

    if (value instanceof String) {
      try {
        return (T)URIComponent.encode((String)value, "UTF-8");
      }
      catch (final UnsupportedEncodingException e) {
        throw new EncodeException(e.getMessage(), jsObject, e);
      }
    }

    return value;
  }

  @SuppressWarnings("unchecked")
  private static <T>T decode(final T value, final JSObject jsObject) throws DecodeException {
    try {
      return value instanceof String ? (T)URIComponent.decode(((String)value), "UTF-8") : value;
    }
    catch (final UnsupportedEncodingException e) {
      throw new DecodeException(e.getMessage(), jsObject._bundle(), e);
    }
  }

  private final JSObject jsObject;
  private final Binding<T> binding;
  private boolean wasSet = false;
  private T value;

  public Property(final JSObject jsObject, final Binding<T> binding) {
    this.jsObject = jsObject;
    this.binding = binding;
  }

  protected void clone(final Property<T> clone) {
    this.wasSet = clone.wasSet;
    this.value = clone.value;
  }

  protected T get() {
    return value;
  }

  protected void set(final T value) {
    this.wasSet = true;
    this.value = value;
  }

  public void clear() {
    this.wasSet = false;
    this.value = null;
  }

  protected boolean wasSet() {
    return wasSet;
  }

  @SuppressWarnings("unchecked")
  protected T encode() throws EncodeException {
    final String error = binding.validate(value);
    if (error != null)
      throw new EncodeException(error, jsObject);

    if (value instanceof Collection<?>) {
      final Collection<T> collection = (Collection<T>)value;
      final Collection<T> encoded = new ArrayList<T>(collection.size());
      for (final T item : collection)
        encoded.add(encode(item, jsObject));

      return (T)encoded;
    }

    return encode(value, jsObject);
  }

  @SuppressWarnings("unchecked")
  protected void decode(final StringBuilderReader reader) throws DecodeException, IOException {
    final String error = binding.validate(value);
    if (error != null) {
      Readers.readFully(reader);
      throw new DecodeException(error, reader.getStringBuilder().toString(), jsObject._bundle());
    }

    if (value instanceof Collection<?>) {
      final Collection<T> collection = (Collection<T>)value;
      final Collection<T> decoded = new ArrayList<T>(collection.size());
      for (final T item : collection)
        decoded.add(decode(item, jsObject));

      this.value = (T)decoded;
    }
    else {
      this.value = decode(value, jsObject);
    }
  }

  @Override
  public int hashCode() {
    final int hashCode = 1917841483;
    return value == null ? hashCode : hashCode ^ 31 * value.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this)
      return true;

    if (!(obj instanceof Property))
      return false;

    final Property<?> that = (Property<?>)obj;
    if (that.value instanceof Number)
      return Numbers.equivalent((Number)value, (Number)that.value);

    return that.value != null ? that.value.equals(value) : value == null;
  }
}