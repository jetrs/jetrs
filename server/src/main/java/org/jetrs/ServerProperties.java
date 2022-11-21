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

public final class ServerProperties {
  /**
   * Server-specific version of the common property {@link CommonProperties#CHUNKED_ENCODING_SIZE}, which, if present, overrides the
   * common property for the server runtime.
   */
  public static final String CHUNKED_ENCODING_SIZE_SERVER = "jetrs.server.chunkedEncodingSize";

  /**
   * Server-specific version of the common property {@link CommonProperties#CONTENT_LENGTH_BUFFER}, which, if present, overrides the
   * common property for the server runtime.
   */
  public static final String CONTENT_LENGTH_BUFFER_SERVER = "jetrs.server.contentLength.buffer";

  private ServerProperties() {
  }
}