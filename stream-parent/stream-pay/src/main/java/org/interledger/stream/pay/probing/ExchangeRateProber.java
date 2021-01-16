package org.interledger.stream.pay.probing;

import org.interledger.link.Link;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.pay.filters.AmountFilter;
import org.interledger.stream.pay.filters.AssetDetailsFilter;
import org.interledger.stream.pay.filters.ExchangeRateFilter;
import org.interledger.stream.pay.filters.FailureFilter;
import org.interledger.stream.pay.filters.MaxPacketAmountFilter;
import org.interledger.stream.pay.filters.SequenceFilter;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.filters.chain.DefaultStreamPacketFilterChain;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.probing.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * A service for probing a particular path in order to determine exchange rates.
 */
public interface ExchangeRateProber {

  /**
   * Accessor for the {@link PaymentSharedStateTracker} for the supplied {@code streamConnection}.
   *
   * @param streamConnection A {@link StreamConnection} to identify a payment.
   *
   * @return An optionally present {@link PaymentSharedStateTracker}.
   */
  Optional<PaymentSharedStateTracker> getPaymentSharedStateTracker(StreamConnection streamConnection);

  /**
   * Probe the payment path for the supplied {@code streamConnection} by sending a variety of unfulfillable ILPv4
   * packets with embedded stream packet of varying value. While each packet will be rejected by the receiver (and thus
   * no value will be transferred), the stream response embedded into each ILPv4 packet will contain an amount that the
   * receiver would have received. This can be used to compute FX rates for the path during the probe. While any rates
   * obtained during probing are not fixed for any duration of time, these rates can be used to inform a user of
   * approximate or estimated rates, allowing the sender or a sender's device/application to make an informed decision
   * about a payment that is desired to be made.
   *
   * @return A {@link ExchangeRateProbeOutcome} with details about the payment path probing operation.
   */
  ExchangeRateProbeOutcome probePath(StreamConnection streamConnection);

  /**
   * The default implementation of {@link ExchangeRateProber}.
   */
  class DefaultExchangeRateProber implements ExchangeRateProber {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StreamPacketEncryptionService streamPacketEncryptionService;
    private final Link<?> link;
    private final ExecutorService executorService;
    private final Map<StreamConnection, PaymentSharedStateTracker> paymentSharedStateTrackersMap;

    private final List<Long> initialTestPacketAmounts;
    private final Duration timeoutDuration;

    /**
     * Required-args Constructor.
     *
     * @param streamPacketEncryptionService An instance of {@link StreamPacketEncryptionService}.
     * @param link                          A {@link Link} to send the ILPv4 packet on.
     */
    public DefaultExchangeRateProber(
      final StreamPacketEncryptionService streamPacketEncryptionService, final Link<?> link
    ) {
      this.streamPacketEncryptionService = Objects.requireNonNull(streamPacketEncryptionService);
      this.link = Objects.requireNonNull(link);

      this.paymentSharedStateTrackersMap = Maps.newConcurrentMap();
      this.executorService = Executors.newFixedThreadPool(10); // TODO: 2? 5?
      this.timeoutDuration = Duration.of(30, ChronoUnit.SECONDS);
      this.initialTestPacketAmounts = Lists.newArrayList(
        1_000_000_000_000L,
        100_000_000_000L,
        10_000_000_000L,
        1_000_000_000L,
        100_000_000L,
        10_000_000L,
        1_000_000L,
        100_000L,
        10_000L,
        1_000L,
        100L,
        10L,
        1L,
        0L
      );
    }

