package org.interledger.ilqp;

import java.util.*;

/**
 * Liquidity curves describe the relationship between input and output amount for a given path
 * between a pair of ledgers.
 * 
 * <p>The curve is expressed as a series of points given as coordinates of the form [inputAmount,
 * outputAmount]. If a sender sends 'inputAmount' units to the connector, the recipient will receive
 * 'outputAmount'. The curve may represent the liquidity through a single connector, or multiple
 * liquidity curves can be combined into one to represent the liquidity through a given path of
 * connectors.
 * 
 * <p>Points are ordered by inputAmount. The inputAmount is strictly increasing from point to point.
 * The outputAmount is monotonically increasing, meaning each successively point must have an equal
 * or greater outputAmount.
 * 
 * <p>The first point represents the minimum amount that can be transacted, while the final point
 * represents the maximum amount that can be transacted.
 * 
 * <p>If a query does not match a point exactly, implementations MUST use linear interpolation. When
 * querying by outputAmount, if multiple points match exactly, the lowest inputAmount of any of
 * these points MUST be returned.
 */
public interface LiquidityCurve {

  public List<LiquidityPoint> getLiquidityPoints();

  /**
   * A builder for instances of {@link LiquidityCurve}.
   */
  class Builder {
    private List<LiquidityPoint> points = new ArrayList<>();

    /**
     * Sets a liquidity curve into this builder, clearing any liquidity points already set.
     * 
     * @param curve An instance of {@link LiquidityCurve}.
     * @return This {@link Builder} instance.
     */
    public Builder liquidityCurve(final LiquidityCurve curve) {
      Objects.requireNonNull(curve);
      this.points.clear();
      this.points.addAll(curve.getLiquidityPoints());
      return this;
    }

    /**
     * Sets an individual liquidity point into the builder to be added to the curve.
     * 
     * @param liquidityPoint An instance of {@link LiquidityPoint}.
     * @return This {@link Builder} instance.
     */
    public Builder liquidityPoint(final LiquidityPoint liquidityPoint) {
      Objects.requireNonNull(liquidityPoint);
      this.points.add(liquidityPoint);
      return this;
    }

    /**
     * The method that actually constructs a {@link LiquidityCurve} instance.
     * 
     * @return An instance of {@link LiquidityCurve}.
     */
    public LiquidityCurve build() {
      return new Builder.Impl(this);
    }

    public static Builder builder() {
      return new Builder();
    }

    /**
     * Concrete implementation of {@link LiquidityCurve}.
     */
    private static class Impl implements LiquidityCurve {
      private final List<LiquidityPoint> curve;

      private Impl(Builder builder) {
        Objects.requireNonNull(builder);

        Objects.requireNonNull(builder.points, "liquiditypoints must not be null!");

        this.curve = new ArrayList<>();

        this.curve.addAll(builder.points);
      }

      @Override
      public List<LiquidityPoint> getLiquidityPoints() {
        return this.curve;
      }

      @Override
      public int hashCode() {
        return Objects.hashCode(curve);
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
        return curve.equals(impl.curve);
      }

      @Override
      public String toString() {
        return "LiquidityCurve.Impl{curve=" + curve + "}";
      }
    }
  }
}
