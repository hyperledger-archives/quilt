package org.interledger.spsp;

import okhttp3.HttpUrl;

import java.util.Objects;

/**
 * Defines how to resolve a payment pointer.
 */
public interface PaymentPointerResolver {

  static PaymentPointerResolver defaultResolver() {
    return new DefaultPaymentPointerResolver();
  }

  /**
   * Resolves PaymentPointer to it's endpoint URL.
   *
   * @param paymentPointer ilp payment pointer
   *
   * @return endpoint url
   *
   * @see "https://interledger.org/rfcs/0026-payment-pointers/#payment-setup-protocol-receiver-endpoint-resolution"
   * @deprecated Prefer the variant of {@link #resolve} that returns an {@link HttpUrl};
   */
  @Deprecated
  default String resolve(final PaymentPointer paymentPointer) {
    Objects.requireNonNull(paymentPointer);
    return resolveHttpUrl(paymentPointer).toString();
  }

  /**
   * Resolves PaymentPointer to it's endpoint URL.
   *
   * @param paymentPointer ilp payment pointer
   *
   * @return endpoint url
   *
   * @see "https://interledger.org/rfcs/0026-payment-pointers/#payment-setup-protocol-receiver-endpoint-resolution"
   */
  HttpUrl resolveHttpUrl(PaymentPointer paymentPointer);

  /**
   * The default implementation of {@link PaymentPointerResolver}.
   */
  class DefaultPaymentPointerResolver implements PaymentPointerResolver {

    @Override
    public HttpUrl resolveHttpUrl(final PaymentPointer paymentPointer) {
      Objects.requireNonNull(paymentPointer);

      // Prefer parsing here over manual assembly so that any existing forward-slashes are interpreted properly.
      return HttpUrl.parse("https://" + paymentPointer.host() + paymentPointer.path());
    }
  }

}
