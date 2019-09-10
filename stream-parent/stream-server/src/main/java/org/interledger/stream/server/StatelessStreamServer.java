package org.interledger.stream.server;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.ImmutableStreamConnectionDetails.Builder;
import org.interledger.stream.StreamConnectionDetails;
import org.interledger.stream.crypto.Random;

import com.google.common.hash.Hashing;

import java.util.Base64;
import java.util.Objects;

/**
 * <p>A stateless implementation of {@link StreamServer} that does **not** maintain STREAM state, but instead fulfills
 * all incoming packets to collect the money.</p>
 *
 * <p>NOTE: This implementation does not currently support handling data sent via STREAM.</p>
 */
public class StatelessStreamServer implements StreamServer {

  final ServerSecretSupplier serverSecretSupplier;

  public StatelessStreamServer(final ServerSecretSupplier serverSecretSupplier) {
    this.serverSecretSupplier = serverSecretSupplier;
  }

  @Override
  public StreamConnectionDetails setupStream(final InterledgerAddress receiverAddress) {
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

  public void rederiveSecret(){

  }

  void receiveMoney() {
  }

}
