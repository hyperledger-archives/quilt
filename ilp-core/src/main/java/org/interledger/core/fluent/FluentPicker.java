package org.interledger.core.fluent;

import com.google.common.primitives.UnsignedLong;

import java.util.Objects;

/**
 * Utilities for operating on {@link UnsignedLong}.
 */
//TODO: Delete if unused? Compare to FUL and FBI
public class FluentPicker {

  /**
   * Compute the smaller of {@code value1} and {@code value2}.
   *
   * @param value1 The first value.
   * @param value2 The second value.
   *
   * @return The smaller of the two supplied values.
   */
  public static <T extends Comparable<T>> T min(final T value1, final T value2) {
    Objects.requireNonNull(value1);
    Objects.requireNonNull(value2);

    if (FluentCompareTo.is(value1).lessThan(value2)) {
      return value1;
    } else {
      return value2;
    }
  }

  /**
   * Compute the larger of {@code value1} and {@code value2}.
   *
   * @param value1 The first value.
   * @param value2 The second value.
   *
   * @return The smaller of the two supplied values.
   */
  public static <T extends Comparable<T>> T max(final T value1, final T value2) {
    Objects.requireNonNull(value1);
    Objects.requireNonNull(value2);

    if (FluentCompareTo.is(value1).greaterThan(value2)) {
      return value1;
    } else {
      return value2;
    }
  }

}
