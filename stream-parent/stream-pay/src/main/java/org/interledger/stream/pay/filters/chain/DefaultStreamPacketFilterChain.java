package org.interledger.stream.pay.filters.chain;

import com.google.common.primitives.UnsignedLong;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.AmountTooLargeErrorData;
import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;
import org.interledger.link.LinkSettings;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.pay.StreamConnection;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation of {@link StreamPacketFilterChain}.
 */
public class DefaultStreamPacketFilterChain implements StreamPacketFilterChain {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final List<StreamPacketFilter> streamPacketFilters;

  private final Link<? extends LinkSettings> link;

  private final StreamEncryptionUtils streamEncryptionUtils;

  private final PaymentSharedStateTracker paymentSharedStateTracker;

  private final CodecContext streamCodecContext = StreamCodecContextFactory.oer();

  // Executes the link sendMoney.
  private static final Executor EXECUTOR = Executors.newCachedThreadPool();

  // The index of the filter to call next...
  private int _filterIndex;

  // TODO: Create a StreamEventPublisher. See code in connector and port from there?
  //private PacketEventPublisher packetEventPublisher;

  /**
   * A chain of filters that are applied to a switchPacket request before attempting to determine the `next-hop` {@link
   * Link} to forward the packet onto.
   *
   * @param streamPacketFilters       A {@link List} of type {@link StreamPacketFilter}.
   * @param link
   * @param streamEncryptionUtils
   * @param paymentSharedStateTracker
   */
  public DefaultStreamPacketFilterChain(
    final List<StreamPacketFilter> streamPacketFilters,
    final Link<? extends LinkSettings> link,
    final StreamEncryptionUtils streamEncryptionUtils,
    final PaymentSharedStateTracker paymentSharedStateTracker
//    final PacketEventPublisher packetEventPublisher
  ) {
    this.streamPacketFilters = Objects.requireNonNull(streamPacketFilters);
    this.link = Objects.requireNonNull(link);
    this.streamEncryptionUtils = Objects.requireNonNull(streamEncryptionUtils);
    this.paymentSharedStateTracker = Objects.requireNonNull(paymentSharedStateTracker);
    this._filterIndex = 0;
  }

  @Override
  public SendState nextState(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    // TODO: Try/catch?
//    try {
    for (int i = 0; i < streamPacketFilters.size(); i++) {
      final SendState nextState = streamPacketFilters.get(i).nextState(streamPacketRequest);

      // Immediately end the payment and wait for all requests to complete
      if (nextState != SendState.Ready) { // <-- Wait should abort the nextState checks.
        return nextState;
      } else {
        continue; // <-- Check the next state.
      }
    }

    return SendState.Ready;
  }

