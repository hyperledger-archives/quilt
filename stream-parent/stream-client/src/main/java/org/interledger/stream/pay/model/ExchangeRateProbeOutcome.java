package org.interledger.stream.pay.model;

import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.stream.pay.model.ImmutableExchangeRateProbeOutcome.Builder;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketAmount;

import org.immutables.value.Value.Immutable;

import java.util.Optional;

/**
 * The outcome of an ExchangeRate probe operation.
 */
@Immutable
public interface ExchangeRateProbeOutcome {

  static Builder builder() {
    return ImmutableExchangeRateProbeOutcome.builder();
  }

  /**
   * The max packet amount probed on the path.
   *
   * @return A {@link MaxPacketAmount}.
   */
  MaxPacketAmount maxPacketAmount();

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
   * @return
   */
  Ratio upperBoundRate();

  /**
   * The realized exchange rate is greater than or equal to this ratio (inclusive) (i.e., destination / source).
   */
  Ratio lowerBoundRate();

}
