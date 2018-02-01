package org.interledger.node.services;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractThreadedService {

  private final ThreadPoolExecutor pool;

  public AbstractThreadedService(ThreadPoolExecutor pool) {
    this.pool = pool;
  }

  protected <T> Future<T> submit(Callable<T> callable) {
    return pool.submit(callable);
  }

  protected void execute(Runnable command) {
    pool.execute(command);
  }

  protected void shutdownAndAwaitTermination() {
    pool.shutdown(); // Disable new tasks from being submitted
    try {

      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {

        pool.shutdownNow(); // Cancel currently executing tasks

        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
          System.err.println("Pool did not terminate");
        }
      }

    } catch (InterruptedException ie) {

      // (Re-)Cancel if current thread also interrupted
      pool.shutdownNow();

      // Preserve interrupt status
      Thread.currentThread().interrupt();

    }
  }

}
