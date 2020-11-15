package org.interledger.stream;

/**
 * Defines the meaning of the {@link SendMoneyRequest#amount()} property.
 *
 * @deprecated This class will be removed in a future version in-favor of ILP Pay functionality.
 */
@Deprecated
public enum SenderAmountMode {

  /**
   * The amount to send is denominated in the sender's units.
   */
  SENDER_AMOUNT,

  /**
   * The amount to send is denominated in the receiver's units.
   */
  RECEIVER_AMOUNT
}
