package org.interledger.stream.receiver;

import static org.interledger.core.fluent.FluentCompareTo.is;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.SharedSecret;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.Denomination;
import org.interledger.stream.StreamException;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.crypto.StreamSharedSecretCrypto;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.frames.StreamMoneyMaxFrame;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * <p>A stateless implementation of {@link StreamReceiver} that does **not** maintain STREAM state, but instead
 * fulfills all incoming packets to collect the money.</p>
 *
 * <p>NOTE: This implementation does not currently support handling data sent via STREAM.</p>
 *
 * <p>Note that, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client,
 * connectors will reject ILP packets that exceed 32kb. This implementation does not overtly check to restrict the size
 * of the data field in any particular {@link InterledgerPreparePacket}, for two reasons. First, this implementation
 * never packs a sufficient number of STREAM frames into a single Prepare packet for this 32kb limit to be an issue;
 * Second, if the ILPv4 RFC ever changes to increase this size limitation, we don't want sender/receiver software to
 * have to be updated across the Interledger.</p>
 */
public class StatelessStreamReceiver implements StreamReceiver {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ServerSecretSupplier serverSecretSupplier;
  private final StreamConnectionGenerator streamConnectionGenerator;
  private final StreamEncryptionService streamEncryptionService;
  private final StreamSharedSecretCrypto streamSharedSecretCrypto;
  private final CodecContext streamCodecContext;

  /**
   * Required-args Constructor.
   *
   * @param serverSecretSupplier      A {@link ServerSecretSupplier}.
   * @param streamConnectionGenerator A {@link StreamConnectionGenerator}.
   * @param streamEncryptionService   A {@link StreamEncryptionService}.
   * @param streamCodecContext        A {@link CodecContext} that can handle Stream encoding and decoding.
   *
   * @deprecated Prefer {@link #StatelessStreamReceiver(ServerSecretSupplier, StreamConnectionGenerator,
   *   StreamSharedSecretCrypto, CodecContext)} instead.
   */
  @Deprecated
  public StatelessStreamReceiver(
    final ServerSecretSupplier serverSecretSupplier,
    final StreamConnectionGenerator streamConnectionGenerator,
    final StreamEncryptionService streamEncryptionService,
    final CodecContext streamCodecContext
  ) {
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier, "serverSecretSupplier must not be null");
    this.streamConnectionGenerator = Objects
      .requireNonNull(streamConnectionGenerator, "connectionGenerator must not be null");

    this.streamEncryptionService = Objects
      .requireNonNull(streamEncryptionService, "streamEncryptionService must not be null");
    this.streamSharedSecretCrypto = null;

