package org.interledger.stream;

/**
 * Defines the meaning of the {@link SendMoneyRequest#amount()} property.
 * @deprecated TODO: Remove
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
