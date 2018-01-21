package org.interledger.cryptoconditions;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * An abstract class that unit tests can extend to gain certain useful functionality for testing
 * purposes.
 */
public abstract class AbstractCryptoConditionTest {

  private static final int DEFAULT_NUM_THREADS = 20;
  private static final Logger STATIC_LOGGER = Logger
      .getLogger(AbstractCryptoConditionTest.class.getName());

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * A new assertion method to expose concurrency bugs.
   *
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  public static void assertConcurrent(
      final String message,
      final List<? extends Runnable> runnables,
      final int maxTimeoutSeconds
  ) throws InterruptedException {
    final int numThreads = runnables.size();
    final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
    final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
    try {
      final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
      final CountDownLatch afterInitBlocker = new CountDownLatch(1);
      final CountDownLatch allDone = new CountDownLatch(numThreads);
      for (final Runnable submittedTestRunnable : runnables) {
        threadPool.submit(() -> {
              allExecutorThreadsReady.countDown();
              try {
                afterInitBlocker.await();
                submittedTestRunnable.run();
              } catch (final Throwable e) {
                exceptions.add(e);
              } finally {
                allDone.countDown();
              }
            }
        );
      }
      // wait until all threads are ready
      assertTrue(
          "Timeout initializing threads! Perform long lasting initializations before "
              + "passing runnables to assertConcurrent",
          allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
      // start all test runners
      afterInitBlocker.countDown();
      assertTrue(message + " timeout! More than" + maxTimeoutSeconds + "seconds",
          allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
    } finally {
      threadPool.shutdownNow();
    }

    exceptions.stream().forEach(e -> STATIC_LOGGER.log(Level.SEVERE, e.getMessage(), e));
    assertTrue(
        String.format("%s.  Exceptions: %s",
            message,
            exceptions.stream().map(e -> e.getMessage()).collect(Collectors.joining(", "))
        ),
        exceptions.isEmpty()
    );
  }

  /**
   * Concurrently runs one or more instances from {@link Runnable} in a multithreaded fashion,
   * relying upon the default number from threads for concurrency.
   */
  protected void runConcurrent(final Runnable... runnableTest) throws InterruptedException {
    this.runConcurrent(DEFAULT_NUM_THREADS, runnableTest);
  }

  /**
   * Concurrently runs one or more instances from {@link Runnable} in a multithreaded fashion,
   * relying upon {@code numThreads} for concurrency.
   */
  protected void runConcurrent(final int numThreads, final Runnable... runnableTest)
      throws InterruptedException {
    final Builder<Runnable> builder = ImmutableList.builder();

    // For each runnableTest, add it numThreads times to the bulider.
    for (final Runnable runnable : runnableTest) {
      for (int i = 0; i < numThreads; i++) {
        builder.add(runnable);
      }
    }

    logger.info(String.format("About to run %s threads...", numThreads));
    // Actually runs the Runnables above using multiple threads.
    assertConcurrent("Test did not complete before the harness timed-out. Please consider "
        + "increasing the timeout value for this test.", builder.build(), 15);
    logger.info(String.format("Ran %s threads!", numThreads));
  }
}
