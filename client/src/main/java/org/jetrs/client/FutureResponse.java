package org.jetrs.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class FutureResponse<T> implements Future<T> {
  private final AtomicBoolean cancelled = new AtomicBoolean();
  private final AtomicBoolean finished = new AtomicBoolean();

  @Override
  public final boolean cancel(final boolean mayInterruptIfRunning) {
    // TODO: Use mayInterruptIfRunning
    if (finished.get())
      return false;

    cancelled.set(true);
    finished.set(true);
    return true;
  }

  @Override
  public final boolean isCancelled() {
    return cancelled.get();
  }

  @Override
  public final boolean isDone() {
    return finished.get();
  }

  @Override
  public final T get() throws InterruptedException, ExecutionException {
    try {
      return get(-1, null);
    }
    catch (final TimeoutException e) {
      throw new IllegalStateException(e);
    }
  }
}