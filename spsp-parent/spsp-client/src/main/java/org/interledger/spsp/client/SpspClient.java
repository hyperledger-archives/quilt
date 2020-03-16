package org.interledger.spsp.client;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;

import okhttp3.HttpUrl;

/**
 * Client operations related to SPSP.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md"
 */
public interface SpspClient {

  /**
   * The Java, JS, Rafiki, and Rust SPSP servers all respond properly to this header.
   */
  String APPLICATION_SPSP4_JSON = "application/spsp4+json";

  /**
   * Calls SPSP endpoint to obtain address and secret needed to setup a payment (for the given pointer). Typically used
   * for setting up a STREAM connection.
   *
   * @param paymentPointer payment pointer to query on
   *
   * @return stream connection details
   *
   * @throws InvalidReceiverClientException thrown if the payment pointer does not point to a valid account
   * @see "https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md#query-get-spsp-endpoint"
   */
  StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
      throws InvalidReceiverClientException;

  /**
   * Calls SPSP endpoint to obtain address and secret needed to setup a payment (for the given SPSP url). Typically used
   * for setting up a STREAM connection.
   *
   * @param spspUrl url for the SPSP account endpoint
   *
   * @return stream connection details
   *
   * @throws InvalidReceiverClientException thrown if the payment pointer does not point to a valid account
   * @see "https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md#query-get-spsp-endpoint"
   */
  StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl)
      throws InvalidReceiverClientException;


}
