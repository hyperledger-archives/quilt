package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;

import java.util.Optional;

/**
 * Defines how to track a payment while considering the amount sent vs amount received, allowing room for
 * path-exchange-rate fluctuations and implementation-defined rules relating to whether or not to continue a payment.
 */
public interface PaymentTracker<T extends SenderAmountMode> {

  /**
   * The original amount of total units to send as part of a Stream payment.
   *
   * @return An {@link UnsignedLong} containing the original amount to send.
   */
  UnsignedLong getOriginalAmount();

  /**
   * Returns the {@link SenderAmountMode} for this payment tracker, used to indicate the type of units that {@link
   * #getOriginalAmount()} represents. For example, 20 units in {@link SenderAmountMode#SENDER_AMOUNT} mode indicates a
   * request to send 20 of the sender's units, with the receiver's units potentially varying depending on
   * path-exchange-rate. Conversely, 20 units in {@link SenderAmountMode#RECEIVER_AMOUNT} mode indicates a request to
   * send as many sender units as it takes for the receiver to receive 20 units, depending on path-exchange-rate.
   *
   * @return A {@link SenderAmountMode} that indicates the meaning of {@link #getOriginalAmount()}.
   */
  T getOriginalAmountMode();

  /**
   * The current number of units that are still outstanding from {@link #getOriginalAmount()}. This value will reduce as
   * more packetized Stream payments are delivered until there is no more left to send.
   *
   * @return An {@link UnsignedLong} that indicates the number of unit left to send.
   */
  UnsignedLong getOriginalAmountLeft();

  /**
   * The current number of the Stream sender's units that have been delivered to the Stream receiver.
   *
   * @return An {@link UnsignedLong} that indicates the number of unit already sent.
   */
  UnsignedLong getDeliveredAmountInSenderUnits();

  /**
   * The current number of the Stream receiver's units that have been delivered to the Stream receiver.
   *
   * @return An {@link UnsignedLong} that indicates the number of unit already sent.
   */
  UnsignedLong getDeliveredAmountInReceiverUnits();

  /**
   * Computes the values that should be used to Prepare a Stream packet, based upon the supplied inputs.
   *
   * @param congestionLimit      An {@link UnsignedLong} representing the number of units that the congestion controller
   *                             has indicated should be sent.
   * @param senderDenomination   A {@link Denomination} representing the asset information for the sender, in order to
   *                             compute path-exchange-rates.
   * @param receiverDenomination A {@link Denomination} representing the asset information for the receiver, in order to
   *                             compute path-exchange-rates.
   *
   * @return A {@link PrepareAmounts} object that contains the correct information to use in the next ILPv4 and Stream
   *     packets as part of a packetized payment flow in STREAM.
   */
  PrepareAmounts getSendPacketAmounts(
      UnsignedLong congestionLimit, Denomination senderDenomination, Optional<Denomination> receiverDenomination
  );

  /**
   * Authorize a packetized payment by properly subtracting the values in {@code prepareAmounts} from any internal
   * tracking values.
   *
   * @param prepareAmounts A {@link PrepareAmounts} that contains discrete ILPv4 and Stream packet amounts for an
   *                       individual Prepare request.
   */
  boolean auth(PrepareAmounts prepareAmounts);

  /**
   * Rollback a packetized payment by properly adding back the values in {@code prepareAmounts} from any internal
   * tracking values.
   *
   * @param prepareAmounts A {@link PrepareAmounts} that contains discrete ILPv4 and Stream packet amounts for an
   *                       individual Prepare request.
   */
  void rollback(PrepareAmounts prepareAmounts, boolean packetRejected);

  /**
   * Finalize and commit any tracking values for a packetized payment using the values in {@code prepareAmounts}.
   *
   * @param prepareAmounts A {@link PrepareAmounts} that contains discrete ILPv4 and Stream packet amounts for an
   *                       individual Prepare request.
   */
  void commit(PrepareAmounts prepareAmounts, UnsignedLong deliveredAmount);

  /**
   * Determine if there is more value to send for a given Stream Payment.
   *
   * @return {@code true} if there is still more value to send; {@code false}.
   */
  boolean moreToSend();

  /**
   * Determine if the full value of the Stream payment was successfully delivered to the receiver.
   *
   * @return {@code true} if payment was successful; {@code false}.
   */
  default boolean successful() {
    return !moreToSend();
  }
}
