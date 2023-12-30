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

import java.util.Comparator;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.libj.lang.Numbers;

class QualifiedMediaType extends MediaType implements Qualified {
  static final Comparator<MediaType> QUALITY_COMPARATOR = (final MediaType o1, final MediaType o2) -> {
    Float q1 = getQuality(o2);
    if (q1 == null)
      q1 = 1f;

    Float q2 = getQuality(o1);
    if (q2 == null)
      q2 = 1f;

    return Float.compare(q1, q2);
  };

  static Float getQuality(final MediaType mediaType) {
    return Numbers.parseFloat(mediaType.getParameters().get("q"));
  }

  /**
   * Creates a new instance of {@link QualifiedMediaType} with the supplied type, subtype and parameters.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param parameters A map of media type parameters, {@code null} is the same as an empty map.
   */
  QualifiedMediaType(final String type, final String subtype, final Map<String,String> parameters) {
    super(type, subtype, parameters);
  }

  /**
   * Creates a new instance of {@link QualifiedMediaType} with the supplied type and subtype.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   */
  QualifiedMediaType(final String type, final String subtype) {
    super(type, subtype);
  }

  /**
   * Creates a new instance of {@link QualifiedMediaType} with the supplied type, subtype and {@value #CHARSET_PARAMETER} parameter.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param charset The {@value #CHARSET_PARAMETER} parameter value. If {@code null} or empty the {@value #CHARSET_PARAMETER}
   *          parameter will not be set.
   */
  QualifiedMediaType(final String type, final String subtype, final String charset) {
    super(type, subtype, charset);
  }

  /**
   * Creates a new instance of {@link QualifiedMediaType} with the supplied {@link MediaType} parameter.
   *
   * @param mediaType The {@link MediaType}.
   * @throws NullPointerException If {@code mediaType} is null.
   */
  QualifiedMediaType(final MediaType mediaType) {
    super(mediaType.getType(), mediaType.getSubtype(), mediaType.getParameters());
  }

  @Override
  public Float getQuality() {
    return getQuality(this);
  }
}