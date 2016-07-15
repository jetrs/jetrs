/* Copyright (c) 2015 Seva Safris
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
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.safris.commons.io.Readers;
import org.safris.commons.lang.PackageLoader;
import org.safris.commons.lang.PackageNotFoundException;
import org.safris.commons.util.Collections;
import org.safris.commons.util.StringBuilderReader;
import org.safris.xrs.xjb.decoder.BooleanDecoder;
import org.safris.xrs.xjb.decoder.NumberDecoder;
import org.safris.xrs.xjb.decoder.ObjectDecoder;
import org.safris.xrs.xjb.decoder.StringDecoder;

public abstract class JSObjectUtil {
  private static final BooleanDecoder booleanDecoder = new BooleanDecoder();
  private static final NumberDecoder numberDecoder = new NumberDecoder();
  private static final StringDecoder stringDecoder = new StringDecoder();
  private static final ObjectDecoder objectDecoder = new ObjectDecoder();

  private static final Map<String,Class<? extends JSObject>> bindings = new HashMap<String,Class<? extends JSObject>>();

  static {
    try {
      PackageLoader.getSystemPackageLoader().loadPackage("json");
    }
    catch (final PackageNotFoundException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  protected static <T>void clone(final Property<T> property, final Property<T> clone) {
    property.clone(clone);
  }

  protected static <T>T get(final Property<T> property) {
    return property.get();
  }

  protected static <T>void set(final Property<T> property, final T value) {
    property.set(value);
  }

  protected static <T>boolean wasSet(final Property<T> property) {
    return property.wasSet();
  }

  protected static <T>T encode(final Property<T> property) throws EncodeException {
    return property.encode();
  }

  protected static <T>void decode(final Property<T> property, final StringBuilderReader reader) throws DecodeException, IOException {
    property.decode(reader);
  }

  protected static void registerBinding(final String name, final Class<? extends JSObject> bindingClass) {
    bindings.put(name, bindingClass);
  }

  protected static String pad(final int depth) {
    final StringBuilder out = new StringBuilder();
    for (int i = 0; i < depth; i++)
      out.append("  ");

    return out.toString();
  }

  protected static char next(final Reader reader) throws IOException {
    int ch;
    while (true) {
      if ((ch = reader.read()) == -1)
        throw new IOException("EOS");

      if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r')
        return (char)ch;
    }
  }

  protected static char nextAny(final Reader reader) throws IOException {
    int ch;
    while (true) {
      if ((ch = reader.read()) == -1)
        throw new IOException("EOS");

      return (char)ch;
    }
  }

  protected static boolean isNull(char ch, final Reader reader) throws IOException {
    return ch == 'n' && next(reader) == 'u' && next(reader) == 'l' && next(reader) == 'l';
  }

  protected static String encode(final JSObject object, final int depth) {
    final StringBuilder out = new StringBuilder("{\n");
    out.append(object._encode(depth)).append("\n").append(pad(depth - 1)).append("}");
    return out.toString();
  }

  protected static <T> String tokenize(final Collection<T> value, final int depth) {
    if (value == null)
      return "null";

    if (value.size() == 0)
      return "[]";

    final StringBuilder out = new StringBuilder();
    for (final T part : value)
      out.append(", ").append(part == null ? "null" : part instanceof JSObject ? encode((JSObject)part, depth) : "\"" + part + "\"");

    return "[" + out.substring(2) + "]";
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected static JSObject decode(final StringBuilderReader reader, char ch, final JSObject jsObject) throws DecodeException, IOException {
    boolean hasOpenBrace = false;
    boolean hasStartQuote = false;
    final StringBuilder out = new StringBuilder();
    while (true) {
      if (ch == '{') {
        if (hasOpenBrace) {
          Readers.readFully(reader);
          throw new DecodeException("Malformed JSON", reader.getStringBuilder().toString(), jsObject._bundle());
        }

        hasOpenBrace = true;
      }
      else {
        if (!hasOpenBrace) {
          if (isNull(ch, reader))
            return null;

          Readers.readFully(reader);
          throw new DecodeException("Malformed JSON", reader.getStringBuilder().toString(), jsObject._bundle());
        }

        try {
          if (ch == '"') {
            if (!hasStartQuote) {
              hasStartQuote = true;
            }
            else {
              hasStartQuote = false;
              ch = next(reader);
              if (ch != ':') {
                Readers.readFully(reader);
                throw new DecodeException("Malformed JSON", reader.getStringBuilder().toString(), jsObject._bundle());
              }

              // Special case for parsing the container object
              final Binding<?> member = jsObject._getBinding(out.toString());
              if (member == null) {
                Readers.readFully(reader);
                throw new DecodeException("Unknown object name: " + out, reader.getStringBuilder().toString(), jsObject._bundle());
              }

              out.setLength(0);
              ch = next(reader);
              final boolean isArray = ch == '[';
              final Object value;
              if (JSObject.class.isAssignableFrom(member.type)) {
                if (member.isAbstract) {
                  Readers.readFully(reader);
                  throw new DecodeException("\"" + member.name + "\" is an abstract type", reader.getStringBuilder().toString(), jsObject._bundle());
                }

                value = isArray ? Collections.asCollection(ArrayList.class, objectDecoder.recurse(reader, member.type, 0)) : decode(reader, ch, (JSObject)member.type.newInstance());
              }
              else if (member.type == String.class) {
                value = isArray ? Collections.asCollection(ArrayList.class, stringDecoder.recurse(reader, 0)) : stringDecoder.decode(reader, ch);
                // final Pattern pattern = member.field.getAnnotation(Pattern.class);
                // if (pattern != null && value != null && !((String)value).matches(pattern.value()))
                // throw new DecodeException("\"" + member.name + "\" does not match pattern \"" + pattern.value() + "\": " + value + "\"", jsObject);
              }
              else if (member.type == Boolean.class) {
                value = isArray ? Collections.asCollection(ArrayList.class, booleanDecoder.recurse(reader, 0)) : booleanDecoder.decode(reader, ch);
              }
              else if (member.type == Number.class) {
                value = isArray ? Collections.asCollection(ArrayList.class, numberDecoder.recurse(reader, 0)) : numberDecoder.decode(reader, ch);
              }
              else {
                throw new UnsupportedOperationException("Unexpected type: " + member.type);
              }

              if (member.required && member.notNull && value == null) {
                Readers.readFully(reader);
                throw new DecodeException("\"" + member.name + "\" cannot be null", reader.getStringBuilder().toString(), jsObject._bundle());
              }

              final Property property = (Property)member.property.get(jsObject);
              property.set(value);
              property.decode(reader);
            }
          }
          else {
            if (ch == '}') {
              for (final Binding<?> binding : jsObject._bindings()) {
                final Property<?> property = (Property<?>)binding.property.get(jsObject);
                if (binding.required) {
                  if (!property.wasSet()) {
                    Readers.readFully(reader);
                    throw new DecodeException("\"" + binding.name + "\" is required", reader.getStringBuilder().toString(), jsObject._bundle());
                  }

                  if (binding.notNull && property.get() == null) {
                    Readers.readFully(reader);
                    throw new DecodeException("\"" + binding.name + "\" cannot be null", reader.getStringBuilder().toString(), jsObject._bundle());
                  }
                }
                else if (property.wasSet() && binding.notNull && property.get() == null) {
                  Readers.readFully(reader);
                  throw new DecodeException("\"" + binding.name + "\" cannot be null", reader.getStringBuilder().toString(), jsObject._bundle());
                }
              }

              return jsObject;
            }

            if (ch != ',') {
              out.append(ch);
            }
          }
        }
        catch (final ReflectiveOperationException e) {
          throw new Error(e);
        }
      }

      ch = next(reader);
    }
  }
}