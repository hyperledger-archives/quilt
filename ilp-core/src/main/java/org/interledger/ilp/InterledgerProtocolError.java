package org.interledger.ilp;

import static org.interledger.ilp.InterledgerProtocolError.ErrorCode.ErrorFamily.FINAL;
import static org.interledger.ilp.InterledgerProtocolError.ErrorCode.ErrorFamily.RELATIVE;
import static org.interledger.ilp.InterledgerProtocolError.ErrorCode.ErrorFamily.TEMPORARY;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacket;
import org.interledger.InterledgerRuntimeException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
  default Instant getTriggeredAt() {
    return Instant.now();
  }

  /**
   * Optional error data, provided for debugging purposes.
   *
   * @return The optional error data.
   */
  Optional<byte[]> getData();

  /**
   * Get the default builder.
   *
   * @return a {@link Builder} instance.
   */
  static Builder builder() {
    return new Builder();
  }

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
    return new Builder(interledgerProtocolError).addForwardedByAddress(forwardedByAddress)
        .build();
  }

  /**
   * <p>A builder for immutable instances of {@link InterledgerProtocolError}.</p>
   *
   * <p>NOTE: This builder class is not thread-safe and generally should not be stored in a field or
   * collection, but instead used immediately to create instances of {@link
   * InterledgerProtocolError}.</p>
   */
  class Builder {

    private ErrorCode errorCode;
    private InterledgerAddress triggeredByAddress;
    private List<InterledgerAddress> forwardedByAddresses;
    private Instant triggeredAt;
    private Optional<byte[]> data;

    /**
     * No-args Constructor.
     */
    public Builder() {
      this.forwardedByAddresses = new LinkedList<>();
      data = Optional.empty();
    }

    public Builder(final InterledgerProtocolError interledgerProtocolError) {
      Objects.requireNonNull(interledgerProtocolError);

      this.errorCode = interledgerProtocolError.getErrorCode();
      this.triggeredByAddress = interledgerProtocolError.getTriggeredByAddress();
      // Defensive copy here, just in case the list is taken from an existing
      // InterledgerProtocolError.
      this.forwardedByAddresses = interledgerProtocolError.getForwardedByAddresses().stream()
          .collect(
              Collectors.toList());
      this.triggeredAt = interledgerProtocolError.getTriggeredAt();
      this.data = interledgerProtocolError.getData();
    }

    /**
     * Builder method to actually construct an instance of {@link InterledgerProtocolError} of the
     * data in this builder.
     *
     * @return a new instance of {@link InterledgerProtocolError} built using the data in this
     *         builder.
     */
    public InterledgerProtocolError build() {
      return new Impl(this);
    }

    public Builder errorCode(final ErrorCode errorCode) {
      this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null!");
      return this;
    }

    public Builder triggeredByAddress(final InterledgerAddress triggeredByAddress) {
      this.triggeredByAddress = Objects
          .requireNonNull(triggeredByAddress, "triggeredByAddress must not be null!");
      return this;
    }

    public Builder forwardedByAddresses(final List<InterledgerAddress> forwardedByAddresses) {
      this.forwardedByAddresses = Objects
          .requireNonNull(forwardedByAddresses, "forwardedByAddresses must not be null!");
      return this;
    }

    public Builder addForwardedByAddress(final InterledgerAddress forwardedByAddress) {
      this.forwardedByAddresses
          .add(Objects
              .requireNonNull(forwardedByAddress, "forwardedByAddress must not be null!"));
      return this;
    }


    public Builder triggeredAt(final Instant triggeredAt) {
      this.triggeredAt = Objects.requireNonNull(triggeredAt, "triggeredAt must not be null!");
      return this;
    }

    public Builder data(final byte[] data) {
      Objects.requireNonNull(data, "data must not be null!");
      this.data = Optional.of(data);
      return this;
    }

    /**
     * A private, immutable implementation of {@link InterledgerProtocolError}. To construct an
     * instance of this class, use an instance of {@link Builder}.
     */
    private static final class Impl implements InterledgerProtocolError {

      private final ErrorCode errorCode;
      private final InterledgerAddress triggeredByAddress;
      private final List<InterledgerAddress> forwardedByAddresses;
      private final Instant triggeredAt;
      private final Optional<byte[]> data;

      private Impl(final Builder builder) {
        Objects.requireNonNull(builder);

        this.errorCode = Objects
            .requireNonNull(builder.errorCode, "errorCode must not be null!");
        this.triggeredByAddress = Objects
            .requireNonNull(builder.triggeredByAddress, "triggeredByAddress must not be null!");

        // Disallow the triggeredBy from being included in the forwardedBy. The rationale is that
        // the triggering node should not accidentally add itself to the forwarding addresses.
        // Likewise, if that ever happens with an incoming error, then we should throw an exception.
        builder.forwardedByAddresses.stream()
            .filter(interledgerAddress -> interledgerAddress.equals(builder.triggeredByAddress))
            .findFirst()
            .ifPresent(interledgerAddress -> {
                  // Throw an exception here because if this occurs, it indicates a packet loop, and
                  // we don't want to simply remove the address from the ForwardedBy list and send
                  // the packet on, because doing so would likely mean it will come back to us.
                  throw new IllegalArgumentException(String.format(
                      "TriggeredByAddress \"%s\" was found in the ForwardedByAddresses list, which "
                          + "indicates an Interledger packet loop!",
                      triggeredByAddress.getValue()));
            }
          );

        // Defensively copy the list of addresses so that mutating the builder doesn't affect this.
        this.forwardedByAddresses = Objects.requireNonNull(
            builder.forwardedByAddresses.stream().collect(Collectors.toList()),
            "forwardedByAddresses must not be null!"
        );

        this.triggeredAt = Optional.ofNullable(builder.triggeredAt).orElse(Instant.now());
        this.data = Objects.requireNonNull(builder.data, "data must not be null!");
      }

      @Override
      public ErrorCode getErrorCode() {
        return errorCode;
      }

      @Override
      public InterledgerAddress getTriggeredByAddress() {
        return triggeredByAddress;
      }

      @Override
      public List<InterledgerAddress> getForwardedByAddresses() {
        return forwardedByAddresses;
      }

      @Override
      public Instant getTriggeredAt() {
        return triggeredAt;
      }

      @Override
      public Optional<byte[]> getData() {
        return data;
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

        if (!errorCode.equals(impl.errorCode)) {
          return false;
        }
        if (!triggeredByAddress.equals(impl.triggeredByAddress)) {
          return false;
        }
        if (!forwardedByAddresses.equals(impl.forwardedByAddresses)) {
          return false;
        }
        if (!triggeredAt.equals(impl.triggeredAt)) {
          return false;
        }

        if (data.isPresent() && impl.getData().isPresent()) {
          return Arrays.equals(data.get(), impl.getData().get());
        } else {
          return data.equals(impl.getData());
        }
      }

      @Override
      public int hashCode() {
        int result = errorCode.hashCode();
        result = 31 * result + triggeredByAddress.hashCode();
        result = 31 * result + forwardedByAddresses.hashCode();
        result = 31 * result + triggeredAt.hashCode();
        result = 31 * result + (data.isPresent() ? data.get().hashCode() : data.hashCode());
        return result;
      }

      @Override
      public String toString() {
        return "Impl{"
            + "errorCode="
            + errorCode
            + ", triggeredByAddress="
            + triggeredByAddress
            + ", forwardedByAddresses="
            + forwardedByAddresses
            + ", triggeredAt="
            + triggeredAt
            + ", data="
            + data
            + '}';
      }
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