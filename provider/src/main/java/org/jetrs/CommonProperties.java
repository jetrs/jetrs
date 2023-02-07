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

import org.libj.lang.Numbers;

public final class CommonProperties {
  /**
   * The size of chunks in bytes for HTTP chunk-encoded messages.
   * <p>
   * Default: {@value #CHUNKED_ENCODING_SIZE_DEFAULT}.
   * <p>
   * Configuration property: <tt>{@value}</tt>
   *
   * @implNote This property is relevant for all messages that specify {@code "chunked"} in the
   *           {@value HttpHeaders#TRANSFER_ENCODING} header, or if the {@value HttpHeaders#CONTENT_LENGTH} header is not specified
   *           and the encoded entity size exceeds the buffer size as configured by the {@link #CONTENT_LENGTH_BUFFER} property,
   *           thus switching the transfer to chunked encoding.
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
   * The default value is {@value #CONTENT_LENGTH_BUFFER_DEFAULT}.
   * </p>
   * <p>
   * The name of the configuration property is <tt>{@value}</tt>.
   * </p>
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
   * Disable specified default providers from automatically loading during startup. Certain providers extend the application's
   * footprint with dependencies (such as XML and AWT), that may possibly be not available in certain runtimes.
   *
   * <blockquote>
   * <table>
   * <caption>Default Providers</caption>
   * <tr><td><b>Key</b></td><td><b>Provider</b></td></tr>
   * <tr><td><code>DATASOURCE</code></td><td><code>javax.activation.DataSource</code></td></tr>
   * <tr><td><code>RENDEREDIMAGE</code></td><td><code>java.awt.image.RenderedImage</code></td></tr>
   * <tr><td><code>SOURCE</code></td><td><code>javax.xml.transform.Source</code></td></tr>
   * <tr><td><code>DOMSOURCE</code></td><td><code>javax.xml.transform.dom.DOMSource</code></td></tr>
   * <tr><td><code>SAXSOURCE</code></td><td><code>javax.xml.transform.sax.SAXSource</code></td></tr>
   * <tr><td><code>STREAMSOURCE</code></td><td><code>javax.xml.transform.stream.StreamSource</code></td></tr>
   * </table>
   * </blockquote>
   *
   * Multiple default providers can be disabled with comma separated keys, or {@code ALL} can be specified to disable all default
   * providers
   */
  public static final String DISABLE_DEFAULT_PROVIDER = "jetrs.disableDefaultProvider";

  /**
   * Specifies whether JAX-RS Services Loading (via SPI) is enabled. If absent or {@code "true"}, JAX-RS Services Loading (via SPI)
   * of {@link javax.ws.rs.core.Feature} or {@link javax.ws.rs.container.DynamicFeature} is enabled.
   */
  public static final String JAXRS_LOAD_SERVICES = "jakarta.ws.rs.loadServices";

  public static int getPropertyValue(final String commonProperty, final String specificProperty, final int defaultValue) {
    String value = System.getProperty(specificProperty);
    if (value != null)
      return Numbers.parseInt(value, defaultValue);

    return getPropertyValue(commonProperty, defaultValue);
  }

  public static int getPropertyValue(final String commonProperty, final int defaultValue) {
    String value = System.getProperty(commonProperty);
    if (value != null)
      return Numbers.parseInt(value, defaultValue);

    return defaultValue;
  }

  public static String getPropertyValue(final String commonProperty, final String specificProperty) {
    final String value = System.getProperty(specificProperty);
    return value != null ? value : System.getProperty(commonProperty);
  }

  private CommonProperties() {
  }
}