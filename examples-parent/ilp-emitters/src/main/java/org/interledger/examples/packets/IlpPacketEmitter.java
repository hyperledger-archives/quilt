package org.interledger.examples.packets;

import static org.interledger.core.InterledgerConstants.ALL_ZEROS_FULFILLMENT;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerPreparePacketBuilder;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.PingLoopbackLink;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.crypto.AesGcmStreamSharedSecretCrypto;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.crypto.StreamSharedSecretCrypto;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamDataFrame;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;

/**
 * Helper class to assemble binary packets for manual testing purposes. All packets are written to files in the `/tmp`
 * directory.
 */
public class IlpPacketEmitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(IlpPacketEmitter.class);

  private static final InterledgerAddress PING_DESTINATION_ADDRESS = InterledgerAddress.of("test.connie");

  private static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress
    .of("test.connie.bob.QeJvQtFp7eRiNhnoAg9PkusR");
  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.connie");

  private static final StreamSharedSecret SHARED_SECRET = StreamSharedSecret
    .of(Base64.getDecoder().decode("nHYRcu5KM5pyw8XehssZtvhEgCgkKP4Do5kJUpk84G4"));

  private static final StreamSharedSecretCrypto SHARED_SECRET_CRYPTO = new AesGcmStreamSharedSecretCrypto();

  /**
   * Main entry point.
   *
   * @param args
   */
  public static void main(String[] args) throws IOException {
    emitPacketsWithNoData();
    emitPacketsWithStreamPayloads();
    emitUnidrectionalPingPacket();
  }

  private static void emitPacketsWithNoData() {
    final InterledgerPreparePacket preparePacket = preparePacketBuilder().build();
    emitPacketToFile("/tmp/testPreparePacket.bin", preparePacket);

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F02_UNREACHABLE)
      .triggeredBy(OPERATOR_ADDRESS)
      .message("")
      .build();
    emitPacketToFile("/tmp/testRejectPacket.bin", rejectPacket);

    final InterledgerFulfillPacket fulfillPacket = InterledgerFulfillPacket.builder()
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .build();
    emitPacketToFile("/tmp/testFulfillPacket.bin", fulfillPacket);
  }

  private static void emitUnidrectionalPingPacket() {

    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
      .destination(PING_DESTINATION_ADDRESS)
      .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
      .executionCondition(PingLoopbackLink.PING_PROTOCOL_CONDITION)
      .amount(UnsignedLong.ONE)
      .build();

    emitPacketToFile("/tmp/testUnidirectionalPingPacket.bin", preparePacket);
  }

  private static void emitPacketsWithStreamPayloads() throws IOException {
    final InterledgerPreparePacket preparePacketWithStreamFrames = preparePacketWithStreamFrames().build();
    emitPacketToFile("/tmp/testPreparePacketWithStreamFrames.bin", preparePacketWithStreamFrames);

    final InterledgerRejectPacket rejectPacketWithStreamFrames = rejectPacketWithStreamFrames();
    emitPacketToFile("/tmp/testRejectPacket.bin", rejectPacketWithStreamFrames);

    final InterledgerFulfillPacket fulfillPacket = fulfillPacketWithStreamFrames();
    emitPacketToFile("/tmp/testFulfillPacket.bin", fulfillPacket);
  }

  private static InterledgerFulfillPacket fulfillPacketWithStreamFrames() throws IOException {
    final StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .prepareAmount(UnsignedLong.ZERO)
      .sequence(UnsignedLong.ONE)
      .frames(Lists.newArrayList(
        StreamMoneyFrame.builder()
          // This aggregator supports only a simple stream-id, which is one.
          .streamId(UnsignedLong.ONE)
          .shares(UnsignedLong.ONE)
          .build(),
        StreamDataFrame.builder()
          .offset(UnsignedLong.ZERO)
          .streamId(UnsignedLong.ONE)
          .data(new byte[8])
          .build()
      ))
      .build();

    // Create the ILP Prepare packet
    final byte[] streamPacketData = toEncrypted(streamPacket);
    return InterledgerFulfillPacket.builder()
      .fulfillment(InterledgerFulfillment.of(new byte[32]))
      .data(streamPacketData)
      .typedData(streamPacket)
      .build();
  }

  private static InterledgerPreparePacketBuilder preparePacketBuilder() {
    return InterledgerPreparePacket.builder()
      .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
      .destination(DESTINATION_ADDRESS)
      .amount(UnsignedLong.ONE)
      .executionCondition(ALL_ZEROS_FULFILLMENT.getCondition());
  }

  private static InterledgerPreparePacketBuilder preparePacketWithStreamFrames() throws IOException {
    final StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .prepareAmount(UnsignedLong.ZERO)
      .sequence(UnsignedLong.ONE)
      .frames(Lists.newArrayList(
        StreamMoneyFrame.builder()
          // This aggregator supports only a simple stream-id, which is one.
          .streamId(UnsignedLong.ONE)
          .shares(UnsignedLong.ONE)
          .build(),
        ConnectionNewAddressFrame.builder()
          .sourceAddress(InterledgerAddress.of("example.connie.bob"))
          .build(),
        ConnectionAssetDetailsFrame.builder()
          .sourceDenomination(org.interledger.stream.Denomination.builder()
            .assetCode("USD")
            .assetScale((short) 2)
            .build())
          .build()
      ))
      .build();

    // Create the ILP Prepare packet
    final byte[] streamPacketData = toEncrypted(streamPacket);
    final InterledgerCondition executionCondition = StreamPacketUtils
      .generateFulfillableFulfillment(SHARED_SECRET, streamPacketData)
      .getCondition();
    return preparePacketBuilder()
      .executionCondition(executionCondition)
      .data(streamPacketData)
      .typedData(streamPacket);
  }

  private static InterledgerRejectPacket rejectPacketWithStreamFrames() throws IOException {
    final StreamPacket streamPacket = StreamPacket.builder()
      .interledgerPacketType(InterledgerPacketType.PREPARE)
      .prepareAmount(UnsignedLong.ZERO)
      .sequence(UnsignedLong.ONE)
      .frames(Lists.newArrayList(
        ConnectionCloseFrame.builder().errorCode(ErrorCodes.NoError).build()
      ))
      .build();

    // Create the ILP Prepare packet
    final byte[] streamPacketData = toEncrypted(streamPacket);

    return InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F02_UNREACHABLE)
      .triggeredBy(OPERATOR_ADDRESS)
      .message("test packet rejection")
      .data(streamPacketData)
      .build();
  }

  private static void emitPacketToFile(final String fileName, final InterledgerPacket interledgerPacket) {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      InterledgerCodecContextFactory.oer().write(interledgerPacket, os);
      FileUtils.writeByteArrayToFile(new File(fileName), os.toByteArray());

      final String transferBase64 = BaseEncoding.base16().encode(os.toByteArray());
      LOGGER.info("{} Hex Bytes: {}", fileName, transferBase64);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  private static byte[] toEncrypted(final StreamPacket streamPacket)
    throws IOException {

    Objects.requireNonNull(streamPacket);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamCodecContextFactory.oer().write(streamPacket, baos);
    final byte[] streamPacketBytes = baos.toByteArray();
    return SHARED_SECRET_CRYPTO.encrypt(SHARED_SECRET, streamPacketBytes);
  }
}
