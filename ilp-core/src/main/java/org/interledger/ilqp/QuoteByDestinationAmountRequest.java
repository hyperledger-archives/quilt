package org.interledger.ilqp;

import org.interledger.InterledgerAddress;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;

/**
 * A request for a quote that specifies the amount to deliver at the destination address.
 */
public interface QuoteByDestinationAmountRequest extends QuoteRequest {

  @Override
  InterledgerAddress getDestinationAccount();

  /**
   * Returns fixed the amount that will arrive at the receiver.
   * @return A {@link BigInteger} amount
   */
  BigInteger getDestinationAmount();

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
   * A builder for instances of {@link QuoteByDestinationAmountRequest}.
   */
  class Builder {

    private InterledgerAddress destinationAccount;
    private BigInteger destinationAmount;
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
     * Set the destination amount into this builder.
     *
     * @param destinationAmount The source amount value.
     * @return This {@link Builder} instance.
     */
    public Builder destinationAmount(final BigInteger destinationAmount) {
      this.destinationAmount = Objects
          .requireNonNull(destinationAmount, "destinationAmount must not be null!");
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
     * The method that actually constructs a QuoteByDestinationAmountRequest.
     *
     * @return An instance of {@link QuoteByDestinationAmountRequest}
     */
    public QuoteByDestinationAmountRequest build() {
      return new Builder.Impl(this);
    }

    /**
     * A private, immutable implementation of {@link QuoteByDestinationAmountRequest}.
     */
    private static class Impl implements QuoteByDestinationAmountRequest {

      private final InterledgerAddress destinationAccount;
      private final BigInteger destinationAmount;
      private final Duration destinationHoldDuration;

      /**
       * Constructs an instance from the values held in the builder.
       *
       * @param builder A Builder used to construct {@link QuoteByDestinationAmountRequest}
       *                instances.
       */
      private Impl(final Builder builder) {
        Objects.requireNonNull(builder);

        this.destinationAccount = Objects.requireNonNull(builder.destinationAccount,
            "destinationAccount must not be null!");

        this.destinationAmount = Objects
            .requireNonNull(builder.destinationAmount, "destinationAmount must not be null!");
        if (this.destinationAmount.compareTo(BigInteger.ZERO) < 0) {
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
      public BigInteger getDestinationAmount() {
        return this.destinationAmount;
      }

      @Override
      public Duration getDestinationHoldDuration() {
        return this.destinationHoldDuration;
      }

      @Override
      public boolean equals(Object object) {
        if (this == object) {
          return true;
        }
        if (object == null || getClass() != object.getClass()) {
          return false;
        }

        Impl impl = (Impl) object;

        if (!destinationAccount.equals(impl.destinationAccount)) {
          return false;
        }
        if (!destinationAmount.equals(impl.destinationAmount)) {
          return false;
        }
        return destinationHoldDuration.equals(impl.destinationHoldDuration);
      }

      @Override
      public int hashCode() {
        int result = destinationAccount.hashCode();
        result = 31 * result + destinationAmount.hashCode();
        result = 31 * result + destinationHoldDuration.hashCode();
        return result;
      }

      @Override
      public String toString() {
        return "QuoteByDestinationAmountRequest.Impl{"
            + "destinationAccount=" + destinationAccount
            + ", destinationAmount=" + destinationAmount
            + ", destinationHoldDuration=" + destinationHoldDuration
            + '}';
      }
    }
  }
}