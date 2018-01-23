package org.interledger.ilp;

import static org.interledger.ilp.InterledgerErrorCode.ErrorFamily.FINAL;
import static org.interledger.ilp.InterledgerErrorCode.ErrorFamily.RELATIVE;
import static org.interledger.ilp.InterledgerErrorCode.ErrorFamily.TEMPORARY;

import org.interledger.InterledgerRuntimeException;

import java.util.Objects;

/**
 * Inspired by HTTP Status Codes, ILP error codes are categorized based upon the intended behavior
 * of the caller when they receive the given error.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0003-interledger-protocol/0003-interledger-protocol.md#ilp-error-codes"
 */
public interface InterledgerErrorCode {

  /**
   * Generic sender error.
   */
  InterledgerErrorCode F00_BAD_REQUEST =
      InterledgerErrorCode.of("F00", "BAD REQUEST");

  /**
   * The ILP packet was syntactically invalid.
   */
  InterledgerErrorCode F01_INVALID_PACKET =
      InterledgerErrorCode.of("F01", "INVALID PACKET");

  /**
   * There was no way to forward the payment, because the destination ILP address was wrong or the
   * connector does not have a route to the destination.
   */
  InterledgerErrorCode F02_UNREACHABLE =
      InterledgerErrorCode.of("F02", "UNREACHABLE");

  /**
   * The amount is invalid, for example it contains more digits of precision than are available on
   * the destination ledger or the amount is greater than the total amount of the given asset in
   * existence.
   */
  InterledgerErrorCode F03_INVALID_AMOUNT =
      InterledgerErrorCode.of("F03", "INVALID AMOUNT");

  /**
   * The receiver deemed the amount insufficient, for example you tried to pay a $100 invoice with
   * $10.
   */
  InterledgerErrorCode F04_INSUFFICIENT_DST_AMOUNT =
      InterledgerErrorCode.of("F04", "INSUFFICIENT DST. AMOUNT");

  /**
   * The receiver generated a different condition and cannot fulfill the payment.
   */
  InterledgerErrorCode F05_WRONG_CONDITION =
      InterledgerErrorCode.of("F05", "WRONG CONDITION");

  /**
   * The receiver was not expecting a payment like this = ErrorCode.of(the memo and destination
   * address don't make sense in that combination, for example if the receiver does not understand
   * the transport protocol used)
   */
  InterledgerErrorCode F06_UNEXPECTED_PAYMENT =
      InterledgerErrorCode.of("F06", "UNEXPECTED PAYMENT");

  /**
   * The receiver is unable to accept this payment due to a constraint. For example, the payment
   * would put the receiver above its maximum account balance.
   */
  InterledgerErrorCode F07_CANNOT_RECEIVE =
      InterledgerErrorCode.of("F07", "CANNOT RECEIVE");

  /**
   * Reserved for application layer protocols. Applications MAY use names other than Application
   * Error.
   */
  InterledgerErrorCode F99_APPLICATION_ERROR =
      InterledgerErrorCode.of("F99", "APPLICATION ERROR");

  /**
   * A generic unexpected exception. This usually indicates a bug or unhandled error case.
   */
  InterledgerErrorCode T00_INTERNAL_ERROR =
      InterledgerErrorCode.of("T00", "INTERNAL ERROR");

  /**
   * The connector has a route or partial route to the destination but was unable to reach the
   * next ledger. Try again later.
   */
  InterledgerErrorCode T01_LEDGER_UNREACHABLE =
      InterledgerErrorCode.of("T01", "LEDGER UNREACHABLE");

  /**
   * The ledger is rejecting requests due to overloading. Try again later.
   */
  InterledgerErrorCode T02_LEDGER_BUSY =
      InterledgerErrorCode.of("T02", "LEDGER BUSY");

  /**
   * The connector is rejecting requests due to overloading. Try again later.
   */
  InterledgerErrorCode T03_CONNECTOR_BUSY =
      InterledgerErrorCode.of("T03", "CONNECTOR BUSY");

  /**
   * The connector would like to fulfill your request, but it doesn't currently have enough money.
   * Try again later.
   */
  InterledgerErrorCode T04_INSUFFICIENT_LIQUIDITY =
      InterledgerErrorCode.of("T04", "INSUFFICIENT LIQUIDITY");

  /**
   * The sender is sending too many payments and is being rate-limited by a ledger or connector.
   * If a connector gets this error because they are being rate-limited, they SHOULD retry the
   * payment through a different route or respond to the sender with a T03: Connector Busy error.
   */
  InterledgerErrorCode T05_RATE_LIMITED =
      InterledgerErrorCode.of("T05", "RATE LIMITED");

  /**
   * Reserved for application layer protocols. Applications MAY use names other than Application
   * Error.
   */
  InterledgerErrorCode T99_APPLICATION_ERROR =
      InterledgerErrorCode.of("T99", "APPLICATION ERROR");

  /**
   * The transfer timed out, meaning the next party in the chain did not respond. This could be
   * because you set your timeout too low or because something look longer than it should. The
   * sender MAY try again with a higher expiry, but they SHOULD NOT do this indefinitely or a
   * malicious connector could cause them to tie up their money for an unreasonably long time.
   */
  InterledgerErrorCode R00_TRANSFER_TIMED_OUT =
      InterledgerErrorCode.of("R00", "TRANSFER TIMED OUT");

