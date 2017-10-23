package org.interledger.ilqp;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Defines a <b>point</b> on the liquidity curve, a mapping between an input amount (X), and an
 * output amount (Y).
 */
public interface LiquidityPoint extends Comparable<LiquidityPoint> {

  /**
   * Returns the input amount associated with a point on the liquidity curve.
   * @return A {@link BigInteger} amount.
   */
  BigInteger getInputAmount();

  /**
   * Returns the output amount associated with a point on the liquidity curve.
   * @return A {@link BigInteger} amount.
   */
  BigInteger getOutputAmount();

  /**
   * Helper-method to access a new {@link Builder} instance.
   *
   * @return A {@link Builder}.
   */
  static Builder builder() {
    return new Builder();
  }

  class Builder {

    private BigInteger inputAmount;
    private BigInteger outputAmount;

    /**
     * Sets the input amount into the builder.
     *
     * @param inputAmount An instance of {@link BigInteger}.
     * @return This {@link Builder} instance.
     */
    public Builder inputAmount(BigInteger inputAmount) {
      this.inputAmount = Objects.requireNonNull(inputAmount);
      return this;
    }

    /**
     * Sets the output amount into the builder.
     *
     * @param outputAmount An instance of {@link BigInteger}.
     * @return This {@link Builder} instance.
     */
    public Builder outputAmount(BigInteger outputAmount) {
      this.outputAmount = Objects.requireNonNull(outputAmount);
      return this;
    }

    /**
     * The method that actually constructs a {@link LiquidityPoint} instance.
     *
     * @return An instance of {@link LiquidityPoint}.
     */
    public LiquidityPoint build() {
      return new Builder.Impl(this);
    }

    public static Builder builder() {
      return new Builder();
    }

    /**
     * A concrete implementation of {@link LiquidityPoint}.
     */
    private static class Impl implements LiquidityPoint {

      private final BigInteger inputAmount;
      private final BigInteger outputAmount;

      private Impl(final Builder builder) {
        Objects.requireNonNull(builder);

        this.inputAmount =
            Objects.requireNonNull(builder.inputAmount, "inputAmount must not be null!");

        this.outputAmount =
            Objects.requireNonNull(builder.outputAmount, "outputAmount must not be null!");
      }

      @Override
      public int compareTo(LiquidityPoint other) {
        /* ordering of liquidity points are based on the input amounts */
        return inputAmount.compareTo(other.getInputAmount());
      }

      @Override
      public BigInteger getInputAmount() {
        return this.inputAmount;
      }

      @Override
      public BigInteger getOutputAmount() {
        return this.outputAmount;
      }

      @Override
      public int hashCode() {
        return Objects.hash(inputAmount, outputAmount);
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
          return false;
        }

        Impl impl = (Impl) obj;

        return Objects.equals(inputAmount, impl.inputAmount)
            && Objects.equals(outputAmount, impl.outputAmount);
      }

      @Override
      public String toString() {
        return "LiquidityPoint.Impl{inputAmount=" + inputAmount + ", outputAmount=" + outputAmount
            + "}";
      }
    }
  }
}
