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

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.libj.lang.Strings;

public class UriInfoImplTest {
  // FIXME: Implement a test around this
  private static void testQueryParameters(final HttpServletRequest request) {
    final MultivaluedArrayHashMap<String,String> queryParametersDecoded = new MultivaluedArrayHashMap<>();
    final MultivaluedArrayHashMap<String,String> queryParametersEncoded = new MultivaluedArrayHashMap<>();
    final URI absoluteUri = URI.create(UriInfoImpl.getAbsoluteUri(request));
    final String queryString = absoluteUri.getRawQuery();
    if (queryString == null || queryString.equals(""))
      return;

    final String[] params = Strings.split(queryString, '&');
    for (final String param : params) { // [A]
      if (param.indexOf('=') >= 0) {
        final String[] nv = param.split("=", 2);
        try {
          final String name = URLDecoder.decode(nv[0], StandardCharsets.UTF_8.name());
          final String value = nv.length > 1 ? nv[1] : "";
          queryParametersEncoded.add(name, value);
          queryParametersDecoded.add(name, URLDecoder.decode(value, StandardCharsets.UTF_8.name()));
        }
        catch (final UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
      else {
        try {
          final String name = URLDecoder.decode(param, StandardCharsets.UTF_8.name());
          queryParametersEncoded.add(name, "");
          queryParametersDecoded.add(name, "");
        }
        catch (final UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    }

    final UriInfoImpl uriInfo = new UriInfoImpl(request, null);
    assertEquals(queryParametersDecoded, uriInfo.getQueryParameters(true));
    assertEquals(queryParametersEncoded, uriInfo.getQueryParameters(false));
  }
}