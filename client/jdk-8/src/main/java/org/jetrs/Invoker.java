/* Copyright (c) 2019 JetRS
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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;

import org.libj.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class Invoker<R> {
  private static final Logger logger = LoggerFactory.getLogger(Invoker.class);
  private static final ConcurrentHashMap<String,ClientDriver> classNameToClientDriver = new ConcurrentHashMap<>();

  private static ClientDriver loadService(final String factoryId, final Supplier<? extends ClientDriver> defaultProvider) {
    final String systemProp = System.getProperty(factoryId);
    if (systemProp != null) {
      ClientDriver driver = classNameToClientDriver.get(systemProp);
      if (driver != null)
        return driver;

      try {
        classNameToClientDriver.put(systemProp, driver = (ClientDriver)Class.forName(systemProp).getDeclaredConstructor().newInstance());
        return driver;
      }
      catch (final ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
        throw new IllegalStateException("Provider " + systemProp + " could not be instantiated: " + e, e);
      }
      catch (final SecurityException e) {
        if (logger.isDebugEnabled()) { logger.debug("Failed to load service " + factoryId + " from a system property", e); }
      }
    }

    ClientDriver driver = classNameToClientDriver.get(factoryId);
    if (driver != null)
      return driver;

    driver = tryLoadService(factoryId, Thread.currentThread().getContextClassLoader());
    if (driver == null) {
      driver = tryLoadService(factoryId, Invoker.class.getClassLoader());
      if (driver == null)
        driver = defaultProvider.get();
    }

    classNameToClientDriver.put(factoryId, driver);
    return driver;
  }

  private static ClientDriver tryLoadService(final String factoryId, final ClassLoader classLoader) {
    try {
      final Iterator<ClientDriver> iterator = ServiceLoader.load(ClientDriver.class, classLoader).iterator();
      return iterator.hasNext() ? iterator.next() : null;
    }
    catch (final Exception | ServiceConfigurationError e) {
      if (logger.isDebugEnabled()) { logger.debug("Failed to load service " + factoryId + ".", e); }
      return null;
    }
  }

  private static ExecutorService getDefaultExecutorService() {
    return Executors.newCachedThreadPool(new ThreadFactoryBuilder().withNamePrefix("JetRS-Client-DefaultExecutor").build()); // FIXME: Make configurable
  }

  private static ScheduledExecutorService getDefaultScheduledExecutorService() {
    return Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().withNamePrefix("JetRS-Client-DefaultScheduledExecutor").build()); // FIXME: Make configurable
  }

  private final ClientDriver driver;
  final ClientImpl client;
  final ClientRuntimeContext runtimeContext;
  final URI uri;
  final ExecutorService executorService;
  final ScheduledExecutorService scheduledExecutorService;
  final long connectTimeoutMs;
  final long readTimeoutMs;

  Invoker(final ClientImpl client, final ClientRuntimeContext runtimeContext, final URI uri, final ExecutorService executorService, final ScheduledExecutorService scheduledExecutorService, final long connectTimeoutMs, final long readTimeoutMs) {
    this.driver = loadService(ClientDriver.JETRS_CLIENT_DRIVER_PROPERTY, Jdk8ClientDriver::new);
    this.client = client;
    this.runtimeContext = runtimeContext;
    this.uri = uri;
    this.executorService = executorService != null ? executorService : getDefaultExecutorService();
    this.scheduledExecutorService = scheduledExecutorService != null ? scheduledExecutorService : getDefaultScheduledExecutorService();
    this.connectTimeoutMs = connectTimeoutMs;
    this.readTimeoutMs = readTimeoutMs;
  }

  public final R get() {
    return method(HttpMethod.GET, null);
  }

  public final R put(final Entity<?> entity) {
    return method(HttpMethod.PUT, entity);
  }

  public final R post(final Entity<?> entity) {
    return method(HttpMethod.POST, entity);
  }

  public final R delete() {
    return method(HttpMethod.DELETE, null);
  }

  public final R head() {
    return method(HttpMethod.HEAD, null);
  }

  public final R options() {
    return method(HttpMethod.OPTIONS, null);
  }

  public final R trace() {
    return method("TRACE", null);
  }

  public abstract R method(String name, Entity<?> entity);

  abstract HashMap<String,Object> getProperties();

  final Invocation build(final String method, final HttpHeadersImpl requestHeaders, final ArrayList<Cookie> cookies, final CacheControl cacheControl, final Entity<?> entity) {
    client.assertNotClosed();
    try {
      return driver.build(client, runtimeContext, uri, method, requestHeaders != null ? requestHeaders.clone() : new HttpHeadersImpl(), cookies, cacheControl, entity, executorService, scheduledExecutorService, getProperties(), connectTimeoutMs, readTimeoutMs);
    }
    catch (final Exception e) {
      throw new ProcessingException(e);
    }
  }
}