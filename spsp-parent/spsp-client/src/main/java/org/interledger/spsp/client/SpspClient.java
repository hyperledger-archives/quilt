package org.interledger.spsp.client;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;

public interface SpspClient {

  StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer) throws InvalidReceiverException;


}
