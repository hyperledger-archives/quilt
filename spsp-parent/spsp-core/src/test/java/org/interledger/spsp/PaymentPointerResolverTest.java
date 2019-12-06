package org.interledger.spsp;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.HttpUrl;
import org.junit.Test;

/**
 * Unit tests for {@link PaymentPointerResolver}.
 */
public class PaymentPointerResolverTest {

  private final PaymentPointerResolver resolver = PaymentPointerResolver.defaultResolver();

  @Test
  public void resolve() {
    assertThat(resolver.resolve(PaymentPointer.of("$example.com/foo"))).isEqualTo("https://example.com/foo");
  }

  @Test
  public void resolveWithLongPath() {
    assertThat(resolver.resolve(PaymentPointer.of("$example.com/foo/bar/baz")))
      .isEqualTo("https://example.com/foo/bar/baz");
  }

  @Test
  public void resolveWithSubdomain() {
    assertThat(resolver.resolve(PaymentPointer.of("$david.example.com")))
      .isEqualTo("https://david.example.com/.well-known/pay");
  }

  @Test
  public void resolveWithSubdomainAndPath() {
    assertThat(resolver.resolve(PaymentPointer.of("$david.example.com/foo")))
      .isEqualTo("https://david.example.com/foo");
  }

  @Test
  public void toUrlWithout() {
    assertThat(resolver.resolve(PaymentPointer.of("$example.com"))).isEqualTo("https://example.com/.well-known/pay");
  }

  @Test
  public void resolveHttpUrl() {
    assertThat(resolver.resolveHttpUrl(PaymentPointer.of("$example.com/foo")))
      .isEqualTo(HttpUrl.parse("https://example.com/foo"));
  }

  @Test
  public void resolveHttpUrlWithLongPath() {
    assertThat(resolver.resolveHttpUrl(PaymentPointer.of("$example.com/foo/bar/baz")))
      .isEqualTo(HttpUrl.parse("https://example.com/foo/bar/baz"));
  }

  @Test
  public void resolveHttpUrlWithSubdomain() {
    assertThat(resolver.resolveHttpUrl(PaymentPointer.of("$david.example.com")))
      .isEqualTo(HttpUrl.parse("https://david.example.com/.well-known/pay"));
  }

  @Test
  public void resolveHttpUrlWithSubdomainAndPath() {
    assertThat(resolver.resolveHttpUrl(PaymentPointer.of("$david.example.com/foo")))
      .isEqualTo(HttpUrl.parse("https://david.example.com/foo"));
  }

  @Test
  public void resolveHttpUrltoUrlWithoutPath() {
    assertThat(resolver.resolveHttpUrl(PaymentPointer.of("$example.com")))
      .isEqualTo(HttpUrl.parse("https://example.com/.well-known/pay"));
  }

}
