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

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.libj.lang.Numbers;

class ServerMediaType extends QualifiedMediaType {
  /** A {@link ServerMediaType} constant representing wildcard {@value #WILDCARD} media type. */
  static final ServerMediaType WILDCARD_TYPE = new ServerMediaType(MediaType.WILDCARD_TYPE);

  /** A {@link ServerMediaType} constant representing wildcard {@value #APPLICATION_OCTET_STREAM} media type. */
  static final ServerMediaType APPLICATION_OCTET_STREAM_TYPE = new ServerMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);

  public static ServerMediaType valueOf(final String string) {
    return MediaTypes.parse(ServerMediaType.class, string);
  }

  static ServerMediaType[] valueOf(final String ... strings) {
    return MediaTypes.parse(ServerMediaType.class, strings);
  }

  /**
   * Creates a new instance of {@link ServerMediaType} with the supplied type, subtype and parameters.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}.
   * @param parameters A map of media type parameters, {@code null} is the same as an empty map.
   * @throws IllegalArgumentException If {@code parameters} contains "q" instead of "qs".
   */
  ServerMediaType(final String type, final String subtype, final Map<String,String> parameters) {
    super(type, subtype, parameters);
    if (getParameters().containsKey("q"))
      throw new IllegalArgumentException("@Produces must specify \"qs\" instead of \"q\" parameter");
  }

  /**
   * Creates a new instance of {@link ServerMediaType} with the supplied type and subtype.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}
   */
  ServerMediaType(final String type, final String subtype) {
    super(type, subtype);
  }

  /**
   * Creates a new instance of {@link ServerMediaType} with the supplied type, subtype and {@value #CHARSET_PARAMETER} parameter.
   *
   * @param type The primary type, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}
   * @param subtype The subtype, {@code null} is equivalent to {@link #MEDIA_TYPE_WILDCARD}
   * @param charset The {@value #CHARSET_PARAMETER} parameter value. If {@code null} or empty the {@value #CHARSET_PARAMETER}
   *          parameter will not be set.
   */
  ServerMediaType(final String type, final String subtype, final String charset) {
    super(type, subtype, charset);
  }

  /**
   * Creates a new instance of {@link ServerMediaType} with the supplied {@link MediaType} parameter.
   *
   * @param mediaType The {@link MediaType}.
   * @throws NullPointerException If {@code mediaType} is null.
   */
  private ServerMediaType(final MediaType mediaType) {
    super(mediaType);
  }

  @Override
  public Float getQuality() {
    return Numbers.parseFloat(getParameters().get("qs"));
  }
}