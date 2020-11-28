package org.interledger.stream.pay.model;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Immutable;
import org.interledger.spsp.PaymentPointer;

@Immutable
public interface QuoteRequest {

  static ImmutableQuoteRequest.Builder builder() {
    return ImmutableQuoteRequest.builder();
  }

  PaymentPointer paymentPointer();

  UnsignedLong amountToSend();

}