    this.streamCodecContext = Objects.requireNonNull(streamCodecContext, "streamCodecContext must not be null");
  }

  /**
   * Required-args Constructor.
   *
   * @param serverSecretSupplier      A {@link ServerSecretSupplier}.
   * @param streamConnectionGenerator A {@link StreamConnectionGenerator}.
   * @param streamSharedSecretCrypto  A {@link StreamSharedSecretCrypto}.
   * @param streamCodecContext        A {@link CodecContext} that can handle Stream encoding and decoding.
   */
  public StatelessStreamReceiver(
    final ServerSecretSupplier serverSecretSupplier,
    final StreamConnectionGenerator streamConnectionGenerator,
    final StreamSharedSecretCrypto streamSharedSecretCrypto,
    final CodecContext streamCodecContext
  ) {
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier, "serverSecretSupplier must not be null");
    this.streamConnectionGenerator = Objects
      .requireNonNull(streamConnectionGenerator, "connectionGenerator must not be null");

    this.streamEncryptionService = null;
    this.streamSharedSecretCrypto = Objects
      .requireNonNull(streamSharedSecretCrypto, "streamSharedSecretCrypto must not be null");

    this.streamCodecContext = Objects.requireNonNull(streamCodecContext, "streamCodecContext must not be null");
  }

  @Override
  public StreamConnectionDetails setupStream(final InterledgerAddress receiverAddress) {
    Objects.requireNonNull(receiverAddress);
    return streamConnectionGenerator.generateConnectionDetails(serverSecretSupplier, receiverAddress);
  }

  @Override
  public InterledgerResponsePacket receiveMoney(
    final InterledgerPreparePacket preparePacket,
    final InterledgerAddress receiverAddress,
    final Denomination denomination
  ) {
    Objects.requireNonNull(preparePacket);
    Objects.requireNonNull(receiverAddress);

    // Will throw if there's an error...
    final SharedSecret deprecatedStreamSharedSecret = this.streamConnectionGenerator
      .deriveSecretFromAddress(serverSecretSupplier, preparePacket.getDestination());
    final StreamSharedSecret streamSharedSecret = StreamSharedSecret.of(deprecatedStreamSharedSecret.key());

    final StreamPacket streamPacket;
    try {
      if (preparePacket.getData().length == 0) {
        return InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT)
          .message("No STREAM packet bytes available to decrypt")
          .triggeredBy(receiverAddress)
          .build();
      }
      // Try to parse the STREAM data from the payload.
      final byte[] streamPacketBytes = this.decryptHelper(streamSharedSecret, preparePacket.getData());
      streamPacket = streamCodecContext.read(StreamPacket.class, new ByteArrayInputStream(streamPacketBytes));
    } catch (Exception e) {
      logger.error(
        "Unable to decrypt packet. preparePacket={} receiverAddress={} error={}",
        preparePacket, receiverAddress, e
      );
      return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT)
        .message("Could not decrypt data")
        .triggeredBy(receiverAddress)
        .build();
    }

    final List<StreamFrame> responseFrames = this.constructResponseFrames(streamPacket, denomination);

    // Generate expectedFulfillment using the shared secret that was pre-negotiated with the sender.
    final InterledgerFulfillment expectedFulfillment
      = StreamPacketUtils.generateFulfillableFulfillment(streamSharedSecret, preparePacket.getData());
    // The packet is fulfillable based upon the condition/expectedFulfillment. However, we need to check the amounts below
    final boolean isFulfillable = this.isFulfillable(preparePacket, expectedFulfillment);

    // Return Fulfill or Reject Packet
    if (isFulfillable && is(preparePacket.getAmount()).greaterThanEqualTo(streamPacket.prepareAmount())) {
      final StreamPacket returnableStreamPacketResponse = StreamPacket.builder()
        .sequence(streamPacket.sequence())
        .interledgerPacketType(InterledgerPacketType.FULFILL)
        .prepareAmount(preparePacket.getAmount())
        .frames(responseFrames)
        .sharedSecret(SharedSecret.of(streamSharedSecret.key()))
        .build();

      logger.debug(
        "Fulfilling prepare packet. preparePacket={} expectedFulfillment={} returnableStreamPacketResponse={}",
        preparePacket, expectedFulfillment, returnableStreamPacketResponse
      );

      try {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streamCodecContext.write(returnableStreamPacketResponse, baos);
        final byte[] returnableStreamPacketBytes = baos.toByteArray();
        final byte[] encryptedReturnableStreamPacketBytes
          = this.encryptHelper(streamSharedSecret, returnableStreamPacketBytes);
        return InterledgerFulfillPacket.builder()
          .fulfillment(expectedFulfillment)
          .data(encryptedReturnableStreamPacketBytes)
          .typedData(returnableStreamPacketResponse)
          .build();

      } catch (IOException e) {
        throw new StreamException(e.getMessage(), e);
      }
    } else {
      // Either the packet was simply not fulfillable, or, the amounts were wrong.
      final StreamPacket returnableStreamPacketResponse = StreamPacket.builder()
        .sequence(streamPacket.sequence())
        .interledgerPacketType(InterledgerPacketType.REJECT)
        .prepareAmount(preparePacket.getAmount())
        .frames(responseFrames)
        .build();

      final String rejectionErrorMessage;
      if (!isFulfillable) {
        logger.debug("Incoming Prepare packet is unfulfillable due to invalid condition/expectedFulfillment mismatch "
          + "or other unknown reasons preparePacket={} expectedFulfillment={}", preparePacket, expectedFulfillment);
        rejectionErrorMessage = "Packet not fulfillable";
      } else {
        logger.debug(
          "Received only: {} when we should have received at least: {}",
          preparePacket.getAmount(), streamPacket.prepareAmount()
        );
        rejectionErrorMessage = "STREAM packet not fulfillable (prepare amount < stream packet prepareAmount)";
      }

      logger.debug(
        "Rejecting Prepare and including encrypted stream packet. preparePacket={} returnableStreamPacketResponse={}",
        preparePacket, returnableStreamPacketResponse
      );

      try {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streamCodecContext.write(returnableStreamPacketResponse, baos);
        final byte[] returnableStreamPacketBytes = baos.toByteArray();
        final byte[] encryptedReturnableStreamPacketBytes
          = this.encryptHelper(streamSharedSecret, returnableStreamPacketBytes);

        return InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
          .message(rejectionErrorMessage)
          .triggeredBy(receiverAddress)
          .data(encryptedReturnableStreamPacketBytes)
          .typedData(returnableStreamPacketResponse)
          .build();
      } catch (IOException e) {
        throw new StreamException(e.getMessage(), e);
      }
    }
  }

  /**
   * Construct the proper Collection of frames for this STREAM response. This method is visible for enhanced test
   * coverage.
   *
   * @param streamPacket A {@link StreamPacket} to inspect when creating response frames.
   * @param denomination The {@link Denomination} of the incoming packet payment.
   *
   * @return A {@link List} of {@link StreamFrame}.
   */
  @VisibleForTesting
  protected List<StreamFrame> constructResponseFrames(
    final StreamPacket streamPacket, final Denomination denomination
  ) {
    Objects.requireNonNull(streamPacket);
    Objects.requireNonNull(denomination);

    final Builder<StreamFrame> responseFrames = ImmutableList.builder();

    if (streamPacket.sequenceIsSafeForSingleSharedSecret()) {
      streamPacket.frames().stream()
        .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.StreamMoney)
        .map($ -> (StreamMoneyFrame) $)
        // Tell the sender the stream can handle lots of money
        .forEach(streamMoneyFrame -> responseFrames.add(StreamMoneyMaxFrame.builder()
          .streamId(streamMoneyFrame.streamId())
          .totalReceived(UnsignedLong.ZERO)
          .receiveMax(UnsignedLong.MAX_VALUE)
          .build()));

      // Add ConnectionNewAddress, but only if the sender sent one. This allows the sender to ask multiple times, if
      // desired.
      streamPacket.frames().stream()
        .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.ConnectionNewAddress)
        .findFirst()
        .map(streamFrame -> responseFrames.add(ConnectionAssetDetailsFrame.builder()
          .sourceDenomination(denomination)
          .build()));
    } else {
      logger.warn("This STREAM Connection's sequence {} was too high for safe encryption. CLOSING the stream!",
        streamPacket.sequence());
      // If the sequence it too high, we should close the Connection.
      responseFrames.add(ConnectionCloseFrame.builder()
        .errorCode(ErrorCodes.ProtocolViolation)
        .errorMessage("Sequence number was to too high for safe encryption")
        .build());
    }

    return responseFrames.build();
  }

  /**
   * Checks to see if a given Prepare packet is fulfillable with the supplied fulfillment.
   *
   * @param preparePacket A {@link InterledgerPreparePacket} to check.
   * @param fulfillment   A published {@link InterledgerFulfillment} to verify the packet against.
   *
   * @return {@code true} if the supplied fulfillment validates the supplied Prepare packet; {@code false} otherwise.
   */
  @VisibleForTesting
  protected boolean isFulfillable(
    final InterledgerPreparePacket preparePacket, final InterledgerFulfillment fulfillment
  ) {
    Objects.requireNonNull(fulfillment);
    Objects.requireNonNull(preparePacket);

    return fulfillment.getCondition().equals(preparePacket.getExecutionCondition());
  }

  /**
   * Decrypt the prepare packet's data using whichever encryption service was passed-in via the constructor.
   *
   * @param streamSharedSecret A {@link StreamSharedSecret}.
   * @param preparePacketData  A byte array containing the data from a prepare packet.
   *
   * @return A byte[] containing the decrypted bytes of a {@link StreamPacket}.
   */
  @VisibleForTesting
  protected byte[] decryptHelper(final StreamSharedSecret streamSharedSecret, byte[] preparePacketData) {
    Objects.requireNonNull(streamSharedSecret);
    Objects.requireNonNull(preparePacketData);

    if (this.streamSharedSecretCrypto != null) {
      return streamSharedSecretCrypto.decrypt(streamSharedSecret, preparePacketData);
    } else if (this.streamEncryptionService != null) {
      return streamEncryptionService.decrypt(SharedSecret.of(streamSharedSecret.key()), preparePacketData);
    } else {
      throw new IllegalStateException(
        "One of either streamSharedSecretCrypto or streamEncryptionService must be non-null"
      );
    }
  }

  /**
   * Decrypt the prepare packet's data using whichever encryption service was passed-in via the constructor.
   *
   * @param streamSharedSecret           A {@link StreamSharedSecret}.
   * @param unencryptedStreamPacketBytes A byte array containing the unencrypted bytes of a Stream packet.
   *
   * @return A byte[] containing the decrypted bytes of a {@link StreamPacket}.
   */
  @VisibleForTesting
  protected byte[] encryptHelper(final StreamSharedSecret streamSharedSecret, byte[] unencryptedStreamPacketBytes) {
    Objects.requireNonNull(streamSharedSecret);
    Objects.requireNonNull(unencryptedStreamPacketBytes);

    if (this.streamSharedSecretCrypto != null) {
      return this.streamSharedSecretCrypto.encrypt(streamSharedSecret, unencryptedStreamPacketBytes);
    } else if (this.streamEncryptionService != null) {
      return this.streamEncryptionService
        .encrypt(SharedSecret.of(streamSharedSecret.key()), unencryptedStreamPacketBytes);
    } else {
      throw new IllegalStateException(
        "One of either streamSharedSecretCrypto or streamEncryptionService must be non-null"
      );
    }
  }
}
