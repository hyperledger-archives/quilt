package org.interledger.core.fluent;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the ratio of two {@link UnsignedLong} numbers: a numerator greater than or equal to 0, and a denominator
 * greater than 0.
 */
public interface Ratio extends Comparable<Ratio> {

  static ImmutableRatio.Builder builder() {
    return ImmutableRatio.builder();
  }

  Ratio ZERO = builder().numerator(BigInteger.ZERO).denominator(BigInteger.ONE).build();
  Ratio ONE = builder().numerator(BigInteger.ONE).denominator(BigInteger.ONE).build();

  /**
   * The numerator, greater than or equal to {@link UnsignedLong#ZERO}.
   *
   * @return An {@link UnsignedLong}.
   */
  BigInteger numerator();

  /**
   * The denominator, greater than {@link BigInteger#ZERO}.
   *
   * @return An {@link BigInteger}.
   */
  BigInteger denominator();

  /**
   * Convert this {@link Ratio} into a {@link BigDecimal}.
   *
   * @return A {@link BigDecimal} representing this ratio, with a precision amount allowed by {@link
   *   MathContext#DECIMAL128}.
   */
  BigDecimal toBigDecimal();

  /**
   * Determine if {@code other} is equal to, but more precise than, this {@link Ratio}.
   *
   * @param other A {@link Ratio} to compare against.
   *
   * @return {@code true} if this ration is equal to, but more precice than, {@code other}; {@code false} otherise.
   */
  boolean equalButMorePrecise(final Ratio other);

  /**
   * Multiply this ratio by the supplied {@code amount} and return the "floor" of the result.
   *
   * @param amount A {@link BigInteger} to multiply this ratio by.
   *
   * @return A {@link BigInteger} representing the "floor" of this ratio multiplied by {@code amount}.
   */
  BigInteger multiplyFloor(final BigInteger amount);

  /**
   * Multiply this ratio by the supplied {@code amount} and return the "floor" of the result. If there's any overflow,
   * then return {@link UnsignedLong#ZERO}. If a non-zero return value during overflow conditions is desired, prefer
   * {@link #multiplyFloor(BigInteger)} instead.
   *
   * @param amount A {@link UnsignedLong} to multiply this ratio by.
   *
   * @return A {@link UnsignedLong} representing the "floor" of this ratio multiplied by {@code amount}, or {@link
   *   UnsignedLong#ZERO} if an overflow would have occurred.
   */
  UnsignedLong multiplyFloorOrZero(final UnsignedLong amount);

  /**
   * Multiply this ratio by the supplied {@code amount} and return the "ceil" of the result.
   *
   * @param amount A {@link BigInteger} to multiply this ratio by.
   *
   * @return A {@link BigInteger} representing the "ceil" of this ratio multiplied by {@code amount}.
   */
  BigInteger multiplyCeil(final BigInteger amount);

  /**
   * Multiply this ratio by the supplied {@code amount} and return the "ceil" of the result. Note that {@link
   * #multiplyCeil(BigInteger)} should be used instead if values larger than {@link UnsignedLong#MAX_VALUE} are
   * desired.
   *
   * @param amount A {@link UnsignedLong} to multiply this ratio by.
   *
   * @return A {@link UnsignedLong} representing the "ceil" of this ratio multiplied by {@code amount}.
   */
  UnsignedLong multiplyCeilOrZero(final UnsignedLong amount);

  /**
   * Return the reciprocal of this ratio, as a new {@link Ratio}.
   *
   * @return A {@link Ratio}.
   */
  Optional<Ratio> reciprocal();

  /**
   * Subtract {@code ratio} from this Ratio.
   *
   * @param ratio A {@link Ratio} to subtract.
   *
   * @return The difference between this ratio and {@code ratio}.
   */
  Ratio subtract(final Ratio ratio);

  /**
   * Determine if this Ratio is positive (i.e., greater-than 0).
   *
   * @return {@code true} if the numerator of this ratio is greater-than-zero; {@code false otherwise}.
   */
  boolean isPositive();

  /**
   * Determine if this Ratio is not positive (i.e., negative or 0).
   *
   * @return {@code true} if the numerator of this ratio is less-than or equal-to zero; {@code false otherwise}.
   */
  boolean isNotPositive();

