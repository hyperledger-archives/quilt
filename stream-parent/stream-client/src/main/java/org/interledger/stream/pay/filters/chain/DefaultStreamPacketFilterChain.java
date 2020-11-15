package org.interledger.stream.pay.filters.chain;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.link.LinkSettings;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.StreamPacketFilter;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.model.StreamPacketRequest;

import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * A default implementation of {@link StreamPacketFilterChain}.
 */
public class DefaultStreamPacketFilterChain implements StreamPacketFilterChain {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  //private final AtomicReference<SendState> sendState;
  private final List<StreamPacketFilter> streamPacketFilters;

  private final Link<? extends LinkSettings> link;

  // Executes the link sendMoney.
  private static final Executor EXECUTOR = Executors.newCachedThreadPool();

  // Loading from the Database is somewhat expensive, so we don't want to do this on every packet processed for a
  // given account. Instead, for higher performance, we only load account settings once per period, and otherwise
  // rely upon AccountSettings found in this cache.
//  private final AccountSettingsLoadingCache accountSettingsLoadingCache;

  // The index of the filter to call next...
  private int _filterIndex;

  // TODO: Create a StreamEventPublisher. See code in connector and port from there?
  //private PacketEventPublisher packetEventPublisher;

  /**
   * A chain of filters that are applied to a switchPacket request before attempting to determine the `next-hop` {@link
   * Link} to forward the packet onto.
   *
   * @param streamPacketFilters A {@link List} of type {@link StreamPacketFilter}.
   * @param link
   */
  public DefaultStreamPacketFilterChain(
      final List<StreamPacketFilter> streamPacketFilters,
      final Link<? extends LinkSettings> link
//    final PacketEventPublisher packetEventPublisher
  ) {
    //this.packetRejector = Objects.requireNonNull(packetRejector);
    this.streamPacketFilters = Objects.requireNonNull(streamPacketFilters);
    this.link = Objects.requireNonNull(link);
    this._filterIndex = 0;
    // this.sendState = new AtomicReference<>(SendState.Ready);
  }

  @Override
  public StreamPacketReply doFilter(final ModifiableStreamPacketRequest streamPacketRequest) {
    Objects.requireNonNull(streamPacketRequest);

    // This entire method MUST be wrapped in a broad try/catch to ensure that the filterChain is never aborted
    // accidentally. If an error is emitted anywhere in the filter-chain, it is always mapped to a reject packet so
    // that the entire filter-chain can always be processed properly.
    // See https://github.com/interledger4j/ilpv4-connector/issues/588
    // try {

    for (int i = 0; i < streamPacketFilters.size(); i++) {

      if (this._filterIndex < this.streamPacketFilters.size()) {
        // Apply all streamClientFilters, but only if the state is not END...
        return streamPacketFilters
            .get(_filterIndex++)
            .doFilter(streamPacketRequest, this);
      } else {

        try {
//            LOGGER.debug(
//                "Sending outbound ILP Prepare. destinationAccountSettings: {}; link={}; packet={};",
//                destinationAccountSettings, link, preparePacket
//            );

          ///////////////////////
          // Check the NextStates
          ///////////////////////

          if (streamPacketRequest.sendState() == SendState.End || streamPacketRequest.sendState().isPaymentError()) {
            // TODO: Do end.
            return StreamPacketReply.builder().sendState(streamPacketRequest.sendState()).build();
          } else if (streamPacketRequest.sendState() == SendState.Wait) {

          } else { // Ready

            final InterledgerPreparePacket preparePacket = constructPreparePacket(streamPacketRequest);

            // Expiry timeout is handled here (not in a filter) on the premise that a filter can fail or allow a
            // developer to abort the filter pipeline, but we never want to allow a developer to accidentally do this so
            // that expiry handling of an outgoing request is always enforced.
            final Duration timeoutDuration = Duration.between(Instant.now(), preparePacket.getExpiresAt());

            // `timeoutDuration` can be negative, so need to perform this check here to make sure we don't send a
            // negative or 0 timeout into the completable future.
//            if (timeoutDuration.isNegative() || timeoutDuration.isZero()) {
//              return packetRejector.reject(
//                  LinkId.of(destinationAccountSettings.accountId().value()),
//                  preparePacket,
//                  R02_INSUFFICIENT_TIMEOUT,
//                  "The connector could not forward the payment, because the timeout was too low"
//              );
//            }

            InterledgerResponsePacket interledgerResponsePacket = CompletableFuture
                .supplyAsync(() -> link.sendPacket(preparePacket), EXECUTOR)
                .get(timeoutDuration.getSeconds(), TimeUnit.SECONDS);

            return interledgerResponsePacket.map(
                interledgerFulfillPacket -> {
                  return StreamPacketReply.builder()
                      .interledgerResponsePacket(Optional.of(interledgerFulfillPacket))
                      .build();
                },
                interledgerRejectPacket -> {
                  return StreamPacketReply.builder()
                      .interledgerResponsePacket(Optional.of(interledgerRejectPacket))
                      .build();
                }
            );
          }

        } catch (InterruptedException | ExecutionException e) {
          logger.error(e.getMessage(), e);
          // TODO:
          return null;
//            return packetRejector.reject(
//                LinkId.of(destinationAccountSettings.accountId().value()),
//                preparePacket,
//                InterledgerErrorCode.T00_INTERNAL_ERROR,
//                String.format("Internal Error: %s", e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
//            );
        } catch (TimeoutException e) {
          //logger.error(e.getMessage(), e);
//          return packetRejector.reject(
//              LinkId.of(destinationAccountSettings.accountId().value()),
//              preparePacket,
//              InterledgerErrorCode.R00_TRANSFER_TIMED_OUT,
//              "Transfer Timed-out"
//          );
          //TODO:
          throw new StreamPayerException(e.getMessage(), e, SendState.Disconnected);
        }
        //catch (Exception e) {
//          logger.error(e.getMessage(), e);
////          return packetRejector.reject(
////              LinkId.of(destinationAccountSettings.accountId().value()),
////              preparePacket,
////              InterledgerErrorCode.T00_INTERNAL_ERROR,
////              String.format("Internal Error: %s", e.getMessage())
////          );
//          // TODO:
//          return null;
//        }

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
    return null;
  }

//  @Override
//  public SendState getState() {
//    return this.sendState.get();
//  }

  private InterledgerPreparePacket constructPreparePacket(StreamPacketRequest streamPacketRequest) {
    // TODO: Do this for real.
    return InterledgerPreparePacket.builder()
        .amount(UnsignedLong.ZERO)
        .destination(InterledgerAddress.of("example.foo"))
        .expiresAt(Instant.now().plus(30, ChronoUnit.SECONDS))
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .build();
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
