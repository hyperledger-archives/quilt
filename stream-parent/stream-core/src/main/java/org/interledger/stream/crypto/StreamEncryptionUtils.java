package org.interledger.stream.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.StreamException;
import org.interledger.stream.StreamPacket;

/**
 * A utility service for encryption operations required by the STREAM protocol.
 */
public class StreamEncryptionUtils {

  // TODO: If we don't need the shared version, then remove this code.
// TODO: Unit test

//
//  private static StreamEncryptionUtils sharedStreamEncryptionUtils;

  private final CodecContext streamCodecContext;
  private final StreamEncryptionService streamEncryptionService;

  /**
   * Required-args Constructor.
   *
   * @param streamEncryptionService A {@link StreamEncryptionService}.
   * @param streamCodecContext      A {@link CodecContext} that can operate on Stream packets.
   */
  public StreamEncryptionUtils(
    final CodecContext streamCodecContext, final StreamEncryptionService streamEncryptionService
  ) {
    this.streamCodecContext = Objects.requireNonNull(streamCodecContext);
    this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
  }

//  /**
//   * Static accessor for a shared instance of this utility. This function exists merely as a utility to avoid new object
//   * instantiation in high-traffic function paths.
//   *
//   * @return A {@link StreamEncryptionUtils}.
//   */
//  public static StreamEncryptionUtils sharedStreamEncryptionUtils() {
//    if (sharedStreamEncryptionUtils == null) {
//      sharedStreamEncryptionUtils = new StreamEncryptionUtils(new AesGcmStreamEncryptionService());
//    }
//    return sharedStreamEncryptionUtils;
//  }

  // TODO: This is unused, but why?

  /**
   * Convert a {@link StreamPacket} to bytes using the CodecContext and then encrypt it using the supplied {@code
   * sharedSecret}.
   *
   * @param sharedSecret The shared secret known only to this client and the remote STREAM receiver, used to encrypt and
   *                     decrypt STREAM frames and packets sent and received inside of ILPv4 packets sent over the
   *                     Interledger between these two entities (i.e., sender and receiver).
   * @param streamPacket A {@link StreamPacket} to encode into ASN.1 OER and then encrypt into a byte array.
   * @return A byte-array containing the encrypted version of an ASN.1 OER encoded {@link StreamPacket}.
   */
  public byte[] toEncrypted(final SharedSecret sharedSecret, final StreamPacket streamPacket) {
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(streamPacket);

    try {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      streamCodecContext.write(streamPacket, baos);
      final byte[] streamPacketBytes = baos.toByteArray();
      return streamEncryptionService.encrypt(sharedSecret, streamPacketBytes);
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
   * @return The decrypted {@link StreamPacket}.
   */
  public StreamPacket fromEncrypted(final SharedSecret sharedSecret, final byte[] encryptedStreamPacketBytes) {
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(encryptedStreamPacketBytes);

    final byte[] streamPacketBytes = this.streamEncryptionService.decrypt(sharedSecret, encryptedStreamPacketBytes);
    try {
      return streamCodecContext.read(StreamPacket.class, new ByteArrayInputStream(streamPacketBytes));
    } catch (IOException e) {
      throw new StreamException(e.getMessage(), e);
    }
  }

}