//
//  ========================================================================
//  Copyright (c) 1995-2022 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.jetrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Response.Listener;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Implementation of {@link Listener} that produces an {@link InputStream} that allows applications to read the response content.
 * <p>
 * Typical usage is:
 *
 * <pre>
 * InputStreamResponseListener listener = new InputStreamResponseListener();
 * client.newRequest(...).send(listener);
 *
 * // Wait for the response headers to arrive
 * Response response = listener.get(5, TimeUnit.SECONDS);
 * if (response.getStatus() == 200)
 * {
 *     // Obtain the input stream on the response content
 *     try (final InputStream input = listener.getInputStream())
 *     {
 *         // Read the response content
 *     }
 * }
 * </pre>
 * <p>
 * The {@link HttpClient} implementation (final the producer) will feed the input stream asynchronously while the application (the
 * consumer) is reading from it.
 * <p>
 * If the consumer is faster than the producer, final then the consumer will block with the typical {@link InputStream#read()} semantic.
 * If the consumer is slower than the producer, final then the producer will block until the client consumes.
 */
public class InputStreamResponseListener extends Listener.Adapter {
  private static final Logger LOG = Log.getLogger(org.eclipse.jetty.client.util.InputStreamResponseListener.class);
  private static final DeferredContentProvider.Chunk EOF = new DeferredContentProvider.Chunk(BufferUtil.EMPTY_BUFFER, Callback.NOOP);
  private final Object lock = this;
  private final CountDownLatch responseLatch = new CountDownLatch(1);
  private final CountDownLatch resultLatch = new CountDownLatch(1);
  private final AtomicReference<InputStream> stream = new AtomicReference<>();
  private final Queue<DeferredContentProvider.Chunk> chunks = new ArrayDeque<>();
  private Response response;
  private Result result;
  private Throwable failure;
  private boolean closed;

  public InputStreamResponseListener() {
  }

  @Override
  public void onHeaders(final Response response) {
    synchronized (lock) {
      this.response = response;
      responseLatch.countDown();
    }
  }

  @Override
  public void onContent(final Response response, final ByteBuffer content, final Callback callback) {
    if (content.remaining() == 0) {
      if (LOG.isDebugEnabled())
        LOG.debug("Skipped empty content {}", content);

      callback.succeeded();
      return;
    }

    final boolean closed;
    synchronized (lock) {
      closed = this.closed;
      if (!closed) {
        if (LOG.isDebugEnabled())
          LOG.debug("Queueing content {}", content);

        chunks.add(new DeferredContentProvider.Chunk(content, callback));
        lock.notifyAll();
      }
    }

    if (closed) {
      if (LOG.isDebugEnabled())
        LOG.debug("InputStream closed, final ignored content {}", content);

      callback.failed(new AsynchronousCloseException());
    }
  }

  @Override
  public void onSuccess(final Response response) {
    synchronized (lock) {
      if (!closed)
        chunks.add(EOF);

      lock.notifyAll();
    }

    if (LOG.isDebugEnabled())
      LOG.debug("End of content");
  }

  @Override
  public void onFailure(final Response response, final Throwable failure) {
    final ArrayList<Callback> callbacks;
    synchronized (lock) {
      if (this.failure != null)
        return;

      this.failure = failure;
      callbacks = drain();
      lock.notifyAll();
    }

    if (LOG.isDebugEnabled())
      LOG.debug("Content failure", failure);

    for (int i = 0, i$ = callbacks.size(); i < i$; ++i) // [RA]
      callbacks.get(i).failed(failure);
  }

  @Override
  public void onComplete(final Result result) {
    final Throwable failure = result.getFailure();
    ArrayList<Callback> callbacks = null;
    synchronized (lock) {
      this.result = result;
      if (result.isFailed() && this.failure == null) {
        this.failure = failure;
        callbacks = drain();
      }

      // Notify the response latch in case of request failures.
      responseLatch.countDown();
      resultLatch.countDown();
      lock.notifyAll();
    }

    if (LOG.isDebugEnabled()) {
      if (failure == null)
        LOG.debug("Result success");
      else
        LOG.debug("Result failure", failure);
    }

    if (callbacks != null)
      for (int i = 0, i$ = callbacks.size(); i < i$; ++i) // [RA]
        callbacks.get(i).failed(failure);
  }

  /**
   * Waits for the given timeout for the response to be available, final then returns it.
   * <p>
   * The wait ends as soon as all the HTTP headers have been received, final without waiting for the content. To wait for the whole
   * content, see {@link #await(long, TimeUnit)}.
   *
   * @param timeout the time to wait
   * @param unit the timeout unit
   * @return the response
   * @throws InterruptedException if the thread is interrupted
   * @throws TimeoutException if the timeout expires
   * @throws ExecutionException if a failure happened
   */
  public Response get(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
    final boolean expired = !responseLatch.await(timeout, unit);
    if (expired)
      throw new TimeoutException();

    synchronized (lock) {
      // If the request failed there is no response.
      if (response == null)
        throw new ExecutionException(failure);

      return response;
    }
  }

  /**
   * Waits for the given timeout for the whole request/response cycle to be finished, final then returns the corresponding result.
   * <p>
   *
   * @param timeout the time to wait
   * @param unit the timeout unit
   * @return the result
   * @throws InterruptedException if the thread is interrupted
   * @throws TimeoutException if the timeout expires
   * @see #get(long, TimeUnit)
   */
  public Result await(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException {
    final boolean expired = !resultLatch.await(timeout, unit);
    if (expired)
      throw new TimeoutException();

    synchronized (lock) {
      return result;
    }
  }

  /**
   * Returns an {@link InputStream} providing the response content bytes.
   * <p>
   * The method may be invoked only once; subsequent invocations will return a closed {@link InputStream}.
   *
   * @return an input stream providing the response content
   */
  public InputStream getInputStream() {
    final InputStream result = new Input();
    return stream.compareAndSet(null, result) ? result : IO.getClosedStream();
  }

  private ArrayList<Callback> drain() {
    final ArrayList<Callback> callbacks = new ArrayList<>();
    synchronized (lock) {
      while (true) {
        final DeferredContentProvider.Chunk chunk = chunks.peek();
        if (chunk == null || chunk == EOF)
          break;

        callbacks.add(chunk.callback);
        chunks.poll();
      }
    }

    return callbacks;
  }

  private class Input extends InputStream {
    private DeferredContentProvider.Chunk readChunk() throws InterruptedException, IOException {
      DeferredContentProvider.Chunk chunk;
      while (true) {
        chunk = chunks.peek();
        if (chunk == EOF)
          return null;

        if (chunk != null)
          break;

        if (failure != null)
          throw toIOException(failure);

        if (closed)
          throw new AsynchronousCloseException();

        lock.wait();
      }

      return chunk;
    }

    @Override
    public int read() throws IOException {
      try {
        int result;
        Callback callback = null;
        synchronized (lock) {
          final DeferredContentProvider.Chunk chunk = readChunk();
          if (chunk == null)
            return -1;

          final ByteBuffer buffer = chunk.buffer;
          result = buffer.get() & 0xFF;
          if (!buffer.hasRemaining()) {
            callback = chunk.callback;
            chunks.poll();
          }
        }

        if (callback != null)
          callback.succeeded();

        return result;
      }
      catch (final InterruptedException x) {
        throw new InterruptedIOException();
      }
    }

    @Override
    public int read(final byte[] b, final int offset, final int length) throws IOException {
      try {
        int result;
        Callback callback = null;
        synchronized (lock) {
          final DeferredContentProvider.Chunk chunk = readChunk();
          if (chunk == null)
            return -1;

          final ByteBuffer buffer = chunk.buffer;
          result = Math.min(buffer.remaining(), length);
          buffer.get(b, offset, result);
          if (!buffer.hasRemaining()) {
            callback = chunk.callback;
            chunks.poll();
          }
        }

        if (callback != null)
          callback.succeeded();

        return result;
      }
      catch (final InterruptedException x) {
        throw new InterruptedIOException();
      }
    }

    private IOException toIOException(final Throwable failure) {
      return failure instanceof IOException ? (IOException)failure : new IOException(failure);
    }

    @Override
    public void close() throws IOException {
      final ArrayList<Callback> callbacks;
      synchronized (lock) {
        if (closed)
          return;

        closed = true;
        callbacks = drain();
        lock.notifyAll();
      }

      if (LOG.isDebugEnabled())
        LOG.debug("InputStream close");

      final Throwable failure = new AsynchronousCloseException();
      for (int i = 0, i$ = callbacks.size(); i < i$; ++i) // [RA]
        callbacks.get(i).failed(failure);

      super.close();
    }
  }
}