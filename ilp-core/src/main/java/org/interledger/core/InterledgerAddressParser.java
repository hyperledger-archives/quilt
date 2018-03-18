package org.interledger.core;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An {@link org.interledger.core.InterledgerAddress}es parser.
 */
final class InterledgerAddressParser {

  static enum Error {
    ADDRESS_OVERFLOW("Address is too long"),
    INVALID_SEGMENT("The '%s' segment has an invalid format"),
    INVALID_SCHEME_PREFIX("The '%s' scheme prefix has an invalid format"),
    MISSING_SCHEME_PREFIX("Address does not start with a scheme prefix"),
    SEGMENTS_UNDERFLOW("Destination address has too few segments");

    private String messageFormat;

    private Error(final String messageFormat) {
      this.messageFormat = messageFormat;
    }

    public String getMessageFormat() {
      return messageFormat;
    }
  }

  private static final int MIN_ADDRESS_LENGTH = 1;
  private static final int MAX_ADDRESS_LENGTH = 1023;
  private static final int DESTINATION_ADDRESS_MIN_SEGMENTS = 2;

  private static final String SCHEME_REGEX = "(g|private|example|peer|self|test[1-3]?)";
  private static final String SEGMENT_REGEX = "[a-zA-Z0-9_~-]+";
  private static final String SEPARATOR_CHARACTER = ".";
  private static final String SEPARATOR_REGEX = "[" + SEPARATOR_CHARACTER + "]";
  private static final String SCHEME_PREFIX_REGEX = SCHEME_REGEX + SEPARATOR_REGEX;
  private static final String SEGMENT_PREFIX_REGEX = SEGMENT_REGEX + SEPARATOR_REGEX;
  private static final String ADDRESS_LENGTH_BOUNDARIES_REGEX = "(?=^.{" + MIN_ADDRESS_LENGTH + ","
      + MAX_ADDRESS_LENGTH + "}$)";

  private static final String ADDRESS_PREFIX_REGEX = ADDRESS_LENGTH_BOUNDARIES_REGEX
      + "^" + SCHEME_PREFIX_REGEX + "(" + SEGMENT_PREFIX_REGEX + ")*$";
  private static final Pattern ADDRESS_PREFIX_PATTERN = Pattern.compile(ADDRESS_PREFIX_REGEX);

  private static final String DESTINATION_ADDRESS_REGEX = ADDRESS_LENGTH_BOUNDARIES_REGEX
      + "^" + SCHEME_PREFIX_REGEX + "(" + SEGMENT_PREFIX_REGEX + ")+"
      + SEGMENT_REGEX + "$";
  private static final Pattern DESTINATION_ADDRESS_PATTERN = Pattern.compile(
      DESTINATION_ADDRESS_REGEX
  );

  private static final String SCHEME_PREFIX_ONLY_REGEX = "^" + SCHEME_PREFIX_REGEX + "$";
  private static final Pattern SCHEME_PREFIX_ONLY_PATTERN = Pattern.compile(
      SCHEME_PREFIX_ONLY_REGEX
  );

  /**
   * Checks and requires that the specified {@code address} is an address prefix per {@link
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
   * @param address A {@link InterledgerAddress} to check.
   *
   * @return {@code address} if its value ends with a dot (.).
   *
   * @throws IllegalArgumentException if the supplied Interledger address is not a ledger-prefix.
   */
  InterledgerAddress requireAddressPrefix(final InterledgerAddress address) {
    return checkIsAddressPrefix(address, (ilpAddress) -> "InterledgerAddress must not be null!",
        (ilpAddress) -> String.format("InterledgerAddress '%s' must be an Address Prefix ending"
            + " with a dot (.)", ilpAddress.getValue()));
  }

  /**
   * Checks and requires that the specified {@code address} is an address prefix per {@link
   * InterledgerAddress#isLedgerPrefix()}, providing an error message upon invalidation.
   *
   * <p>This method is designed primarily for doing parameter validation in methods and
   * constructors, as demonstrated below:</p> <blockquote>
   * <pre>
   * public Foo(InterledgerAddress bar) {
   *     this.ledgerPrefix = InterledgerAddress.requireAddressPrefix(bar,
   *         bar + " must be an address prefix);
   * }
   * </pre>
   * </blockquote>
   *
   * @param address A {@link InterledgerAddress} to check.
   * @param errorMessage An error message to output upon invalidation.
   *
   * @return {@code address} if its value ends with a dot (.).
   *
   * @throws IllegalArgumentException if the supplied Interledger address is not a
   *     ledger-prefix. Embeds the {@code errorMessage}.
   */
  InterledgerAddress requireAddressPrefix(final InterledgerAddress address,
      final String errorMessage) {
    Objects.requireNonNull(errorMessage);
    return checkIsAddressPrefix(address, (ilpAddress) -> errorMessage,
        (ilpAddress) -> errorMessage);
  }

  /**
   * Checks and requires that the specified {@code address} is not an address prefix per
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
   * @param address A {@link InterledgerAddress} to check.
   *
   * @return {@code address} if its value ends with a dot (.).
   *
   * @throws IllegalArgumentException if the supplied Interledger address is not a ledger-prefix.
   */
  InterledgerAddress requireNotAddressPrefix(final InterledgerAddress address) {
    return checkIsNotAddressPrefix(address, (ilpAddress) -> "InterledgerAddress must not be null!",
        (ilpAddress) -> String.format("InterledgerAddress '%s' must NOT be an Address Prefix ending"
            + " with a dot (.)", address.getValue()));
  }

