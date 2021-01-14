package org.interledger.stream.crypto;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.StreamException;
import org.interledger.stream.StreamPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * A utility service for encryption operations required by the STREAM protocol.
 */
public class StreamPacketEncryptionService {

  // TODO: Unit tests

  private final CodecContext streamCodecContext;
  private final SharedSecretCrypto sharedSecretCrypto;

  /**
   * Required-args Constructor.
   *
   * @param streamCodecContext A {@link CodecContext} that can operate on Stream packets.
   * @param sharedSecretCrypto A {@link SharedSecretCrypto}.
   */
  public StreamPacketEncryptionService(
    final CodecContext streamCodecContext, final SharedSecretCrypto sharedSecretCrypto
  ) {
    this.streamCodecContext = Objects.requireNonNull(streamCodecContext);
    this.sharedSecretCrypto = Objects.requireNonNull(sharedSecretCrypto);
  }

  /**
   * Convert a {@link StreamPacket} to bytes using the CodecContext and then encrypt it using the supplied {@code
   * sharedSecret}.
   *
   * @param sharedSecret The shared secret known only to this client and the remote STREAM receiver, used to encrypt and
   *                     decrypt STREAM frames and packets sent and received inside of ILPv4 packets sent over the
   *                     Interledger between these two entities (i.e., sender and receiver).
   * @param streamPacket A {@link StreamPacket} to encode into ASN.1 OER and then encrypt into a byte array.
   *
   * @return A byte-array containing the encrypted version of an ASN.1 OER encoded {@link StreamPacket}.
   */
  public byte[] toEncrypted(final SharedSecret sharedSecret, final StreamPacket streamPacket) {
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(streamPacket);

    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCodecContext.write(streamPacket, baos);
      final byte[] streamPacketBytes = baos.toByteArray();
      return sharedSecretCrypto.encrypt(sharedSecret, streamPacketBytes);
    } catch (IOException e) {
      throw new StreamException(e.getMessage(), e);
    }
  }

  /**
   * Convert the encrypted bytes of a stream packet into a {@link StreamPacket} using the CodecContext and {@code
   * sharedSecret}.
   *
   * @param sharedSecret               The shared secret known only to this client and the remote STREAM receiver, used
   *                                   to encrypt and decrypt STREAM frames and packets sent and received inside of
   *                                   ILPv4 packets sent over the Interledger between these two entities (i.e., sender
   *                                   and receiver).
   * @param encryptedStreamPacketBytes A byte-array containing an encrypted ASN.1 OER encoded {@link StreamPacket}.
   *
   * @return The decrypted {@link StreamPacket}.
   */
  public StreamPacket fromEncrypted(final SharedSecret sharedSecret, final byte[] encryptedStreamPacketBytes) {
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(encryptedStreamPacketBytes);

    final byte[] streamPacketBytes = this.sharedSecretCrypto.decrypt(sharedSecret, encryptedStreamPacketBytes);
    try {
      return streamCodecContext.read(StreamPacket.class, new ByteArrayInputStream(streamPacketBytes));
    } catch (IOException e) {
      throw new StreamException(e.getMessage(), e);
    }
  }

}