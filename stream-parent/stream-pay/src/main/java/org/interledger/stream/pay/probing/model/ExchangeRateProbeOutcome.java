package org.interledger.stream.pay.probing.model;

import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketAmount;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The outcome of an ExchangeRate probe operation.
 */
@Immutable
@JsonSerialize(as = ImmutableExchangeRateProbeOutcome.class)
@JsonDeserialize(as = ImmutableExchangeRateProbeOutcome.class)
public interface ExchangeRateProbeOutcome {

  static ImmutableExchangeRateProbeOutcome.Builder builder() {
    return ImmutableExchangeRateProbeOutcome.builder();
  }

  /**
   * An object that allows for fine-grained interpretation of the max-packet amount for a path. This value can differ
   * slightly from the verified path-capacity because the verified amount maybe be less-than the discovered max-packet
   * amount. For example, imagine an exchange-rate probe on a path with a maxPacketAmount of 5 that sends two packets,
   * one for 10 and one for 1. In this example, the maxPacketAmount will be 9 (imprecise) whereas the verified
   * path-capacity will be 1.
   *
   * @return A {@link MaxPacketAmount}.
   */
  MaxPacketAmount maxPacketAmount();

  /**
   * The greatest amount that the recipient has acknowledged to have received. Note: this is always reduced so it's
   * never greater than the max packet amount.
   *
   * @return
   */
  @Default
  default UnsignedLong verifiedPathCapacity() {
    return UnsignedLong.ZERO;
  }

  /**
   * The source account's denomination. Optional because some senders don't specify or care about their own exchange
   * rate (e.g., in same-currency scenarios).
   *
   * @return
   */
  Optional<Denomination> sourceDenomination();

  /**
   * The destination account's denomination is often not known until after an FX probe (or some other STREAM
   * interaction), so we allow for it to be returned here. Per IL-RFC-29, the denomination is not allowed to change once
   * it has been established, so if this value is present here, it means that this value was shared by the receiver.
   */
  Optional<Denomination> destinationDenomination();

  /**
   * The realized exchange-rate is less than this ratio (exclusive) (i.e., destination / source).
   *
   * @return A {@link Ratio}.
   */
  Ratio upperBoundRate();

  /**
   * The realized exchange rate is greater than or equal to this ratio (inclusive) (i.e., destination / source).
   *
   * @return A {@link Ratio}.
   */
  Ratio lowerBoundRate();

  /**
   * A collection of replies of type {@link StreamPacketReply} that had an error during transmission while attempting a
   * rate probe.
   *
   * @return A {@link Collection} of type {@link StreamPacketReply}.
   */
  List<StreamPacketReply> errorPackets();

}
