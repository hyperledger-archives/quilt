package org.interledger.ilqp;

import org.interledger.InterledgerAddress;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;

/**
 * A request for a quote with a fixed source amount to determine the destination amount.
 */
public interface QuoteBySourceAmountRequest extends QuoteRequest {

  @Override
  InterledgerAddress getDestinationAccount();

  /**
   * Returns the amount the sender wishes to send, denominated in the asset of the source ledger.
   * @return A {@link BigInteger} amount.
   */
  BigInteger getSourceAmount();

  @Override
  Duration getDestinationHoldDuration();

  /**
   * Helper-method to access a new {@link Builder} instance.
   *
   * @return A {@link Builder}.
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for instances of {@link QuoteBySourceAmountRequest}.
   */
  class Builder {

    private InterledgerAddress destinationAccount;
    private BigInteger sourceAmount;
    private Duration destinationHoldDuration;

    public static Builder builder() {
      return new Builder();
    }

    /**
     * Set the destination account address into this builder.
     *
     * @param destinationAccount An instance of {@link InterledgerAddress}.
     * @return This {@link Builder} instance.
     */
    public Builder destinationAccount(
        final InterledgerAddress destinationAccount) {
      this.destinationAccount = Objects.requireNonNull(destinationAccount);
      return this;
    }

    /**
     * Set the source amount into this builder.
     *
     * @param sourceAmount The source amount value.
     * @return This {@link Builder} instance.
     */
    public Builder sourceAmount(final BigInteger sourceAmount) {
      this.sourceAmount = Objects.requireNonNull(sourceAmount);
      return this;
    }

    /**
     * Set the destination hold duration into this builder.
     *
     * @param destinationHoldDuration An instance of {@link Duration}.
     * @return This {@link Builder} instance.
     */
    public Builder destinationHoldDuration(final Duration destinationHoldDuration) {
      this.destinationHoldDuration = Objects.requireNonNull(destinationHoldDuration);
      return this;
    }

    /**
     * The method that actually constructs a QuoteBySourceAmountRequest.
     *
     * @return An instance of {@link QuoteBySourceAmountRequest}.
     */
    public QuoteBySourceAmountRequest build() {
      return new Builder.Impl(this);
    }

    /**
     * A private, immutable implementation of {@link QuoteBySourceAmountRequest}.
     */
    private static class Impl implements QuoteBySourceAmountRequest {

      private final InterledgerAddress destinationAccount;
      private final BigInteger sourceAmount;
      private final Duration destinationHoldDuration;

      /**
       * No-args Constructor.
       */
      private Impl(final Builder builder) {
        Objects.requireNonNull(builder);

        this.destinationAccount = Objects.requireNonNull(builder.destinationAccount,
            "destinationAccount must not be null!");

        this.sourceAmount = Objects
            .requireNonNull(builder.sourceAmount, "sourceAmount must not be null!");
        if (this.sourceAmount.compareTo(BigInteger.ZERO) < 0) {
          throw new IllegalArgumentException("destinationAmount must be at least 0!");
        }

        this.destinationHoldDuration = Objects.requireNonNull(builder.destinationHoldDuration,
            "destinationHoldDuration must not be null!");

      }

      @Override
      public InterledgerAddress getDestinationAccount() {
        return this.destinationAccount;
      }

      @Override
      public BigInteger getSourceAmount() {
        return this.sourceAmount;
      }

      @Override
      public Duration getDestinationHoldDuration() {
        return this.destinationHoldDuration;
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

        if (!destinationAccount.equals(impl.destinationAccount)) {
          return false;
        }
        if (!sourceAmount.equals(impl.sourceAmount)) {
          return false;
        }
        return destinationHoldDuration.equals(impl.destinationHoldDuration);
      }

      @Override
      public int hashCode() {
        int result = destinationAccount.hashCode();
        result = 31 * result + sourceAmount.hashCode();
        result = 31 * result + destinationHoldDuration.hashCode();
        return result;
      }

      @Override
      public String toString() {
        return "QuoteBySourceAmountRequest.Impl{"
            + "destinationAccount=" + destinationAccount
            + ", sourceAmount=" + sourceAmount
            + ", destinationHoldDuration=" + destinationHoldDuration
            + '}';
      }
    }
  }

}