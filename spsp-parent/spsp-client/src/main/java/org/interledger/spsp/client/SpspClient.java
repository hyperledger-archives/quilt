package org.interledger.spsp.client;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;

public interface SpspClient {

  String ACCEPT_SPSP_JSON = "application/spsp4+json, application/spsp+json";

  StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer) throws InvalidReceiverException;

}
