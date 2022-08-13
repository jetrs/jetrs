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

import static org.libj.lang.Assertions.*;

import java.util.Map;

import javax.ws.rs.core.MediaType;

class CompatibleMediaType extends MediaType {
  /** A {@link CompatibleMediaType} constant representing wildcard {@value #WILDCARD} media type. */
  final static CompatibleMediaType WILDCARD_TYPE = new CompatibleMediaType(MediaType.WILDCARD_TYPE, null, 0);

  private final int distance;

  /**
   * Creates a new instance of {@link CompatibleMediaType} with the supplied type, subtype and parameters.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param parameters A map of media type parameters, {@code null} is the same as an empty map.
   * @param distance The distance.
   */
  CompatibleMediaType(final String type, final String subtype, final Map<String,String> parameters, final int distance) {
    super(type, subtype, parameters);
    this.distance = distance;
  }

  /**
   * Creates a new instance of {@link CompatibleMediaType} with the supplied type and subtype.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}
   * @param distance The distance.
   */
  CompatibleMediaType(final String type, final String subtype, final int distance) {
    super(type, subtype);
    this.distance = distance;
  }

  /**
   * Creates a new instance of {@link CompatibleMediaType} with the supplied type, subtype and {@value #CHARSET_PARAMETER}
   * parameter.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}
   * @param charset The {@value #CHARSET_PARAMETER} parameter value. If {@code null} or empty the {@value #CHARSET_PARAMETER}
   *          parameter will not be set.
   * @param distance The distance.
   */
  CompatibleMediaType(final String type, final String subtype, final String charset, final int distance) {
    super(type, subtype, charset);
    this.distance = distance;
  }

  /**
   * Creates a new instance of {@link CompatibleMediaType} with the supplied {@link MediaType} and {@code distance} parameters.
   *
   * @param mediaType The {@link MediaType}.
   * @param parameters A map of media type parameters, null is the same as an empty map.
   * @param distance The distance.
   * @throws IllegalArgumentException If {@code mediaType} is null.
   */
  CompatibleMediaType(final MediaType mediaType, final Map<String,String> parameters, final int distance) {
    super(assertNotNull(mediaType).getType(), mediaType.getSubtype(), parameters);
    this.distance = distance;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;

    if (!(obj instanceof CompatibleMediaType))
      return false;

    final CompatibleMediaType that = (CompatibleMediaType)obj;
    return distance == that.distance && super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode() ^ distance;
  }
}