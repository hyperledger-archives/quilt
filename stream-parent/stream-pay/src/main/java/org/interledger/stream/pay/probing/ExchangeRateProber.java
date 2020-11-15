package org.interledger.stream.pay.probing;

import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.OracleExchangeRateService;
import org.interledger.link.Link;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.pay.AbstractPayWrapper;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.AmountFilter;
import org.interledger.stream.pay.filters.AssetDetailsFilter;
import org.interledger.stream.pay.filters.ExchangeRateFilter;
import org.interledger.stream.pay.filters.MaxPacketAmountFilter;
import org.interledger.stream.pay.filters.SequenceFilter;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.filters.chain.DefaultStreamPacketFilterChain;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.probing.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.trackers.AssetDetailsTracker;
import org.interledger.stream.pay.trackers.ExchangeRateTracker;
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
   * Probe the payment path that corresponds to the supplied {@code streamConnection} by sending a variety of
   * unfulfillable ILPv4 packets with embedded stream packet of varying value. While each packet will be rejected by the
   * receiver (and thus no value will be transferred), the stream response embedded into each ILPv4 packet will contain
   * an amount that the receiver would have received. This can be used to compute FX rates for the path during the
   * probe. While any rates obtained during probing are not fixed for any duration of time, these rates can be used to
   * inform a user of approximate or estimated rates, allowing the sender or a sender's device/application to make an
   * informed decision about a payment that is desired to be made.
   *
   * @return A {@link ExchangeRateProbeOutcome} with estimated FX details for the payment path.
   */
  ExchangeRateProbeOutcome probePath(StreamConnection streamConnection);

  /**
   * Probe the payment path that corresponds to the supplied {@code streamConnection} by sending a single zero-value
   * packet (in order to obtain the destination accounts denomination). Note that this method will complete much faster
   * than {@link #probePath(StreamConnection)} because only a single packet will be sent as part of the probe using this
   * method.
   *
   * @return A {@link ExchangeRateProbeOutcome} with estimated FX details for the payment path.
   */
  ExchangeRateProbeOutcome probePathUsingExternalRates(StreamConnection streamConnection);

  /**
   * The default implementation of {@link ExchangeRateProber}.
   */
  class DefaultExchangeRateProber extends AbstractPayWrapper implements ExchangeRateProber {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Link<?> link;
    private final OracleExchangeRateService oracleExchangeRateService;
    private final Map<StreamConnection, PaymentSharedStateTracker> paymentSharedStateTrackersMap;
    private final ExecutorService executorService;

    private final List<Long> probedPathPacketAmounts;
    private final List<Long> skipPathProbePacketAmounts;
    private final Duration timeoutDuration;

    /**
     * Required-args Constructor.
     *
     * @param streamPacketEncryptionService An instance of {@link StreamPacketEncryptionService}.
     * @param link                          A {@link Link} to send the ILPv4 packet on.
     * @param oracleExchangeRateService           An {@link OracleExchangeRateService}.
     */
    public DefaultExchangeRateProber(
      final StreamPacketEncryptionService streamPacketEncryptionService,
      final Link<?> link,
      final OracleExchangeRateService oracleExchangeRateService
    ) {
      super(streamPacketEncryptionService);
      this.link = Objects.requireNonNull(link);
      this.oracleExchangeRateService = Objects.requireNonNull(oracleExchangeRateService);

      this.paymentSharedStateTrackersMap = Maps.newConcurrentMap();
      // TODO [New Feature] Consider making this configurable.
      this.executorService = Executors.newFixedThreadPool(3);
      this.timeoutDuration = Duration.of(30, ChronoUnit.SECONDS);
      this.probedPathPacketAmounts = Lists.newArrayList(
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
      this.skipPathProbePacketAmounts = Lists.newArrayList(0L);
    }

    @Override
    public ExchangeRateProbeOutcome probePathUsingExternalRates(final StreamConnection streamConnection) {
      // Because this operation is skipping the _typical_ rate probe, and instead sending only a single stream packet
      // in order to discover the destination account's denomination. Thus, the "probed rates" are returned from here
      // with a 1:1 value, which is the likely outcome because generally path-probing is skipped only when the source
      // and destination currencies are the same. However, if the source and destination denominations are different
      // for some reason, then we need to return an FX rate that is _not_ 1:1. Thus, we simply return the Oracle FX
      // rates for the currency pair (i.e., the real-world external rates). Note that in the general case, checking the
      // real-world rates for an identical currency pair is unnecessary because the two currencies are the same and
      // should have a 1:1 rate. However, it is possible that two currencies with the same rate may have some sort of
      // fee derivation in their FX rate, so we _always_ consult the Oracle, even in a 1:1 FX scenario.
      final ExchangeRateProbeOutcome exchangeRateProbeOutcome = this
        .probePathHelper(streamConnection, this.skipPathProbePacketAmounts);

      // If the source denomination isn't present, then default to a 1:1 outcome. If it _is_ present, then make sure
      // a destination denomination is present.
      return streamConnection.getSourceAccountDetails().denomination()
        .map(sourceDenomination -> {
          final ExchangeRateTracker exchangeRateTracker = this.getPaymentSharedStateTracker(streamConnection)
            .map(PaymentSharedStateTracker::getExchangeRateTracker)
            .orElseThrow(() -> new StreamPayerException(
              String.format("No PaymentSharedStateTracker found for streamConnection=%s", streamConnection),
              SendState.ConnectorError
            ));

          final Denomination destinationDenomination = this.getPaymentSharedStateTracker(streamConnection)
            .map(PaymentSharedStateTracker::getAssetDetailsTracker)
            .map(AssetDetailsTracker::getDestinationAccountDetails)
            .orElseThrow(() -> new StreamPayerException(
              String.format("No PaymentSharedStateTracker found for streamConnection=%s", streamConnection),
              SendState.RateProbeFailed
            ))
            .denomination()
            .orElseThrow(() -> new StreamPayerException(
              "No destination asset details returned from receiver",
              SendState.UnknownDestinationAsset)
            );

          exchangeRateTracker.initializeRates(sourceDenomination, destinationDenomination);

          return ExchangeRateProbeOutcome.builder().from(exchangeRateProbeOutcome)
            .lowerBoundRate(exchangeRateTracker.getLowerBoundRate())
            .upperBoundRate(exchangeRateTracker.getUpperBoundRate())
            .build();

        })
        .map($ -> (ExchangeRateProbeOutcome) $)
        .orElse(exchangeRateProbeOutcome);
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
      return this.probePathHelper(streamConnection, this.probedPathPacketAmounts);
    }

    // TODO: Javadoc
    protected ExchangeRateProbeOutcome probePathHelper(
      final StreamConnection streamConnection, final List<Long> packetAmountsList
    ) {
      Objects.requireNonNull(streamConnection);
      Objects.requireNonNull(packetAmountsList);

      // If this is the first usage of streamConnection, then construct a new PaymentTracker instances. Otherwise,
      // re-use existing trackers so the new payment can benefit from historical information.
      final PaymentSharedStateTracker paymentSharedStateTracker = paymentSharedStateTrackersMap.computeIfAbsent(
        streamConnection, ($) -> new PaymentSharedStateTracker($, oracleExchangeRateService)
      );

      final List<StreamPacketFilter> streamPacketFilters = Lists.newArrayList(
        // First so all other controllers log the sequence number
        new SequenceFilter(paymentSharedStateTracker),

        // FailureFilter: Don't use the failure filter because ALL probe packets will reject, and this will cause a
        // false negative. It's also tolerable to not use the FailureFilter because there are only 14 packets to send,
        // and so sending those packets even if there's a terminal error is a small enough risk to ignore.

        // Fail-fast on destination asset detail conflict
        new AssetDetailsFilter(paymentSharedStateTracker),

        // Fail-fast if max packet amount is 0
        new MaxPacketAmountFilter(paymentSharedStateTracker.getMaxPacketAmountTracker()),

        // PacingFilter: Don't use because probing only sends a limited number of packets, no need to limit how
        // frequently packets are send.

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
      CompletableFuture<Void> allFutures = CompletableFuture
        .allOf(packetAmountsList.stream()
          .map(UnsignedLong::valueOf)
          .map(prepareAmount -> {
            // Send a StreamPacket and get a response.
            final Supplier<StreamPacketReply> streamResponseSupplier = () -> {

              // Probe packets should never fulfill.
              final ModifiableStreamPacketRequest streamPacketRequest = ModifiableStreamPacketRequest.create()
                .setIsFulfillable(false)
                .setSourceAmount(prepareAmount);
              final StreamPacketFilterChain filterChain = new DefaultStreamPacketFilterChain(
                streamPacketFilters, link, getStreamEncryptionService(), paymentSharedStateTracker
              );

              // Handle SendState stuff...
              final SendState nextSendState = filterChain.nextState(streamPacketRequest);
              if (nextSendState == SendState.Ready || nextSendState == SendState.Wait) {
                // Packet is sent via Link at end of this chain.
                return filterChain.doFilter(streamPacketRequest);
              } else if (nextSendState.isPaymentError()) {
                logger.info("Ending Payment with sendState={}", nextSendState);
                if (shouldCloseConnection(nextSendState)) {
                  this.closeConnection(streamConnection, SendState.getCorrespondingErrorCode(nextSendState));
                }
                return StreamPacketReply.builder()
                  .interledgerPreparePacket(streamPacketRequest.interledgerPreparePacket())
                  .build();
              } else {
                throw new RuntimeException(String.format("Encountered unhandled sendState: %s", nextSendState));
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

      // TODO: Introduce a boolean to indicate if the path-probe is skipped.
      final Ratio lowerBoundRate;
      final Ratio upperBoundRate;
      if (packetAmountsList.size() == 1) {
        lowerBoundRate = Ratio.ONE;
        upperBoundRate = Ratio.ONE;
      } else {
        lowerBoundRate = paymentSharedStateTracker.getExchangeRateTracker().getLowerBoundRate();
        upperBoundRate = paymentSharedStateTracker.getExchangeRateTracker().getUpperBoundRate();
      }

      return ExchangeRateProbeOutcome.builder()
        .sourceDenomination(streamConnection.getSourceAccountDetails().denomination())
        .destinationDenomination(
          paymentSharedStateTracker.getAssetDetailsTracker().getDestinationAccountDetails().denomination()
        )
        .maxPacketAmount(paymentSharedStateTracker.getMaxPacketAmountTracker().getMaxPacketAmount())
        .verifiedPathCapacity(paymentSharedStateTracker.getMaxPacketAmountTracker().verifiedPathCapacity())
        .lowerBoundRate(lowerBoundRate)
        .upperBoundRate(upperBoundRate)
        .errorPackets(errorReplies)
        .build();
    }

    @Override
    public Optional<PaymentSharedStateTracker> getPaymentSharedStateTracker(final StreamConnection streamConnection) {
      Objects.requireNonNull(streamConnection);
      return Optional.ofNullable(paymentSharedStateTrackersMap.get(streamConnection));
    }

    @Override
    protected Link<?> getLink() {
      return this.link;
    }
  }
}