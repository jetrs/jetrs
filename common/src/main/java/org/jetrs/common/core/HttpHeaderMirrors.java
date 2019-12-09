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

import java.util.HashMap;
import java.util.Map;

import org.libj.util.MirrorList;
import org.libj.util.MirrorList.Mirror;

final class HttpHeaderMirrors {
  private static final Map<String,Mirror<String,Object>> forward = new HashMap<>();
  private static final Map<String,Mirror<Object,String>> reverse = new HashMap<>();

  static Mirror<String,Object> getMirrorForward(final String key) {
    Mirror<String,Object> mirror = forward.get(key);
    if (mirror == null) {
      forward.put(key, mirror = new MirrorList.Mirror<String,Object>() {
        @Override
        public Object valueToReflection(final String reflection) {
          return HttpHeadersUtil.valueToReflection(key, reflection, true);
        }

        @Override
        public String reflectionToValue(final Object value) {
          return HttpHeadersUtil.reflectionToValue(value);
        }
      });

      reverse.put(key, mirror.reverse());
    }

    return mirror;
  }

  static Mirror<Object,String> getMirrorReverse(final String key) {
    Mirror<Object,String> mirror = reverse.get(key);
    if (mirror == null) {
      reverse.put(key, mirror = new MirrorList.Mirror<Object,String>() {
        @Override
        public String valueToReflection(final Object value) {
          return HttpHeadersUtil.reflectionToValue(value);
        }

        @Override
        public Object reflectionToValue(final String reflection) {
          return HttpHeadersUtil.valueToReflection(key, reflection, true);
        }
      });

      forward.put(key, mirror.reverse());
    }

    return mirror;
  }

  private HttpHeaderMirrors() {
  }
}