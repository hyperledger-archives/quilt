package org.interledger.stream.pay.model;

import org.interledger.spsp.PaymentPointer;
import org.interledger.stream.pay.model.ImmutableQuoteRequest.Builder;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Immutable;

@Immutable
public interface QuoteRequest {

  static Builder builder() {
    return ImmutableQuoteRequest.builder();
  }

  PaymentPointer paymentPointer();

  UnsignedLong amountToSend();

}
