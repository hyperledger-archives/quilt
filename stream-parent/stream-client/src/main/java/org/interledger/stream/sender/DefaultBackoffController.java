package org.interledger.stream.sender;

import static org.interledger.core.InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.link.exceptions.LinkRetriesExceededException;

import io.github.resilience4j.retry.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class DefaultBackoffController implements BackoffController {

  private final Retry retry;

  private final int maxAttempts;

  public DefaultBackoffController() {
    this(Duration.ofMillis(500), 2.0, 5);
  }

  public DefaultBackoffController(final Duration duration, final double multiplier,
                                  final int maxAttempts) {
    this.maxAttempts = maxAttempts;
    // use random backoff to avoid multiple backed off packets from hitting concurrently
    IntervalFunction interval = IntervalFunction.ofExponentialRandomBackoff(duration, multiplier);
    RetryConfig config = constructConfig(maxAttempts, interval);
    retry = Retry.of("DefaultBackoffControllerRetry", config);
  }

  protected RetryConfig constructConfig(int maxAttempts, IntervalFunction interval) {
    return RetryConfig.custom()
        .maxAttempts(maxAttempts)
        .intervalFunction(interval)
        .retryOnResult(responsePacket -> {
          AtomicBoolean retry = new AtomicBoolean(false);
          ((InterledgerResponsePacket) responsePacket).handle(
            (fulfill) -> retry.set(false),
            (reject) -> retry.set(rejectIsRetryable(reject))
          );
          return retry.get();
        })
        .build();
  }

  protected boolean rejectIsRetryable(InterledgerRejectPacket reject) {
    // if we have a temporary error but it's not due to liquidity, then we're safe to retry
    return reject.getCode().getErrorFamily() == InterledgerErrorCode.ErrorFamily.TEMPORARY &&
      reject.getCode() != T04_INSUFFICIENT_LIQUIDITY;
  }

  public static class LinkSendRejectTrackingWrapper {

    private final AtomicInteger rejects = new AtomicInteger(0);

    private final Link link;

    public LinkSendRejectTrackingWrapper(Link link) {
      this.link = link;
    }

    public InterledgerResponsePacket sendWrapped(InterledgerPreparePacket preparePacket) {
      InterledgerResponsePacket response = link.sendPacket(preparePacket);
      response.handle(
        (fulfill) -> {},
        (reject) -> rejects.incrementAndGet()
      );
      return response;
    }

    public int rejectsReceived() {
      return rejects.get();
    }
  }

  @Override
  public InterledgerResponsePacket sendWithBackoff(final Link link, final InterledgerPreparePacket preparePacket,
                                                   final AtomicInteger numRejectedPackets) {
    Objects.requireNonNull(link);
    Objects.requireNonNull(preparePacket);

    final LinkSendRejectTrackingWrapper wrapper = new LinkSendRejectTrackingWrapper(link);
    Function<InterledgerPreparePacket, InterledgerResponsePacket> retryableFunction =
      Retry.decorateFunction(retry, (prepare) -> wrapper.sendWrapped(preparePacket));
    InterledgerResponsePacket response = retryableFunction.apply(preparePacket);

    numRejectedPackets.addAndGet(wrapper.rejectsReceived());

    response.handle((fulfill) -> {}, (reject) -> {
      if (rejectIsRetryable(reject)) {
        throw new LinkRetriesExceededException(
          String.format("Max retries of retryable reject exceeded {%d}", maxAttempts),
          link.getLinkId()
        );
      }
    });
    return response;
  }
}
