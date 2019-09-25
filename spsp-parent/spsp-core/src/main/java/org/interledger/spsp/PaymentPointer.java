package org.interledger.spsp;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

/**
 * A standardized identifier for payment accounts.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0026-payment-pointers/0026-payment-pointers.md"
 */
public interface PaymentPointer {

  /**
   * Parses a payment pointer string into a @{code PaymentPointer}.
   * @param value text
   * @return payment pointer
   * @throws IllegalArgumentException if cannot be parsed
   * @throws IllegalStateException if parsed payment pointer has invalid host and/or path values
   */
  static PaymentPointer of(String value) {
    if (!value.startsWith("$")) {
      throw new IllegalArgumentException("PaymentPointers must begin with $");
    }
    String address = value.substring(1);
    int pathIndex = address.indexOf("/");
    if (pathIndex >= 0) {
      return ImmutablePaymentPointer.builder()
        .host(address.substring(0, pathIndex))
        .path(address.substring(pathIndex))
        .build();
    } else {
      return ImmutablePaymentPointer.builder()
        .host(address)
        .build();
    }
  }

  /**
   * A host as defined by RFC-3986, which is generally (though not always) a registered name intended for lookup in
   * the DNS.
   *
   * @return A {@link String} containing the `host` portion of this Payment Pointer.
   *
   * @see "https://tools.ietf.org/html/rfc3986#page-18"
   */
  String host();

  /**
   * An optional path as defined by RFC-3986.
   *
   * @return A {@link String} containing the `path` portion of this Payment Pointer.
   *
   * @see "https://tools.ietf.org/html/rfc3986#page-22"
   */
  String path();

  @Immutable
  abstract class AbstractPaymentPointer implements PaymentPointer {

    private static final String WELL_KNOWN = "/.well-known/pay";

    @Default
    public String path() {
      return WELL_KNOWN;
    }

    @Override
    public String toString() {
      return "$" + host() + path();
    }

    @Check
    AbstractPaymentPointer validate() {
      Preconditions.checkState(!host().isEmpty(), "PaymentPointers must specify a host");
      if (Strings.isNullOrEmpty(path())) {
        return ImmutablePaymentPointer.builder().from(this)
          .path(WELL_KNOWN)
          .build();
      }
      // Normalize acceptable input.
      if (path().equals("/")) {
        return ImmutablePaymentPointer.builder().from(this)
          .path(WELL_KNOWN)
          .build();
      }
      Preconditions.checkState(path().startsWith("/"), "path must start with a forward-slash");
      Preconditions.checkState(
          CharMatcher.ascii().matchesAllOf(toString()), "PaymentPointers may only contain ASCII characters");

      return this;
    }
  }

}
