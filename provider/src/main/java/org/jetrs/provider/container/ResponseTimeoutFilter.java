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

package org.jetrs.provider.container;

import static org.libj.lang.Assertions.*;

import java.util.concurrent.ConcurrentLinkedDeque;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.libj.lang.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ContainerRequestFilter} and {@link ContainerResponseFilter} that enforces an explicit maximum run time for all
 * {@link ContainerRequestContext}s.
 * <p>
 * The {@link ResponseTimeoutFilter#ResponseTimeoutFilter(long)} constructor accepts an int value representing the timeout in
 * milliseconds. The {@link #onTimeout(ContainerRequestContext,long)} method is call for every {@link ContainerRequestContext} that
 * has not finished after the {@code timeout} elapses.
 * <p>
 * The {@code boolean} value returned from {@link #onTimeout(ContainerRequestContext,long)} dictates whether the thread serving the
 * specified {@link ContainerRequestContext} is to be {@linkplain Thread#interrupt() interrupted}.
 * <p>
 * An {@link javax.ws.rs.ext.ExceptionMapper} can be used to map the {@link InterruptedException} to specify the desired code or
 * content to return to the caller.
 */
public abstract class ResponseTimeoutFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final Logger logger = LoggerFactory.getLogger(ResponseTimeoutFilter.class);
  private static final String EXPIRE_TIME = ResponseTimeoutFilter.class.getName() + ".EXPIRE_TIME";
  private static final String THREAD = ResponseTimeoutFilter.class.getName() + ".THREAD";

  private final ConcurrentLinkedDeque<ContainerRequestContext> requestContexts = new ConcurrentLinkedDeque<>();

  protected final long timeout;
  private final Thread reaper;

  /**
   * Creates a new {@link ResponseTimeoutFilter} with a expire {@code timeout} of 30s.
   */
  public ResponseTimeoutFilter() {
    this(30000);
  }

  /**
   * Creates a new {@link ResponseTimeoutFilter} with the provided expire {@code timeout}.
   *
   * @param timeout The time in milliseconds after which the {@link #onTimeout(ContainerRequestContext,long)} method it so be called
   *          for all {@link ContainerRequestContext}s.
   * @throws IllegalArgumentException If {@code timeout} is negative.
   * @implNote A {@code timeout} of {@code 0} creates a noop {@link ResponseTimeoutFilter}.
   */
  public ResponseTimeoutFilter(final long timeout) {
    this.timeout = assertNotNegative(timeout);
    if (timeout == 0) {
      this.reaper = null;
    }
    else {
      this.reaper = new Thread("RequestTimeoutFilterReaper") {
        @Override
        public void run() {
          while (true) {
            try {
              final ContainerRequestContext requestContext = requestContexts.peek();
              if (requestContext == null) {
                synchronized (reaper) {
                  reaper.wait();
                }
              }
              else {
                final Object expireTime = requestContext.getProperty(EXPIRE_TIME);
                if (expireTime == null) {
                  requestContexts.pop();
                  if (logger.isWarnEnabled()) logger.warn("ResponseTimeoutFilter: Unable to check expire time: " + ObjectUtil.simpleIdentityString(requestContext) + ".getProperty(" + EXPIRE_TIME + ") = null");
                }
                else {
                  final long diff = (Long)expireTime - System.currentTimeMillis();
                  if (diff > 0) {
                    synchronized (reaper) {
                      reaper.wait(diff);
                    }
                  }
                  else {
                    requestContexts.pop();
                    final Thread thread = (Thread)requestContext.getProperty(THREAD);
                    if (thread == null) {
                      if (logger.isWarnEnabled()) logger.warn("ResponseTimeoutFilter: Unable to enforce expire time: " + ObjectUtil.simpleIdentityString(requestContext) + ".getProperty(" + THREAD + ") = null");
                    }
                    else if (onTimeout(requestContext, timeout - diff)) {
                      thread.interrupt();
                    }
                  }
                }
              }
            }
            catch (final InterruptedException e) {
              if (logger.isWarnEnabled()) logger.warn(e.getMessage(), e);
            }
          }
        }
      };

      reaper.setDaemon(true);
      reaper.start();
    }
  }

  /**
   * Callback method that is called when the expire {@link #timeout} elapses for the specified {@link ContainerRequestContext}.
   *
   * @param requestContext The {@link ContainerRequestContext} for which the expire {@link #timeout} has elapsed.
   * @param elapsed The elapsed time of the specified {@link ContainerRequestContext}.
   * @return {@code true} for the {@link ResponseTimeoutFilter} to {@linkplain Thread#interrupt() interrupt} the thread serving the
   *         specified {@link ContainerRequestContext}, or {@code false} to allow the thread to continue execution until its
   *         uninterrupted completion.
   */
  protected abstract boolean onTimeout(ContainerRequestContext requestContext, long elapsed);

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    if (timeout == 0 || requestContext.getUriInfo().getMatchedResources().size() == 0)
      return;

    final long timestamp = System.currentTimeMillis();
    requestContext.setProperty(EXPIRE_TIME, timestamp + timeout);
    requestContext.setProperty(THREAD, Thread.currentThread());
    requestContexts.add(requestContext);
    if (requestContexts.size() == 1) {
      synchronized (reaper) {
        reaper.notify();
      }
    }
  }

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
    if (timeout > 0 && requestContext.getUriInfo().getMatchedResources().size() > 0)
      requestContexts.remove(requestContext);
  }
}