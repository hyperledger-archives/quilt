package org.interledger.stream.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.stream.StreamConnectionDetails;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Unit tests for {@link StatelessStreamReceiver}.
 */
public class StatelessStreamReceiverTest {

  @Mock
  private StreamConnectionGenerator streamConnectionGeneratorMock;

  @Mock
  private StreamEncryptionService streamEncryptionServiceMock;

  private ServerSecretSupplier serverSecretSupplier;

  private CodecContext streamCodecContext;

  private StreamConnectionGenerator streamConnectionGenerator;

  private StreamEncryptionService streamEncryptionService;

  private StreamReceiver streamReceiver;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.streamConnectionGenerator = new SpspStreamConnectionGenerator();
    this.streamEncryptionService = new JavaxStreamEncryptionService();
    this.streamCodecContext = StreamCodecContextFactory.oer();

    this.serverSecretSupplier = () -> new byte[32];
    this.streamReceiver = new StatelessStreamReceiver(
        serverSecretSupplier, streamConnectionGeneratorMock, streamEncryptionServiceMock, StreamCodecContextFactory.oer()
    );
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullServerSecret() {
    try {
      new StatelessStreamReceiver(null, new SpspStreamConnectionGenerator(), streamEncryptionServiceMock,
          StreamCodecContextFactory.oer());
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("serverSecretSupplier must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullConnectionGenerator() {
    try {
      new StatelessStreamReceiver(serverSecretSupplier, null, streamEncryptionServiceMock,
          StreamCodecContextFactory.oer());
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("connectionGenerator must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullStreamEncryptionService() {
    try {
      new StatelessStreamReceiver(serverSecretSupplier, streamConnectionGeneratorMock, null, StreamCodecContextFactory.oer());
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("streamEncryptionService must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void generateConnectionDetailsWithNullCodecContextFactory() {
    try {
      new StatelessStreamReceiver(
          serverSecretSupplier, streamConnectionGeneratorMock, streamEncryptionServiceMock, null
      );
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("streamCodecContext must not be null"));
      throw e;
    }
  }

  @Test
  public void setupStream() {
    InterledgerAddress receiverAddress = InterledgerAddress.of("example.receiver");
    streamReceiver.setupStream(receiverAddress);

    Mockito.verify(streamConnectionGeneratorMock).generateConnectionDetails(Mockito.any(), eq(receiverAddress));
  }

  @Test
  public void testFulfillsValidPacket() throws IOException {
    streamReceiver = new StatelessStreamReceiver(
        serverSecretSupplier, streamConnectionGenerator, streamEncryptionService, StreamCodecContextFactory.oer()
    );
    final InterledgerAddress clientAddress = InterledgerAddress.of("example.destination");

    final StreamConnectionDetails connectionDetails = streamConnectionGenerator
        .generateConnectionDetails(serverSecretSupplier, clientAddress);

    final StreamPacket testStreamPacket = StreamPacket.builder()
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        .prepareAmount(UnsignedLong.ZERO)
        .sequence(UnsignedLong.ONE)
        .addFrames(StreamMoneyFrame.builder()
            .streamId(UnsignedLong.ONE)
            .shares(UnsignedLong.ONE)
            .build())
        .build();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    streamCodecContext.write(testStreamPacket, baos);
    final byte[] encryptedStreamPacketBytes = streamEncryptionService
        .encrypt(serverSecretSupplier.get(), baos.toByteArray());

    final InterledgerCondition executionCondition = StreamUtils
        .generatedFulfillableFulfillment(serverSecretSupplier.get(), encryptedStreamPacketBytes).getCondition();

    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .destination(connectionDetails.destinationAddress())
        .amount(BigInteger.valueOf(100L))
        .expiresAt(Instant.EPOCH)
        .data(encryptedStreamPacketBytes)
        .executionCondition(executionCondition)
        .build();

    this.streamReceiver.receiveMoney(preparePacket, clientAddress)
        .handle((fulfillPacket -> {
          assertThat(fulfillPacket.getFulfillment().getCondition(), is(executionCondition));
        }), rejectPacket -> {
          fail("should have fulfilled");
        });
  }
}
