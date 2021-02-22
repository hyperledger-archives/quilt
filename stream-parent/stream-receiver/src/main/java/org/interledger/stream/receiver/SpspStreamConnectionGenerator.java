package org.interledger.stream.receiver;

import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.crypto.Random;
import org.interledger.core.SharedSecret;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A {@link StreamConnectionGenerator} that generates SPSP-compatible connection details.
 */
@SuppressWarnings("UnstableApiUsage")
public class SpspStreamConnectionGenerator implements StreamConnectionGenerator {

  private static final Charset US_ASCII = StandardCharsets.US_ASCII;
  // private static final byte[] STREAM_SERVER_SECRET_GENERATOR = "ilp_stream_secret_generator".getBytes(US_ASCII);

  private final byte[] streamServerSecretGenerator;

  /**
   * No-args Constructor.
   */
  public SpspStreamConnectionGenerator() {
    // Note that by default, we are using the same magic bytes as the Javascript implementation but this is not
    // strictly necessary. These magic bytes need to be the same for the server that creates the STREAM details for a
    // given packet and for the server that fulfills those packets, but in the vast majority of cases those two servers
    // will be running the same STREAM implementation so it doesn't matter what this string is. However, for more
    // control, see the required-args Constructor.
    this("ilp_stream_shared_secret");
  }

  /**
   * Required-args constructor.
   *
   * @param streamServerSecretGenerator A set of magic bytes that act as a secret-generator seed for generating SPSP
   *                                    shared secrets. These magic bytes need to be the same for the server that
   *                                    creates the STREAM details for a given packet and for the server that fulfills
   *                                    those packets, but in the vast majority of cases those two servers will be
   *                                    running the same STREAM implementation so it doesn't matter what this string
   *                                    is.
   */
  public SpspStreamConnectionGenerator(final String streamServerSecretGenerator) {
    this.streamServerSecretGenerator = Objects.requireNonNull(streamServerSecretGenerator)
        .getBytes(StandardCharsets.US_ASCII);
  }

  @Override
  public StreamConnectionDetails generateConnectionDetails(
      final ServerSecretSupplier serverSecretSupplier, final InterledgerAddress receiverAddress
  ) {
    Objects.requireNonNull(serverSecretSupplier, "serverSecretSupplier must not be null");
    Objects.requireNonNull(receiverAddress, "receiverAddress must not be null");
    Preconditions.checkArgument(serverSecretSupplier.get().length >= 32, "Server secret must be 32 bytes");

    final byte[] token = Random.randBytes(18);
    final String tokenBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    final InterledgerAddress destinationAddress = receiverAddress.with(tokenBase64);

    // Note the shared-secret is generated from the token's base64-encoded String bytes rather than from the
    // _actual_ Base64-unencoded bytes. E.g., "foo".getBytes() is not the same as Base64.getDecoder().decode("foo")
    final byte[] sharedSecret = Hashing
        .hmacSha256(secretGenerator(serverSecretSupplier))
        .hashBytes(tokenBase64.getBytes(StandardCharsets.US_ASCII))
        .asBytes();

    return StreamConnectionDetails.builder()
        .destinationAddress(destinationAddress)
        .sharedSecret(SharedSecret.of(sharedSecret))
        .build();
  }

  @Override
  public SharedSecret deriveSecretFromAddress(
      final ServerSecretSupplier serverSecretSupplier, final InterledgerAddress receiverAddress
  ) {
    Objects.requireNonNull(receiverAddress);

    final String receiverAddressAsString = receiverAddress.getValue();
    // For Javascript compatibility, the `localpart` is not treated as a base64-encoded string of bytes, but is instead
    // treated simply as US-ASCII bytes.
    final String localPart = receiverAddressAsString.substring(receiverAddressAsString.lastIndexOf(".") + 1);
    final byte[] sharedSecret = Hashing
        .hmacSha256(secretGenerator(serverSecretSupplier))
        .hashBytes(localPart.getBytes(StandardCharsets.US_ASCII))
        .asBytes();
    return SharedSecret.of(sharedSecret);
  }

  /**
   * Helper method to compute HmacSha256 on {@link #streamServerSecretGenerator}.
   *
   * @param serverSecretSupplier A {@link Supplier} for this node's main secret, which is the root seed for all derived
   *                             secrets provided by this node.
   *
   * @return A secret derived from a primary secret.
   */
  private byte[] secretGenerator(final ServerSecretSupplier serverSecretSupplier) {
    Objects.requireNonNull(serverSecretSupplier);
    return Hashing.hmacSha256(serverSecretSupplier.get()).hashBytes(this.streamServerSecretGenerator).asBytes();
  }
}
