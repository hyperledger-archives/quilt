package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import static org.interledger.core.InterledgerErrorCode.ErrorFamily.FINAL;
import static org.interledger.core.InterledgerErrorCode.ErrorFamily.RELATIVE;
import static org.interledger.core.InterledgerErrorCode.ErrorFamily.TEMPORARY;

import java.util.Objects;

/**
 * <p>Inspired by HTTP Status Codes, ILP error codes are categorized based upon the intended behavior of the caller
 * when they receive the given error.</p>
 *
 * <p>Final Errors: Indicate that the payment is invalid and should not be retried unless the details are
 * changed.</p>
 *
 * <p>Temporary Errors: Indicate a failure on the part of the receiver or an intermediary system that is unexpected
 * or likely to be resolved soon. Senders SHOULD retry the same payment again, possibly after a short delay.</p>
 *
 * <p>Relative Errors: Indicate that the payment did not have enough of a margin in terms of money or time. However,
 * it is impossible to tell whether the sender did not provide enough error margin or the path suddenly became too slow
 * or illiquid. The sender MAY retry the payment with a larger safety margin.</p>
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0003-interledger-protocol/0003-interledger-protocol.md#ilp-error-codes"
 */
public interface InterledgerErrorCode {

  String F00_BAD_REQUEST_CODE = "F00";
  String F01_INVALID_PACKET_CODE = "F01";
  String F02_UNREACHABLE_CODE = "F02";
  String F03_INVALID_AMOUNT_CODE = "F03";
  String F04_INSUFFICIENT_DST_AMOUNT_CODE = "F04";
  String F05_WRONG_CONDITION_CODE = "F05";
  String F06_UNEXPECTED_PAYMENT_CODE = "F06";
  String F07_CANNOT_RECEIVE_CODE = "F07";
  String F08_AMOUNT_TOO_LARGE_CODE = "F08";
  String F99_APPLICATION_ERROR_CODE = "F99";

  String T00_INTERNAL_ERROR_CODE = "T00";
  String T01_PEER_UNREACHABLE_CODE = "T01";
  String T02_PEER_BUSY_CODE = "T02";
  String T03_CONNECTOR_BUSY_CODE = "T03";
  String T04_INSUFFICIENT_LIQUIDITY_CODE = "T04";
  String T05_RATE_LIMITED_CODE = "T05";
  String T99_APPLICATION_ERROR_CODE = "T99";

  String R00_TRANSFER_TIMED_OUT_CODE = "R00";
  String R01_INSUFFICIENT_SOURCE_AMOUNT_CODE = "R01";
  String R02_INSUFFICIENT_TIMEOUT_CODE = "R02";
  String R99_APPLICATION_ERROR_CODE = "R99";

  /**
   * Generic sender error.
   */
  InterledgerErrorCode F00_BAD_REQUEST
      = InterledgerErrorCode.of(F00_BAD_REQUEST_CODE, "BAD REQUEST");

  /**
   * The ILP packet was syntactically invalid.
   */
  InterledgerErrorCode F01_INVALID_PACKET
      = InterledgerErrorCode.of(F01_INVALID_PACKET_CODE, "INVALID PACKET");

  /**
   * There was no way to forward the payment, because the destination ILP address was wrong or the connector does not
   * have a route to the destination.
   */
  InterledgerErrorCode F02_UNREACHABLE
      = InterledgerErrorCode.of(F02_UNREACHABLE_CODE, "UNREACHABLE");

  /**
   * The amount is invalid, for example it contains more digits of precision than are available on the destination
   * ledger or the amount is greater than the total amount of the given asset in existence.
   */
  InterledgerErrorCode F03_INVALID_AMOUNT
      = InterledgerErrorCode.of(F03_INVALID_AMOUNT_CODE, "INVALID AMOUNT");

  /**
   * The receiver deemed the amount insufficient, for example you tried to pay a $100 invoice with $10.
   */
  InterledgerErrorCode F04_INSUFFICIENT_DST_AMOUNT
      = InterledgerErrorCode.of(F04_INSUFFICIENT_DST_AMOUNT_CODE, "INSUFFICIENT DESTINATION AMOUNT");

  /**
   * The receiver generated a different condition and cannot fulfill the payment.
   */
  InterledgerErrorCode F05_WRONG_CONDITION =
      InterledgerErrorCode.of(F05_WRONG_CONDITION_CODE, "WRONG CONDITION");

  /**
   * The receiver was not expecting a payment like this (the data and destination address don't make sense in that
   * combination, for example if the receiver does not understand the transport protocol used).
   */
  InterledgerErrorCode F06_UNEXPECTED_PAYMENT =
      InterledgerErrorCode.of(F06_UNEXPECTED_PAYMENT_CODE, "UNEXPECTED PAYMENT");

  /**
   * The receiver (beneficiary) is unable to accept this payment due to a constraint. For example, the payment would put
   * the receiver above its maximum account balance.
   */
  InterledgerErrorCode F07_CANNOT_RECEIVE
      = InterledgerErrorCode.of(F07_CANNOT_RECEIVE_CODE, "CANNOT RECEIVE");

