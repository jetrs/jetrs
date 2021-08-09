/* Copyright (c) 2019 LibJ
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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.libj.util.MirrorList;

public class MirrorMultivaluedMapTest {
  @Test
  public void test() {
    final MirrorList.Mirror<String,Integer> mirror = new MirrorList.Mirror<String,Integer>() {
      @Override
      public Integer valueToReflection(final String value) {
        return Integer.valueOf(value);
      }

      @Override
      public String reflectionToValue(final Integer reflection) {
        return String.valueOf(reflection);
      }
    };

    final MirrorMultivaluedMap<String,String,Integer> map = new MirrorMultivaluedMap<>(new HashMap<>(), new HashMap<>(), new MirrorMultivaluedMap.Mirror<String,String,Integer>() {
      private MirrorList.Mirror<Integer,String> reverse;

      @Override
      public MirrorList<Integer,String> valueToReflection(final String key, final List<String> value) {
        return new MirrorList<>(new ArrayList<>(), value != null ? value : new ArrayList<>(), reverse == null ? reverse = mirror.reverse() : reverse);
      }

      @Override
      public MirrorList<String,Integer> reflectionToValue(final String key, final List<Integer> reflection) {
        return new MirrorList<>(new ArrayList<>(), reflection != null ? reflection : new ArrayList<>(), mirror);
      }
    });

    map.putSingle("1", "1");
    assertEquals(Integer.valueOf(1), map.getMirrorMap().getFirst("1"));

    map.getMirrorMap().putSingle("2", 2);
    assertEquals(Integer.valueOf(2), map.getMirrorMap().getFirst("2"));

    final Iterator<Map.Entry<String,List<String>>> stringIterator = map.entrySet().iterator();
    final Map.Entry<String,List<String>> stringNext = stringIterator.next();
    stringIterator.remove();
    assertFalse(map.getMirrorMap().containsKey(stringNext.getKey()));

    final Iterator<Map.Entry<String,List<Integer>>> integerIterator = map.getMirrorMap().entrySet().iterator();
    final Map.Entry<String,List<Integer>> integerNext = integerIterator.next();
    integerIterator.remove();
    assertFalse(map.containsKey(integerNext.getKey()));
  }
}