package org.interledger.stream.server;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.StreamConnectionDetails;

public interface ConnectionGenerator {

  /**
   * Generate new connection details for a STREAM.
   *
   * @param receiverAddress The {@link InterledgerAddress} of the receiver.
   *
   * @return A {@link StreamConnectionDetails} that is unique on every call.
   */
  StreamConnectionDetails generateConnectionDetails(
      ServerSecretSupplier serverSecretSupplier, InterledgerAddress receiverAddress
  );

  /**
   * Derive the  `shared_secret` from a `destination_account`, returning an error if the address has been modified in
   * any way or if the packet was not generated with the same server secret.
   *
   * @param receiverAddress The {@link InterledgerAddress} of the STREAM-compatible receiver.
   */
  void deriveSecret(InterledgerAddress receiverAddress);

}
