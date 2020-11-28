package org.interledger.stream.receiver.testutils;

import static org.interledger.core.fluent.FluentCompareTo.is;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.stream.crypto.SharedSecret;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.fx.Denomination;
import org.interledger.stream.StreamException;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.StreamConnectionGenerator;
import org.interledger.stream.receiver.StreamReceiver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * <p>An implementation of {@link StreamReceiver} that sends valid STREAM frames that are _always_ empty, for testing
 * purposes only.</p>
 */
public class AlwaysEmptyStreamReceiver implements StreamReceiver {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ServerSecretSupplier serverSecretSupplier;
  private final StreamConnectionGenerator streamConnectionGenerator;
  private final StreamEncryptionService streamEncryptionService;
  private final CodecContext streamCodecContext;

  public AlwaysEmptyStreamReceiver(
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

    final StreamPacket streamPacket;
    try {
      // Try to parse the STREAM data from the payload.
      final byte[] streamPacketBytes = streamEncryptionService.decrypt(streamSharedSecret, preparePacket.getData());
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

    final Builder<StreamFrame> responseFrames = ImmutableList.builder();

    // Generate fulfillment using the shared secret that was pre-negotiated with the sender.
    final InterledgerFulfillment fulfillment = StreamUtils
        .generatedFulfillableFulfillment(streamSharedSecret, preparePacket.getData());

    final boolean isFulfillable = fulfillment.getCondition().equals(preparePacket.getExecutionCondition());

    // Return Fulfill or Reject Packet
    if (isFulfillable && is(preparePacket.getAmount()).greaterThanEqualTo(streamPacket.prepareAmount())) {
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
      } else if (is(preparePacket.getAmount()).lessThan(streamPacket.prepareAmount())) {
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