  /**
   * Determine if this Ratio is a whole integer value (i.e., has no decimal component).
   *
   * @return {@code true} if {@link #toBigDecimal()} returns a whole-number integer value; {@code false otherwise}.
   */
  boolean isInteger();

  /**
   * Determine if this Ratio is a positive (i.e., greater-than 0) and also a whole integer value (i.e., has no decimal
   * component).
   *
   * @return {@code true} if the numerator of this ratio is greater-than-zero and the {@link #toBigDecimal()} returns an
   *   whole-number integer value; {@code false otherwise}.
   */
  boolean isPositiveInteger();

  /**
   * Determine if this Ratio is negative (i.e., less-than 0).
   *
   * @return {@code true} if the numerator of this ratio is less-than zero; {@code false otherwise}.
   */
  boolean isNegative();

  /**
   * Determine if this Ratio is zero.
   *
   * @return {@code true} if the numerator of this ratio is equal-to zero; {@code false otherwise}.
   */
  boolean isZero();

  /**
   * Immutable implementation of {@link Ratio}.
   */
  @Immutable
  abstract class AbstractRatio implements Ratio {

    @Default
    @Override
    public BigInteger numerator() {
      return BigInteger.ONE;
    }

    @Default
    @Override
    public BigInteger denominator() {
      return BigInteger.ONE;
    }

    @Override
    @Derived
    public BigDecimal toBigDecimal() {
      Preconditions.checkState(
        FluentCompareTo.is(denominator()).greaterThan(BigInteger.ZERO), "Denominator must be greater-than 0"
      );

      try {
        return new BigDecimal(this.numerator()).divide(new BigDecimal(this.denominator()));
      } catch (ArithmeticException e) {
        return new BigDecimal(this.numerator()).divide(new BigDecimal(this.denominator()), MathContext.DECIMAL128);
      }
    }

