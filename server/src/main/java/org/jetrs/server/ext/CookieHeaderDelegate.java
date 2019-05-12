/* Copyright (c) 2019 OpenJAX
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

package org.jetrs.server.ext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.RuntimeDelegate;

public class CookieHeaderDelegate implements RuntimeDelegate.HeaderDelegate<Cookie> {
  public static Map<String,Cookie> parse(final String[] values) {
    if (values.length == 0)
      return Collections.EMPTY_MAP;

    final Map<String,Cookie> nameToCookie = new HashMap<>();
    for (final String value : values) {
      final Cookie cookie = Cookie.valueOf(value);
      if (cookie != null)
        nameToCookie.put(cookie.getName(), cookie);
    }

    return nameToCookie;
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
}