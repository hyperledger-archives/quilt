package org.interledger.node.exceptions;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;

public class InvalidFulfillmentException extends InterledgerProtocolException {

  public InvalidFulfillmentException(
      InterledgerPreparePacket request, InterledgerFulfillPacket response) {
    super(createRejectMessage(request, response));
  }

  private static InterledgerRejectPacket createRejectMessage(
      InterledgerPreparePacket request, InterledgerFulfillPacket response) {
    return null;
  }

}
