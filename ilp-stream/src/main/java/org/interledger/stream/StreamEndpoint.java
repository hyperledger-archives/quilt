package org.interledger.stream;

import org.interledger.core.InterledgerAddress;

import com.google.common.primitives.UnsignedLong;

public interface StreamEndpoint {

  /**
   * Send money to a remote destination address.
   *
   * @param sharedSecret       A shared secret held only between this client and the receiver.
   * @param destinationAddress An {@link InterledgerAddress} for the receiver of this money.
   * @param amount             An {@link UnsignedLong} representing the amount of units to send to the receiver.
   */
  void sendMoney(
      final byte[] sharedSecret,
      final InterledgerAddress destinationAddress,
      final UnsignedLong amount
  );

  void sendData();

}