  @Override
  public StreamPacketReply doFilter(final StreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    // This entire method MUST be wrapped in a broad try/catch to ensure that the filterChain is never aborted
    // accidentally. If an error is emitted anywhere in the filter-chain, it is always mapped to a reject packet so
    // that the entire filter-chain can always be processed properly.
    // See https://github.com/interledger4j/ilpv4-connector/issues/588

    try {

      for (int i = 0; i < streamPacketFilters.size(); i++) {
        if (this._filterIndex < this.streamPacketFilters.size()) {
          // Apply all streamClientFilters, but only if the state is not END...
          return streamPacketFilters
            .get(_filterIndex++)
            .doFilter(streamPacketRequest, this);
        } else {

          ///////////////////////
          // Check the SendState before sending.
          ///////////////////////

          if (streamPacketRequest.sendState() == SendState.End ||
            streamPacketRequest.sendState() == SendState.Wait ||
            streamPacketRequest.sendState().isPaymentError()) {
            // TODO: We can extract PaymentErrors out of StreamPacketReply on ground that we should throw an exception
            // instead. That means isPaymentError will go away and not need to be handled.
            return StreamPacketReply.builder()
              .interledgerPreparePacket(streamPacketRequest.interledgerPreparePacket())
              .build();
          } else { // Ready

            final InterledgerPreparePacket preparePacket = constructPreparePacket(streamPacketRequest);

            // Expiry timeout is handled here (not in a filter) on the premise that a filter can fail or allow a
            // developer to abort the filter pipeline, but we never want to allow a developer to accidentally do this so
            // that expiry handling of an outgoing request is always enforced.
            final Duration timeoutDuration = Duration.between(Instant.now(), preparePacket.getExpiresAt());
            if (timeoutDuration.isNegative() || timeoutDuration.isZero()) {
              // `timeoutDuration` can be negative, so need to perform this check here to make sure we don't send a
              // negative or 0 timeout into the completable future.
              logger.error("Invalid timeout. timeoutDuration={}", timeoutDuration);
              return StreamPacketReply.builder()
                .interledgerPreparePacket(preparePacket)
                .build();
            }

            try {
              // Send the preparePacket (fully assembled by all filters)
              final InterledgerResponsePacket interledgerResponsePacket = CompletableFuture
                .supplyAsync(() -> link.sendPacket(preparePacket), EXECUTOR)
                .get(timeoutDuration.getSeconds(), TimeUnit.SECONDS);

              // Map to a StreamPacketReply or an F08 payload.

              return interledgerResponsePacket.map(
                interledgerFulfillPacket -> {
                  final Optional<StreamPacket> typedStreamPacket = StreamPacketUtils.mapToStreamPacket(
                    interledgerResponsePacket.getData(),
                    this.paymentSharedStateTracker.getStreamConnection().getSharedSecret(),
                    streamEncryptionUtils
                  );
                  return StreamPacketReply.builder()
                    .interledgerPreparePacket(preparePacket)
                    .interledgerResponsePacket(interledgerFulfillPacket.withTypedDataOrThis(typedStreamPacket))
                    .build();
                },
                interledgerRejectPacket -> {
                  if (interledgerRejectPacket.getCode() == InterledgerErrorCode.F08_AMOUNT_TOO_LARGE) {
                    Optional<?> typedData = Optional.empty();
                    // Decode the F08
                    try {
                      typedData = Optional.ofNullable(
                        streamCodecContext.read(AmountTooLargeErrorData.class,
                          new ByteArrayInputStream(interledgerRejectPacket.getData()))
                      );
                    } catch (IOException e) {
                      logger.warn("Unable to decode AmountTooLargeErrorData", e);
                    }
                    return StreamPacketReply.builder()
                      .interledgerPreparePacket(preparePacket)
                      .interledgerResponsePacket(interledgerRejectPacket.withTypedDataOrThis(typedData))
                      .build();
                  } else {
                    final Optional<StreamPacket> typedStreamPacket = StreamPacketUtils.mapToStreamPacket(
                      interledgerResponsePacket.getData(),
                      this.paymentSharedStateTracker.getStreamConnection().getSharedSecret(),
                      streamEncryptionUtils
                    );
                    return StreamPacketReply.builder()
                      .interledgerPreparePacket(preparePacket)
                      .interledgerResponsePacket(interledgerRejectPacket.withTypedDataOrThis(typedStreamPacket))
                      .build();
                  }
                }
              );
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
              logger.error(e.getMessage(), e);
              return StreamPacketReply.builder()
                .interledgerPreparePacket(preparePacket)
                .build();
            }
          }
        }
      }
      return StreamPacketReply.builder()
//        .sendState(SendState.End)
        .build();
    }
    //catch (StreamPayerException spe) {
//      logger.error(spe.getMessage(), spe);
//      return StreamPacketReply.builder()
//        .interledgerPreparePacket(spe.)
//        .sendState(SendState.End).build();
//    }
    catch (Exception e) {
      // Handler of last resort.
      // TOdO: FIXME!
      logger.error(e.getMessage(), e);
      return StreamPacketReply.builder()
//        .sendState(SendState.End)
        .build();
    }
  }

  // Wait 50ms (?)
  //Thread.sleep(50);

//        final InterledgerPreparePacket preparePacket = this.constructPreparePacket(streamPacketRequest);
//
//        // ...and then send the new packet to its destination on the correct outbound link.
//        // TODO: Handle.
//        logger.debug("Sending outbound ILP Prepare with STREAM Packet: link={} packet={}", link, preparePacket);
//        StreamReply streamReply = link.sendPacket(preparePacket).map(
//            interledgerFulfillPacket -> {
//
//              // TODO: Use typed data if present.
////              interledgerFulfillPacket.typedData()
//              final UnsignedLong amountDelivered = UnsignedLong.ZERO;
//
//              return StreamFulfill.builder()
//                  //.amountDelivered(amountDelivered)
//                  // TODO:
////                  .interledgerFulfillPacket(interledgerFulfillPacket)
//                  .build();
//            },
//            interledgerRejectPacket -> {
//              return StreamReject.builder()
//                  // TODO:
////                  .interledgerRejectPacket(interledgerRejectPacket)
//                  .build();
//            }
//        );

