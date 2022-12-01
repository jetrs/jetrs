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
  private static final String TIMESTAMP = ResponseTimeoutFilter.class.getName() + ".TIMESTAMP";
  private static final String PATH = ResponseTimeoutFilter.class.getName() + ".PATH";

  public static class RequestTimeout {
    private final Thread thread;
    private final String path;
    private final long timestamp;

    private final long timeout;
    private final long expiryTime;

    private RequestTimeout(final Thread thread, final String path, final long timestamp, final long timeout) {
      this.thread = thread;
      this.path = path;
      this.timestamp = timestamp;

      this.timeout = timeout;
      this.expiryTime = timestamp + timeout;
    }

    public Thread getThread() {
      return thread;
    }

    public String getPath() {
      return path;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public long getTimeout() {
      return timeout;
    }

    public long getExpiryTime() {
      return expiryTime;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this)
        return true;

      if (!(obj instanceof RequestTimeout))
        return false;

      final RequestTimeout that = (RequestTimeout)obj;
      if (thread.getId() != that.thread.getId())
        return false;

      if (timestamp != that.timestamp)
        return false;

      return path.equals(that.path);
    }
  }

  private final ConcurrentLinkedDeque<RequestTimeout> requestTimeouts = new ConcurrentLinkedDeque<>();

  private final long timeout;
  private final Thread requestTimeoutReaper;

  protected abstract void onTimeout(RequestTimeout requestTimeout);

  public ResponseTimeoutFilter() {
    this(30000);
  }

  public ResponseTimeoutFilter(final long timeout) {
    this.timeout = timeout;
    this.requestTimeoutReaper = new Thread("RequestTimeoutReaper") {
      @Override
      public void run() {
        while (true) {
          try {
            if (requestTimeouts.size() == 0) {
              synchronized (requestTimeoutReaper) {
                requestTimeoutReaper.wait();
              }
            }
            else {
              final RequestTimeout requestTimeout = requestTimeouts.peek();
              final long diff = requestTimeout.expiryTime - System.currentTimeMillis();
              if (diff > 0) {
                synchronized (requestTimeoutReaper) {
                  requestTimeoutReaper.wait(diff);
                }
              }
              else {
                requestTimeouts.pop();
                onTimeout(requestTimeout);
              }
            }
          }
          catch (final InterruptedException e) {
            logger.warn(e.getMessage(), e);
          }
        }
      }
    };

    requestTimeoutReaper.setDaemon(true);
    requestTimeoutReaper.start();
  }

  @Override
  public void filter(final ContainerRequestContext requestContext) {
    final long timestamp = System.currentTimeMillis();
    final String path = requestContext.getUriInfo().getPath();
    requestContext.setProperty(TIMESTAMP, timestamp);
    requestContext.setProperty(PATH, path);

    requestTimeouts.add(new RequestTimeout(Thread.currentThread(), path, timestamp, timeout));
    if (requestTimeouts.size() == 1) {
      synchronized (requestTimeoutReaper) {
        requestTimeoutReaper.notify();
      }
    }
  }

  @Override
  public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
    if (requestContext.getUriInfo().getMatchedResources().size() > 0) {
      final Long timestamp = (Long)requestContext.getProperty(TIMESTAMP);
      final String path = (String)requestContext.getProperty(PATH);
      if (timestamp == null || path == null)
        throw new IllegalStateException("timestamp = " + timestamp + ", path = \"" + path + "\"");

      requestTimeouts.remove(new RequestTimeout(Thread.currentThread(), path, timestamp, timeout));
    }
  }
}