  /**
   * Either the sender did not send enough money or the exchange rate changed before the payment
   * was prepared. The sender MAY try again with a higher amount, but they SHOULD NOT do this
   * indefinitely or a malicious connector could steal money from them.
   */
  InterledgerErrorCode R01_INSUFFICIENT_SOURCE_AMOUNT =
      InterledgerErrorCode.of("R01", "INSUFFICIENT SOURCE AMOUNT");

  /**
   * The connector could not forward the payment, because the timeout was too low to subtract its
   * safety margin. The sender MAY try again with a higher expiry, but they SHOULD NOT do this
   * indefinitely or a malicious connector could cause them to tie up their money for an
   * unreasonably long time.
   */
  InterledgerErrorCode R02_INSUFFICIENT_TIMEOUT =
      InterledgerErrorCode.of("R02", "INSUFFICIENT TIMEOUT");

  /**
   * Reserved for application layer protocols. Applications MAY use names other than Application
   * Error.
   */
  InterledgerErrorCode R99_APPLICATION_ERROR =
      InterledgerErrorCode.of("R99", "APPLICATION ERROR");
  
  
  /**
   * Accessor for this error's code, which is the definitive identifier of an Interledger Protocol
   * error. For example, "F00".
   *
   * @return The code of the error.
   */
  String getCode();

  /**
   * <p>Accessor for this error code's name.</p>
   *
   * <p>Implementations SHOULD NOT depend on this name, but should instead depend on the code
   * defined above. This name is primarily provided as a convenience to facilitate debugging by
   * humans. If the name does not match the code, the code is the definitive identifier of the
   * error.</p>
   *
   * @return The name of the error code.
   */
  String getName();

  /**
   * Accessor for this code's error family.
   *
   * @return A {@link ErrorFamily}.
   */
  ErrorFamily getErrorFamily();


  /**
   * Helper method to construct an instance of {@link InterledgerErrorCode}.
   * @param code The definitive identifier of the error.
   * @param name The name of the error code.
   * @return An {@link InterledgerErrorCode}
   */
  static InterledgerErrorCode of(final String code, final String name) {
    return new Builder().setCode(code).setName(name).build();
  }

  static InterledgerErrorCode valueOf(final String code) {
    //TODO Implement this
    throw new RuntimeException("Not implemented.");
  }

  /**
   * A builder for constructing instances of {@link InterledgerErrorCode}.
   */
  final class Builder {

    private String code;
    private String name;

    public InterledgerErrorCode.Builder setCode(final String code) {
      this.code = Objects.requireNonNull(code);
      return this;
    }

    public InterledgerErrorCode.Builder setName(final String name) {
      this.name = Objects.requireNonNull(name);
      return this;
    }

    public InterledgerErrorCode build() {
      return new InterledgerErrorCode.Builder.Impl(this);
    }

    /**
     * An internal implementation of {@link InterledgerErrorCode}.
     */
    private final class Impl implements InterledgerErrorCode {

      private final String code;
      private final String name;
      private final ErrorFamily errorFamily;

      public Impl(Builder builder) {
        this.code = Objects.requireNonNull(builder.code, "code MUST not be null!")
            .trim();
        this.name = Objects.requireNonNull(builder.name, "name MUST not be null!")
            .trim();

        if (code.length() < 3) {
          throw new InterledgerRuntimeException(
              "Per IL-RFC-3, error code length must be at least 3 characters!");
        }

        // NOTE: Per the R99_APPLICATION_ERROR, applications may use custom names, so no
        // validation should be performed on names.

        switch (code.charAt(0)) {
          case 'F':
            this.errorFamily = FINAL;
            break;
          case 'T':
            this.errorFamily = TEMPORARY;
            break;
          case 'R':
            this.errorFamily = RELATIVE;
            break;
          default:
            throw new IllegalArgumentException(
                "code must start with 'F', 'T' or 'R'.");
        }
      }

      @Override
      public String getCode() {
        return code;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public ErrorFamily getErrorFamily() {
        return errorFamily;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
          return false;
        }

        Builder.Impl impl = (Builder.Impl) obj;

        return code.equals(impl.code);
      }

      @Override
      public int hashCode() {
        return code.hashCode();
      }

      @Override
      public String toString() {
        return "InterledgerErrorCode{"
            + "code='" + code + '\''
            + ", name='" + name + '\''
            + ", errorFamily=" + errorFamily
            + '}';
      }


    }
  }

  /**
   * The family of an {@link InterledgerErrorCode}, used as a grouping semantic, which is determined
   * by the first letter of the error code's code value.
   */
  enum ErrorFamily {

    /**
     * Final errors indicate that the payment is invalid and should not be retried unless the
     * details are changed.
     */
    FINAL('F'),

    /**
     * Temporary errors indicate a failure on the part of the receiver or an intermediary system
     * that is unexpected or likely to be resolved soon. Senders SHOULD retry the same payment
     * again, possibly after a short delay.
     */
    TEMPORARY('T'),

    /**
     * Relative errors indicate that the payment did not have enough of a margin in terms of money
     * or time. However, it is impossible to tell whether the sender did not provide enough error
     * margin or the path suddenly became too slow or illiquid. The sender MAY retry the payment
     * with a larger safety margin.
     */
    RELATIVE('R');

    private final String prefix;

    ErrorFamily(final char prefix) {
      this.prefix = Character.toString(prefix);
    }

    @Override
    public String toString() {
      return prefix;
    }
  }
  
}