package org.interledger.stream.sender;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * For testing purposes, an executor service that sleeps before submitting or executing tasks to simulate conditions
 * where there is a delay between when a task is submitted and when it is executed.
 */
class SleepyExecutorService implements ExecutorService {

  private final ExecutorService delegate;
  private final long sleep;

  /**
   * Wraps an ExecutorService and sleeps before delegating submit/execute calls to the underlying delegate.
   *
   * @param delegate    wrapped ExecutorService that is delegated to
   * @param sleepMillis how long to sleep before execution
   */
  SleepyExecutorService(ExecutorService delegate, long sleepMillis) {
    this.delegate = delegate;
    this.sleep = sleepMillis;
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }


  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return withSleep(() -> delegate.submit(task));
  }


  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return withSleep(() -> delegate.submit(task, result));
  }


  @Override
  public Future<?> submit(Runnable task) {
    return withSleep(() -> delegate.submit(task));
  }


  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate.invokeAll(tasks);
  }


  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
    long timeout,
    TimeUnit unit) throws InterruptedException {
    return delegate.invokeAll(tasks, timeout, unit);
  }


  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
    throws InterruptedException, ExecutionException {
    return delegate.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    withSleep(() -> delegate.execute(command));
  }

  private <T> T withSleep(Callable<T> task) {
    try {
      Thread.sleep(sleep);
      return task.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void withSleep(Runnable task) {
    try {
      Thread.sleep(sleep);
      task.run();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
