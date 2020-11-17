package org.interledger.stream.pay.probing;

import static org.interledger.stream.utils.StreamPacketUtils.DEFAULT_STREAM_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedLong;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.link.Link;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.errors.StreamConnectionClosedException;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.pay.StreamConnection;
import org.interledger.stream.pay.probing.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;
import org.interledger.stream.utils.StreamPacketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service for probing a particular path in order to determine exchange rates.
 */
public interface ExchangeRateProber {

  /**
   * Accessor for the {@link PaymentSharedStateTracker} for the supplied {@code streamConnection}.
   *
   * @param streamConnection A {@link StreamConnection} to identify a payment.
   * @return An optionally present {@link PaymentSharedStateTracker}.
   */
  Optional<PaymentSharedStateTracker> getPaymentSharedStateTracker(StreamConnection streamConnection);

  /**
   * // TODO: Javadoc
   *
   * @return
   */
  ExchangeRateProbeOutcome probePath(StreamConnection streamConnection);

  /**
   * The default implementation of {@link ExchangeRateProber}.
   */
  class Default implements ExchangeRateProber {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StreamEncryptionUtils streamEncryptionUtils;
    private final Link<?> link;
    private final ExecutorService executorService;
    private final Map<StreamConnection, PaymentSharedStateTracker> paymentSharedStateTrackersMap;

    private final List<Long> initialTestPacketAmounts;
    private Duration timeoutDuration;

    public Default(
      final StreamEncryptionUtils streamEncryptionUtils,
      final Link<?> link
    ) {
      this.streamEncryptionUtils = Objects.requireNonNull(streamEncryptionUtils);
      this.link = Objects.requireNonNull(link);

      this.paymentSharedStateTrackersMap = Maps.newConcurrentMap();
      this.executorService = Executors.newFixedThreadPool(10); // TODO: 2? 5?
      this.timeoutDuration = Duration.of(30, ChronoUnit.SECONDS);
      this.initialTestPacketAmounts = Lists.newArrayList(
        0L,
        1_000_000_000_000L,
        1_000_000_000_00L,
        1_000_000_000_0L,
        1_000_000_000L,
        1_000_000_00L,
        1_000_000_0L,
        1_000_000L,
        1_000_00L,
        1_000_0L,
        1_000L,
        100L,
        10L,
        1L
      );
    }

