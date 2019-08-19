package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
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

import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.SCHEME_PATTERN;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.SEGMENT_PATTERN;
import static org.interledger.core.InterledgerAddress.AbstractInterledgerAddress.SEPARATOR_REGEX;

import org.interledger.core.InterledgerAddress.AllocationScheme;

import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>Represents a prefix for an {@link InterledgerAddress}. This class differs only slightly from
 * {@link InterledgerAddress} in that is allows any valid Interledger address as well as allocation
 * schemes, whereas {@link InterledgerAddress} requires both an {@link AllocationScheme} as well as
 * a segment in order to be valid.</p>
 */
public interface InterledgerAddressPrefix {

  // Prefixes for all Allocation Schemes.
  InterledgerAddressPrefix GLOBAL = InterledgerAddressPrefix.from(AllocationScheme.GLOBAL);
  InterledgerAddressPrefix PRIVATE = InterledgerAddressPrefix.from(AllocationScheme.PRIVATE);
  InterledgerAddressPrefix EXAMPLE = InterledgerAddressPrefix.from(AllocationScheme.EXAMPLE);
  InterledgerAddressPrefix PEER = InterledgerAddressPrefix.from(AllocationScheme.PEER);
  InterledgerAddressPrefix SELF = InterledgerAddressPrefix.from(AllocationScheme.SELF);
  InterledgerAddressPrefix TEST = InterledgerAddressPrefix.from(AllocationScheme.TEST);
  InterledgerAddressPrefix TEST1 = InterledgerAddressPrefix.from(AllocationScheme.TEST1);
  InterledgerAddressPrefix TEST2 = InterledgerAddressPrefix.from(AllocationScheme.TEST2);
  InterledgerAddressPrefix TEST3 = InterledgerAddressPrefix.from(AllocationScheme.TEST3);

  /**
   * Constructor to allow quick construction from a {@link String}.
   *
   * @param value String representation of an Interledger Address
   *
   * @return an {@link InterledgerAddressPrefix} instance.
   *
   * @throws NullPointerException if {@code value} is <tt>null</tt>.
   */
  static InterledgerAddressPrefix of(final String value) {
    Objects.requireNonNull(value, "value must not be null!");
    return builder().value(value).build();
  }

  /**
   * Convert from an {@link InterledgerAddress} into an {@link InterledgerAddressPrefix}.
   *
   * @param interledgerAddress An {@link InterledgerAddress} to convert from.
   *
   * @return A corresponding {@link InterledgerAddressPrefix}.
   */
  static InterledgerAddressPrefix from(final InterledgerAddress interledgerAddress) {
    return builder().value(interledgerAddress.getValue()).build();
  }

  /**
   * Convert from an {@link AllocationScheme} into an {@link InterledgerAddressPrefix}.
   *
   * @param allocationScheme An {@link AllocationScheme} to convert from.
   *
   * @return A corresponding {@link InterledgerAddressPrefix}.
   */
  static InterledgerAddressPrefix from(final AllocationScheme allocationScheme) {
    return builder().value(allocationScheme.getValue()).build();
  }

  /**
   * <p>Construct a default builder.</p>
   *
   * @return An {@link InterledgerAddressBuilder} instance.
   */
  static InterledgerAddressPrefixBuilder builder() {
    return new InterledgerAddressPrefixBuilder();
  }

  /**
   * <p>Accessor for this address's value as a non-null {@link String}. For example:
   * <code>us.usd.bank.account</code></p>
   *
   * @return A {@link String} representation of this Interledger address.
   */
  String getValue();

  /**
   * <p>Tests if this {@link InterledgerAddressPrefix} starts with the specified {@code addressSegment}.</p>
   *
   * @param addressSegment An {@link String} prefix to compare against.
   *
   * @return {@code true} if this {@link InterledgerAddressPrefix} begins with the specified prefix.
   */
  @SuppressWarnings("unused")
  default boolean startsWith(final String addressSegment) {
    Objects.requireNonNull(addressSegment, "addressSegment must not be null!");
    return this.getValue().startsWith(addressSegment);
  }

  /**
   * <p>Tests if this {@link InterledgerAddressPrefix} starts with the specified {@code addressPrefix}.</p>
   *
   * @param addressPrefix An {@link InterledgerAddressPrefix} to compare against.
   *
   * @return {@code true} if the supplied {@code addressPrefix} begins with the specified prefix.
   */
  @SuppressWarnings("unused")
  default boolean startsWith(final InterledgerAddressPrefix addressPrefix) {
    Objects.requireNonNull(addressPrefix, "addressPrefix must not be null!");
    return this.getValue().startsWith(addressPrefix.getValue());
  }