  /**
   * The packet amount is higher than the maximum a connector is willing to forward. Senders MAY send another packet
   * with a lower amount. Connectors that produce this error SHOULD encode the amount they received and their maximum in
   * the data to help senders determine how much lower the packet amount should be.
   */
  InterledgerErrorCode F08_AMOUNT_TOO_LARGE
      = InterledgerErrorCode.of(F08_AMOUNT_TOO_LARGE_CODE, "AMOUNT TOO LARGE");

  /**
   * Reserved for application layer protocols. Applications MAY use names other than Application Error.
   */
  InterledgerErrorCode F99_APPLICATION_ERROR =
      InterledgerErrorCode.of(F99_APPLICATION_ERROR_CODE, "APPLICATION ERROR");

  /**
   * A generic unexpected exception. This usually indicates a bug or unhandled error case.
   */
  InterledgerErrorCode T00_INTERNAL_ERROR =
      InterledgerErrorCode.of(T00_INTERNAL_ERROR_CODE, "INTERNAL ERROR");

  /**
   * The connector has a route or partial route to the destination but was unable to reach the next connector. Try again
   * later.
   */
  InterledgerErrorCode T01_PEER_UNREACHABLE =
      InterledgerErrorCode.of(T01_PEER_UNREACHABLE_CODE, "PEER UNREACHABLE");

  /**
   * The next connector is rejecting requests due to overloading. If a connector gets this error, they SHOULD retry the
   * payment through a different route or respond to the sender with a T03: Connector Busy error.
   */
  InterledgerErrorCode T02_PEER_BUSY =
      InterledgerErrorCode.of(T02_PEER_BUSY_CODE, "PEER BUSY");

  /**
   * The connector is rejecting requests due to overloading. Try again later.
   */
  InterledgerErrorCode T03_CONNECTOR_BUSY =
      InterledgerErrorCode.of(T03_CONNECTOR_BUSY_CODE, "CONNECTOR BUSY");

  /**
   * The connector would like to fulfill your request, but either the sender or a connector does not currently have
   * sufficient balance or bandwidth. Try again later.
   */
  InterledgerErrorCode T04_INSUFFICIENT_LIQUIDITY =
      InterledgerErrorCode.of(T04_INSUFFICIENT_LIQUIDITY_CODE, "INSUFFICIENT LIQUIDITY");

  /**
   * The sender is sending too many payments and is being rate-limited by a ledger or connector. If a connector gets
   * this error because they are being rate-limited, they SHOULD retry the payment through a different route or respond
   * to the sender with a T03: Connector Busy error.
   */
  InterledgerErrorCode T05_RATE_LIMITED =
      InterledgerErrorCode.of(T05_RATE_LIMITED_CODE, "RATE LIMITED");

  /**
   * Reserved for application layer protocols. Applications MAY use names other than Application Error.
   */
  InterledgerErrorCode T99_APPLICATION_ERROR =
      InterledgerErrorCode.of(T99_APPLICATION_ERROR_CODE, "APPLICATION ERROR");

  /**
   * The transfer timed out, meaning the next party in the chain did not respond. This could be because you set your
   * timeout too low or because something look longer than it should. The sender MAY try again with a higher expiry, but
   * they SHOULD NOT do this indefinitely or a malicious connector could cause them to tie up their money for an
   * unreasonably long time.
   */
  InterledgerErrorCode R00_TRANSFER_TIMED_OUT =
      InterledgerErrorCode.of(R00_TRANSFER_TIMED_OUT_CODE, "TRANSFER TIMED OUT");

  /**
   * The amount received by a connector in the path was too little to forward (zero or less). Either the sender did not
   * send enough money or the exchange rate changed. The sender MAY try again with a higher amount, but they SHOULD NOT
   * do this indefinitely or a malicious connector could steal money from them.
   */
  InterledgerErrorCode R01_INSUFFICIENT_SOURCE_AMOUNT =
      InterledgerErrorCode.of(R01_INSUFFICIENT_SOURCE_AMOUNT_CODE, "INSUFFICIENT SOURCE AMOUNT");

  /**
   * The connector could not forward the payment, because the timeout was too low to subtract its safety margin. The
   * sender MAY try again with a higher expiry, but they SHOULD NOT do this indefinitely or a malicious connector could
   * cause them to tie up their money for an unreasonably long time.
   */
  InterledgerErrorCode R02_INSUFFICIENT_TIMEOUT =
      InterledgerErrorCode.of(R02_INSUFFICIENT_TIMEOUT_CODE, "INSUFFICIENT TIMEOUT");

  /**
   * Reserved for application layer protocols. Applications MAY use names other than Application Error.
   */
  InterledgerErrorCode R99_APPLICATION_ERROR =
      InterledgerErrorCode.of(R99_APPLICATION_ERROR_CODE, "APPLICATION ERROR");

  /**
   * Helper method to construct an instance of {@link InterledgerErrorCode}.
   *
   * @param code The definitive identifier of the error.
   * @param name The name of the error code.
   *
   * @return An {@link InterledgerErrorCode}
   */
  static InterledgerErrorCode of(final String code, final String name) {
    return new Builder().setCode(code).setName(name).build();
  }