  /////////////
  // Packet Tracking
  // TODO:
  //this.trackPacket(sourceAccountSettings, preparePacket, nextHopInfo, nextHopAccountSettings, response);

  //return streamReply;
//      }
//    } catch (Exception e) {
//      // If anything in the filterchain emits an exception, this is considered a failure case. These always translate
//      // into a rejection.
//      logger.error("Failure in StreamClientFilterChain: " + e.getMessage(), e);
//
//      final Builder builder = StreamReject.builder();
//      if (InterledgerRuntimeException.class.isAssignableFrom(e.getClass())) {
//        InterledgerProtocolException ipe = ((InterledgerProtocolException) e);
//        // TODO Add the STREAM Frames into the response.
//        //        builder.frames(
////            ipe.getInterledgerRejectPacket().getData()
////        )
//        return StreamReject.builder()
//            .interledgerResponsePacket(ipe.getInterledgerRejectPacket())
//            .build();
//      } else {
//        // TODO: Finish
//        return StreamReject.builder()
//            .build();
//      }
//    }
  //return null;

//  @Override
//  public SendState getState() {
//    return this.sendState.get();
//  }

  /**
   * Default maximum duration that a ILP Prepare can be in-flight before it should be rejected
   */
  private static final Duration DEFAULT_PACKET_TIMEOUT = Duration.of(30, ChronoUnit.SECONDS);

  // TODO: This should be constructed after all nextStates have been evaluated, and put onto the StreamPacketRequest
  // before filtering...?
  private InterledgerPreparePacket constructPreparePacket(final StreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    final StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      // TODO: Consider UnsignedInt for StreamPacket.
      .prepareAmount(streamPacketRequest.minDestinationAmount()) // <-- The min amount the receiver should accept
      .sequence(UnsignedLong.valueOf(streamPacketRequest.sequence().longValue()))
      .frames(streamPacketRequest.requestFrames())
      .build();

    final StreamConnection streamConnection = this.paymentSharedStateTracker.getStreamConnection();
    final byte[] streamPacketData = streamEncryptionUtils
      .toEncrypted(streamConnection.getSharedSecret(), streamPacket);
    final InterledgerCondition executionCondition;
    if (streamPacketRequest.isFulfillable()) {
      // Create the ILP Prepare packet
      executionCondition = StreamPacketUtils.generatedFulfillableFulfillment(
        streamConnection.getSharedSecret(), streamPacketData
      ).getCondition();
    } else {
      executionCondition = StreamPacketUtils.unfulfillableCondition();
    }

    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .destination(streamConnection.getDestinationAddress())
      .amount(streamPacketRequest.sourceAmount())
      .executionCondition(executionCondition)
      .expiresAt(DateUtils.now().plus(DEFAULT_PACKET_TIMEOUT))
      .data(streamPacketData)
      .typedData(streamPacket)
      .build();

    return preparePacket;
  }

//  /**
//   * Track this packet by emitting proper events depending on the response.
//   */
//  private void trackPacket(
//      final AccountSettings sourceAccountSettings,
//      final InterledgerPreparePacket preparePacket,
//      final NextHopInfo nextHopInfo,
//      final AccountSettings nextHopAccountSettings,
//      final InterledgerResponsePacket response
//  ) {
//    try {
//      BigDecimal fxRate = nextHopPacketMapper.determineExchangeRate(
//          sourceAccountSettings, nextHopAccountSettings, preparePacket
//      );
//      response.handle(interledgerFulfillPacket ->
//          packetEventPublisher.publishFulfillment(
//              sourceAccountSettings,
//              nextHopAccountSettings,
//              preparePacket,
//              nextHopInfo.nextHopPacket(),
//              fxRate,
//              interledgerFulfillPacket.getFulfillment()
//          ), (rejectPacket) ->
//          packetEventPublisher.publishRejectionByNextHop(
//              sourceAccountSettings,
//              nextHopAccountSettings,
//              preparePacket,
//              nextHopInfo.nextHopPacket(),
//              fxRate,
//              rejectPacket
//          )
//      );
//    } catch (Exception e) {
//      logger.warn("Could not publish event", e);
//    }
//  }

}
