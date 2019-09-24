package org.interledger.spsp;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentPointerResolverTest {

  private final PaymentPointerResolver resolver = PaymentPointerResolver.defaultResolver();

  @Test
  public void resolve() {
    assertThat(resolver.resolve(PaymentPointer.of("$example.com/foo"))).isEqualTo("https://example.com/foo");
  }

  @Test
  public void toUrlWithout() {
    assertThat(resolver.resolve(PaymentPointer.of("$example.com"))).isEqualTo("https://example.com/.well-known/pay");
  }

}