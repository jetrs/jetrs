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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.jetty.util.UrlEncoded;
import org.libj.net.FilterServletInputStream;

class FormServletInputStream extends FilterServletInputStream {
  private final String characterEncoding;
  private Charset charset;
  private MultivaluedHashMap<String,String> formParameterEncodedMap;
  private MultivaluedHashMap<String,String> formParameterDecodedMap;

  FormServletInputStream(final ServletInputStream in, final String characterEncoding) {
    super(in);
    this.characterEncoding = characterEncoding;
  }

  private Charset getCharacterEncoding() {
    return charset == null ? charset = characterEncoding != null ? Charset.forName(characterEncoding) : StandardCharsets.ISO_8859_1 : charset;
  }

  @SuppressWarnings({"cast", "rawtypes", "unchecked"})
  MultivaluedHashMap<String,String> getFormParameterMap(final boolean decoded) {
    if (formParameterEncodedMap == null) {
      try {
        formParameterEncodedMap = EntityUtil.readFormParamsEncoded(in, getCharacterEncoding());
      }
      catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    if (!decoded)
      return formParameterEncodedMap;

    if (formParameterDecodedMap == null) {
      formParameterDecodedMap = new MultivaluedHashMap((MultivaluedMap<?,?>)formParameterEncodedMap);
      if (formParameterDecodedMap.size() > 0) {
        final Charset charset = getCharacterEncoding();
        for (final Map.Entry<String,List<String>> entry : formParameterDecodedMap.entrySet()) {
          final ArrayList<String> values = (ArrayList<String>)entry.getValue();
          for (int i = 0, i$ = values.size(); i < i$; ++i) { // [RA]
            final String value = values.get(i);
            values.set(i, UrlEncoded.decodeString(value, 0, value.length(), charset));
          }
        }
      }
    }

    return formParameterDecodedMap;
  }

  @Override
  public void close() throws IOException {
    charset = null;
    formParameterEncodedMap = null;
    formParameterDecodedMap = null;
    super.close();
  }
}