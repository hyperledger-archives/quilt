package org.interledger.stream.receiver;

import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.ImmutableStreamConnectionDetails.Builder;
import org.interledger.spsp.SharedSecret;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.StreamException;
import org.interledger.stream.crypto.Random;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A {@link StreamConnectionGenerator} that generates SPSP-compatible connection details.
 */
public class SpspStreamConnectionGenerator implements StreamConnectionGenerator {

  private static final Charset US_ASCII = StandardCharsets.US_ASCII;
  private static final byte[] STREAM_SERVER_SECRET_GENERATOR = "ilp_stream_secret_generator".getBytes(US_ASCII);

  @Override
  public StreamConnectionDetails generateConnectionDetails(
      final ServerSecretSupplier serverSecretSupplier, final InterledgerAddress receiverAddress
  ) {
    Objects.requireNonNull(serverSecretSupplier, "serverSecretSupplier must not be null");
    Objects.requireNonNull(receiverAddress, "receiverAddress must not be null");
    Preconditions.checkArgument(serverSecretSupplier.get().length >= 32, "Server secret must be 32 bytes");

    // base_address + "." + 32-bytes encoded as base64url
    final Builder streamConnectionDetailsBuilder = StreamConnectionDetails.builder();

    final byte[] randomBytes = Random.randBytes(18);
    final byte[] sharedSecret = Hashing
        .hmacSha256(secretGenerator(serverSecretSupplier))
        .hashBytes(randomBytes)
        .asBytes();

    final String destinationAddressPrecursor =
        receiverAddress.with(Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)).getValue();

    // The authTag is the first 14 bytes of the HmacSha256 of destinationAddressPrecursor
    final byte[] authTag = Arrays.copyOf(
        Hashing.hmacSha256(sharedSecret)
            .hashBytes(destinationAddressPrecursor.getBytes(US_ASCII))
            .asBytes(),
        14
    );

    final InterledgerAddress destinationAddress = InterledgerAddress.of(
        destinationAddressPrecursor + Base64.getUrlEncoder().withoutPadding().encodeToString(authTag)
    );

    return streamConnectionDetailsBuilder
        .sharedSecret(SharedSecret.of(sharedSecret))
        .destinationAddress(destinationAddress)
        .build();
  }

  @Override
  public byte[] deriveSecretFromAddress(
      final ServerSecretSupplier serverSecretSupplier, final InterledgerAddress receiverAddress
  ) {
    Objects.requireNonNull(receiverAddress);

    final String receiverAddressAsString = receiverAddress.getValue();
    final String localPart = receiverAddressAsString.substring(receiverAddressAsString.lastIndexOf(".") + 1);

    final byte[] localPartBytes = Base64.getUrlDecoder().decode(localPart);
    if (localPartBytes.length != 32) {
      throw new StreamException(
          String.format("Invalid Receiver Address (should have been 32 byte long): %s", receiverAddress));
    }

    // Bytes 0 through 17
    final byte[] randomBytes = Arrays.copyOf(localPartBytes, 18);
    final byte[] sharedSecret = Hashing.hmacSha256(secretGenerator(serverSecretSupplier)).hashBytes(randomBytes)
        .asBytes();
    // Bytes 18 through 31
    final byte[] authTag = Arrays.copyOfRange(localPartBytes, 18, localPartBytes.length);

    // The Address without the final 18 bytes.
    String addressWithoutBytes = receiverAddressAsString.substring(0, receiverAddressAsString.length() - 19);
    byte[] derivedAuthTag = Hashing.hmacSha256(sharedSecret)
        .hashBytes(addressWithoutBytes.getBytes(US_ASCII)).asBytes();
    derivedAuthTag = Arrays.copyOf(derivedAuthTag, 14);

    if (!Arrays.equals(derivedAuthTag, authTag)) {
      throw new StreamException("Invalid Receiver Address (derived AuthTag failure)!");
    }

    return sharedSecret;
  }

  /**
   * Helper method to compute HmacSha256 on {@link #STREAM_SERVER_SECRET_GENERATOR}.
   *
   * @param serverSecretSupplier A {@link Supplier} for this node's main secret, which is the root seed for all derived
   *                             secrets provided by this node.
   *
   * @return A secret derived from a primary secret.
   */
  private byte[] secretGenerator(final ServerSecretSupplier serverSecretSupplier) {
    Objects.requireNonNull(serverSecretSupplier);
    return Hashing.hmacSha256(serverSecretSupplier.get()).hashBytes(STREAM_SERVER_SECRET_GENERATOR).asBytes();
  }
}