    /**
     * IMPLEMENTATION NOTE: Currently the Java Connector doesn't send back meta-data as part of an F08 rejection, which
     * is a bug that will be rectified via https://github.com/interledger4j/ilpv4-connector/issues/660). One
     * optimization would be to perform a binary-search to precisely discover the maxPacketAmount. For example, imagine
     * a path that has a maxPacketAmount of 5. Probing with 10 will yield an F08, whereas probing with 1 will yield an
     * F99 or some other error. Ideally, the system would send another packet with value of 7, and then maybe 3.5, and
     * then 5 to discover a precise maxPacketAmount. However, the logic to implement this is more complicated with this
     * implementation because here we don't actually loop continuously until a precise rate is found. Instead, we merely
     * send 14 probe packets and then just pick the closest one. This is sufficient for now, because once issue 660 is
     * fixed, then the F08 packets _should_ yield the appropriate maxPacketAmount after just one packet, which would be
     * the largest packet sent.
     *
     * @param streamConnection A {@link StreamConnection}.
     *
     * @return A {@link ExchangeRateProbeOutcome}.
     */
    @Override
    public ExchangeRateProbeOutcome probePath(final StreamConnection streamConnection) {
      Objects.requireNonNull(streamConnection);

      // If this is the first usage of streamConnection, then construct a new PaymentTracker instances. Otherwise,
      // re-use existing trackers so the new payment can benefit from historical information.
      final PaymentSharedStateTracker paymentSharedStateTracker = paymentSharedStateTrackersMap.computeIfAbsent(
        streamConnection, PaymentSharedStateTracker::new
      );

      final List<StreamPacketFilter> streamPacketFilters = Lists.newArrayList(
        // First so all other controllers log the sequence number
        new SequenceFilter(paymentSharedStateTracker),
        // Fail-fast on terminal rejects or timeouts
        new FailureFilter(),
        // Fail-fast on destination asset detail conflict
        new AssetDetailsFilter(paymentSharedStateTracker),
        // Fail-fast if max packet amount is 0
        new MaxPacketAmountFilter(paymentSharedStateTracker.getMaxPacketAmountTracker()),
        // NOTE: Probing only sends a limited number of packets, no need to limit how frequently packets are sent
        // (i.e., don't add a PacingFilter).
        new AmountFilter(paymentSharedStateTracker),
        new ExchangeRateFilter(paymentSharedStateTracker.getExchangeRateTracker())
      );

      // Collects any replies that have an exception.
      final Set<StreamPacketReply> errorReplies = Collections.synchronizedSet(new HashSet<>());

      // Assemble a bunch of packets for each amount.
      // Send the packets in parallel.
      // Process each response in parallel.
      // return the outcome.

      // Handle the result
      // TODO: Ignore replies that aren't authentic

      CompletableFuture<Void> allFutures = CompletableFuture
        .allOf(this.initialTestPacketAmounts.stream()
            .map(UnsignedLong::valueOf)
            .map(prepareAmount -> {
              // Send a StreamPacket and get a response.
              final Supplier<StreamPacketReply> streamResponseSupplier = () -> {

                // Probe packets should never fulfill.
                final ModifiableStreamPacketRequest streamPacketRequest = ModifiableStreamPacketRequest.create()
                  .setIsFulfillable(false)
                  .setSourceAmount(prepareAmount);
                final StreamPacketFilterChain filterChain = new DefaultStreamPacketFilterChain(
                  streamPacketFilters, link, streamPacketEncryptionService, paymentSharedStateTracker
                );

                // Handle SendState stuff...
                final SendState nextSendState = filterChain.nextState(streamPacketRequest);
                if (nextSendState == SendState.Ready) {

                  // Packet is sent via Link at end of this chain.
                  return filterChain.doFilter(streamPacketRequest);
                } else if (nextSendState == SendState.End || nextSendState.isPaymentError()) {
                  // Wait for any requests to stop.
//                if (shouldCloseConnection(nextSendState)) {
                  // TODO: Close connection
//                }
                  return filterChain.doFilter(streamPacketRequest);
                } else {
                  // TODO: Log a Wait?
                  return filterChain.doFilter(streamPacketRequest);
                }
              };

              // Execute the Supplier
              return CompletableFuture.supplyAsync(streamResponseSupplier, executorService)
                // Logging
                .handle((streamPacketReply, throwable) -> {
                  if (throwable != null) {
                    logger.error("ExchangeRateProbe packet failed: " + throwable.getMessage(), throwable);
                  }
                  if (streamPacketReply != null) {
                    if (logger.isDebugEnabled()) {
                      logger.debug("StreamPacketReply={}", streamPacketReply);
                    }
                    // Add the packet to the error packets if there's an exception
                    streamPacketReply.exception().ifPresent(e -> errorReplies.add(streamPacketReply));
                  }
                  return streamPacketReply; // Null or not.
                });
            })
            // Passing empty array is most performant.
            // See https://stackoverflow.com/questions/53284214/toarray-with-pre-sized-array
            .toArray(CompletableFuture[]::new)
        ).whenComplete(($, error) -> {
          executorService.shutdown();
          if (error != null) {
            logger.error("ExchangeRateProbe failed: " + error.getMessage(), error);
          }
        });

      // Wait for all futures to complete. We ignore the actual CF responses because we only need data here from the
      // shared state tracker.
      try {
        allFutures.get(timeoutDuration.getSeconds(), TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException e) {
        logger.error(String.format("RateProbe request did not fully complete. error=%s", e.getMessage()), e);
      } catch (TimeoutException e) {
        logger.warn(
          String.format("RateProbe request did not complete within 30 seconds. error=%s", e.getMessage()), e
        );
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }

      return ExchangeRateProbeOutcome.builder()
        .sourceDenomination(streamConnection.getSourceAccountDetails().denomination())
        .destinationDenomination(
          paymentSharedStateTracker.getAssetDetailsTracker().getDestinationAccountDetails().denomination()
        )
        .maxPacketAmount(paymentSharedStateTracker.getMaxPacketAmountTracker().getMaxPacketAmount())
        .verifiedPathCapacity(paymentSharedStateTracker.getMaxPacketAmountTracker().verifiedPathCapacity())
        .lowerBoundRate(paymentSharedStateTracker.getExchangeRateTracker().getLowerBoundRate())
        .upperBoundRate(paymentSharedStateTracker.getExchangeRateTracker().getUpperBoundRate())
        .errorPackets(errorReplies)
        .build();
    }

    @Override
    public Optional<PaymentSharedStateTracker> getPaymentSharedStateTracker(final StreamConnection streamConnection) {
      Objects.requireNonNull(streamConnection);
      return Optional.ofNullable(paymentSharedStateTrackersMap.get(streamConnection));
    }
  }
}