package org.interledger.stream.pay.trackers;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPacket;
import org.interledger.fx.Denomination;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.utils.StreamPacketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that tracks asset details as part of a STREAM payment.
 */
public class AssetDetailsTracker {

  private final static Logger logger = LoggerFactory.getLogger(AssetDetailsTracker.class);

  private final AtomicBoolean remoteKnowsOurAccount;

  private final AtomicReference<AccountDetails> sourceAccountDetailsRef;
  private final AtomicReference<AccountDetails> destinationAccountDetailsRef;

  private final AtomicBoolean remoteAssetChanged;

  public AssetDetailsTracker(final InterledgerAddress destinationAddress) {
    this.destinationAccountDetailsRef = new AtomicReference(
      AccountDetails.builder()
        .interledgerAddress(destinationAddress)
        .build()
    );

    this.sourceAccountDetailsRef = new AtomicReference();
    this.remoteKnowsOurAccount = new AtomicBoolean();
    this.remoteAssetChanged = new AtomicBoolean();
  }

  public AccountDetails getSourceAccountDetails() {
    return sourceAccountDetailsRef.get();
  }

  // TODO: Consider just storing an AccountDetails instead?
  public AccountDetails getDestinationAccountDetails() {
    return destinationAccountDetailsRef.get();
  }

  public void handleDestinationDetails(final StreamPacketReply streamPacketReply) {
    Objects.requireNonNull(streamPacketReply);

    // TODO: Create a new Tracker that can handle a CNA frame (ConnectionAddressTracker). Should work like this one,
    // but allow a receiver to migrate their address during a STREAM. Note, check RFC first to ensure this is still
    // legal.

    // TODO: Prefer using StreamPacketUtils
    streamPacketReply.interledgerResponsePacket()
      // Assume the the StreamPacket is inside of typedData.
      .map(InterledgerPacket::typedData)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter($ -> StreamPacket.class.isAssignableFrom($.getClass()))
      .map($ -> (StreamPacket) $)
      // Map streamPacket to CAD frame.
      .map(streamPacket -> StreamPacketUtils.getConnectionAssetDetailsFrame(streamPacket))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .ifPresent(connectionAssetDetailsFrame -> {
        final AccountDetails destinationAccountDetailsSnapshot = destinationAccountDetailsRef.get();
        if (destinationAccountDetailsSnapshot.denomination().isPresent() == false) {
          // First time. Ignore a failure on the compare and set because it's possible another thread beat us here.
          // We assume it set things properly and made it first, and ignore this
          destinationAccountDetailsRef.compareAndSet(
            destinationAccountDetailsSnapshot,
            AccountDetails.builder()
              .from(destinationAccountDetailsSnapshot)
              .denomination(connectionAssetDetailsFrame.sourceDenomination())
              .build());
        } else {
          // Here, the denomination has already been set. So check the snapshot against the CAD frame.
          final Denomination denominationSnapshot = destinationAccountDetailsSnapshot.denomination().get();
          if (denominationSnapshot.equals(connectionAssetDetailsFrame.sourceDenomination()) == false) {
            throw new StreamPayerException(
              String.format(
                "Ending payment: remote unexpectedly changed destination asset from %s to %s",
                destinationAccountDetailsSnapshot.denomination().get(), connectionAssetDetailsFrame.sourceDenomination()
              ), SendState.DestinationAssetConflict
            );
          } else {
            // The Denominations haven't changed, but this is a duplicate frame, so warn.
            logger.warn(
              "Stream receiver sent a duplicate Connection Asset Details, but this will be ignored. "
                + "connectionAssetDetailsFrame={}", connectionAssetDetailsFrame
            );
          }
        }
      });
  }

  public boolean getRemoteKnowsOurAccount() {
    return remoteKnowsOurAccount.get();
  }

  //
//  public AtomicReference<InterledgerAddress> getDestinationAddress() {
//    return destinationAddress;
//  }
//
//  public AtomicReference<Denomination> getDestinationDenonimation() {
//    return destinationDenonimation;
//  }
//
  public boolean getRemoteAssetChanged() {
    return remoteAssetChanged.get();
  }
}
