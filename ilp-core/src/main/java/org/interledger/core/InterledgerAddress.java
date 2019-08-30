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
import org.immutables.value.Value.Lazy;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Interledger Protocol (ILP) Addresses serve as both an <tt>identifier</tt> and a <tt>locator</tt> for ILP nodes
 * (e.g., connectors, mini-connectors, clients, senders, receivers, listeners, etc.)</p>
 *
 * <p>Interledger is a graph where <tt>Nodes</tt> are the vertices and <tt>Accounts</tt> are the edges. A fulfilled ILP
 * packet will modify the balances for all accounts along the path between a sending Node and a receiving Node. This is
 * no different for a connector than for an SPSP receiver (both are ILP nodes and in both cases, the accounts whose
 * balances change are all accounts along the path).</p>
 *
 * <p>The identifier+locator primitive defined by an InterledgerAddress also provides a way to route ILP packets to
 * their intended destination through a series of Nodes, including any number of ILP Connectors (this happens after
 * address-lookup using a higher-level protocol such as
 * <tt>SPSP</tt>).</p>
 *
 * <p>Addresses are <tt>not</tt> meant to be user-facing, but allow several ASCII
 * characters for easy debugging.</p>
 *
 * <p>Note that because an InterledgerAddress represents an Interledger <tt>Node</tt>, ILP payments are always
 * addressed to a Node, and _not_ to an account. For example, there will usually be a 1:1 relationship between a
 * receiver and the receiver's account (e.g., if a node is running a local moneyd). However, even in these cases, it is
 * important to note that a payment is still addressed to the receiver Node, and not to the account. This is because
 * balances are not always tracked/recorded on every node. In fact, some accounts are limited only by bandwidth, while
 * others are not limited nor recorded at all.</p>
 *
 * <p>Interledger Addresses have the following requirements:</p>
 *
 * <ol>
 * <li>The address MUST begin with a prefix indicating the allocation scheme.</li>
 * <li>Each "segment" of the address MUST contain one or more of the following characters:
 * Alphanumeric characters, upper or lower case (Addresses are **case-sensitive** so that they can contain data encoded
 * in formats such as base64url.); Underscore (`_`); Tilde (`~`); Hyphen (`-`)
 * </li>
 * <li>Each segment MUST be separated from other segments by a period character (`.`).</li>
 * <li>Addresses MUST NOT end in a period (`.`) character, and MUST contain at least one segment
 * after the allocation scheme prefix.</li>
 * <li>The total length of an ILP Address must be no more than **1023 characters** including the
 * allocation scheme prefix, separators, and all segments.</li>
 * </ol>
 *
 * <p>The formal specification for an Interledger Addresses is defined in Interledger RFC #15.</p>
 *
 * @see "https://github.com/interledger/rfcs/tree/master/0015-ilp-addresses"
 * @see "https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009
 *     -simple-payment-setup-protocol.md"
 */
public interface InterledgerAddress {

  /**
   * Constructor to allow quick construction from a {@link String} representation of an ILP address.
   *
   * @param value String representation of an Interledger Address
   *
   * @return an {@link InterledgerAddress} instance.
   *
   * @throws NullPointerException if {@code value} is <tt>null</tt>.
   */
  static InterledgerAddress of(final String value) {
    Objects.requireNonNull(value, "value must not be null!");
    return builder().value(value).build();
  }

  /**
   * <p>Construct a default builder.</p>
   *
   * @return An {@link InterledgerAddressBuilder} instance.
   */
  static InterledgerAddressBuilder builder() {
    return new InterledgerAddressBuilder();
  }

  /**
   * <p>Accessor for this address's value as a non-null {@link String}. For example:
   * <code>us.usd.bank.account</code></p>
   *
   * @return A {@link String} representation of this Interledger address.
   */
  String getValue();

  /**
   * <p>Return this address's allocation scheme.</p>
   *
   * @return A {@link AllocationScheme} representing the prefix of this address.
   */
  AllocationScheme getAllocationScheme();

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
   * @return {@code true} if the supplied {@code interledgerAddress} begins with the specified prefix.
   */
  default boolean startsWith(final InterledgerAddress interledgerAddress) {
    Objects.requireNonNull(interledgerAddress, "interledgerAddress must not be null!");
    return this.getValue().startsWith(interledgerAddress.getValue());
  }