    @Override
    public ExchangeRateProbeOutcome probePath(final StreamConnection streamConnection) {
      Objects.requireNonNull(streamConnection);

      // If this is the first usage of streamConnection, then construct a new PaymentTracker instances. Otherwise,
      // re-use existing trackers so the new payment can benefit from historical information.
      final PaymentSharedStateTracker paymentSharedStateTracker = paymentSharedStateTrackersMap.computeIfAbsent(
        streamConnection, $ -> new PaymentSharedStateTracker($)
      );

      // Assemble a bunch of packets for each amount.
      // Send the packets in parallel.
      // Process each response in parallel.
      // return the outcome.

      // Handle the result
      // TODO: Ignore replies that aren't authentic
      List<CompletableFuture<Optional<StreamPacketReply>>> allProbePackets = this.initialTestPacketAmounts.stream()
        .map($ -> UnsignedLong.valueOf($))
        .map(prepareAmount -> {
          // Send a StreamPacket and get a response.
          final Supplier<Optional<StreamPacketReply>> streamResponseSupplier = () -> {
            final Supplier<InterledgerPreparePacket> preparePacketSupplier;
            try {
              preparePacketSupplier = constructPacketForProbing(streamConnection, prepareAmount);
            } catch (StreamConnectionClosedException e) {
              throw new RuntimeException(e.getMessage(), e);
            }

            final InterledgerPreparePacket preparePacket = preparePacketSupplier.get();
            logger.debug("Probing with packet={}", preparePacket);
            return link.sendPacket(preparePacket)
              ///////////////////////////
              // Capture the StreamPacket response
              .map(
                fulfillPacket -> StreamPacketUtils
                  .withStreamPacket(fulfillPacket, streamConnection.getSharedSecret(), streamEncryptionUtils),
                rejectPacket -> StreamPacketUtils
                  .withStreamPacket(rejectPacket, streamConnection.getSharedSecret(), streamEncryptionUtils)
              )
              .handleAndReturn(interledgerFulfillPacket -> {
                  // TODO: Move this down to where CAD are processed on the StreamPacketReply
                  if (StreamPacketUtils.isAuthentic(interledgerFulfillPacket)) {
                    // Adjust the path capacity with the ack'd amount
                    paymentSharedStateTracker.getMaxPacketAmountTracker().adjustPathCapacity(prepareAmount);
                  }
                },
                interledgerRejectPacket -> {
                  if (interledgerRejectPacket.getCode() == InterledgerErrorCode.F08_AMOUNT_TOO_LARGE) {
                    paymentSharedStateTracker.getMaxPacketAmountTracker()
                      .reduceMaxPacketAmount(interledgerRejectPacket, prepareAmount);
                  } else {
                    // TODO: Move this down to where CAD are processed on the StreamPacketReply
                    if (StreamPacketUtils.isAuthentic(interledgerRejectPacket)) {
                      // Adjust the path capacity with the ack'd amount
                      paymentSharedStateTracker.getMaxPacketAmountTracker().adjustPathCapacity(prepareAmount);
                    }
                  }
                })
              ///////////////////////////
              // Map to StreamPacketReply
              .mapResponse(interledgerResponsePacket -> Optional.of(StreamPacketReply.builder()
                .interledgerResponsePacket(interledgerResponsePacket)
                .build())
              )
              ///////////////////////////
              // Capture ConnectionAssetDetails
              .map(streamPacketReply -> {
                paymentSharedStateTracker.getAssetDetailsTracker().handleDestinationDetails(streamPacketReply);
                return streamPacketReply;
              });
          };

          final CompletableFuture<Optional<StreamPacketReply>> future = CompletableFuture
            .supplyAsync(streamResponseSupplier, executorService)
            // Handle the StreamPacketResult
            .handle((streamPacketReply, throwable) -> {
              if (throwable != null) {
                // TODO: Log packet details?
                logger.error("ExchangeRateProbe packet failed: " + throwable.getMessage(), throwable);
                return streamPacketReply;
              } else {
                logger.info("StreamPacketReply={}", streamPacketReply);
                streamPacketReply
                  .map(StreamPacketReply::interledgerResponsePacket)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .map(InterledgerResponsePacket::typedData)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .filter($ -> StreamPacket.class.isAssignableFrom($.getClass()))
                  .map($ -> (StreamPacket) $)
                  // TODO: Ignore replies that aren't authentic. We should check this against the StreamConnection,
                  // although if we assume that if typed-data exists, then it was properly decrypted by the link,
                  // this this check is maybe not necessary? Still, we should assert that the StreamConnection can
                  // decrypt this packet. Consider moving isAuthentic to StreamConnection?
                  //.filter(StreamUtils::isAuthentic)
                  .map(StreamPacket::prepareAmount)
                  .filter(receivedAmount -> FluentCompareTo.is(prepareAmount).greaterThan(UnsignedLong.ZERO))
                  .ifPresent(claimedReceivedAmount ->
                    paymentSharedStateTracker.getExchangeRateTracker().updateRate(prepareAmount, claimedReceivedAmount)
                  );

                return streamPacketReply;
              }
            });

          return future;
        })
        .collect(Collectors.toList());

      CompletableFuture<Void> allFutures = CompletableFuture
        .allOf(allProbePackets.toArray(new CompletableFuture[allProbePackets.size()]))
        .whenComplete(($, error) -> {
          executorService.shutdown();
          if (error != null) {
            logger.error("ExchangeRateProbe failed: " + error.getMessage(), error);
          }
        });

      // Wait for all futures to complete.
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
        .lowerBoundRate(paymentSharedStateTracker.getExchangeRateTracker().getLowerBoundRate())
        .upperBoundRate(paymentSharedStateTracker.getExchangeRateTracker().getUpperBoundRate())
        .build();
    }

    @VisibleForTesting
    protected Supplier<InterledgerPreparePacket> constructPacketForProbing(
      final StreamConnection streamConnection, final UnsignedLong prepareAmount
    ) throws StreamConnectionClosedException {

      final List<StreamFrame> frames = Lists.newArrayList(
        StreamMoneyFrame.builder()
          // Probing always uses the default STREAM ID
          .streamId(DEFAULT_STREAM_ID)
          .shares(UnsignedLong.ONE)
          .build(),
        // Send a CNA frame on all probing calls in order to ensure we get back the CAD frame for FX purposes.
        ConnectionNewAddressFrame.builder()
          .sourceAddress(streamConnection.getSourceAccountDetails().interledgerAddress())
          .build()
      );

      final StreamPacket streamPacket = StreamPacket.builder()
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        // If the STREAM packet is sent on an ILP Prepare, this represents the minimum the receiver should accept.
        .prepareAmount(UnsignedLong.ZERO)
        .sequence(UnsignedLong.valueOf(streamConnection.nextSequence().longValue()))
        .frames(frames)
        .build();

      // Create the ILP Prepare packet
      final byte[] streamPacketData = this.streamEncryptionUtils
        .toEncrypted(streamConnection.getSharedSecret(), streamPacket);
      final InterledgerCondition executionCondition = StreamUtils.unfulfillableCondition();

      final Supplier<InterledgerPreparePacket> preparePacket = () -> InterledgerPreparePacket.builder()
        .destination(streamConnection.getDestinationAddress())
        .amount(prepareAmount)
        .executionCondition(executionCondition)
        .expiresAt(DateUtils.now().plusSeconds(30L))
        .data(streamPacketData)
        // Added here for JVM convenience, but only the bytes above are encoded to ASN.1 OER
        .typedData(streamPacket)
        .build();

      return preparePacket;
    }

    public Optional<PaymentSharedStateTracker> getPaymentSharedStateTracker(final StreamConnection streamConnection) {
      Objects.requireNonNull(streamConnection);
      return Optional.ofNullable(paymentSharedStateTrackersMap.get(streamConnection));
    }
  }
}