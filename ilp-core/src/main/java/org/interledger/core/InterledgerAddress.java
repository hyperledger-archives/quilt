package org.interledger.core;

import org.interledger.annotations.Immutable;

import org.immutables.value.Value;

import java.util.Objects;
import java.util.Optional;

/**
 * Interledger Protocol (ILP) Addresses identify Ledger accounts (or groups of Ledger accounts) in
 * an ILP network, and provide a way to route a payment to its intended destination.
 *
 * <p>Interledger Addresses can be subdivided into two categories:</p>
 *
 * <p> <b>Destination Addresses</b> are complete addresses that can receive payments. A destination
 * address always maps to one account in a ledger, though it can also provide more specific
 * information, such as an invoice ID or a sub-account. Destination addresses MUST NOT end in a
 * period (.) character. </p>
 *
 * <p> <b>Address Prefixes</b> are incomplete addresses representing a grouping of destination
 * addresses. Many depths of grouping are possible, for example: groups of accounts or sub-accounts;
 * an individual ledger or sub-ledger; or entire neighborhoods of ledgers. Address prefixes MUST end
 * in a period (.) character. </p>
 *
 * <p> The formal specification for an Interledger Addresses is defined in <a
 * href="https://github.com/interledger/rfcs/tree/master/0015-ilp-addresses">Interledger RFC
 * #15</a>. </p>
 *
 * @see "https://github.com/interledger/rfcs/tree/master/0015-ilp-addresses"
 */
public interface InterledgerAddress {

  InterledgerAddressParser ADDRESS_PARSER = new InterledgerAddressParser();

  /**
   * Constructor to allow quick construction from a String representation of an ILP address.
   *
   * @param value String representation of an Interledger Address
   *
   * @return an {@link InterledgerAddress} instance.
   */
  static InterledgerAddress of(final String value) {
    return new InterledgerAddressBuilder().value(value).build();
  }

  /**
   * Helper method to determine if an Interledger Address conforms to the specifications outlined in
   * Interledger RFC #15.
   *
   * @param value A {@link String} representing a potential Interledger Address value.
   *
   * @return {@code true} if the supplied {@code value} conforms to the requirements of RFC 15;
   *     {@code false} otherwise.
   *
   * @see "https://github.com/interledger/rfcs/tree/master/0015-ilp-addresses"
   */
  static boolean isValid(final String value) {
    Objects.requireNonNull(value);
    ADDRESS_PARSER.validate(value);
    return true;
  }

  /**
   * Checks and requires that the specified {@code addressPrefix} is an address prefix per {@link
   * InterledgerAddress#isLedgerPrefix()}.
   *
   * <p>This method is designed primarily for doing parameter validation in methods and
   * constructors, as demonstrated below:</p> <blockquote>
   * <pre>
   * public Foo(InterledgerAddress bar) {
   *     this.ledgerPrefix = InterledgerAddress.requireAddressPrefix(bar);
   * }
   * </pre>
   * </blockquote>
   *
   * @param addressPrefix A {@link InterledgerAddress} to check.
   *
   * @return {@code ledgerPrefix} if its value ends with a dot (.).
   *
   * @throws IllegalArgumentException if the supplied Interledger address is not a ledger-prefix.
   */
  static InterledgerAddress requireAddressPrefix(final InterledgerAddress addressPrefix) {
    Objects.requireNonNull(addressPrefix, "addressPrefix must not be null!");
    if (!addressPrefix.isLedgerPrefix()) {
      throw new IllegalArgumentException(
          String.format("InterledgerAddress '%s' must be an Address Prefix ending with a dot (.)",
              addressPrefix.getValue()
          )
      );
    } else {
      return addressPrefix;
    }
  }

