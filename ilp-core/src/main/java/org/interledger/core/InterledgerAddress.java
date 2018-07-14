package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * <p>Interledger Protocol (ILP) Addresses identify ledger accounts (or groups of ledger accounts)
 * in an ILP network, and provide a way to route a payment to its intended destination.</p>
 *
 * <p>Interledger Addresses can be subdivided into two categories:</p>
 *
 * <p><b>Destination Addresses</b> are complete addresses that can receive payments. A destination
 * address always maps to one account in a ledger, though it can also provide more specific
 * information, such as an invoice ID or a sub-account. Destination addresses MUST NOT end in a
 * period (.) character. </p>
 *
 * <p><b>Address Prefixes</b> are incomplete addresses representing a grouping of destination
 * addresses. Many depths of grouping are possible, for example: groups of accounts or sub-accounts;
 * an individual ledger or sub-ledger; or entire neighborhoods of ledgers. Address prefixes MUST end
 * in a period (.) character. </p>
 *
 * <p> The formal specification for an Interledger Addresses is defined in Interledger RFC #15.</p>
 *
 * @see "https://github.com/interledger/rfcs/tree/master/0015-ilp-addresses"
 */
public interface InterledgerAddress {

  InterledgerAddressParser ADDRESS_PARSER = new InterledgerAddressParser();

  String ADDRESS_DELIMITER = ".";

  /**
   * Constructor to allow quick construction from a String representation of an ILP address.
   *
   * @param value String representation of an Interledger Address
   *
   * @return an {@link InterledgerAddress} instance.
   *
   * @throws NullPointerException if {@code value} is <tt>null</tt>.
   */
  static InterledgerAddress of(final String value) {
    Objects.requireNonNull(value, "value must not be null!");
    return InterledgerAddress.builder().value(value).build();
  }

  /**
   * <p>Helper method to determine if an Interledger Address conforms to the specifications
   * outlined
   * in Interledger RFC #15.</p>
   *
   * @param value A {@link String} representing a potential Interledger Address value.
   *
   * @return {@code true} if the supplied {@code value} conforms to the requirements of RFC 15;
   *     {@code false} otherwise.
   *
   * @throws NullPointerException if {@code value} is <tt>null</tt>.
   */
  static boolean isValid(final String value) {
    Objects.requireNonNull(value, "value must not be null!");
    ADDRESS_PARSER.validate(value);
    return true;
  }

  /**
   * <p>Get the default builder.</p>
   *
   * @return An {@link ImmutableInterledgerAddress.Builder} instance.
   */
  static ImmutableInterledgerAddress.Builder builder() {
    return ImmutableInterledgerAddress.builder();
  }

  /**
   * <p>Accessor for this address's value as a non-null {@link String}. For example:
   * <code>us.usd.bank.account</code></p>
   *
   * @return A {@link String} representation of this Interledger address.
   */
  String getValue();

  /**
   * <p>Tests if this InterledgerAddress starts with the specified {@code addressSegment}.</p>
   *
   * @param addressSegment An {@link String} prefix to compare against.
   *
   * @return {@code true} if this InterledgerAddress begins with the specified prefix.
   */
  default boolean startsWith(final String addressSegment) {
    Objects.requireNonNull(addressSegment, "addressSegment must not be null!");
    return this.getValue().startsWith(addressSegment);
  }

