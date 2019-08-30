package org.interledger.core;

/**
 * Constants available to all Interledger libraries.
 */
public class InterledgerConstants {

  // An empty fulfillment, used in various scenarios peer-wise scenarios where the condition/fulfillment is
  // unimportant for processing an ILP packet.
  public static final InterledgerFulfillment ALL_ZEROS_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);
  public static final InterledgerCondition ALL_ZEROS_CONDITION = ALL_ZEROS_FULFILLMENT.getCondition();


}
