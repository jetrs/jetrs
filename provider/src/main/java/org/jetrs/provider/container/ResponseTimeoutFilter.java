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

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.libj.lang.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ContainerRequestFilter} and {@link ContainerResponseFilter} that invokes a callback upon the expiration of an explicit
 * maximum run time for all {@link ContainerRequestContext}s.
 * <p>
 * The {@link ResponseTimeoutFilter#ResponseTimeoutFilter(long)} constructor accepts an int value representing the timeout in
 * milliseconds. The {@link #onTimeout(ContainerRequestContext,Thread,long)} method is call after the {@code timeout} elapses for
 * every {@link ContainerRequestContext} that has not completed the Filter Response Chain (i.e. the {@link ContainerResponseFilter}s
 * have not been invoked for the {@link ContainerRequestContext}).
 * <p>
 * The {@link #onTimeout(ContainerRequestContext,Thread,long)} method can be overridden to close the underlying connection, or to
 * interrupt the thread if the business logic serving the {@link ContainerRequestContext} is checking {@link Thread#interrupt()}.
 */
public abstract class ResponseTimeoutFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final Logger logger = LoggerFactory.getLogger(ResponseTimeoutFilter.class);
  private static final String EXPIRE_TIME = ResponseTimeoutFilter.class.getName() + ".EXPIRE_TIME";
  private static final String THREAD = ResponseTimeoutFilter.class.getName() + ".THREAD";

  private final ConcurrentLinkedQueue<ContainerRequestContext> requestContexts = new ConcurrentLinkedQueue<>();

  protected final long timeout;
  private final Thread reaper;

  /**
   * Creates a new {@link ResponseTimeoutFilter} with an expire {@code timeout} of 30s.
   */
  public ResponseTimeoutFilter() {
    this(30000);
  }

  /**
   * Creates a new {@link ResponseTimeoutFilter} with the provided expire {@code timeout}.
   *
   * @param timeout The time in milliseconds after which the {@link #onTimeout(ContainerRequestContext,Thread,long)} method it so be
   *          called.
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
          do {
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
                  requestContexts.poll();
                  if (logger.isErrorEnabled()) logger.error("ResponseTimeoutFilter: Unable to check expire time: " + ObjectUtil.simpleIdentityString(requestContext) + ".getProperty(" + EXPIRE_TIME + ") = null: " + requestContext.getUriInfo().getPath());
                }
                else {
                  final long diff = (Long)expireTime - System.currentTimeMillis();
                  if (diff > 0) {
                    synchronized (reaper) {
                      reaper.wait(diff);
                    }
                  }
                  else {
                    requestContexts.poll();
                    final Thread thread = (Thread)requestContext.getProperty(THREAD);
                    if (thread == null) {
                      if (logger.isErrorEnabled()) logger.error("ResponseTimeoutFilter: Unable to enforce expire time: " + ObjectUtil.simpleIdentityString(requestContext) + ".getProperty(" + THREAD + ") = null: " + requestContext.getUriInfo().getPath());
                    }
                    else {
                      onTimeout(requestContext, thread, timeout - diff);
                    }
                  }
                }
              }
            }
            catch (final InterruptedException e) {
              if (logger.isWarnEnabled()) logger.warn(e.getMessage(), e);
            }
            catch (final Exception e) {
              if (logger.isErrorEnabled()) logger.error(e.getMessage(), e);
            }
          }
          while (true);
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
   * @param thread The {@link Thread} serving the {@link ContainerRequestContext}.
   * @param elapsed The elapsed time of the specified {@link ContainerRequestContext}.
   */
  protected abstract void onTimeout(ContainerRequestContext requestContext, Thread thread, long elapsed);

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