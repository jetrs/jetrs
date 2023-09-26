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

public final class CommonProperties {
  /**
   * The size of chunks in bytes for HTTP chunk-encoded messages.
   * <p>
   * Default: {@value #CHUNKED_ENCODING_SIZE_DEFAULT}.
   * <p>
   * Configuration property: <tt>{@value}</tt>
   *
   * @implNote This property is relevant for all messages that specify {@code "chunked"} in the {@value HttpHeaders#TRANSFER_ENCODING}
   *           header, or if the {@value HttpHeaders#CONTENT_LENGTH} header is not specified and the encoded entity size exceeds the
   *           buffer size as configured by the {@link #CONTENT_LENGTH_BUFFER} property, thus switching the transfer to chunked
   *           encoding.
   * @see #CONTENT_LENGTH_BUFFER
   */
  public static final String CHUNKED_ENCODING_SIZE = "jetrs.chunkedEncodingSize";

  /**
   * Default size of chunks for HTTP chunk-encoded messages.
   */
  public static final int CHUNKED_ENCODING_SIZE_DEFAULT = 4096;

  /**
   * An integer value that defines the buffer size used to buffer the outbound message entity in order to determine its size and set
   * the value of the HTTP <tt>{@value HttpHeaders#CONTENT_LENGTH}</tt> header.
   * <p>
   * If the entity size exceeds the configured buffer size, buffering would be cancelled and the transfer would switch to chunked
   * encoding. A value less than or equal to zero disables the buffering of the entity.
   * <p>
   * Default: {@value #CONTENT_LENGTH_BUFFER_DEFAULT}.
   * <p>
   * Configuration property: <tt>{@value}</tt>
   *
   * @implNote This property is irrelevant for all messages that specify {@code "chunked"} in the
   *           {@value HttpHeaders#TRANSFER_ENCODING} header.
   * @see #CHUNKED_ENCODING_SIZE
   */
  public static final String CONTENT_LENGTH_BUFFER = "jetrs.contentLength.buffer";

  /**
   * Default size of the buffer used for the outbound message entity in order to determine its size and set the value of the HTTP
   * <code>{@value HttpHeaders#CONTENT_LENGTH}</code> header.
   */
  public static final int CONTENT_LENGTH_BUFFER_DEFAULT = 8192;

  /**
   * Disable standard providers for specified entity classes from automatically loading during startup.
   * @formatter:off
   * <blockquote>
   * <table>
   * <caption>Standard Providers</caption>
   * <tr><td><code>java.io.File</code></td></tr>
   * <tr><td><code>java.io.InputStream</code></td></tr>
   * <tr><td><code>java.io.Reader</code></td></tr>
   * <tr><td><code>java.lang.Boolean</code></td></tr>
   * <tr><td><code>java.lang.Character</code></td></tr>
   * <tr><td><code>java.lang.Number</code></td></tr>
   * <tr><td><code>javax.activation.DataSource</code></td></tr>
   * <tr><td><code>javax.ws.rs.core.MultivaluedMap</code></td></tr>
   * <tr><td><code>javax.ws.rs.core.StreamingOutput</code></td></tr>
   * <tr><td><code>javax.xml.bind.JAXBElement</code></td></tr>
   * <tr><td><code>javax.xml.transform.Source</code></td></tr>
   * </table>
   * </blockquote>
   * @formatter:on
   * Multiple default providers can be disabled with comma separated keys, or {@code "*"} can be specified to disable all default
   * providers.
   *
   * @implNote A provider will be disabled if the name of its declared entity class or each superclass matches one of the provided
   *           class names set for this property.
   */
  public static final String DISABLE_STANDARD_PROVIDER = "jetrs.disableStandardProvider";

  /**
   * Specifies whether JAX-RS Services Loading (via SPI) is enabled. If absent or {@code "true"}, JAX-RS Services Loading (via SPI) of
   * {@link javax.ws.rs.core.Feature} or {@link javax.ws.rs.container.DynamicFeature} is enabled.
   */
  public static final String JAXRS_LOAD_SERVICES = "jakarta.ws.rs.loadServices";

  private CommonProperties() {
  }
}