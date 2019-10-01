package org.interledger.stream;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.Wrapped;
import org.interledger.core.Wrapper;
import org.interledger.stream.crypto.SharedSecret;

import com.google.common.hash.Hashing;
import org.immutables.value.Value;

import java.util.Objects;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  @Value.Immutable
  @Wrapped
  static abstract class _StreamConnectionId extends Wrapper<String> {

    /**
     * Constructs a {@link StreamConnectionId} that is derived from from the supplied {@code receiverAddress} and {@code
     * sharedSecret} by computing an HMAC-SHA-256 of the receiver address using the shared-secret as a key so that the
     * actual shared secret isn't hanging around in memory from call to call. In this way the connection id will also be
     * uniquely scoped to a receiver address plus shared secret combination.
     *
     * @param receiverAddress The {@link InterledgerAddress} of the receiver of this STREAM payment.
     * @param sharedSecret    The {@link SharedSecret} used to encrypt and decrypt packets transmitted over this
     *                        connection.
     *
     * @return A {@link StreamConnectionId} constructed from the supplied inputs.
     */
    public static StreamConnectionId from(
        final InterledgerAddress receiverAddress, final SharedSecret sharedSecret
    ) {
      return StreamConnectionId.of(Hashing
          .hmacSha256(Objects.requireNonNull(sharedSecret, "sharedSecret must not be null").key())
          .hashBytes(
              Objects.requireNonNull(receiverAddress, "receiverAddress must not be null").getValue().getBytes())
          .toString()
      );
    }
  }
}