  /**
   * Checks and requires that the specified {@code address} is not an address prefix per
   * {@link InterledgerAddress#isLedgerPrefix()}, providing an error message upon invalidation.
   *
   *
   * <p>This method is designed primarily for doing parameter validation in methods and
   * constructors, as demonstrated below:</p> <blockquote>
   * <pre>
   * public Foo(InterledgerAddress bar) {
   *     this.nonLedgerPrefix = InterledgerAddress.requireNotAddressPrefix(bar,
   *         bar + " must be a destination address");
   * }
   * </pre>
   * </blockquote>
   *
   * @param address A {@link InterledgerAddress} to check.
   * @param errorMessage An error message to output upon invalidation.
   *
   * @return {@code address} if its value ends with a dot (.).
   *
   * @throws IllegalArgumentException if the supplied Interledger address is not a
   *     ledger-prefix. Embeds the {@code errorMessage}.
   */
  InterledgerAddress requireNotAddressPrefix(final InterledgerAddress address,
      final String errorMessage) {
    Objects.requireNonNull(errorMessage);
    return checkIsNotAddressPrefix(address, (ilpAddress) -> errorMessage,
        (ilpAddress) -> errorMessage);
  }

  /**
   * Validates an ILP address.
   * 
   * @param addressString The ILP address to validate
   * @throws IllegalArgumentException When validation is rejected
   */
  void validate(final String addressString) throws IllegalArgumentException {
    if (isFullyValid(addressString)) {
      return;
    }
    throw new IllegalArgumentException(getFirstInvalidityCause(addressString));
  }

  /**
   * Determines if an ILP address is a scheme prefix.
   * 
   * @param addressString The ILP address to evaluate
   * @return True if address is a scheme prefix, false otherwise
   */
  boolean isSchemePrefix(final String addressString) {
    return SCHEME_PREFIX_ONLY_PATTERN.matcher(addressString).matches();
  }

  private InterledgerAddress checkIsAddressPrefix(final InterledgerAddress address,
      final Function<InterledgerAddress, String> errorMessageIfNullAddress,
      final Function<InterledgerAddress, String> errorMessageIfNotAddressPrefix) {
    Objects.requireNonNull(address, errorMessageIfNullAddress.apply(address));
    if (!address.isLedgerPrefix()) {
      throw new IllegalArgumentException(errorMessageIfNotAddressPrefix.apply(address));
    } else {
      return address;
    }
  }

  private InterledgerAddress checkIsNotAddressPrefix(final InterledgerAddress address,
      final Function<InterledgerAddress, String> errorMessageIfNullAddress,
      final Function<InterledgerAddress, String> errorMessageIfAddressPrefix) {
    Objects.requireNonNull(address, errorMessageIfNullAddress.apply(address));
    if (address.isLedgerPrefix()) {
      throw new IllegalArgumentException(errorMessageIfAddressPrefix.apply(address));
    } else {
      return address;
    }
  }

  private boolean isFullyValid(final String addressString) {
    return (addressString.endsWith(SEPARATOR_CHARACTER) ? ADDRESS_PREFIX_PATTERN
        : DESTINATION_ADDRESS_PATTERN).matcher(addressString).matches();
  }

  private String getFirstInvalidityCause(final String invalidAddressString) {
    final List<String> schemeAndSegments = Arrays.asList(invalidAddressString
        .split(SEPARATOR_REGEX, -1));
    final int schemeAndSegmentsSize = schemeAndSegments.size();

    // validates scheme prefix existence
    //     'schemeAndSegmentsSize < 2' ensures scheme is followed by a trailing separator
    //     (i.e. scheme prefix = scheme + separator)
    if (invalidAddressString.isEmpty() || schemeAndSegmentsSize < 2) {
      return String.format(Error.MISSING_SCHEME_PREFIX.getMessageFormat());
    }
    // validates scheme prefix format
    final String schemePrefix = schemeAndSegments.get(0);
    if (!Pattern.compile(SCHEME_REGEX).matcher(schemePrefix).matches()) {
      return String.format(Error.INVALID_SCHEME_PREFIX.getMessageFormat(), schemePrefix);
    }

    // validates each segment format
    final List<String> segments = schemeAndSegments.stream().skip(1).collect(Collectors.toList());;
    final int segmentsSize = segments.size();
    final Matcher segmentMatcher = Pattern.compile(SEGMENT_REGEX).matcher("");
    final Optional<String> invalidSegment = segments.stream().filter(segment -> {
      segmentMatcher.reset(segment);
      return !segmentMatcher.matches();
    }).findFirst();
    if (invalidSegment.isPresent()) {
      return String.format(Error.INVALID_SEGMENT.getMessageFormat(), invalidSegment.get());
    }

    // validates the minimum number of segments for a destination address
    final boolean isDestinationAddress = !invalidAddressString.endsWith(SEPARATOR_CHARACTER);
    if (isDestinationAddress && segmentsSize < DESTINATION_ADDRESS_MIN_SEGMENTS) {
      return String.format(Error.SEGMENTS_UNDERFLOW.getMessageFormat());
    }

    // validates max address length
    if (!Pattern.compile(ADDRESS_LENGTH_BOUNDARIES_REGEX).matcher(invalidAddressString).matches()) {
      return String.format(Error.ADDRESS_OVERFLOW.getMessageFormat());
    }

    // fault: should have found an error cause
    throw new RuntimeException();
  }

}