  /**
   * Helper method to get an instance of {@link InterledgerErrorCode} from a standard code.
   *
   * @param code The definitive identifier of the error.
   *
   * @return An {@link InterledgerErrorCode}
   */
  static InterledgerErrorCode valueOf(final String code) {
    Objects.requireNonNull(code);

    if (code.equals(InterledgerErrorCode.F00_BAD_REQUEST.getCode())) {
      return InterledgerErrorCode.F00_BAD_REQUEST;
    }

    if (code.equals(InterledgerErrorCode.F01_INVALID_PACKET.getCode())) {
      return InterledgerErrorCode.F01_INVALID_PACKET;
    }

    if (code.equals(InterledgerErrorCode.F02_UNREACHABLE.getCode())) {
      return InterledgerErrorCode.F02_UNREACHABLE;
    }

    if (code.equals(InterledgerErrorCode.F03_INVALID_AMOUNT.getCode())) {
      return InterledgerErrorCode.F03_INVALID_AMOUNT;
    }

    if (code.equals(InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT.getCode())) {
      return InterledgerErrorCode.F04_INSUFFICIENT_DST_AMOUNT;
    }

    if (code.equals(InterledgerErrorCode.F05_WRONG_CONDITION.getCode())) {
      return InterledgerErrorCode.F05_WRONG_CONDITION;
    }

    if (code.equals(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT.getCode())) {
      return InterledgerErrorCode.F06_UNEXPECTED_PAYMENT;
    }

    if (code.equals(InterledgerErrorCode.F07_CANNOT_RECEIVE.getCode())) {
      return InterledgerErrorCode.F07_CANNOT_RECEIVE;
    }

    if (code.equals(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE.getCode())) {
      return InterledgerErrorCode.F08_AMOUNT_TOO_LARGE;
    }

    if (code.equals(InterledgerErrorCode.F99_APPLICATION_ERROR.getCode())) {
      return InterledgerErrorCode.F99_APPLICATION_ERROR;
    }

    if (code.equals(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT.getCode())) {
      return InterledgerErrorCode.R00_TRANSFER_TIMED_OUT;
    }

    if (code.equals(InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT.getCode())) {
      return InterledgerErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT;
    }

    if (code.equals(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT.getCode())) {
      return InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT;
    }

    if (code.equals(InterledgerErrorCode.R99_APPLICATION_ERROR.getCode())) {
      return InterledgerErrorCode.R99_APPLICATION_ERROR;
    }

    if (code.equals(InterledgerErrorCode.T00_INTERNAL_ERROR.getCode())) {
      return InterledgerErrorCode.T00_INTERNAL_ERROR;
    }

    if (code.equals(InterledgerErrorCode.T01_PEER_UNREACHABLE.getCode())) {
      return InterledgerErrorCode.T01_PEER_UNREACHABLE;
    }

    if (code.equals(InterledgerErrorCode.T02_PEER_BUSY.getCode())) {
      return InterledgerErrorCode.T02_PEER_BUSY;
    }

    if (code.equals(InterledgerErrorCode.T03_CONNECTOR_BUSY.getCode())) {
      return InterledgerErrorCode.T03_CONNECTOR_BUSY;
    }

    if (code.equals(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY.getCode())) {
      return InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY;
    }

    if (code.equals(InterledgerErrorCode.T05_RATE_LIMITED.getCode())) {
      return InterledgerErrorCode.T05_RATE_LIMITED;
    }

    if (code.equals(InterledgerErrorCode.T99_APPLICATION_ERROR.getCode())) {
      return InterledgerErrorCode.T99_APPLICATION_ERROR;
    }

    throw new InterledgerRuntimeException("Unknown Error Code.");
  }

  /**
   * Accessor for this error's code, which is the definitive identifier of an Interledger Protocol error. For example,
   * "F00".
   *
   * @return The code of the error.
   */
  String getCode();

  /**
   * <p>Accessor for this error code's name.</p>
   *
   * <p>Implementations SHOULD NOT depend on this name, but should instead depend on the code
   * defined above. This name is primarily provided as a convenience to facilitate debugging by humans. If the name does
   * not match the code, the code is the definitive identifier of the error.</p>
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
   * The family of an {@link InterledgerErrorCode}, used as a grouping semantic, which is determined by the first letter
   * of the error code's code value.
   */
  enum ErrorFamily {

    /**
     * Final errors indicate that the payment is invalid and should not be retried unless the details are changed.
     */
    FINAL('F'),

    /**
     * Temporary errors indicate a failure on the part of the receiver or an intermediary system that is unexpected or
     * likely to be resolved soon. Senders SHOULD retry the same payment again, possibly after a short delay.
     */
    TEMPORARY('T'),

    /**
     * Relative errors indicate that the payment did not have enough of a margin in terms of money or time. However, it
     * is impossible to tell whether the sender did not provide enough error margin or the path suddenly became too slow
     * or illiquid. The sender MAY retry the payment with a larger safety margin.
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

}
