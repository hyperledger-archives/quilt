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

  Ratio ZERO = builder().numerator(UnsignedLong.ZERO).denominator(UnsignedLong.ONE).build();
  Ratio ONE = builder().numerator(UnsignedLong.ONE).denominator(UnsignedLong.ONE).build();

  /**
   * The numerator, greater than or equal to {@link UnsignedLong#ZERO}.
   *
   * @return An {@link UnsignedLong}.
   */
  @Default
  default UnsignedLong numerator() {
    return UnsignedLong.ONE;
  }

  /**
   * The denominator, greater than {@link UnsignedLong#ZERO}.
   *
   * @return An {@link UnsignedLong}.
   */
  @Default
  default UnsignedLong denominator() {
    return UnsignedLong.ONE;
  }

  @Derived
  default boolean isPositive() {
    return FluentCompareTo.is(this.numerator()).greaterThan(UnsignedLong.ZERO);
  }

  @Derived
  default BigDecimal toBigDecimal() {
    try {
      return new BigDecimal(this.numerator().bigIntegerValue())
        .divide(new BigDecimal(this.denominator().bigIntegerValue()));
    } catch (ArithmeticException e) {
      return new BigDecimal(this.numerator().bigIntegerValue())
        .divide(new BigDecimal(this.denominator().bigIntegerValue()), 10, RoundingMode.HALF_EVEN);
    }
  }

  /**
   * Construct a new {@link Ratio} from the supplied {@code bigDecimal} by scaling the value until it is a whole
   * number.
   *
   * @param bigDecimal A {@link BigDecimal} to turn into a {@link Ratio}.
   * @return The constructed {@link Ratio} that holds two {@link UnsignedLong} values.
   */
  static Ratio from(BigDecimal bigDecimal) {
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
   * @return The constructed {@link Ratio} that holds two {@link UnsignedLong} values.
   */
  static Ratio from(final BigInteger numerator, final BigInteger denominator) {
    Objects.requireNonNull(numerator);
    Objects.requireNonNull(denominator);

    // toBigIntegerExact should never throw because of the scaling operation above.
    final UnsignedLong a;
    if (numerator.compareTo(UnsignedLong.MAX_VALUE.bigIntegerValue()) > 0) {
      a = UnsignedLong.MAX_VALUE;
    } else {
      a = UnsignedLong.valueOf(numerator);
    }

    final UnsignedLong b;
    if (denominator.compareTo(UnsignedLong.MAX_VALUE.bigIntegerValue()) > 0) {
      b = UnsignedLong.MAX_VALUE;
    } else {
      b = UnsignedLong.valueOf(denominator);
    }

    return Ratio.builder()
      .numerator(a)
      .denominator(b)
      .build();
  }

  static boolean isIntegerValue(final BigDecimal bd) {
    Objects.requireNonNull(bd);
    return bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0;
  }

  // TODO: Unit test.
  @Derived
  default int compareTo(Ratio other) {
    if (FluentCompareTo.is(this.numerator().times(other.denominator()))
      .greaterThan(this.denominator().times(other.numerator()))
    ) {
      return 1;
    } else if (FluentCompareTo.is(this.numerator().times(other.denominator()))
      .lessThan(this.denominator().times(other.numerator()))) {
      return -1;
    } else {
      return 0;
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
  default UnsignedLong timesFloor(final UnsignedLong amount) {
    Objects.requireNonNull(amount);
    return amount.times(this.numerator()).dividedBy(this.denominator());
  }

  // TODO: Unit test
  @Derived
  default UnsignedLong timesCeil(UnsignedLong amount) {
    boolean isPositive = FluentCompareTo.is(modulo(amount, this)).greaterThan(UnsignedLong.ZERO);
    if (isPositive) {
      return (amount.times(this.numerator()).dividedBy(this.denominator())).plus(UnsignedLong.ONE);
    } else {
      return amount.times(this.numerator()).dividedBy(this.denominator());
    }
  }

  @Lazy
  default Optional<Ratio> reciprocal() {
    if (FluentUnsignedLong.of(this.numerator()).isPositive()) {
      return Optional.of(Ratio.builder()
        .numerator(this.denominator())
        .denominator(this.numerator())
        .build());
    } else {
      return Optional.empty();
    }
  }

  // TODO: Unit test
  static UnsignedLong modulo(UnsignedLong amount1, final Ratio r) {
    return amount1.times(r.numerator()).mod(r.denominator());
  }

  // TODO: Unit test
  @Derived
  default Ratio subtract(final Ratio r) {
    // TODO: Remove this check and introduce isPostive.
    if (FluentCompareTo.is(r).greaterThan(this)) {
      return Ratio.ZERO;
    } else {
      final UnsignedLong a = this.numerator().times(r.denominator()).minus(r.numerator().times(this.denominator()));
      final UnsignedLong b = this.denominator().times(r.denominator());
      return Ratio.builder().numerator(a).denominator(b).build();
    }
  }

  @Value.Check
  default void check() {
    Preconditions.checkState(
      FluentCompareTo.is(denominator()).greaterThan(UnsignedLong.ZERO), "Denominator must be greater-than 0"
    );
  }
}