  /**
   * <p>Return a new {@link InterledgerAddressPrefix} by suffixing the supplied {@code addressSegment}
   * onto the current address.</p>
   *
   * @param addressSegment A {@link String} to be appended to this address as an additional
   *                       segment.
   *
   * @return A new instance representing the original address with a newly specified final segment.
   */
  default InterledgerAddressPrefix with(final String addressSegment) {
    Objects.requireNonNull(addressSegment, "addressSegment must not be null!");

    // `+` operator uses StringBuilder internally, so for small numbers of appends, `+` is
    // equivalent in performance, but provides better code readability.
    return InterledgerAddressPrefix.of(this.getValue() + "." + addressSegment);
  }

  /**
   * <p>Return this address's prefix.</p>
   *
   * <p>If this address has only a single segment after the allocation scheme, then this method
   * returns {@link Optional#empty()}. Otherwise, this method returns a new {@link
   * InterledgerAddressPrefix} containing the characters inside of {@link #getValue()}, up-to but
   * excluding last period.</p>
   *
   * <p>For example, calling this method on an address <tt>g.example.alice</tt> would yield a new
   * address containing <tt>g.example</tt>. However, calling this method on an address like
   * <tt>g.example</tt> would yield {@link Optional#empty()}.</p>
   *
   * @return An optionally present parent-prefix as an {@link InterledgerAddressPrefix}.
   */
  default Optional<InterledgerAddressPrefix> getPrefix() {
    // An address will always contain at least one period (.), so we can always
    final String value = getValue();
    final boolean hasDot = value.contains(".");
    if (hasDot) {
      return Optional.of(
          InterledgerAddressPrefix.builder()
              .value(value.substring(0, value.lastIndexOf(".")))
              .build()
      );
    } else {
      return Optional.empty();
    }
  }

  /**
   * <p>Determines if this ILP Address has a parent-prefix.</p>
   *
   * @return {@code true} if this address has more than two segments after the allocation scheme.
   *     Otherwise return {@code false}.
   */
  @SuppressWarnings("unused")
  default boolean hasPrefix() {
    return getPrefix().isPresent();
  }

  /**
   * Compute the root-prefix of the supplied {@code address}. If this prefix is already a
   * root-prefix (i.e., an AllocationScheme) then this prefix is returned.
   *
   * @return An {@link InterledgerAddressPrefix} representing the root prefix for the supplied
   *     Interledger address.
   */
  default InterledgerAddressPrefix getRootPrefix() {
    return this.getPrefix()
        .map(InterledgerAddressPrefix::getRootPrefix)
        .orElse(this);
  }

  /**
   * <p>An implementation of {@link InterledgerAddressPrefix} that enforces allowed value per
   * RFC-15.</p>
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
  abstract class AbstractInterledgerAddressPrefix implements InterledgerAddressPrefix {

    private static final String VALID_ADDRESS_PREFIX_REGEX
        = "(?=^.{1,1023}$)^(g|private|example|peer|self|test[1-3]?|local)([.][a-zA-Z0-9_~-]+)*$";
    private static final Pattern VALID_ADDRESS_PATTERN = Pattern
        .compile(VALID_ADDRESS_PREFIX_REGEX);

    private static final int ADDRESS_MIN_SEGMENTS = 1;

    private static final String ADDRESS_LENGTH_BOUNDARIES_REGEX = "(?=^.{1,1021}$)";
    private static final Pattern ADDRESS_LENGTH_BOUNDARIES_PATTERN = Pattern
        .compile(ADDRESS_LENGTH_BOUNDARIES_REGEX);

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

      // validates max address length
      if (!ADDRESS_LENGTH_BOUNDARIES_PATTERN.matcher(invalidAddressString).matches()) {
        return Error.ADDRESS_OVERFLOW.getMessageFormat();
      }

      // fault: should have found an error cause
      throw new IllegalArgumentException(String.format(
          "Unable to find error for invalid InterledgerAddress: %s. Please report this as a bug!", invalidAddressString
      ));
    }

    /**
     * A potential error that might be emitted if an invalid Interledger Address is encountered.
     */
    public enum Error {
      INVALID_SCHEME_PREFIX("The '%s' AllocationScheme is invalid!"),
      ILLEGAL_ENDING("An InterledgerAddressPrefix MUST not end with a period (.) character"),
      ADDRESS_OVERFLOW("InterledgerAddressPrefix is too long"),
      INVALID_SEGMENT("The '%s' segment has an invalid format"),
      SEGMENTS_UNDERFLOW("InterledgerAddressPrefix has too few segments");

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
