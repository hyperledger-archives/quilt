package org.interledger.stream.pay.trackers;

import org.interledger.core.InterledgerAddress;
import org.interledger.fx.Denomination;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A service that tracks asset details as part of a STREAM payment.
 */
public class AssetDetailsTracker {

  private static final Logger LOGGER = LoggerFactory.getLogger(AssetDetailsTracker.class);

  private final AccountDetails sourceAccountDetails;
  private final AtomicReference<AccountDetails> destinationAccountDetailsRef;

  private final AtomicBoolean remoteKnowsOurAccount;
  private final AtomicBoolean remoteAssetChanged;

  /**
   * Required-args Constructor.
   *
   * @param sourceAccountDetails An {@link AccountDetails}.
   * @param destinationAddress   An {@link InterledgerAddress}.
   */
  public AssetDetailsTracker(final AccountDetails sourceAccountDetails, final InterledgerAddress destinationAddress) {
    this.sourceAccountDetails = Objects.requireNonNull(sourceAccountDetails);
    this.destinationAccountDetailsRef = new AtomicReference<>(
      AccountDetails.builder()
        .interledgerAddress(destinationAddress)
        .build()
    );

    this.remoteKnowsOurAccount = new AtomicBoolean();
    this.remoteAssetChanged = new AtomicBoolean();
  }

  /**
   * Handle the destination details for the supplied {@code streamPacketReply}.
   *
   * @param streamPacketReply A {@link StreamPacketReply}.
   */
  public void handleDestinationDetails(final StreamPacketReply streamPacketReply) {
    Objects.requireNonNull(streamPacketReply);

    this.remoteKnowsOurAccount.set(this.remoteKnowsOurAccount.get() || streamPacketReply.isAuthentic());

    // TODO [NewFeature]: Create a new Tracker that can handle a CNA frame (ConnectionAddressTracker). Should work
    // like this one, but allow a receiver to migrate their address during a STREAM. Note, check RFC first to ensure
    // this is still legal.

    streamPacketReply.streamPacket()
      .filter(streamPacket -> StreamPacketUtils.countConnectionAssetDetailsFrame(streamPacket) > 1)
      .ifPresent(streamPacket -> {
        throw new StreamPayerException(
          "Only one ConnectionAssetDetails frame allowed on a single connection",
          SendState.DestinationAssetConflict
        );
      });

    streamPacketReply.streamPacket()
      .map(StreamPacketUtils::findConnectionAssetDetailsFrame)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .ifPresent(connectionAssetDetailsFrame -> {
        final AccountDetails destinationAccountDetailsSnapshot = destinationAccountDetailsRef.get();
        if (!destinationAccountDetailsSnapshot.denomination().isPresent()) {
          // First time. Ignore a failure on the compare and set because it's possible another thread beat us here.
          // We assume it set things properly and made it first, and ignore this
          destinationAccountDetailsRef.compareAndSet(
            destinationAccountDetailsSnapshot,
            AccountDetails.builder()
              .from(destinationAccountDetailsSnapshot)
              .denomination(org.interledger.stream.Denomination.to(connectionAssetDetailsFrame.sourceDenomination()))
              .build()
          );
        } else {
          // Here, the denomination has already been set. So check the snapshot against the CAD frame.
          final Denomination denominationSnapshot = destinationAccountDetailsSnapshot.denomination().get();
          if (!denominationSnapshot
            .equals(org.interledger.stream.Denomination.to(connectionAssetDetailsFrame.sourceDenomination()))) {
            throw new StreamPayerException(
              String.format(
                "Ending payment: remote unexpectedly changed destination asset from %s to %s",
                destinationAccountDetailsSnapshot.denomination().get(), connectionAssetDetailsFrame.sourceDenomination()
              ), SendState.DestinationAssetConflict
            );
          } else {
            // The Denominations haven't changed, but this is a duplicate frame, so warn.
            LOGGER.debug(
              "Stream receiver sent a duplicate Connection Asset Details, but this will be ignored. " +
                "connectionAssetDetailsFrame={}", connectionAssetDetailsFrame
            );
          }
        }
      });
  }

  /**
   * Accessor for the source account details.
   *
   * @return An {@link AccountDetails}.
   */
  public AccountDetails getSourceAccountDetails() {
    return this.sourceAccountDetails;
  }

  /**
   * Accessor for the destination account details.
   *
   * @return An {@link AccountDetails}.
   */
  public AccountDetails getDestinationAccountDetails() {
    return destinationAccountDetailsRef.get();
  }

  /**
   * Accessor for indicating if the remote knows our account.
   *
   * @return {@code true} if the remote knows our account; {@code false} otherwise.
   */
  public boolean getRemoteKnowsOurAccount() {
    return remoteKnowsOurAccount.get();
  }

  /**
   * Accessor for indicating if the remote asset details changed during a payment.
   *
   * @return {@code true} if the remote asset details changed; {@code false} otherwise.
   */
  public boolean getRemoteAssetChanged() {
    return remoteAssetChanged.get();
  }
}
