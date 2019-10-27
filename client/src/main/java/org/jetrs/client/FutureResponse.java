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