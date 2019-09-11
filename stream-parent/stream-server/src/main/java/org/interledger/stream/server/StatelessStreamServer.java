package org.interledger.stream.server;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.StreamConnectionDetails;
import org.interledger.stream.StreamException;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.crypto.StreamEncryptionService;
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
 * <p>A stateless implementation of {@link StreamServer} that does **not** maintain STREAM state, but instead fulfills
 * all incoming packets to collect the money.</p>
 *
 * <p>NOTE: This implementation does not currently support handling data sent via STREAM.</p>
 */
public class StatelessStreamServer implements StreamServer {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ServerSecretSupplier serverSecretSupplier;
  private final ConnectionGenerator connectionGenerator;
  private final StreamEncryptionService streamEncryptionService;
  private final CodecContext streamCodecContext;

  public StatelessStreamServer(
      final ServerSecretSupplier serverSecretSupplier, final ConnectionGenerator connectionGenerator,
      final StreamEncryptionService streamEncryptionService, final CodecContext streamCodecContext
  ) {
    this.serverSecretSupplier = Objects.requireNonNull(serverSecretSupplier, "serverSecretSupplier must not be null");
    this.connectionGenerator = Objects.requireNonNull(connectionGenerator, "connectionGenerator must not be null");
    this.streamEncryptionService = Objects
        .requireNonNull(streamEncryptionService, "streamEncryptionService must not be null");
    this.streamCodecContext = Objects.requireNonNull(streamCodecContext, "streamCodecContext must not be null");
  }

  @Override
  public StreamConnectionDetails setupStream(final InterledgerAddress receiverAddress) {
    Objects.requireNonNull(receiverAddress);
    return connectionGenerator.generateConnectionDetails(serverSecretSupplier, receiverAddress);
  }

  /**
   * @param preparePacket The actual {@link InterledgerPreparePacket} with a {@link InterledgerPreparePacket#getDestination()}
   *                      that includes information that could only have been created by this receiver.
   * @param clientAddress A {@link InterledgerAddress} of the account this packet should be delivered to.
   */
  @Override
  public InterledgerResponsePacket receiveMoney(
      final InterledgerPreparePacket preparePacket, final InterledgerAddress clientAddress
  ) {
    Objects.requireNonNull(preparePacket);
    Objects.requireNonNull(clientAddress);

    // Will throw if there's an error...
    this.connectionGenerator.deriveSecretFromAddress(serverSecretSupplier, preparePacket.getDestination());

    // Generate fulfillment using the shared secret that was pre-negotiated with the sender.
    final InterledgerFulfillment fulfillment = StreamUtils
        .generatedFulfillableFulfillment(serverSecretSupplier.get(), preparePacket.getData());
    final boolean isFulfillable = fulfillment.getCondition().equals(preparePacket.getExecutionCondition());

    // Try to parse the STREAM data from the payload.
    final byte[] streamPacketBytes = streamEncryptionService
        .decrypt(serverSecretSupplier.get(), preparePacket.getData());
    final StreamPacket streamPacket;
    try {
      streamPacket = streamCodecContext.read(StreamPacket.class, new ByteArrayInputStream(streamPacketBytes));
    } catch (IOException e) {
      logger.error(
          "Unable to decrypt packet. preparePacket={} clientAddress={} error={}",
          preparePacket, clientAddress, e
      );
      return InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT)
          .message("Could not decrypt data")
          .triggeredBy(clientAddress)
          .build();
    }

    final Builder<StreamFrame> responseFrames = ImmutableList.builder();

    // TODO send asset code and scale back to sender also

    // Handle STREAM frames
    streamPacket.frames().stream()
        .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.StreamMoney)
        .map($ -> (StreamMoneyFrame) $)
        // Tell the sender the stream can handle lots of money
        .forEach(streamMoneyFrame -> responseFrames.add(StreamMoneyMaxFrame.builder()
            .streamId(streamMoneyFrame.streamId())
            .totalReceived(UnsignedLong.ZERO)
            .receiveMax(UnsignedLong.MAX_VALUE)
            .build()
        ));

    // Return Fulfill or Reject Packet
    if (isFulfillable && preparePacket.getAmount().compareTo(streamPacket.prepareAmount().bigIntegerValue()) >= 0) {
      final StreamPacket returnableStreamPacketResponse = StreamPacket.builder()
          .sequence(streamPacket.sequence())
          .interledgerPacketType(InterledgerPacketType.FULFILL)
          .prepareAmount(UnsignedLong.valueOf(preparePacket.getAmount()))
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
            .encrypt(serverSecretSupplier.get(), returnableStreamPacketBytes);

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
          .prepareAmount(UnsignedLong.valueOf(preparePacket.getAmount()))
          .frames(responseFrames.build())
          .build();

      if (isFulfillable) {
        logger.debug("Packet is unfulfillable. preparePacket={}", preparePacket);
      } else if (
          preparePacket.getAmount().compareTo(streamPacket.prepareAmount().bigIntegerValue()) < 0
      ) {
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
            .encrypt(serverSecretSupplier.get(), returnableStreamPacketBytes);

        return InterledgerRejectPacket.builder()
            .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
            .message("STREAM packet not fulfillable (prepare amount < stream packet amount)")
            .triggeredBy(clientAddress)
            .data(encryptedReturnableStreamPacketBytes)
            .build();

      } catch (IOException e) {
        throw new StreamException(e.getMessage(), e);
      }
    }
  }
}
