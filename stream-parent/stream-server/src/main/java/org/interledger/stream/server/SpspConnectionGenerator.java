package org.interledger.stream.server;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.ImmutableStreamConnectionDetails.Builder;
import org.interledger.stream.StreamConnectionDetails;
import org.interledger.stream.crypto.Random;

import com.google.common.hash.Hashing;

import java.util.Base64;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A {@link ConnectionGenerator} that generates SPSP-compatible connection details.
 */
public class SpspConnectionGenerator implements ConnectionGenerator {

  @Override
  public StreamConnectionDetails generateConnectionDetails(
      final ServerSecretSupplier serverSecretSupplier, final InterledgerAddress receiverAddress
  ) {
    Objects.requireNonNull(serverSecretSupplier);
    Objects.requireNonNull(receiverAddress);

    // base_address + "." + 32-bytes encoded as base64url
    final Builder streamConnectionDetailsBuilder = StreamConnectionDetails.builder();

    final byte[] randomBytes = Random.randBytes(32);
    final byte[] shared_secret = Hashing.hmacSha256(serverSecretSupplier.get()).hashBytes(randomBytes).asBytes();

    final InterledgerAddress destinationAddress = receiverAddress
        .with(Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes));

    final byte[] authTag = Hashing.hmacSha256(shared_secret)
        .hashBytes(destinationAddress.getValue().substring(0, 14).getBytes())
        .asBytes();

    return streamConnectionDetailsBuilder
        .sharedSecret(Base64.getEncoder().encodeToString(shared_secret))
        .destinationAddress(destinationAddress.with(Base64.getUrlEncoder().withoutPadding().encodeToString(authTag)))
        .build();
  }

  public void deriveSecret(final InterledgerAddress receiverAddress) {
    Objects.requireNonNull(receiverAddress);

    final String[] localPart = Stream.of(receiverAddress.getValue().split("."))
        .;


  }

}
