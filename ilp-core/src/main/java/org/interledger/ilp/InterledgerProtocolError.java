package org.interledger.ilp;

import static org.interledger.ilp.InterledgerProtocolError.ErrorCode.ErrorFamily.FINAL;
import static org.interledger.ilp.InterledgerProtocolError.ErrorCode.ErrorFamily.RELATIVE;
import static org.interledger.ilp.InterledgerProtocolError.ErrorCode.ErrorFamily.TEMPORARY;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacket;
import org.interledger.InterledgerRuntimeException;
import org.interledger.annotations.Immutable;

import org.immutables.value.Value;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>Interledger errors may be generated at any point during an Interledger payment.</p>
 *
 * <p>For example, Connectors that are notified that an outgoing transfer was rejected MUST reject
 * the corresponding incoming transfer with the same error.</p>
 *
 * <p>Additionally, Connectors SHOULD include their ILP address in the forwardedBy field in the
 * error. Connectors SHOULD NOT modify errors in any other way.</p>
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0003-interledger-protocol/0003-interledger
 *     -protocol.md#ilp-error-format"
 */
public interface InterledgerProtocolError extends InterledgerPacket {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerProtocolErrorBuilder} instance.
   */
  static InterledgerProtocolErrorBuilder builder() {
    return new InterledgerProtocolErrorBuilder();
  }

  /**
   * The Interledger Error Code for this error.
   *
   * @return An {@link ErrorCode}.
   */
  ErrorCode getErrorCode();

  /**
   * The {@link InterledgerAddress} of the entity that originally emitted the error.
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress getTriggeredByAddress();

  /**
   * ILP addresses of any connectors that relayed the error message.
   *
   * @return A {@link List} of errorFamily {@link InterledgerAddress}.
   */
  List<InterledgerAddress> getForwardedByAddresses();

  /**
   * The time when the error was initially emitted.
   *
   * @return {@link Instant}.
   */
  Instant getTriggeredAt();

  /**
   * Optional error data, provided for debugging purposes.
   *
   * @return The optional error data.
   */
  Optional<byte[]> getData();

  /**
   * <p>Constructs an {@link InterledgerProtocolError} that wraps an existing error and adds a new
   * forwardedBy address to the list.</p>
   *
   * <p>Per IL-RFC-3, when a node receives an Interledger error, it SHOULD not change any of the
   * details about the error, except for adding itself to the forwarded-by list, if desired.</p>
   *
   * @param interledgerProtocolError An existing error that will eventually be forwarded.
   * @param forwardedByAddress       A {@link InterledgerAddress} to be added to the list of
   *                                 forwarders.
   *
   * @return a new instance of {@link InterledgerProtocolError} with forwardedByAddress data
   */
  static InterledgerProtocolError withForwardedAddress(
      final InterledgerProtocolError interledgerProtocolError,
      final InterledgerAddress forwardedByAddress
  ) {
    return new InterledgerProtocolErrorBuilder().from(interledgerProtocolError)
        .addForwardedByAddresses(forwardedByAddress)
        .build();
  }

  @Immutable
  abstract class AbstractInterledgerProtocolError implements InterledgerProtocolError {

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      InterledgerProtocolError impl = (InterledgerProtocolError) object;

      if (!getErrorCode().equals(impl.getErrorCode())) {
        return false;
      }
      if (!getTriggeredByAddress().equals(impl.getTriggeredByAddress())) {
        return false;
      }
      if (!getForwardedByAddresses().equals(impl.getForwardedByAddresses())) {
        return false;
      }
      if (!getTriggeredAt().equals(impl.getTriggeredAt())) {
        return false;
      }

      if (getData().isPresent() && impl.getData().isPresent()) {
        return Arrays.equals(getData().get(), impl.getData().get());
      } else {
        return getData().equals(impl.getData());
      }
    }

    @Override
    public int hashCode() {
      int result = getErrorCode().hashCode();
      result = 31 * result + getTriggeredByAddress().hashCode();
      result = 31 * result + getForwardedByAddresses().hashCode();
      result = 31 * result + getTriggeredAt().hashCode();
      result = 31 * result + (getData().isPresent()
          ? Arrays.hashCode(getData().get()) : getData().hashCode());
      return result;
    }

    @Override
    public String toString() {
      return "InterledgerProtocolError{"
          + "errorCode="
          + getErrorCode()
          + ", triggeredByAddress="
          + getTriggeredByAddress()
          + ", forwardedByAddresses="
          + getForwardedByAddresses()
          + ", triggeredAt="
          + getTriggeredAt()
          + ", data="
          + getData()
          + '}';
    }

    /**
     * Pre check verification that there is not a packet loop.
     */
    @Value.Check
    void check() {

      // Disallow the triggeredBy from being included in the forwardedBy. The rationale is that
      // the triggering node should not accidentally add itself to the forwarding addresses.
      // Likewise, if that ever happens with an incoming error, then we should throw an exception.
      this.getForwardedByAddresses().stream()
          .filter(interledgerAddress -> interledgerAddress.equals(this.getTriggeredByAddress()))
          .findFirst()
          .ifPresent(interledgerAddress -> {
            // Throw an exception here because if this occurs, it indicates a packet loop, and
            // we don't want to simply remove the address from the ForwardedBy list and send
            // the packet on, because doing so would likely mean it will come back to us.
            throw new IllegalArgumentException(String.format(
                "TriggeredByAddress \"%s\" was found in the ForwardedByAddresses list, which "
                    + "indicates an Interledger packet loop!",
                this.getTriggeredByAddress().getValue()));
          });
    }
  }


  /**
   * Inspired by HTTP Status Codes, ILP error codes are categorized based upon the intended behavior
   * of the caller when they receive the given error.
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0003-interledger-protocol/0003-interledger-protocol.md#ilp-error-codes"
   */
  interface ErrorCode {

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
     * Helper method to construct an instance of {@link ErrorCode}.
     * @param code The definitive identifier of the error.
     * @param name The name of the error code.
     * @return An {@link ErrorCode}
     */
    static ErrorCode of(final String code, final String name) {
      return new Builder().setCode(code).setName(name).build();
    }

    /**
     * A builder for constructing instances of {@link ErrorCode}.
     */
    final class Builder {

      private String code;
      private String name;

      public Builder setCode(final String code) {
        this.code = Objects.requireNonNull(code);
        return this;
      }

      public Builder setName(final String name) {
        this.name = Objects.requireNonNull(name);
        return this;
      }

      public ErrorCode build() {
        return new Impl(this);
      }

      /**
       * An internal implementation of {@link ErrorCode}.
       */
      private final class Impl implements ErrorCode {

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

          Impl impl = (Impl) obj;

          return code.equals(impl.code);
        }

        @Override
        public int hashCode() {
          return code.hashCode();
        }

        @Override
        public String toString() {
          return "ErrorCode.Impl{"
              + "code='" + code + '\''
              + ", name='" + name + '\''
              + ", errorFamily=" + errorFamily
              + '}';
        }


      }
    }

    /**
     * The family of an {@link ErrorCode}, used as a grouping semantic, which is determined by the
     * first letter of the error code's code value.
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

    /**
     * Generic sender error.
     */
    ErrorCode F00_BAD_REQUEST = ErrorCode.of("F00", "BAD REQUEST");

    /**
     * The ILP packet was syntactically invalid.
     */
    ErrorCode F01_INVALID_PACKET = ErrorCode.of("F01", "INVALID PACKET");

    /**
     * There was no way to forward the payment, because the destination ILP address was wrong or the
     * connector does not have a route to the destination.
     */
    ErrorCode F02_UNREACHABLE = ErrorCode.of("F02", "UNREACHABLE");

    /**
     * The amount is invalid, for example it contains more digits of precision than are available on
     * the destination ledger or the amount is greater than the total amount of the given asset in
     * existence.
     */
    ErrorCode F03_INVALID_AMOUNT = ErrorCode.of("F03", "INVALID AMOUNT");

    /**
     * The receiver deemed the amount insufficient, for example you tried to pay a $100 invoice with
     * $10.
     */
    ErrorCode F04_INSUFFICIENT_DST_AMOUNT = ErrorCode.of("F04", "INSUFFICIENT DST. AMOUNT");

    /**
     * The receiver generated a different condition and cannot fulfill the payment.
     */
    ErrorCode F05_WRONG_CONDITION = ErrorCode.of("F05", "WRONG CONDITION");

    /**
     * The receiver was not expecting a payment like this = ErrorCode.of(the memo and destination
     * address don't make sense in that combination, for example if the receiver does not understand
     * the transport protocol used)
     */
    ErrorCode F06_UNEXPECTED_PAYMENT = ErrorCode.of("F06", "UNEXPECTED PAYMENT");

    /**
     * The receiver is unable to accept this payment due to a constraint. For example, the payment
     * would put the receiver above its maximum account balance.
     */
    ErrorCode F07_CANNOT_RECEIVE = ErrorCode.of("F07", "CANNOT RECEIVE");

    /**
     * Reserved for application layer protocols. Applications MAY use names other than Application
     * Error.
     */
    ErrorCode F99_APPLICATION_ERROR = ErrorCode.of("F99", "APPLICATION ERROR");

    /**
     * A generic unexpected exception. This usually indicates a bug or unhandled error case.
     */
    ErrorCode T00_INTERNAL_ERROR = ErrorCode.of("T00", "INTERNAL ERROR");

    /**
     * The connector has a route or partial route to the destination but was unable to reach the
     * next ledger. Try again later.
     */
    ErrorCode T01_LEDGER_UNREACHABLE = ErrorCode.of("T01", "LEDGER UNREACHABLE");

    /**
     * The ledger is rejecting requests due to overloading. Try again later.
     */
    ErrorCode T02_LEDGER_BUSY = ErrorCode.of("T02", "LEDGER BUSY");

    /**
     * The connector is rejecting requests due to overloading. Try again later.
     */
    ErrorCode T03_CONNECTOR_BUSY = ErrorCode.of("T03", "CONNECTOR BUSY");

    /**
     * The connector would like to fulfill your request, but it doesn't currently have enough money.
     * Try again later.
     */
    ErrorCode T04_INSUFFICIENT_LIQUIDITY = ErrorCode.of("T04", "INSUFFICIENT LIQUIDITY");

    /**
     * The sender is sending too many payments and is being rate-limited by a ledger or connector.
     * If a connector gets this error because they are being rate-limited, they SHOULD retry the
     * payment through a different route or respond to the sender with a T03: Connector Busy error.
     */
    ErrorCode T05_RATE_LIMITED = ErrorCode.of("T05", "RATE LIMITED");

    /**
     * Reserved for application layer protocols. Applications MAY use names other than Application
     * Error.
     */
    ErrorCode T99_APPLICATION_ERROR = ErrorCode.of("T99", "APPLICATION ERROR");

    /**
     * The transfer timed out, meaning the next party in the chain did not respond. This could be
     * because you set your timeout too low or because something look longer than it should. The
     * sender MAY try again with a higher expiry, but they SHOULD NOT do this indefinitely or a
     * malicious connector could cause them to tie up their money for an unreasonably long time.
     */
    ErrorCode R00_TRANSFER_TIMED_OUT = ErrorCode.of("R00", "TRANSFER TIMED OUT");

    /**
     * Either the sender did not send enough money or the exchange rate changed before the payment
     * was prepared. The sender MAY try again with a higher amount, but they SHOULD NOT do this
     * indefinitely or a malicious connector could steal money from them.
     */
    ErrorCode R01_INSUFFICIENT_SOURCE_AMOUNT = ErrorCode
        .of("R01", "INSUFFICIENT SOURCE AMOUNT");

    /**
     * The connector could not forward the payment, because the timeout was too low to subtract its
     * safety margin. The sender MAY try again with a higher expiry, but they SHOULD NOT do this
     * indefinitely or a malicious connector could cause them to tie up their money for an
     * unreasonably long time.
     */
    ErrorCode R02_INSUFFICIENT_TIMEOUT = ErrorCode.of("R02", "INSUFFICIENT TIMEOUT");

    /**
     * Reserved for application layer protocols. Applications MAY use names other than Application
     * Error.
     */
    ErrorCode R99_APPLICATION_ERROR = ErrorCode.of("R99", "APPLICATION ERROR");

  }

}