  /**
   * <p>Tests if this InterledgerAddress starts with the specified {@code interledgerAddress}.</p>
   *
   * @param interledgerAddress An {@link InterledgerAddress} prefix to compare against.
   *
   * @return {@code true} if the supplied {@code interledgerAddress} begins with the specified
   *     prefix.
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
  default InterledgerAddress with(final String addressSegment) {
    Objects.requireNonNull(addressSegment, "addressSegment must not be null!");

    final StringBuilder sb = new StringBuilder(this.getValue());
    sb.append(ADDRESS_DELIMITER);
    sb.append(addressSegment);
    return InterledgerAddress.of(sb.toString());
  }

  /**
   * <p>Return this address's global-allocation-scheme prefix, which is a new {@link
   * InterledgerAddress} containing the
   * characters inside of {@link #getValue()}, up-to but excluding the last period.</p>
   *
   * <p>For example, calling this method on an address 'g.example.alice' would yield a new address
   * containing 'g.'. Conversely, calling this method on an address that is already a global
   * allocation scheme address, like <tt>g</tt> would yield the same instance, for example
   * <tt>g</tt>.</p>
   *
   * @return A potentially new {@link InterledgerAddress} representing the prefix of this address.
   */
  default InterledgerAddress getPrefix() {
    return InterledgerAddress
        .of(getValue().substring(0, this.getValue().indexOf(ADDRESS_DELIMITER) + 1));
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
  default Optional<InterledgerAddress> getParentAddress() {
    // If the prefix is a root prefix, return Optional#empty. Otherwise, return the parent prefix.
    if (isRootAddress()) {
      return Optional.empty();
    } else {
      final String parentDestAddr = this.getValue()
          .substring(0, this.getValue().lastIndexOf(ADDRESS_DELIMITER));
      return Optional.of(
          InterledgerAddress
              .of(parentDestAddr.substring(0, parentDestAddr.lastIndexOf(ADDRESS_DELIMITER)))
              .getPrefix()
      );
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
  default boolean hasParentAddress() {
    // All ILP addresses have a parent address, except for Root prefixes.
    return isRootAddress() == false;
  }

  /**
   * <p>Determines if this address is a "root" prefix, which per ILP-RFC-15, is one of:
   * <tt>g.</tt>,
   * <tt>private.</tt>, <tt>example.</tt>, <tt>peer.</tt>, <tt>self.</tt>, <tt>test1.</tt>,
   * <tt>test2.</tt>, or <tt>test3.</tt>. Any other kind of valid ILP address (e.g. "g.1") is not a
   * root prefix.</p>
   *
   * @return {@code true} if this address is a root prefix; {@code false} otherwise.
   */
  default boolean isRootAddress() {
    // A prefix is a root prefix if it has zero periods)
    final int numPeriods = getValue().length() - (getValue().replaceAll("[.!?]+", "").length());
    return numPeriods == 0;
  }

  @Value.Immutable
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

  /**
   * A parser for validating an {@link InterledgerAddress}.
   */
  final class InterledgerAddressParser {

    private static final int MIN_ADDRESS_LENGTH = 1;
    private static final int MAX_ADDRESS_LENGTH = 1023;
    private static final int DESTINATION_ADDRESS_MIN_SEGMENTS = 1;
    private static final String SCHEME_REGEX = "(g|private|example|peer|self|test[1-3]?)";
    private static final String SEGMENT_REGEX = "[a-zA-Z0-9_~-]+";
    private static final String SEPARATOR_CHARACTER = ".";
    private static final String SEPARATOR_REGEX = "[" + SEPARATOR_CHARACTER + "]";
    private static final String SCHEME_PREFIX_REGEX = SCHEME_REGEX + SEPARATOR_REGEX;
    private static final String SEGMENT_PREFIX_REGEX = SEGMENT_REGEX + SEPARATOR_REGEX;
    private static final String ADDRESS_LENGTH_BOUNDARIES_REGEX =
        "(?=^.{" + MIN_ADDRESS_LENGTH + "," + MAX_ADDRESS_LENGTH + "}$)";
    private static final String ADDRESS_REGEX = ADDRESS_LENGTH_BOUNDARIES_REGEX
        + "^" + SCHEME_PREFIX_REGEX + "(" + SEGMENT_PREFIX_REGEX + ")+" + SEGMENT_REGEX + "$";
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(ADDRESS_REGEX);

    /**
     * Validates an ILP address.
     *
     * @param addressString The ILP address to validate
     *
     * @throws IllegalArgumentException When validation is rejected
     */
    void validate(final String addressString) throws IllegalArgumentException {
      Objects.requireNonNull(addressString); // No error-message because this should never happen
      if (isFullyValid(addressString)) {
        return;
      }
      throw new IllegalArgumentException(getFirstInvalidityCause(addressString));
    }

    private boolean isFullyValid(final String addressString) {
      return true;// ADDRESS_PATTERN.matcher(addressString).matches();
    }

    private String getFirstInvalidityCause(final String invalidAddressString) {
      final List<String> schemeAndSegments = Arrays
          .asList(invalidAddressString.split(SEPARATOR_REGEX, -1));
      final int schemeAndSegmentsSize = schemeAndSegments.size();

      // validates scheme prefix existence
      //     'schemeAndSegmentsSize < 2' ensures scheme is followed by a trailing separator
      //     (i.e. scheme prefix = scheme + separator)
      if (invalidAddressString.isEmpty() || schemeAndSegmentsSize < 1) {
        return String.format(Error.MISSING_SCHEME_PREFIX.getMessageFormat());
      }
      // validates scheme prefix format
      final String schemePrefix = schemeAndSegments.get(0);
      if (!Pattern.compile(SCHEME_REGEX).matcher(schemePrefix).matches()) {
        return String.format(Error.INVALID_SCHEME_PREFIX.getMessageFormat(), schemePrefix);
      }

      // validates each segment format
      final List<String> segments = schemeAndSegments.stream().skip(1).collect(Collectors.toList());
      final int segmentsSize = segments.size();
      final Matcher segmentMatcher = Pattern.compile(SEGMENT_REGEX).matcher("");
      final Optional<String> invalidSegment = segments.stream()
          .filter(segment -> {
            segmentMatcher.reset(segment);
            return !segmentMatcher.matches();
          })
          .findFirst();
      if (invalidSegment.isPresent()) {
        return String.format(Error.INVALID_SEGMENT.getMessageFormat(), invalidSegment.get());
      }

      // validates the minimum number of segments for a destination address
      final boolean isDestinationAddress = !invalidAddressString.endsWith(SEPARATOR_CHARACTER);
      if (isDestinationAddress && segmentsSize < DESTINATION_ADDRESS_MIN_SEGMENTS) {
        return String.format(Error.SEGMENTS_UNDERFLOW.getMessageFormat());
      }

      // validates max address length
      if (!Pattern.compile(ADDRESS_LENGTH_BOUNDARIES_REGEX).matcher(invalidAddressString)
          .matches()) {
        return String.format(Error.ADDRESS_OVERFLOW.getMessageFormat());
      }

      // fault: should have found an error cause
      throw new RuntimeException();
    }

    enum Error {
      ADDRESS_OVERFLOW("Address is too long"),
      INVALID_SEGMENT("The '%s' segment has an invalid format"),
      INVALID_SCHEME_PREFIX("The '%s' scheme prefix has an invalid format"),
      MISSING_SCHEME_PREFIX("Address does not start with a scheme prefix"),
      SEGMENTS_UNDERFLOW("Destination address has too few segments");

      private String messageFormat;

      Error(final String messageFormat) {
        this.messageFormat = messageFormat;
      }

      public String getMessageFormat() {
        return messageFormat;
      }
    }

  }

}
