package org.interledger.stream.receiver;

import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.SharedSecret;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.StreamException;

import java.util.function.Supplier;

/**
 * Defines how to generate the details of a STREAM Connection.
 */
public interface StreamConnectionGenerator {

  /**
   * Generate new connection details for a STREAM.
   *
   * @param serverSecretSupplier A {@link Supplier} for the server's secret.
   * @param receiverAddress      The {@link InterledgerAddress} of the receiver.
   *
   * @return A {@link StreamConnectionDetails} that is unique on every call.
   */
  StreamConnectionDetails generateConnectionDetails(
      ServerSecretSupplier serverSecretSupplier, InterledgerAddress receiverAddress
  ) throws StreamException;

  /**
   * Derive the  `shared secret` from {@code receiverAddress}, returning an error if the address has been modified in
   * any way or if the packet was not generated with the same server secret.
   *
   * @param serverSecretSupplier A {@link Supplier} for the server's secret.
   * @param receiverAddress      The {@link InterledgerAddress} of the STREAM-compatible receiver.
   *
   * @return A byte array containing the shared secret used to construct the address.
   */
  SharedSecret deriveSecretFromAddress(ServerSecretSupplier serverSecretSupplier, InterledgerAddress receiverAddress)
      throws StreamException;

}
