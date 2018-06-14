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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A parser for validating an {@link InterledgerAddress}.
 */
final class InterledgerAddressParser {

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
      + "^" + SCHEME_PREFIX_REGEX + "(" + SEGMENT_PREFIX_REGEX + ")+" + SEGMENT_REGEX + "$";
  private static final Pattern DESTINATION_ADDRESS_PATTERN = Pattern.compile(
      DESTINATION_ADDRESS_REGEX
  );
  private static final String SCHEME_PREFIX_ONLY_REGEX = "^" + SCHEME_PREFIX_REGEX + "$";
  private static final Pattern SCHEME_PREFIX_ONLY_PATTERN = Pattern.compile(
      SCHEME_PREFIX_ONLY_REGEX
  );

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

  /**
   * Determines if an ILP address is a scheme prefix.
   *
   * @param addressString The ILP address to evaluate
   *
   * @return True if address is a scheme prefix, false otherwise
   */
  boolean isSchemePrefix(final String addressString) {
    Objects.requireNonNull(addressString); // No error-message because this should never happen
    return SCHEME_PREFIX_ONLY_PATTERN.matcher(addressString).matches();
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
    if (!Pattern.compile(ADDRESS_LENGTH_BOUNDARIES_REGEX).matcher(invalidAddressString).matches()) {
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
