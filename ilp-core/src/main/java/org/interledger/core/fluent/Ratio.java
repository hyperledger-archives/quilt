package org.interledger.core.fluent;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.interledger.core.fluent.ImmutableRatio.Builder;

/**
 * Represents the ratio of two {@link UnsignedLong} numbers: a numerator greater than or equal to 0, and a denominator
 * greater than 0.
 */
@Immutable
public interface Ratio extends Comparable<Ratio> {

  static Builder builder() {
    return ImmutableRatio.builder();
  }

  Ratio ZERO = builder().numerator(BigInteger.ZERO).denominator(BigInteger.ONE).build();
  Ratio ONE = builder().numerator(BigInteger.ONE).denominator(BigInteger.ONE).build();

  /**
   * The numerator, greater than or equal to {@link UnsignedLong#ZERO}.
   *
   * @return An {@link UnsignedLong}.
   */
  @Default
  default BigInteger numerator() {
    return BigInteger.ONE;
  }

  /**
   * The denominator, greater than {@link BigInteger#ZERO}.
   *
   * @return An {@link BigInteger}.
   */
  @Default
  default BigInteger denominator() {
    return BigInteger.ONE;
  }

  @Derived
  default BigDecimal toBigDecimal() {
    try {
      return new BigDecimal(this.numerator()).divide(new BigDecimal(this.denominator()));
    } catch (ArithmeticException e) {
      return new BigDecimal(this.numerator()).divide(new BigDecimal(this.denominator()), 10, RoundingMode.HALF_EVEN);
    }
  }

  /**
   * Construct a new {@link Ratio} from the supplied {@code bigDecimal} by scaling the value until it is a whole
   * number.
   *
   * @param bigDecimal A {@link BigDecimal} to turn into a {@link Ratio}.
   * @return The constructed {@link Ratio} that holds two {@link BigInteger} values.
   */
  static Ratio from(final BigDecimal bigDecimal) {
    Objects.requireNonNull(bigDecimal);

    BigDecimal e = BigDecimal.TEN;
    while (!isIntegerValue(bigDecimal.multiply(e))) {
      e = e.multiply(BigDecimal.TEN);
    }

    // toBigIntegerExact should never throw because of the scaling operation above.
    final BigInteger aBigInt = bigDecimal.multiply(e).toBigIntegerExact();
    return Ratio.from(aBigInt, e.toBigIntegerExact());
  }

  /**
   * Construct a new {@link Ratio} from the supplied {@code bigInteger} by scaling the value until it is a whole
   * number.
   *
   * @param numerator
   * @param denominator
   * @return The constructed {@link Ratio} that holds two {@link BigInteger} values.
   */
  static Ratio from(final BigInteger numerator, final BigInteger denominator) {
    Objects.requireNonNull(numerator);
    Objects.requireNonNull(denominator);

    return Ratio.builder()
      .numerator(numerator)
      .denominator(denominator)
      .build();
  }

  /**
   * Construct a new {@link Ratio} from the supplied {@code bigInteger} by scaling the value until it is a whole
   * number.
   *
   * @param numerator
   * @param denominator
   * @return The constructed {@link Ratio} that holds two {@link BigInteger} values.
   */
  static Ratio from(final UnsignedLong numerator, final UnsignedLong denominator) {
    Objects.requireNonNull(numerator);
    Objects.requireNonNull(denominator);

    return Ratio.from(numerator.bigIntegerValue(), denominator.bigIntegerValue());
  }

  static boolean isIntegerValue(final BigDecimal bd) {
    Objects.requireNonNull(bd);
    return bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0;
  }

  @Derived
  default int compareTo(final Ratio other) {
    Objects.requireNonNull(other);

    BigInteger first = this.numerator().multiply(other.denominator());
    BigInteger second = this.denominator().multiply(other.numerator());

    return first.compareTo(second);
  }

  @Derived
  default boolean equalButMorePrecise(final Ratio other) {
    Objects.requireNonNull(other);

    BigInteger first = this.numerator().multiply(other.denominator());
    BigInteger second = this.denominator().multiply(other.numerator());

    int result = first.compareTo(second);

    if (result == 0) {
      // If the two ratios are equal, then check to see which denominator is bigger.
      // If this has a larger denominator, then it is equal but has more precision, so return true.
      return this.denominator().compareTo(other.denominator()) == 1;
    } else {
      return false;
    }
  }

