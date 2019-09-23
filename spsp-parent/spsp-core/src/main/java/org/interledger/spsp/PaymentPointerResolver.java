package org.interledger.spsp;

public interface PaymentPointerResolver {

  String resolve(PaymentPointer paymentPointer);

  class DefaultPaymentPointerResolver implements PaymentPointerResolver {
    @Override
    public String resolve(PaymentPointer paymentPointer) {
      return "https://" + paymentPointer.host() + "/spsp" + paymentPointer.path();
    }
  }

}