    @Override
    @Derived
    public boolean equalButMorePrecise(final Ratio other) {
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

    @Override
    @Derived
    public BigInteger multiplyFloor(final BigInteger amount) {
      Objects.requireNonNull(amount);
      return amount.multiply(this.numerator()).divide(this.denominator());
    }

    @Override
    @Derived
    public UnsignedLong multiplyFloorOrZero(final UnsignedLong amount) {
      Objects.requireNonNull(amount);
      BigInteger newValue = multiplyFloor(amount.bigIntegerValue());

      if (FluentCompareTo.is(newValue).greaterThanEqualTo(UnsignedLong.MAX_VALUE.bigIntegerValue())) {
        // Overflow detected. Return 0 because this method is typically used to compute a destination amount, and an
        // overflow using UnsignedLongs would deliver no value. If a non-zero value is desired, prefer multplyFloor
        // instead.
        return UnsignedLong.ZERO;
      } else {
        return UnsignedLong.valueOf(newValue);
      }
    }

    @Override
    @Derived
    public BigInteger multiplyCeil(final BigInteger amount) {
      Objects.requireNonNull(amount);
      boolean isPositive = FluentCompareTo.is(modulo(amount, this)).greaterThan(BigInteger.ZERO);
      if (isPositive) {
        return (amount.multiply(this.numerator()).divide(this.denominator())).add(BigInteger.ONE);
      } else {
        return amount.multiply(this.numerator()).divide(this.denominator());
      }
    }

    @Override
    @Derived
    public UnsignedLong multiplyCeilOrZero(final UnsignedLong amount) {
      Objects.requireNonNull(amount);
      Objects.requireNonNull(amount);
      BigInteger newValue = multiplyCeil(amount.bigIntegerValue());

      if (FluentCompareTo.is(newValue).greaterThan(UnsignedLong.MAX_VALUE.bigIntegerValue())) {
        // Overflow detected. Return 0 because this method is typically used to compute a destination amount, and an
        // overflow using UnsignedLongs would deliver no value. If a non-zero value is desired, prefer multplyFloor
        // instead.
        return UnsignedLong.ZERO;
      } else {
        return UnsignedLong.valueOf(newValue);
      }
    }

    @Override
    @Lazy
    public Optional<Ratio> reciprocal() {
      if (FluentBigInteger.of(this.numerator()).isPositive()) {
        return Optional.of(Ratio.builder()
          .numerator(this.denominator())
          .denominator(this.numerator())
          .build());
      } else {
        return Optional.empty();
      }
    }

    @Override
    @Derived
    public Ratio subtract(final Ratio ratio) {
      Objects.requireNonNull(ratio);
      final BigInteger a = this.numerator().multiply(ratio.denominator())
        .subtract(ratio.numerator().multiply(this.denominator()));
      final BigInteger b = this.denominator().multiply(ratio.denominator());
      return Ratio.builder().numerator(a).denominator(b).build();
    }

    @Override
    @Derived
    public boolean isPositive() {
      return FluentCompareTo.is(this.numerator()).greaterThan(BigInteger.ZERO);
    }

    @Override
    @Derived
    public boolean isNotPositive() {
      return !isPositive();
    }

    @Override
    @Derived
    public boolean isInteger() {
      return isIntegerValue(this.toBigDecimal());
    }

    @Override
    @Derived
    public boolean isPositiveInteger() {
      return isPositive() && isInteger();
    }

    @Override
    @Derived
    public boolean isNegative() {
      return FluentCompareTo.is(this.numerator()).lessThan(BigInteger.ZERO);
    }

    @Override
    @Derived
    public boolean isZero() {
      return FluentCompareTo.is(this.numerator()).equalTo(BigInteger.ZERO);
    }

    @Override
    @Derived
    public int compareTo(final Ratio other) {
      Objects.requireNonNull(other);

      BigInteger first = this.numerator().multiply(other.denominator());
      BigInteger second = this.denominator().multiply(other.numerator());

      return first.compareTo(second);
    }

    @Override
    public String toString() {
      return this.numerator() + "/" + this.denominator() + "[" + (this.toBigDecimal()) + "]";
    }

    @Value.Check
    void check() {
      // Denominator must be positive!
      Preconditions.checkState(
        FluentCompareTo.is(denominator()).greaterThan(BigInteger.ZERO), "Denominator must be greater-than 0"
      );
    }

    /**
     * Compute the result of amount {@code modulo} modulo {@code ratio}.
     *
     * @param amount A {@link BigInteger}.
     * @param ratio  A {@link Ratio}.
     *
     * @return The result of amount {@code modulo} modulo {@code ratio}, as a {@link BigInteger}.
     */
    private static BigInteger modulo(final BigInteger amount, final Ratio ratio) {
      Objects.requireNonNull(amount);
      Objects.requireNonNull(ratio);
      return (amount.multiply(ratio.numerator())).mod(ratio.denominator());
    }

    /**
     * Compute if {@code bd} is a whole integer value.
     *
     * @param bd A {@link BigDecimal}.
     *
     * @return {@code true} if {@code bd} is a whole integer value; {@code false otherwise}.
     */
    private static boolean isIntegerValue(final BigDecimal bd) {
      Objects.requireNonNull(bd);
      return bd.signum() == 0 || bd.scale() <= 0 || bd.stripTrailingZeros().scale() <= 0;
    }
  }

  /**
   * Construct a new {@link Ratio} from the supplied {@code bigDecimal} by scaling the value until it is a whole
   * number.
   *
   * @param bigDecimal A {@link BigDecimal} to turn into a {@link Ratio}.
   *
   * @return The constructed {@link Ratio} that holds two {@link BigInteger} values.
   */
  static Ratio from(final BigDecimal bigDecimal) {
    Objects.requireNonNull(bigDecimal);

    BigDecimal e = BigDecimal.TEN;
    while (!AbstractRatio.isIntegerValue(bigDecimal.multiply(e))) {
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
   *
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
   *
   * @return The constructed {@link Ratio} that holds two {@link BigInteger} values.
   */
  static Ratio from(final UnsignedLong numerator, final UnsignedLong denominator) {
    Objects.requireNonNull(numerator);
    Objects.requireNonNull(denominator);

    return Ratio.from(numerator.bigIntegerValue(), denominator.bigIntegerValue());
  }
}