  /**
   * <p>Tests if this InterledgerAddress starts with the specified {@code addressPrefix}.</p>
   *
   * @param addressPrefix An {@link InterledgerAddressPrefix} to compare against.
   *
   * @return {@code true} if this InterledgerAddress begins with the specified prefix else {@code false}.
   */
  default boolean startsWith(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix, "addressPrefix must not be null!");
    return this.startsWith(addressPrefix.getValue());
  }

  /**
   * <p>Return a new {@link InterledgerAddress} by suffixing the supplied {@code addressSegment}
   * onto the current address.</p>
   *
   * @param addressSegment A {@link String} to be appended to this address as an additional segment.
   *
   * @return A new instance representing the original address with a newly specified final segment.
   */
  default InterledgerAddress with(final String addressSegment) {
    Objects.requireNonNull(addressSegment, "addressSegment must not be null!");

    // `+` operator uses StringBuilder internally, so for small numbers of appends, `+` is
    // equivalent in performance, but provides better code readability.
    return InterledgerAddress.of(this.getValue() + "." + addressSegment);
  }

  /**
   * <p>Return this address's prefix.</p>
   *
   * <p>If this address has only a single segment after the allocation scheme, then this method
   * returns {@link Optional#empty()}. Otherwise, this method returns a new {@link InterledgerAddress} containing the
   * characters inside of {@link #getValue()}, up-to but excluding last period.</p>
   *
   * <p>For example, calling this method on an address <tt>g.example.alice</tt> would yield a new
   * address prefix containing <tt>g.example</tt>. Likewise, calling this method on an address like
   * <tt>g.example</tt> would yield <tt>g</tt>.</p>
   *
   * @return An optionally present parent-prefix as an {@link InterledgerAddressPrefix}.
   */
  default InterledgerAddressPrefix getPrefix() {
    // An address will always contain at least one period (.), so we can always return its prefix.
    final String value = getValue();
    return InterledgerAddressPrefix.builder()
            .value(value.substring(0, value.lastIndexOf(".")))
            .build();
  }

  /**
   * The first part of an {@link AllocationScheme}, which indicates to which ILP network the address belongs to.
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0015-ilp-addresses"
   */
  interface AllocationScheme {

    AllocationScheme GLOBAL = AllocationScheme.of("g");
    AllocationScheme PRIVATE = AllocationScheme.of("private");
    AllocationScheme EXAMPLE = AllocationScheme.of("example");
    AllocationScheme PEER = AllocationScheme.of("peer");
    AllocationScheme SELF = AllocationScheme.of("self");
    AllocationScheme TEST = AllocationScheme.of("test");
    AllocationScheme TEST1 = AllocationScheme.of("test1");
    AllocationScheme TEST2 = AllocationScheme.of("test2");
    AllocationScheme TEST3 = AllocationScheme.of("test3");

    /**
     * Constructor to allow quick construction from a {@link String} representation of an ILP address allocation
     * scheme.
     *
     * @param value String representation of an Interledger Address allocation scheme.
     *
     * @return an {@link AllocationScheme} instance.
     *
     * @throws NullPointerException if {@code value} is <tt>null</tt>.
     */
    static AllocationScheme of(final String value) {
      Objects.requireNonNull(value, "value must not be null!");
      return builder().value(value).build();
    }

    /**
     * <p>Construct a default builder.</p>
     *
     * @return An {@link AllocationSchemeBuilder} instance.
     */
    static AllocationSchemeBuilder builder() {
      return new AllocationSchemeBuilder();
    }

    /**
     * Accessor for the value of this {@link InterledgerAddress}.
     *
     * @return A {@link String} with the value of this address.
     */
    String getValue();

    /**
     * Create an {@link InterledgerAddress} from this {@link AllocationScheme} by appending the supplied {@code value}.
     *
     * @param value The String parameter to create the Interledger address with.
     * @return A {@link InterledgerAddress} with the value of an address.
     */
    default InterledgerAddress with(final String value) {
      Objects.requireNonNull(value, "value must not be null!");
      return InterledgerAddress.of(this.getValue() + "." + value);
    }

    /**
     * <p>An implementation of {@link AllocationScheme} that enforces allowed
     * value per RFC-15.</p>
     *
     * <p>This immutable is interned because it only holds a {@link String} value, which itself
     * is interned via the Java String pool.</p>
     */
    @Value.Immutable
    @Value.Style(
        typeBuilder = "*Builder",
        visibility = Value.Style.ImplementationVisibility.PRIVATE,
        builderVisibility = Value.Style.BuilderVisibility.PUBLIC,
        defaults = @Value.Immutable(intern = true))
    abstract class AbstractAllocationScheme implements AllocationScheme {

      private static final String SCHEME_REGEX = "(g|private|example|peer|self|test[1-3]?)$";
      private static final Pattern SCHEME_PREFIX_ONLY_PATTERN = Pattern.compile(SCHEME_REGEX);

      /**
       * Precondition enforcer that ensures the value is a valid Interledger Address.
       *
       * @see "https://github.com/interledger/rfcs/blob/master/0015-ilp-addresses/0015-ilp-addresses.md"
       */
      @Value.Check
      void check() {
        if (!SCHEME_PREFIX_ONLY_PATTERN.matcher(getValue()).matches()) {
          throw new IllegalArgumentException(
              String.format(Error.INVALID_SCHEME_PREFIX.getMessageFormat(), getValue())
          );
        }
      }

      /**
       * A potential error that might be emitted if an invalid AllocationScheme is encountered.
       */
      enum Error {
        INVALID_SCHEME_PREFIX("The '%s' AllocationScheme is invalid!");

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

  /**
   * <p>An implementation of {@link InterledgerAddress} that enforces allowed value per RFC-15.</p>
   *
   * <p>This immutable is interned because it only holds a {@link String} value, which itself is
   * interned via the Java String pool.</p>
   */
  @Value.Immutable
  @Value.Style(
      typeBuilder = "*Builder",
      visibility = Value.Style.ImplementationVisibility.PRIVATE,
      builderVisibility = Value.Style.BuilderVisibility.PUBLIC,
      defaults = @Value.Immutable(intern = true))
  abstract class AbstractInterledgerAddress implements InterledgerAddress {

    static final String SEPARATOR_REGEX = "[.]";

    private static final String SCHEME_REGEX = "(g|private|example|peer|self|test[1-3]?)";
    static final Pattern SCHEME_PATTERN = Pattern.compile(SCHEME_REGEX);

    private static final String VALID_ADDRESS_REGEX
        = "(?=^.{1,1023}$)^(g|private|example|peer|self|test[1-3]?|local)([.][a-zA-Z0-9_~-]+)+$";
    private static final Pattern VALID_ADDRESS_PATTERN = Pattern.compile(VALID_ADDRESS_REGEX);

    private static final int ADDRESS_MIN_SEGMENTS = 2;

    private static final String SEGMENT_REGEX = "[a-zA-Z0-9_~-]+";
    static final Pattern SEGMENT_PATTERN = Pattern.compile(SEGMENT_REGEX);

    private static final String ADDRESS_LENGTH_BOUNDARIES_REGEX = "(?=^.{1,1023}$)";
    private static final Pattern ADDRESS_LENGTH_BOUNDARIES_PATTERN = Pattern
        .compile(ADDRESS_LENGTH_BOUNDARIES_REGEX);

    /**
     * Validation of an ILP address occurs via Regex, so we don't need to aggressively compute this value. Thus, it is
     * marked <tt>Lazy</tt> so that immutables will not generate this value unless it is called.
     */
    @Override
    @Lazy
    public AllocationScheme getAllocationScheme() {
      return AllocationScheme.of(getValue().substring(0, getValue().indexOf('.')));
    }

    /**
     * Precondition enforcer that ensures the value is a valid Interledger Address.
     *
     * @see "https://github.com/interledger/rfcs/blob/master/0015-ilp-addresses/0015-ilp-addresses.md"
     */
    @Value.Check
    void check() {
      if (!VALID_ADDRESS_PATTERN.matcher(getValue()).matches()) {
        // For performance reasons, we only do we do deeper introspection of the error if the input
        // fails the VALID_ADDRESS_PATTERN check.
        throw new IllegalArgumentException(getFirstInvalidityCause(getValue()));
      }
    }

    /**
     * Inspect an invalid Address String and determine the cause.
     *
     * @param invalidAddressString A {@link String} containing an invalid Interledger Address.
     *
     * @return An error String.
     */
    private String getFirstInvalidityCause(final String invalidAddressString) {
      // validate no trailing period.
      if (invalidAddressString.endsWith(".")) {
        return Error.ILLEGAL_ENDING.getMessageFormat();
      }

      // validates scheme prefix existence
      if (invalidAddressString.isEmpty()) {
        return Error.MISSING_SCHEME_PREFIX.getMessageFormat();
      }

      final List<String> schemeAndSegments = Arrays.asList(
          invalidAddressString.split(SEPARATOR_REGEX, -1)
      );
      // validates scheme prefix format
      final String schemePrefix = schemeAndSegments.get(0);
      if (!SCHEME_PATTERN.matcher(schemePrefix).matches()) {
        return String.format(Error.INVALID_SCHEME_PREFIX.getMessageFormat(), schemePrefix);
      }

      // validates each segment format
      final List<String> segments = schemeAndSegments.stream().skip(1).collect(Collectors.toList());
      final int segmentsSize = segments.size();
      final Matcher segmentMatcher = SEGMENT_PATTERN.matcher("");
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
      if (segmentsSize < ADDRESS_MIN_SEGMENTS) {
        return Error.SEGMENTS_UNDERFLOW.getMessageFormat();
      }

      // validates max address length
      if (!ADDRESS_LENGTH_BOUNDARIES_PATTERN.matcher(invalidAddressString).matches()) {
        return Error.ADDRESS_OVERFLOW.getMessageFormat();
      }

      // fault: should have found an error cause
      throw new IllegalArgumentException(String.format(
          "Unable to find error for invalid InterledgerAddress: %s", invalidAddressString
      ));
    }

    /**
     * A potential error that might be emitted if an invalid Interledger Address is encountered.
     */
    enum Error {
      INVALID_SCHEME_PREFIX("The '%s' AllocationScheme is invalid!"),
      ILLEGAL_ENDING("An InterledgerAddress MUST not end with a period (.) character"),
      ADDRESS_OVERFLOW("InterledgerAddress is too long"),
      INVALID_SEGMENT("The '%s' segment has an invalid format"),
      MISSING_SCHEME_PREFIX("InterledgerAddress does not start with a scheme prefix"),
      SEGMENTS_UNDERFLOW("InterledgerAddress has too few segments");

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