  /**
   * Multiply a number by a ratio and return the "floor" value.
   *
   * @param amount
   * @return
   */
  // TODO: Unit test to validate floor.
  @Derived
  default BigInteger multiplyFloor(final BigInteger amount) {
    Objects.requireNonNull(amount);
    return amount.multiply(this.numerator()).divide(this.denominator());
  }

  /**
   * Multiply a number by a ratio and return the "floor" value.
   *
   * @param amount
   * @return
   */
  // TODO: Unit test to validate floor.
  @Derived
  default UnsignedLong multiplyFloor(final UnsignedLong amount) {
    Objects.requireNonNull(amount);
    BigInteger newValue = multiplyFloor(amount.bigIntegerValue());

    if (FluentCompareTo.is(newValue).greaterThan(UnsignedLong.MAX_VALUE.bigIntegerValue())) {
      // Overflow detected. We need to round to MAX_VALUE probably.
      return UnsignedLong.MAX_VALUE;
    } else {
      return UnsignedLong.valueOf(newValue);
    }
  }

  // TODO: Unit test
  @Derived
  default BigInteger multiplyCeil(final BigInteger amount) {
    Objects.requireNonNull(amount);
    boolean isPositive = FluentCompareTo.is(modulo(amount, this)).greaterThan(BigInteger.ZERO);
    if (isPositive) {
      return (amount.multiply(this.numerator()).divide(this.denominator())).add(BigInteger.ONE);
    } else {
      return amount.multiply(this.numerator()).divide(this.denominator());
    }
  }

  // TODO: Unit test
  @Derived
  default UnsignedLong multiplyCeil(final UnsignedLong amount) {
    Objects.requireNonNull(amount);
    Objects.requireNonNull(amount);
    BigInteger newValue = multiplyCeil(amount.bigIntegerValue());

    if (FluentCompareTo.is(newValue).greaterThan(UnsignedLong.MAX_VALUE.bigIntegerValue())) {
      // Overflow detected. We need to round to MAX_VALUE probably.
      return UnsignedLong.MAX_VALUE;
    } else {
      return UnsignedLong.valueOf(newValue);
    }
  }

  @Lazy
  default Optional<Ratio> reciprocal() {
    if (FluentBigInteger.of(this.numerator()).isPositive()) {
      return Optional.of(Ratio.builder()
        .numerator(this.denominator())
        .denominator(this.numerator())
        .build());
    } else {
      return Optional.empty();
    }
  }

  // TODO: Unit test
  static BigInteger modulo(BigInteger amount, final Ratio ratio) {
    Objects.requireNonNull(amount);
    Objects.requireNonNull(ratio);
    return amount.multiply(ratio.numerator()).mod(ratio.denominator());
  }

  @Derived
  default Ratio subtract(final Ratio ratio) {
    Objects.requireNonNull(ratio);
    final BigInteger a = this.numerator().multiply(ratio.denominator())
      .subtract(ratio.numerator().multiply(this.denominator()));
    final BigInteger b = this.denominator().multiply(ratio.denominator());
    return Ratio.builder().numerator(a).denominator(b).build();
  }

  @Derived
  default boolean isPositive() {
    return FluentCompareTo.is(this.numerator()).greaterThan(BigInteger.ZERO);
  }

  @Derived
  default boolean isNotPositive() {
    return !isPositive();
  }

  @Derived
  default boolean isPositiveInteger() {
    return isPositive() && isIntegerValue(this.toBigDecimal());
  }

//  @Derived
//  default boolean isNegative() {
//    return FluentCompareTo.is(this.numerator()).lessThan(BigInteger.ZERO);
//  }

  @Derived
  default boolean isZero() {
    return FluentCompareTo.is(this.numerator()).equalTo(BigInteger.ZERO);
  }

  @Value.Check
  default void check() {
    // Denominator must be positive!
    Preconditions.checkState(
      FluentCompareTo.is(denominator()).greaterThan(BigInteger.ZERO), "Denominator must be greater-than 0"
    );
  }
}
