package org.interledger.ildcp;

import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;

import org.immutables.value.Value.Derived;

import java.util.Base64;

/**
 * An extension of {@link InterledgerFulfillPacket} that can be used as an IL-DCP response over
 * Interledger.
 */
public interface IldcpResponsePacket extends InterledgerFulfillPacket {

  InterledgerCondition EXECUTION_CONDITION = InterledgerCondition.of(
      Base64.getDecoder().decode("Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=")
  );

  byte[] EMPTY_DATA = new byte[32];

  /**
   * The fulfillment of an ILP packet for IL-DCP is always a 32-byte octet string all filled with
   * zeros.
   */
  @Derived
  default InterledgerFulfillment getFulfillment() {
    return InterledgerFulfillment.of(EMPTY_DATA);
  }

}