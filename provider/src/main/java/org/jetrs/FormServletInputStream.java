/* Copyright (c) 2022 JetRS
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;

import org.eclipse.jetty.util.UrlEncoded;
import org.libj.net.FilterServletInputStream;

class FormServletInputStream extends FilterServletInputStream {
  private final String characterEncoding;
  private Charset charset;
  private UnmodifiableMultivaluedArrayHashMap<String,String> formParameterEncodedMap;
  private UnmodifiableMultivaluedArrayHashMap<String,String> formParameterDecodedMap;

  FormServletInputStream(final ServletInputStream in, final String characterEncoding) {
    super(in);
    this.characterEncoding = characterEncoding;
  }

  private Charset getCharacterEncoding() {
    return charset == null ? charset = characterEncoding != null ? Charset.forName(characterEncoding) : StandardCharsets.ISO_8859_1 : charset;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  UnmodifiableMultivaluedArrayHashMap<String,String> getFormParameterMap(final boolean decoded) throws IOException {
    if (formParameterEncodedMap == null)
      formParameterEncodedMap = EntityUtil.readFormParamsEncoded(in, getCharacterEncoding());

    if (!decoded)
      return formParameterEncodedMap;

    if (formParameterDecodedMap == null) {
      if (formParameterEncodedMap.size() == 0) {
        formParameterDecodedMap = new UnmodifiableMultivaluedArrayHashMap();
      }
      else {
        formParameterDecodedMap = new UnmodifiableMultivaluedArrayHashMap(formParameterEncodedMap);
        final Charset charset = getCharacterEncoding();
        for (final Map.Entry<String,List<String>> entry : formParameterDecodedMap.entrySet()) { // [S]
          final ArrayList<String> values = (ArrayList<String>)entry.getValue();
          for (int i = 0, i$ = values.size(); i < i$; ++i) { // [RA]
            final String value = values.get(i);
            values.set(i, UrlEncoded.decodeString(value, 0, value.length(), charset));
          }
        }
      }

      formParameterDecodedMap.setUnmodifiable();
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