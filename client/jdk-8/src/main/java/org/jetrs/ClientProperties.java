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

public final class ClientProperties {
  /**
   * Client-specific version of the common property {@link CommonProperties#CHUNKED_ENCODING_SIZE}, which, if present, overrides the
   * common property for the client runtime.
   */
  public static final String CHUNKED_ENCODING_SIZE_CLIENT = "jetrs.client.chunkedEncodingSize";

  /**
   * Client-specific version of the common property {@link CommonProperties#CONTENT_LENGTH_BUFFER}, which, if present,
   * overrides the common property for the client runtime.
   */
  public static final String CONTENT_LENGTH_BUFFER_CLIENT = "jetrs.client.contentLength.buffer";

  /**
   * Follow redirects to the URI declared in 3xx responses.
   * <p>
   * Default: {@value #FOLLOW_REDIRECTS_DEFAULT}.
   * <p>
   * To disable, set the property to {@code "false"}.
   * <p>
   * Configuration property: <tt>{@value}</tt>
   */
  public static final String FOLLOW_REDIRECTS = "jetrs.client.followRedirects";

  /**
   * Default value for {@link #FOLLOW_REDIRECTS} property.
   */
  public static final boolean FOLLOW_REDIRECTS_DEFAULT = true;

  /**
   * Sets the maximum number of redirects to be followed.
   * <p>
   * Default: {@value #MAX_REDIRECTS_DEFAULT}.
   * <p>
   * To disable, set the property to {@code "false"}.
   * <p>
   * Configuration property: <tt>{@value}</tt>
   *
   * @implNote This property is irrelevant {@value #FOLLOW_REDIRECTS} is set to {@code "false"}.
   */
  public static final String MAX_REDIRECTS = "jetrs.client.maxRedirects";

  /**
   * Default value for {@link #MAX_REDIRECTS} property.
   */
  public static final int MAX_REDIRECTS_DEFAULT = 5;

  /**
   * The size of the thread pool for the executor service to be used if an explicit executor is not provided via
   * {@link javax.ws.rs.client.ClientBuilder#executorService(java.util.concurrent.ExecutorService)}.
   * <p>
   * When a positive value is provided, a cached thread pool is initialized by default with a maximum number of threads limited by
   * the specified value.
   * <p>
   * When a non-positive value is provided, or if the property is absent, a cached thread pool is initialized by default, which
   * creates new thread for every new request (see {@link java.util.concurrent.Executors}).
   * <p>
   * Configuration property: <tt>{@value}</tt>
   */
  public static final String EXECUTOR_THREADPOOL_SIZE = "jetrs.client.executor.threadPoolSize";

  /**
   * The size of the thread pool for the scheduled executor service to be used if an explicit executor is not provided via
   * {@link javax.ws.rs.client.ClientBuilder#scheduledExecutorService(java.util.concurrent.ScheduledExecutorService)}.
   * <p>
   * When a positive value is provided, a fixed thread pool is initialized by default with a maximum number of threads limited by
   * the specified value.
   * <p>
   * When a non-positive value is provided, or if the property is absent, a single-thread thread pool is initialized by default.
   * <p>
   * Configuration property: <tt>{@value}</tt> Scheduler thread pool size.
   */
  public static final String SCHEDULED_EXECUTOR_THREADPOOL_SIZE = "jetrs.client.scheduledExecutor.threadPoolSize";

  /**
   * If {@code true}, the strict validation of HTTP specification compliance will be suppressed.
   * <p>
   * By default, Jersey client runtime performs certain HTTP compliance checks (such as which HTTP methods can facilitate non-empty
   * request entities etc.) in order to fail fast with an exception when user tries to establish a communication non-compliant with
   * HTTP specification. Users who need to override these compliance checks and avoid the exceptions being thrown by Jersey client
   * runtime for some reason, can set this property to {@code true}. As a result, the compliance issues will be merely reported in a
   * log and no exceptions will be thrown.
   * </p>
   * <p>
   * Note that the property suppresses the Jersey layer exceptions. Chances are that the non-compliant behavior will cause different
   * set of exceptions being raised in the underlying I/O connector layer.
   * </p>
   * <p>
   * This property can be configured in a client runtime configuration or directly on an individual request. In case of conflict,
   * request-specific property value takes precedence over value configured in the runtime configuration.
   * </p>
   * <p>
   * The default value is {@code false}.
   * </p>
   * <p>
   * The name of the configuration property is <tt>{@value}</tt>.
   * </p>
   *
   * @since 2.2
   */
  public static final String SUPPRESS_HTTP_COMPLIANCE_VALIDATION = "jetrs.client.suppressHttpComplianceValidation";

  /**
   * The URI of a HTTP proxy to be used by the client.
   * <p>
   * Default: {@code null}.
   * <p>
   * Configuration property: <tt>{@value}</tt>
   */
  public static final String PROXY_URI = "jetrs.client.proxy.uri";

  /**
   * The username to be used for the HTTP proxy specified with the {@link #PROXY_URI} property.
   * <p>
   * Default: {@code null}.
   * <p>
   * Configuration property: <tt>{@value}</tt>
   */
  public static final String PROXY_USERNAME = "jetrs.client.proxy.username";

  /**
   * The password to be used for the HTTP proxy specified with the {@link #PROXY_URI} property.
   * <p>
   * Default: {@code null}.
   * <p>
   * Configuration property: <tt>{@value}</tt>
   */
  public static final String PROXY_PASSWORD = "jetrs.client.proxy.password";

  private ClientProperties() {
  }
}