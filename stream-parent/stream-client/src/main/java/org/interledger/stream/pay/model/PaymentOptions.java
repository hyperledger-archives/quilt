package org.interledger.stream.pay.model;

import java.math.BigDecimal;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.interledger.fx.Slippage;
import org.interledger.spsp.PaymentPointer;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.model.ImmutablePaymentOptions.Builder;

@Immutable
public interface PaymentOptions {

  static Builder builder() {
    return ImmutablePaymentOptions.builder();
  }

  /**
   * The Account details of the sender. Note that in certain use-cases this can be populated via IL-DCP or with a
   * private network value if the details are not meaningful.
   *
   * @return An {@link AccountDetails}.
   */
  AccountDetails senderAccountDetails();

  PaymentPointer destinationPaymentPointer();

  @Default
  default Slippage slippage() {
    return Slippage.ONE_PERCENT;
  }

  /**
   * Fixed amount to send to the recipient, in normalized source-units with arbitrary precision (E.g., if the sender
   * wants to send three dollars, then this value would be 3.0, even though the source-account sending the payment may
   * have a different precision).
   *
   * @return
   */
  // TODO: Use a Money here so that the currency is reflected in the amount? If the desired currency doesn't match the
  //  account, then we can throw an exception because the sender should know how the source currency code ahead of time.
  BigDecimal amountToSend();

}
