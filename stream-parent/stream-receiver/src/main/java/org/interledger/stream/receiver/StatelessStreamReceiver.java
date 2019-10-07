package org.interledger.stream.receiver;

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
import org.interledger.stream.StreamUtils;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ErrorCode;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.frames.StreamMoneyMaxFrame;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * <p>A stateless implementation of {@link StreamReceiver} that does **not** maintain STREAM state, but instead
 * fulfills all incoming packets to collect the money.</p>
 *
 * <p>NOTE: This implementation does not currently support handling data sent via STREAM.</p>
 *
 * <p>Note that, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client,
 * connectors will reject ILP packets that exceed 32kb. This implementation does not overtly check to restrict the size
 * of thedatafield in any particular {@link InterledgerPreparePacket}, for two reasons. First, this implementation never
 * packs a sufficient number of STREAM frames into a single Prepare packet for this 32kb limit to be an issue; Second,
 * if the ILPv4 RFC ever changes to increase this size limitation, we don't want sender/receiver software to have to be
 * updated across the Interledger.</p>
 */
public class StatelessStreamReceiver implements StreamReceiver {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ServerSecretSupplier serverSecretSupplier;
  private final StreamConnectionGenerator streamConnectionGenerator;
  private final StreamEncryptionService streamEncryptionService;
  private final CodecContext streamCodecContext;

  public StatelessStreamReceiver(
      final ServerSecretSupplier serverSecretSupplier, final StreamConnectionGenerator streamConnectionGenerator,
      final StreamEncryptionService streamEncryptionService, final CodecContext streamCodecContext
  ) {
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier, "serverSecretSupplier must not be null");
    this.streamConnectionGenerator = Objects
        .requireNonNull(streamConnectionGenerator, "connectionGenerator must not be null");
    this.streamEncryptionService = Objects
        .requireNonNull(streamEncryptionService, "streamEncryptionService must not be null");
    this.streamCodecContext = Objects.requireNonNull(streamCodecContext, "streamCodecContext must not be null");
  }

  @Override
  public StreamConnectionDetails setupStream(final InterledgerAddress receiverAddress) {
    Objects.requireNonNull(receiverAddress);
    return streamConnectionGenerator.generateConnectionDetails(serverSecretSupplier, receiverAddress);
  }

  @Override
  public InterledgerResponsePacket receiveMoney(
      final InterledgerPreparePacket preparePacket, final InterledgerAddress receiverAddress,
      final Denomination denomination
  ) {
    Objects.requireNonNull(preparePacket);
    Objects.requireNonNull(receiverAddress);

    // Will throw if there's an error...
    final SharedSecret spspSharedSecret = this.streamConnectionGenerator
        .deriveSecretFromAddress(serverSecretSupplier, preparePacket.getDestination());
    final SharedSecret streamSharedSecret = SharedSecret.of(spspSharedSecret.key());

    // Try to parse the STREAM data from the payload.
    final byte[] streamPacketBytes = streamEncryptionService.decrypt(streamSharedSecret, preparePacket.getData());
    final StreamPacket streamPacket;
    try {
      streamPacket = streamCodecContext.read(StreamPacket.class, new ByteArrayInputStream(streamPacketBytes));
    } catch (IOException e) {
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
          .errorCode(ErrorCode.ProtocolViolation)
          .errorMessage("Sequence number was to too high for safe encryption")
          .build());
    }

    // Generate fulfillment using the shared secret that was pre-negotiated with the sender.
    final InterledgerFulfillment fulfillment = StreamUtils
        .generatedFulfillableFulfillment(streamSharedSecret, preparePacket.getData());
    final boolean isFulfillable = fulfillment.getCondition().equals(preparePacket.getExecutionCondition());

    // Return Fulfill or Reject Packet
    if (isFulfillable && preparePacket.getAmount().compareTo(streamPacket.prepareAmount()) >= 0) {
      final StreamPacket returnableStreamPacketResponse = StreamPacket.builder()
          .sequence(streamPacket.sequence())
          .interledgerPacketType(InterledgerPacketType.FULFILL)
          .prepareAmount(preparePacket.getAmount())
          .frames(responseFrames.build())
          .build();

      logger.debug(
          "Fulfilling prepare packet. preparePacket={} fulfillment={} returnableStreamPacketResponse={}",
          preparePacket, fulfillment, returnableStreamPacketResponse
      );

      try {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streamCodecContext.write(returnableStreamPacketResponse, baos);
        final byte[] returnableStreamPacketBytes = baos.toByteArray();
        final byte[] encryptedReturnableStreamPacketBytes = streamEncryptionService
            .encrypt(streamSharedSecret, returnableStreamPacketBytes);

        return InterledgerFulfillPacket.builder()
            .fulfillment(fulfillment)
            .data(encryptedReturnableStreamPacketBytes)
            .build();

      } catch (IOException e) {
        throw new StreamException(e.getMessage(), e);
      }
    } else {
      final StreamPacket returnableStreamPacketResponse = StreamPacket.builder()
          .sequence(streamPacket.sequence())
          .interledgerPacketType(InterledgerPacketType.REJECT)
          .prepareAmount(preparePacket.getAmount())
          .frames(responseFrames.build())
          .build();

      if (isFulfillable) {
        logger.debug("Packet is unfulfillable. preparePacket={}", preparePacket);
      } else if (preparePacket.getAmount().compareTo(streamPacket.prepareAmount()) < 0) {
        logger.debug(
            "Received only: {} when we should have received at least: {}",
            preparePacket.getAmount(), streamPacket.prepareAmount()
        );
      }

      logger.debug(
          "Rejecting Prepare and including encrypted stream packet. preparePacket={} returnableStreamPacketResponse={}",
          preparePacket, returnableStreamPacketResponse
      );

      try {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streamCodecContext.write(returnableStreamPacketResponse, baos);
        final byte[] returnableStreamPacketBytes = baos.toByteArray();
        final byte[] encryptedReturnableStreamPacketBytes = streamEncryptionService
            .encrypt(streamSharedSecret, returnableStreamPacketBytes);

        return InterledgerRejectPacket.builder()
            .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
            .message("STREAM packet not fulfillable (prepare amount < stream packet amount)")
            .triggeredBy(receiverAddress)
            .data(encryptedReturnableStreamPacketBytes)
            .build();
      } catch (IOException e) {
        throw new StreamException(e.getMessage(), e);
      }
    }
  }
}
