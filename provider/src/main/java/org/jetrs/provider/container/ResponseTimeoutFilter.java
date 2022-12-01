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
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public abstract class ResponseTimeoutFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final Logger logger = LoggerFactory.getLogger(ResponseTimeoutFilter.class);
  private static final String EXPIRE_TIME = ResponseTimeoutFilter.class.getName() + ".EXPIRE_TIME";

  private final ConcurrentLinkedDeque<ContainerRequestContext> requestContexts = new ConcurrentLinkedDeque<>();

  private final long timeout;
  private final Thread reaper;

  public ResponseTimeoutFilter() {
    this(30000);
  }

  protected abstract void onTimeout(ContainerRequestContext requestContext, long elapsed);

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
              if (requestContexts.size() == 0) {
                synchronized (reaper) {
                  reaper.wait();
                }
              }
              else {
                final ContainerRequestContext requestContext = requestContexts.peek();
                final long diff = (Long)requestContext.getProperty(EXPIRE_TIME) - System.currentTimeMillis();
                if (diff > 0) {
                  synchronized (reaper) {
                    reaper.wait(diff);
                  }
                }
                else {
                  requestContexts.pop();
                  onTimeout(requestContext, timeout - diff);
                }
              }
            }
            catch (final InterruptedException e) {
              logger.warn(e.getMessage(), e);
            }
          }
        }
      };

      reaper.setDaemon(true);
      reaper.start();
    }
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    if (timeout == 0)
      return;

    final long timestamp = System.currentTimeMillis();
    requestContext.setProperty(EXPIRE_TIME, timestamp + timeout);
    requestContexts.add(requestContext);
    if (requestContexts.size() == 1) {
      synchronized (reaper) {
        reaper.notify();
      }
    }
  }

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
    if (timeout == 0 || requestContext.getUriInfo().getMatchedResources().size() == 0)
      return;

    final Long timestamp = (Long)requestContext.getProperty(EXPIRE_TIME);
    if (timestamp == null)
      throw new IllegalStateException("timestamp is null");

    requestContexts.remove(requestContext);
  }
}