package org.interledger.stream;

import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.SharedSecret;
import org.interledger.fx.Denomination;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.crypto.Random;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCode;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyMaxFrame;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilities for helping interact with Stream packets.
 */
@SuppressWarnings("UnstableApiUsage")
public class StreamPacketUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamPacketUtils.class);

  /**
   * A {@link Set} of all frames that can be used to indicate that a STREAM should be closed.
   */
  public static final Set<StreamFrameType> CLOSING_FRAMES = Sets.newHashSet(
    StreamFrameType.ConnectionClose,
    StreamFrameType.StreamClose
  );

  /**
   * The string "ilp_stream_fulfillment" is encoded as UTF-8 or ASCII (the byte representation is the same with both
   * encodings).
   */
  private static final byte[] ILP_STREAM_FULFILLMENT = "ilp_stream_fulfillment".getBytes(StandardCharsets.UTF_8);

  /**
   * The algorithm name for STREAM HMAC operations.
   */
  private static final String HMAC_SHA256_ALG_NAME = "HmacSHA256";

  /**
   * The default STREAM id.
   */
  public static final UnsignedLong DEFAULT_STREAM_ID = UnsignedLong.ONE;

  /**
   * If the sender does not want the receiver to be able to fulfill the payment (as for an informational quote), they
   * can generate an unfulfillable random condition.
   *
   * @return A {@link InterledgerCondition} that is not fulfillable.
   */
  public static InterledgerCondition unfulfillableCondition() {
    return InterledgerCondition.of(Random.randBytes(32));
  }

  /**
   * <p>If the sender _does_ want the receiver to be able to fulfill the condition, the condition MUST be generated
   * from a fulfillment in the following manner: First, the shared_secret is the cryptographic seed exchanged during
   * Setup. The string "ilp_stream_fulfillment" is encoded as UTF-8 or ASCII (the byte representation is the same with
   * both encodings). Finally, the data is the encrypted STREAM packet.</p>
   *
   * @param sharedSecret The cryptographic seed exchanged during STREAM Setup.
   * @param data         The encrypted STREAM packet in ASN.1 OER bytes.
   *
   * @return An {@link InterledgerFulfillment} that can be used to prove a payment.
   *
   * @deprecated Will be removed once {@link SharedSecret} is removed.
   */
  @Deprecated
  public static InterledgerFulfillment generateFulfillableFulfillment(
    final SharedSecret sharedSecret, final byte[] data
  ) {
    return generateFulfillableFulfillment(StreamSharedSecret.of(sharedSecret.key()), data);
  }

  /**
   * <p>If the sender _does_ want the receiver to be able to fulfill the condition, the condition MUST be generated
   * from a fulfillment in the following manner: First, the shared_secret is the cryptographic seed exchanged during
   * Setup. The string "ilp_stream_fulfillment" is encoded as UTF-8 or ASCII (the byte representation is the same with
   * both encodings). Finally, the data is the encrypted STREAM packet.</p>
   *
   * @param sharedSecret The cryptographic seed exchanged during STREAM Setup.
   * @param data         The encrypted STREAM packet in ASN.1 OER bytes.
   *
   * @return An {@link InterledgerFulfillment} that can be used to prove a payment.
   */
  public static InterledgerFulfillment generateFulfillableFulfillment(
    final StreamSharedSecret sharedSecret, final byte[] data
  ) {
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(data);

    // hmac_key = hmac_sha256(shared_secret, "ilp_stream_fulfillment");
    final SecretKey secretKey = new SecretKeySpec(sharedSecret.key(), HMAC_SHA256_ALG_NAME);
    final byte[] hmacKey = Hashing.hmacSha256(secretKey).hashBytes(ILP_STREAM_FULFILLMENT).asBytes();

    // fulfillment = hmac_sha256(hmac_key, data);
    final SecretKey hmacSecretKey = new SecretKeySpec(hmacKey, HMAC_SHA256_ALG_NAME);
    final byte[] fulfillmentBytes = Hashing.hmacSha256(hmacSecretKey).hashBytes(data).asBytes();

    return InterledgerFulfillment.of(fulfillmentBytes);
  }

  /**
   * A helper function to map from a byte-array of ILP packet binary data to an instance of {@link StreamPacket}.
   *
   * @param ilpPacketData         A byte-array of binary data representing an encrypted {@link StreamPacket}.
   * @param sharedSecret          A {@link SharedSecret} to decrypt the binary data.
   * @param streamEncryptionUtils An instance of {@link StreamEncryptionUtils} to decrypt with.
   *
   * @return An optionally-present instance of {@link StreamPacket}.
   *
   * @deprecated Prefer {@link #mapToStreamPacket(byte[], StreamSharedSecret, StreamPacketEncryptionService)} instead.
   */
  @Deprecated
  public static Optional<StreamPacket> mapToStreamPacket(
    final byte[] ilpPacketData, final SharedSecret sharedSecret, final StreamEncryptionUtils streamEncryptionUtils
  ) {
    Objects.requireNonNull(ilpPacketData);
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(streamEncryptionUtils);

    try {
      return Optional.of(ilpPacketData)
        .filter(data -> data.length > 0) // <-- Ensures we don't even try with empty data payloads.
        .map(data -> streamEncryptionUtils.fromEncrypted(sharedSecret, data));
    } catch (Exception e) {
      LOGGER.error("Unable to decrypt ILP response packet's data. packetData={}", ilpPacketData, e);
      return Optional.empty();
    }
  }

  /**
   * A helper function to map from a byte-array of ILP packet binary data to an instance of {@link StreamPacket}.
   *
   * @param ilpPacketData                 A byte-array of binary data representing an encrypted {@link StreamPacket}.
   * @param streamSharedSecret            A {@link SharedSecret} to decrypt the binary data.
   * @param streamPacketEncryptionService An instance of {@link StreamPacketEncryptionService} to decrypt with.
   *
   * @return An optionally-present instance of {@link StreamPacket}.
   */
  public static Optional<StreamPacket> mapToStreamPacket(
    final byte[] ilpPacketData,
    final StreamSharedSecret streamSharedSecret,
    final StreamPacketEncryptionService streamPacketEncryptionService
  ) {
    Objects.requireNonNull(ilpPacketData);
    Objects.requireNonNull(streamSharedSecret);
    Objects.requireNonNull(streamPacketEncryptionService);

    try {
      return Optional.of(ilpPacketData)
        .filter(data -> data.length > 0) // <-- Ensures we don't even try with empty data payloads.
        .map(data -> streamPacketEncryptionService.fromEncrypted(streamSharedSecret, data));
    } catch (Exception e) {
      LOGGER.error("Unable to decrypt ILP response packet's data. packetData={}", ilpPacketData, e);
      return Optional.empty();
    }
  }

  /**
   * Map an {@link InterledgerResponsePacket} to an optionally-present {@link StreamPacket}.
   *
   * @param responsePacket An {@link InterledgerResponsePacket}.
   *
   * @return An optionally-present {@link StreamPacket}.
   */
  public static Optional<StreamPacket> mapToStreamPacket(final InterledgerResponsePacket responsePacket) {
    Objects.requireNonNull(responsePacket);
    return responsePacket.typedData()
      .filter(typedData -> StreamPacket.class.isAssignableFrom(typedData.getClass()))
      .map($ -> (StreamPacket) $);
  }

  /**
   * Search for the denomination from a {@link ConnectionAssetDetailsFrame}, if that frame is found in the packet.
   *
   * @param streamPacket A {@link StreamPacket}.
   *
   * @return An optionally-present {@link Denomination} if it can be found from a {@link ConnectionAssetDetailsFrame}.
   */
  public static Optional<Denomination> findDenominationFromFrames(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);

    return streamPacket.frames().stream()
      .filter(frame -> frame.streamFrameType().equals(StreamFrameType.ConnectionAssetDetails))
      .map(frame -> ((ConnectionAssetDetailsFrame) frame))
      .map(ConnectionAssetDetailsFrame::sourceDenomination)
      .map(deprecatedDenomination -> org.interledger.fx.Denomination.builder()
        .assetCode(deprecatedDenomination.assetCode())
        .assetScale(deprecatedDenomination.assetScale())
        .build())
      .filter($ -> Denomination.class.isAssignableFrom($.getClass()))
      .map($ -> (Denomination) $)
      .findFirst();
  }

  /**
   * Search for a new connection address from a {@link ConnectionNewAddressFrame}, if that frame is found in the
   * packet.
   *
   * @param streamPacket A {@link StreamPacket}.
   *
   * @return An optionally-present {@link InterledgerAddress} if it can be found from a {@link
   *   ConnectionNewAddressFrame}.
   */
  public static Optional<InterledgerAddress> findNewConnectionAddressFromFrames(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return streamPacket.frames().stream()
      .filter(frame -> frame.streamFrameType().equals(StreamFrameType.ConnectionNewAddress))
      .map(frame -> ((ConnectionNewAddressFrame) frame).sourceAddress())
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

  /**
   * Determine if a {@link StreamPacket} has any frames that would make the Stream eligible to be closed.
   *
   * @param streamPacket A {@link StreamPacket}.
   *
   * @return {@code true} if the stream packet has frames that indicate the stream should be closed; {@code false}
   *   otherwise.
   */
  public static boolean hasStreamCloseFrames(StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return hasStreamCloseFrames(streamPacket.frames());
  }

  /**
   * Determine if any {@link StreamPacket} in a {@link Collection} has a frame that would make the Stream eligible to be
   * closed.
   *
   * @param streamFrames A {@link Collection} of type {@link StreamPacket}.
   *
   * @return {@code true} if the stream packet has frames that indicate the stream should be closed; {@code false}
   *   otherwise.
   */
  public static boolean hasStreamCloseFrames(final Collection<StreamFrame> streamFrames) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .map(StreamFrame::streamFrameType)
      .anyMatch(CLOSING_FRAMES::contains);
  }

  /**
   * Determine if any {@link StreamFrame} in a {@link StreamPacket} would make the Stream eligible to be closed.
   *
   * @param streamPacket A {@link StreamPacket}.
   *
   * @return {@code true} if the stream packet has frames that indicate the stream should be closed; {@code false}
   *   otherwise.
   */
  public static Optional<ConnectionCloseFrame> findConnectionCloseFrame(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return findConnectionCloseFrame(streamPacket.frames());
  }

  /**
   * Find a {@link ConnectionCloseFrame} in a {@link Collection} of frames.
   *
   * @param streamFrames A {@link Collection} of type {@link StreamPacket}.
   *
   * @return {@code true} if the stream packet has frames that indicate the stream should be closed; {@code false}
   *   otherwise.
   */
  public static Optional<ConnectionCloseFrame> findConnectionCloseFrame(final Collection<StreamFrame> streamFrames) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.ConnectionClose)
      .filter(streamFrame -> ConnectionCloseFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (ConnectionCloseFrame) $)
      .findFirst();
  }

  /**
   * Find a {@link StreamCloseFrame} in a {@link StreamPacket}.
   *
   * @param streamPacket A {@link StreamPacket}.
   *
   * @return An optionally-present {@link StreamCloseFrame}.
   */
  public static Optional<StreamCloseFrame> findStreamCloseFrame(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return findStreamCloseFrame(streamPacket.frames());
  }

  /**
   * Find a {@link StreamCloseFrame} in a {@link Collection} of frames.
   *
   * @param streamFrames A {@link Collection} of type {@link StreamFrame}.
   *
   * @return {@code true} if any stream frame has a {@link StreamCloseFrame}; {@code false} otherwise.
   */
  public static Optional<StreamCloseFrame> findStreamCloseFrame(final Collection<StreamFrame> streamFrames) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.StreamClose)
      .filter(streamFrame -> StreamCloseFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (StreamCloseFrame) $)
      .findFirst();
  }

  /**
   * Find a {@link ConnectionAssetDetailsFrame} in a {@link StreamPacket}.
   *
   * @param streamPacket A {@link StreamFrame} to inspect.
   *
   * @return {@code true} if the stream packet has a {@link ConnectionAssetDetailsFrame}; {@code false} otherwise.
   */
  public static Optional<ConnectionAssetDetailsFrame> findConnectionAssetDetailsFrame(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return findConnectionAssetDetailsFrame(streamPacket.frames());
  }

  /**
   * Find a {@link ConnectionAssetDetailsFrame} in a {@link Collection} of stream frames.
   *
   * @param streamFrames A {@link Collection} of type {@link StreamFrame}.
   *
   * @return {@code true} if any stream frame has a {@link ConnectionAssetDetailsFrame}; {@code false} otherwise.
   */
  public static Optional<ConnectionAssetDetailsFrame> findConnectionAssetDetailsFrame(
    final Collection<StreamFrame> streamFrames
  ) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.ConnectionAssetDetails)
      .filter(streamFrame -> ConnectionAssetDetailsFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (ConnectionAssetDetailsFrame) $)
      .findFirst();
  }

  /**
   * Count the number of {@link ConnectionAssetDetailsFrame} in a {@link StreamPacket}.
   *
   * @param streamPacket A {@link StreamPacket}.
   *
   * @return The number of {@link ConnectionAssetDetailsFrame} in the packet.
   */
  public static long countConnectionAssetDetailsFrame(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return streamPacket.frames().stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.ConnectionAssetDetails)
      .filter(streamFrame -> ConnectionAssetDetailsFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (ConnectionAssetDetailsFrame) $)
      .count();
  }

  /**
   * Find a {@link StreamMoneyMaxFrame} in a {@link StreamPacket}.
   *
   * @param streamPacket A {@link StreamFrame} to inspect.
   *
   * @return {@code true} if the stream packet has a {@link StreamMoneyMaxFrame}; {@code false} otherwise.
   */
  public static Collection<StreamMoneyMaxFrame> findStreamMaxMoneyFrames(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return findStreamMaxMoneyFrames(streamPacket.frames());
  }

  /**
   * Find a {@link StreamMoneyMaxFrame} in a {@link Collection} of stream frames.
   *
   * @param streamFrames A {@link Collection} of type {@link StreamFrame}.
   *
   * @return {@code true} if any stream frame has a {@link StreamMoneyMaxFrame}; {@code false} otherwise.
   */
  public static Collection<StreamMoneyMaxFrame> findStreamMaxMoneyFrames(final Collection<StreamFrame> streamFrames) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.StreamMoneyMax)
      .filter(streamFrame -> StreamMoneyMaxFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (StreamMoneyMaxFrame) $)
      .collect(Collectors.toList());
  }

  /**
   * Determine if an {@link InterledgerResponsePacket} authentic (i.e., if it has a {@link StreamPacket} that could be
   * decrypted out of the ILP packet's data payload.
   *
   * @param interledgerResponsePacket A {@link InterledgerResponsePacket} to inspect.
   *
   * @return {@code true} if the response is authentic; {@code false} otherwise.
   */
  public static boolean hasAuthenticStreamPacket(final InterledgerResponsePacket interledgerResponsePacket) {
    Objects.requireNonNull(interledgerResponsePacket);

    return StreamPacketUtils.mapToStreamPacket(interledgerResponsePacket).isPresent();
  }

  /**
   * Construct a Prepare packet with a Stream packet inside that will close the Stream.
   *
   * @param streamConnection              A {@link StreamConnection}/
   * @param streamPacketEncryptionService A {@link StreamPacketEncryptionService}.
   *
   * @return An {@link InterledgerPreparePacket} that can be sent to close the stream.
   */
  public static InterledgerPreparePacket constructPacketToCloseStream(
    final StreamConnection streamConnection,
    final StreamPacketEncryptionService streamPacketEncryptionService,
    final ErrorCode errorCode
  ) {
    Objects.requireNonNull(streamConnection);
    Objects.requireNonNull(streamPacketEncryptionService);
    Objects.requireNonNull(errorCode);

    final StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      // Per IL-RFC-29, this is the min amount the receiver should accept. We expect this packet to reject, so setting
      // it to 0 is fine.
      .prepareAmount(UnsignedLong.ZERO)
      .sequence(UnsignedLong.valueOf(streamConnection.nextSequence().longValue()))
      .addFrames(
        StreamCloseFrame.builder()
          .streamId(DEFAULT_STREAM_ID)
          .errorCode(errorCode)
          .build(),
        ConnectionCloseFrame.builder()
          .errorCode(errorCode)
          .build()
      )
      .build();

    final byte[] streamPacketData = streamPacketEncryptionService
      .toEncrypted(streamConnection.getStreamSharedSecret(), streamPacket);

    return InterledgerPreparePacket.builder()
      .destination(streamConnection.getDestinationAddress())
      .amount(UnsignedLong.ZERO)
      .executionCondition(StreamPacketUtils.unfulfillableCondition())
      .expiresAt(DateUtils.now().plus(Duration.ofSeconds(60)))
      .data(streamPacketData)
      .typedData(streamPacket)
      .build();
  }
}
