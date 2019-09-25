package org.interledger.spsp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentPointerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void parseHostAndPath() {
    assertThat(PaymentPointer.of("$example.com/foo"))
      .isEqualTo(ImmutablePaymentPointer.builder().host("example.com").path("/foo").build());
  }

  @Test
  public void wellKnownPath() {
    assertThat(PaymentPointer.of("$example.com").path())
      .isEqualTo("/.well-known/pay");
  }

  @Test
  public void wellKnownPathWithTrailingSlash() {
    assertThat(PaymentPointer.of("$example.com/").path())
      .isEqualTo("/.well-known/pay");
  }

  @Test
  public void builderWithWellKnownPathWithTrailingSlash() {
    assertThat(ImmutablePaymentPointer.builder().host("example.com").path("/").build().path())
      .isEqualTo("/.well-known/pay");
  }

  @Test
  public void builderWithWellKnownPathWithoutTrailingSlash() {
    assertThat(ImmutablePaymentPointer.builder().host("example.com").path("").build().path())
      .isEqualTo("/.well-known/pay");
  }

  @Test
  public void accountPath() {
    assertThat(PaymentPointer.of("$example.com/foo").path())
      .isEqualTo("/foo");
  }

  @Test
  public void parseHostOnly() {
    assertThat(PaymentPointer.of("$example.com"))
      .isEqualTo(ImmutablePaymentPointer.builder().host("example.com").path("/").build());
    assertThat(PaymentPointer.of("$bob.example.com"))
      .isEqualTo(ImmutablePaymentPointer.builder().host("bob.example.com").path("/").build());
    assertThat(PaymentPointer.of("$localhost"))
      .isEqualTo(ImmutablePaymentPointer.builder().host("localhost").path("/").build());
  }

  @Test
  public void exceptionIfDoesntStartWithDollarSign() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("PaymentPointers must begin with $");
    PaymentPointer.of("foo");
  }

  @Test
  public void exceptionIfHostNameEmpty() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("PaymentPointers must specify a host");
    PaymentPointer.of("$");
  }

  @Test
  public void zalgoIsTonyThePonyCannotBePaid() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("PaymentPointers may only contain ASCII characters");
    PaymentPointer.of("$ZA̡͊͠͝LGΌ IS̯͈͕̹̘̱ͮ TO͇̹̺ͅƝ̴ȳ̳ TH̘Ë͖́̉ ͠P̯͍̭O̚N̐Y̡");
  }

}