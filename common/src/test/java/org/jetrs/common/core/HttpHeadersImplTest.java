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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.jetrs.common.ext.RuntimeDelegateTest;
import org.jetrs.common.util.HttpHeadersMap;
import org.jetrs.common.util.MirrorQualityList;
import org.junit.Test;
import org.libj.util.MirrorList;
import org.libj.util.function.TriConsumer;

public class HttpHeadersImplTest extends RuntimeDelegateTest {
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testAddFirst(final HttpHeadersMap<? super String,?,?> headers, final String name, final Object value) {
    if (value instanceof String)
      throw new IllegalArgumentException();

    final String string = value.toString();
    if (headers instanceof HttpHeadersImpl) {
      ((HttpHeadersMap)headers).addFirst(name, string);
      final MirrorList<?,?> a = ((HttpHeadersMap)headers).get(name);
      final MirrorList<?,?> b = ((HttpHeadersMap)headers.getMirrorMap()).get(name);
      assertEquals(a.getMirrorList(), b);
      assertEquals(b.getMirrorList(), a);
      assertEquals(string, headers.getFirst(name));
      assertEquals(value, headers.getMirrorMap().getFirst(name));
    }
    else {
      ((HttpHeadersMap)headers).addFirst(name, value);
      assertEquals(value, headers.getFirst(name));
      assertEquals(string, headers.getMirrorMap().getFirst(name));
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void testGetAdd(final HttpHeadersMap<? super String,?,?> headers, final String name, final Object value) {
    if (value instanceof String)
      throw new IllegalArgumentException();

    List list = headers.get(name);
    if (list == null)
      headers.put(name, list = new ArrayList<>());

    list = headers.get(name);
    final String string = value.toString();
    if (headers instanceof HttpHeadersImpl) {
      list.add(string);
      assertEquals(string, headers.getFirst(name));
      assertEquals(value, headers.getMirrorMap().getFirst(name));
    }
    else {
      list.add(value);
      assertEquals(value, headers.getFirst(name));
      assertEquals(string, headers.getMirrorMap().getFirst(name));
    }
  }

  @SuppressWarnings("rawtypes")
  private static void testAddFirstRemove(final HttpHeadersMap<String,?,?> headers, final String name, final Object value, final TriConsumer<? super HttpHeadersMap,? super String,Object> consumer) {
    assertSize(0, headers);
    consumer.accept(headers, name, value);
    assertSize(1, headers.getMirrorMap());

    testRemove(headers.getMirrorMap(), name, value);
    assertSize(0, headers.getMirrorMap());
    consumer.accept(headers.getMirrorMap(), name, value);
    assertSize(1, headers);
    testRemove(headers, name, value);
    assertSize(0, headers.getMirrorMap());

    assertSize(0, headers.getMirrorMap());
    consumer.accept(headers.getMirrorMap(), name, value);
    assertSize(1, headers);
    testRemove(headers.getMirrorMap(), name, value);
    assertSize(0, headers.getMirrorMap());
    consumer.accept(headers, name, value);
    assertSize(1, headers.getMirrorMap());
    consumer.accept(headers, name, value);
    assertSize(2, headers.get(name));
  }

  private static void testRemove(final HttpHeadersMap<String,?,?> headers, final String name, final Object value) {
    if (value instanceof String)
      throw new IllegalArgumentException();

    assertFalse(headers.remove(name, null));
    if (headers instanceof HttpHeadersImpl) {
      assertTrue(headers.remove(name, Collections.singletonList(value.toString())));
    }
    else {
      assertTrue(headers.remove(name, Collections.singletonList(value)));
    }
  }

  private static void assertSize(final int expected, final HttpHeadersMap<String,?,?> headers) {
    assertEquals(expected, headers.size());
    assertEquals(expected, headers.getMirrorMap().size());
  }

  private static void assertSize(final int expected, final MirrorList<?,?> list) {
    assertEquals(expected, list.size());
    assertEquals(expected, list.getMirrorList().size());
  }

  @Test
  public void testAddFirst() {
    final String name = HttpHeaders.ACCEPT;
    final MediaType value = MediaType.valueOf("application/json;q=.5");
    final HttpHeadersImpl headers = new HttpHeadersImpl();

    testAddFirstRemove(headers, name, value, HttpHeadersImplTest::testAddFirst);
    headers.clear();
    testAddFirstRemove(headers.getMirrorMap(), name, value, HttpHeadersImplTest::testAddFirst);
  }

  @Test
  public void testGetAdd() {
    final String name = HttpHeaders.ACCEPT;
    final MediaType value = MediaType.valueOf("application/json;q=.5");
    final HttpHeadersImpl headers = new HttpHeadersImpl();

    testAddFirstRemove(headers, name, value, HttpHeadersImplTest::testGetAdd);
    headers.clear();
    testAddFirstRemove(headers.getMirrorMap(), name, value, HttpHeadersImplTest::testGetAdd);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testStory() {
    final HttpHeadersImpl headers = new HttpHeadersImpl();

    final String acceptName = HttpHeaders.ACCEPT;
    final MediaType acceptValue1 = MediaType.valueOf("application/json;q=.5");
    headers.getMirrorMap().putSingle(acceptName, acceptValue1);
    assertEquals(acceptValue1, headers.getMirrorMap().getFirst(acceptName));
    assertEquals(acceptValue1.toString(), headers.getFirst(acceptName));

    final MediaType acceptValue2 = MediaType.valueOf("application/xml;q=.6");
    headers.add(acceptName, acceptValue2.toString());
    assertEquals(acceptValue2, headers.getMirrorMap().getFirst(acceptName));
    assertEquals(acceptValue2.toString(), headers.getFirst(acceptName));

    final String contentTypeName = HttpHeaders.CONTENT_TYPE;
    final List<Object> contentTypes = new ArrayList<>();
    final MediaType contentType1 = MediaType.valueOf("text/html; charset=UTF-8; q=.1");
    contentTypes.add(contentType1);
    headers.getMirrorMap().put(contentTypeName, contentTypes);
//    contentTypes.add(Boolean.TRUE);

    assertEquals(contentType1, headers.getMirrorMap().getFirst(contentTypeName));
    assertEquals(contentType1.toString(), headers.getFirst(contentTypeName));

    final MediaType contentType2 = MediaType.valueOf("text/plain; charset=UTF-8; q=.9");
    headers.get(contentTypeName).add(contentType2.toString());
    assertEquals(contentType2, headers.getMirrorMap().getFirst(contentTypeName));
    assertEquals(contentType2.toString(), headers.getFirst(contentTypeName));

    final MediaType contentType3 = MediaType.valueOf("text/xml; charset=UTF-8; q=1");
    headers.get(contentTypeName).getMirrorList().add(contentType3);
    assertEquals(contentType3, headers.getMirrorMap().getFirst(contentTypeName));
    assertEquals(contentType3.toString(), headers.getFirst(contentTypeName));

    final MediaType contentType4 = MediaType.valueOf("text/csv; charset=UTF-8; q=.2");
    headers.get(contentTypeName).add(contentType4.toString());
    assertEquals(contentType3, headers.getMirrorMap().getFirst(contentTypeName));
    assertEquals(contentType3.toString(), headers.getFirst(contentTypeName));

    final Iterator<Map.Entry<String,List<Object>>> mapIterator = headers.getMirrorMap().entrySet().iterator();
    final Map.Entry<String,?> entry = mapIterator.next();
    mapIterator.remove();

    final String key = entry.getKey();
    final MirrorQualityList<MediaType,String> list = (MirrorQualityList<MediaType,String>)entry.getValue();
    assertFalse(headers.containsKey(key));

    final ListIterator<MediaType> listIterator = list.listIterator();
    final MediaType value = listIterator.next();

    listIterator.remove();
    assertFalse(list.contains(value));
    assertFalse(list.getMirrorList().contains(value.toString()));

    list.getMirrorList().add(value.toString());
    assertTrue(list.contains(value));

    headers.put(key, list.getMirrorList());
    assertTrue(headers.getMirrorMap().containsKey(key));

    assertSame(list.getMirrorList(), headers.get(key));
    assertSame(list, headers.getMirrorMap().get(key));
  }
}