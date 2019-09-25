package org.interledger.spsp.client;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;

/**
 * Client operations related to SPSP.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md"
 */
public interface SpspClient {

  String ACCEPT_SPSP_JSON = "application/spsp4+json, application/spsp+json";

  /**
   * Calls SPSP endpoint to obtain address and secret needed to setup a payment (for the given pointer).
   * Typically used for setting up a STREAM connection.
   * @see "https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md#query-get-spsp-endpoint"
   * @param paymentPointer payment pointer to query on
   * @return stream connection details
   * @throws InvalidReceiverClientException thrown if the payment pointer does not point to a valid account
   */
  StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
      throws InvalidReceiverClientException;

}
