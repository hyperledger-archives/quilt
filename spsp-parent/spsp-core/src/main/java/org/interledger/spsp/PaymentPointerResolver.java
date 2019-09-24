package org.interledger.spsp;

public interface PaymentPointerResolver {

  static PaymentPointerResolver defaultResolver() {
    return new DefaultPaymentPointerResolver();
  }

  /**
   * Resolves PaymentPointer to it's endpoint URL per
   * https://interledger.org/rfcs/0026-payment-pointers/#payment-setup-protocol-receiver-endpoint-resolution
   * @param paymentPointer ilp payment pointer
   * @return endpoint url
   */
  String resolve(PaymentPointer paymentPointer);

  class DefaultPaymentPointerResolver implements PaymentPointerResolver {
    @Override
    public String resolve(PaymentPointer paymentPointer) {
      return "https://" + paymentPointer.host() + paymentPointer.path();
    }
  }

}
