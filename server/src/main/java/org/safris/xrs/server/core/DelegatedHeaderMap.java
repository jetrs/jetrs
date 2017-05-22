/* Copyright (c) 2016 lib4j
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

package org.safris.xrs.server.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.safris.commons.util.MirroredDelegatedList;
import org.safris.xrs.server.util.DelegatedMultivaluedHashMap;
import org.safris.xrs.server.util.MediaTypes;
import org.safris.xrs.server.util.MirroredDelegatedMultivaluedHashMap;

public class DelegatedHeaderMap extends MirroredDelegatedMultivaluedHashMap<String,String,Object> implements Cloneable {
  private static final long serialVersionUID = -424669813370868690L;

  public DelegatedHeaderMap(final HttpServletResponse response) {
    super(HashMap.class, ArrayList.class, new MirroredDelegatedList.Mirror<String,Object>() {
      @Override
      public Object reflect(final String value) {
        return value == null ? null : MediaTypes.parse(value);
      }
    }, new MirroredDelegatedList.Mirror<Object,String>() {
      @Override
      public String reflect(final Object value) {
        if (value == null)
          return null;

        if (value instanceof MediaType)
          return ((MediaType)value).toString();

        throw new IllegalArgumentException("Unexpected type: " + value.getClass());
      }
    }, new DelegatedMultivaluedHashMap.MultivaluedMapDelegate<String,String>() {
      @Override
      public void put(final String key, final List<String> value) {
        if (value != null && value.size() > 0)
          response.setHeader(key, value.get(0));

        for (int i = 1; i < value.size(); i++)
          response.addHeader(key, value.get(i));
      }

      @Override
      public void remove(final Object key) {
        throw new UnsupportedOperationException("Removal of header key from HttpServletResponse is not defined.");
      }

      @Override
      public void putSingle(final String key, final String value) {
        response.setHeader(key, value);
      }

      @Override
      public void add(final String key, final String value) {
        response.addHeader(key, value);
      }

      @Override
      public void addFirst(final String key, final String value) {
        final Collection<String> headers = response.getHeaders(key);
        response.setHeader(key, value);
        for (final String header : headers)
          response.addHeader(key, header);
      }

      private Collection<String> ensureIndexValid(final String key, final int index) {
        final Collection<String> headers = response.getHeaders(key);
        if (index < 0 || headers.size() < index)
          throw new IndexOutOfBoundsException("size = " + headers.size() + ", index == " + index);

        return headers;
      }

      @Override
      public void add(final String key, final int index, final String element) {
        final Collection<String> headers = ensureIndexValid(key, index);
        if (headers.size() == 0) {
          if (index != 0)
            throw new IndexOutOfBoundsException("size = " + headers.size() + ", index == " + index);

          response.setHeader(key, element);
        }
        else {
          final Iterator<String> iterator = headers.iterator();
          response.setHeader(key, index == 0 ? element : iterator.next());
          int i = 1;
          while (iterator.hasNext()) {
            if (index == i)
              response.addHeader(key, element);

            response.addHeader(key, iterator.next());
          }
        }
      }

      @Override
      public void remove(final String key, final int index) {
        final Collection<String> headers = ensureIndexValid(key, index);
        final Iterator<String> iterator = headers.iterator();
        int i = 0;
        while (iterator.hasNext()) {
          if (index != i)
            response.addHeader(key, iterator.next());
        }
      }
    }, null);
  }

  public DelegatedHeaderMap(final DelegatedHeaderMap copy) {
    super(HashMap.class, copy.listType, copy.getMirror(), copy.getMirroredMap().getMirror(), copy.delegate, copy.getMirroredMap().getDelegate());
  }

  @Override
  public DelegatedHeaderMap clone() {
    return new DelegatedHeaderMap(this);
  }
}