  /**
   * Checks and requires that the specified {@code addressPrefix} is not an address prefix per
   * {@link InterledgerAddress#isLedgerPrefix()}.
   *
   *
   * <p>This method is designed primarily for doing parameter validation in methods and
   * constructors, as demonstrated below:</p> <blockquote>
   * <pre>
   * public Foo(InterledgerAddress bar) {
   *     this.nonLedgerPrefix = InterledgerAddress.requireNotAddressPrefix(bar);
   * }
   * </pre>
   * </blockquote>
   *
   * @param addressPrefix A {@link InterledgerAddress} to check.
   *
   * @return {@code addressPrefix} if its value ends with a dot (.).
   *
   * @throws IllegalArgumentException if the supplied Interledger address is not a ledger-prefix.
   */
  static InterledgerAddress requireNotAddressPrefix(final InterledgerAddress addressPrefix) {
    Objects.requireNonNull(addressPrefix, "addressPrefix must not be null!");
    if (addressPrefix.isLedgerPrefix()) {
      throw new IllegalArgumentException(
          String
              .format("InterledgerAddress '%s' must NOT be an Address Prefix ending with a dot (.)",
                  addressPrefix.getValue()
              )
      );
    } else {
      return addressPrefix;
    }
  }

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerAddressBuilder} instance.
   */
  static InterledgerAddressBuilder builder() {
    return new InterledgerAddressBuilder();
  }

  /**
   * Return this address's value as a non-null {@link String}. For example:
   * <code>us.usd.bank.account</code>
   *
   * @return A {@link String} representation of this Interledger address.
   */
  String getValue();

  /**
   * Tests if this Interledger address represents a ledger prefix.
   *
   * @return True if the address is a ledger prefix, false otherwise.
   */
  default boolean isLedgerPrefix() {
    return getValue().endsWith(".");
  }

  /**
   * Tests if this InterledgerAddress starts with the specified {@code addressSegment}.
   *
   * @param addressSegment An {@link String} prefix to compare against.
   *
   * @return {@code true} if this InterledgerAddress begins with the specified prefix.
   */
  default boolean startsWith(final String addressSegment) {
    Objects.requireNonNull(addressSegment, "addressSegment must not be null!");
    return this.getValue()
        .startsWith(addressSegment);
  }

  /**
   * Tests if this InterledgerAddress starts with the specified {@code interledgerAddress}.
   *
   * @param interledgerAddress An {@link InterledgerAddress} prefix to compare against.
   *
   * @return {@code true} if this InterledgerAddress begins with the specified prefix.
   */
  default boolean startsWith(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null!");
    return this.startsWith(interledgerAddress.getValue());
  }

  /**
   * <p>Return a new InterledgerAddress by postfixing the supplied {@code segment} to this address.
   * </p>
   *
   * <p>This method can be used to construct both address prefixes and destination addresses. For
   * example, if the value of this address is '<code>us.usd.</code>', then calling this method with
   * an argument of '<code>bob</code>' would result in a new Interledger Address with a value of
   * '<code>us.usd.bob</code>', which is a destination address.</p>
   *
   * <p>Likewise, if the value of this address is '<code>us.usd.pacific.</code>', then calling this
   * method with an argument of '<code>creditunions.</code>' would result in a new Interledger
   * Address with a value of '<code>us.usd.pacific.creditunions.</code>', which is an address
   * prefix.</p>
   *
   * @param addressSegment A {@link String} to be appended to this address as an additional
   *                       segment.
   *
   * @return A new instance representing the original address with a newly specified final segment.
   */
  default InterledgerAddress with(String addressSegment) {
    Objects.requireNonNull(addressSegment, "addressSegment must not be null!");

    final StringBuilder sb = new StringBuilder(this.getValue());
    if (!this.isLedgerPrefix()) {
      sb.append(".");
    }
    sb.append(addressSegment);

    return InterledgerAddress.of(sb.toString());
  }

  /**
   * <p>Return this address's prefix, which is a new {@link InterledgerAddress} containing the
   * characters inside of {@link #getValue()}, up-to and including the last period. If this address
   * is already a prefix, then this instance is instead returned unchanged.</p>
   *
   * <p>For example, calling this method on an address 'g.example.alice' would yield a new address
   * containing 'g.example.'. Conversely, calling this method on an address that is already a
   * prefix, like 'g.example.' would yield the same instance, 'g.example.'.</p>
   *
   * @return A potentially new {@link InterledgerAddress} representing the prefix of this address.
   */
  default InterledgerAddress getPrefix() {
    if (this.isLedgerPrefix()) {
      return this;
    } else {
      return InterledgerAddress.of(getValue().substring(0, this.getValue().lastIndexOf(".") + 1));
    }
  }

  /**
   * <p>Return this address's parent prefix.</p>
   *
   * <p>If this address is a destination address, then this method returns a new {@link
   * InterledgerAddress} containing the characters inside of {@link #getValue()}, up-to and
   * including last period. If this address is instead a prefix, then this instance returns a new
   * {@link InterledgerAddress} containing the characters inside of {@link #getValue()}, up-to and
   * including second-to-last period, unless this address is a root prefix, in which case, {@link
   * Optional#empty()} is returned.</p>
   *
   * <p>For example, calling this method on an address 'g.example.alice' would yield a new address
   * containing 'g.example.'. Likewise, calling this method on an address that is already a prefix,
   * like 'g.example.' would yield 'g.'. Finally, calling this method on a root prefix, like "self."
   * would yield {@link Optional#empty()}.</p>
   *
   * @return An optionally present parent-prefix.
   */
  default Optional<InterledgerAddress> getParentPrefix() {
    // If this address is not a prefix, then just return the prefix. Otherwise, look deeper.
    if (this.isLedgerPrefix()) {
      // If the prefix is a root prefix, return Optional#empty. Otherwise, return the parent prefix.
      if (isRootPrefix()) {
        return Optional.empty();
      } else {
        // Call getPrefix with the account portion's parent.
        // This means jumping over any destination address (i.e. account portion) in order to
        // traverse address prefixes without hitting destination address restrictions
        final String parentDestAddr = this.getValue().substring(0,
            this.getValue().lastIndexOf("."));
        return Optional.of(InterledgerAddress.of(parentDestAddr.substring(0,
            parentDestAddr.lastIndexOf(".") + 1)
        ).getPrefix());
      }
    } else {
      return Optional.of(this.getPrefix());
    }
  }

  /**
   * <p>Determines if this ILP Address has a parent-prefix.</p>
   *
   * <p>If this address is a destination address, then it has a parent prefix. However, if the
   * address is a prefix, then it only has a parent if it is _not_ a Root Prefix.</p>
   *
   * @return {@code true} if this address is a destination address. Otherwise (if this address is a
   *     prefix address), then return {@code false} if this address is a Root prefix; otherwise,
   *     return {@code true}.
   */
  default boolean hasParentPrefix() {
    // All ILP addresse have a parent prefix, except for Root prefixes.
    return isRootPrefix() == false;
  }

  /**
   * <p>Determines if this address is a "root" prefix, which per ILP-RFC-15, is one of: "g.",
   * "private.", "example.", "peer.", "self.", "test1.", "test2.", or "test3.". Any other kind of
   * valid ILP address (e.g. "g.1") is not a root prefix.</p>
   *
   * @return {@code true} if this address is a root prefix; {@code false} otherwise.
   */
  default boolean isRootPrefix() {
    // Alternate implementation (Note: A prefix is a root prefix if it has only a single period)
    // final int numPeriods = getValue().length() - getValue().replaceAll("[.!?]+", "").length();
    // return numPeriods == 1;
    return ADDRESS_PARSER.isSchemePrefix(getValue());
  }

  @Immutable
  abstract class AbstractInterledgerAddress implements InterledgerAddress {

    /**
     * Precondition enforcer that ensures the value is a valid Interledger Address.
     *
     * @see "https://github.com/interledger/rfcs/blob/master/0015-ilp-addresses/0015-ilp-addresses.md"
     */
    @Value.Check
    void check() {
      try {
        InterledgerAddress.isValid(getValue());
      } catch (final IllegalArgumentException e) {
        throw new IllegalArgumentException("Address is invalid", e);
      }
    }
  }

}
